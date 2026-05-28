#!/usr/bin/env python3
"""
MModelX Model Training Script
Supports LightGBM and XGBoost training from JSON sample data.

Usage (local mode):
    python train_model.py \
        --train train.json \
        --val val.json \
        --test test.json \
        --model-type lightgbm \
        --label-col is_churned \
        --params '{"num_leaves":31}' \
        --output model.txt \
        --metrics-output metrics.json

Usage (K8s mode - env vars):
    MINIO_ENDPOINT=http://minio:9000
    MINIO_ACCESS_KEY=minioadmin
    MINIO_SECRET_KEY=minioadmin
    DATASET_NAME=churn_prediction_sample_v1
    DATASET_VERSION=v20260527093751
    MODEL_TYPE=lightgbm
    EXPERIMENT_NAME=churn_prediction_v1
    PARAMS_JSON='{"num_leaves":31}'
    LABEL_COL=is_churned
    OUTPUT_PATH=models/churn_prediction_v1/model.txt
    METRICS_OUTPUT_PATH=models/churn_prediction_v1/metrics.json
"""

import argparse
import json
import sys
import os
import warnings
import tempfile

warnings.filterwarnings("ignore")


def get_minio_client():
    """Create MinIO client from environment variables."""
    try:
        from minio import Minio
    except ImportError:
        print("[Train] minio SDK not installed, trying urllib...")
        return None

    endpoint = os.environ.get("MINIO_ENDPOINT", "http://localhost:9000")
    access_key = os.environ.get("MINIO_ACCESS_KEY", "minioadmin")
    secret_key = os.environ.get("MINIO_SECRET_KEY", "minioadmin")

    # Remove protocol prefix for MinIO client
    endpoint = endpoint.replace("http://", "").replace("https://", "")
    secure = os.environ.get("MINIO_SECURE", "false").lower() == "true"

    return Minio(endpoint, access_key=access_key, secret_key=secret_key, secure=secure)


def download_from_minio(client, bucket_object, local_path):
    """Download a file from MinIO. bucket_object format: bucket/object"""
    if client is None:
        raise RuntimeError("MinIO client not available")
    parts = bucket_object.split("/", 1)
    if len(parts) != 2:
        raise ValueError(f"Invalid MinIO path: {bucket_object}")
    bucket, obj_name = parts
    client.fget_object(bucket, obj_name, local_path)
    print(f"[Train] Downloaded {bucket_object} -> {local_path}")


def upload_to_minio(client, local_path, bucket_object):
    """Upload a file to MinIO. bucket_object format: bucket/object"""
    if client is None:
        raise RuntimeError("MinIO client not available")
    parts = bucket_object.split("/", 1)
    if len(parts) != 2:
        raise ValueError(f"Invalid MinIO path: {bucket_object}")
    bucket, obj_name = parts
    client.fput_object(bucket, obj_name, local_path)
    print(f"[Train] Uploaded {local_path} -> {bucket_object}")


def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def prepare_data(data_list, label_col, exclude_cols=None):
    """Convert list of dicts to features (X) and labels (y)."""
    import pandas as pd

    if exclude_cols is None:
        exclude_cols = {"entity_id", "timestamp", label_col}

    df = pd.DataFrame(data_list)

    # Extract label
    y = df[label_col].astype(int).values

    # Extract features (exclude metadata and label)
    feature_cols = [c for c in df.columns if c not in exclude_cols]
    X = df[feature_cols].copy()

    # Handle categorical columns (device_type etc.)
    categorical_cols = X.select_dtypes(include=["object"]).columns.tolist()
    for col in categorical_cols:
        X[col] = X[col].astype("category")

    return X, y, feature_cols, categorical_cols


def train_lightgbm(X_train, y_train, X_val, y_val, params, categorical_cols):
    import lightgbm as lgb

    # Build dataset
    train_data = lgb.Dataset(X_train, label=y_train, categorical_feature=categorical_cols)
    val_data = lgb.Dataset(X_val, label=y_val, categorical_feature=categorical_cols, reference=train_data)

    # Default params merge
    default_params = {
        "objective": "binary",
        "metric": "auc",
        "boosting_type": "gbdt",
        "verbose": -1,
        "num_leaves": 31,
        "learning_rate": 0.05,
        "feature_fraction": 0.8,
        "bagging_fraction": 0.8,
        "bagging_freq": 5,
    }
    default_params.update(params)

    # Remove non-LGBM params
    lgb_params = {k: v for k, v in default_params.items() if k not in {"num_rounds", "early_stopping_rounds"}}
    num_rounds = int(params.get("num_rounds", 100))
    early_stopping = int(params.get("early_stopping_rounds", 10))

    model = lgb.train(
        lgb_params,
        train_data,
        num_boost_round=num_rounds,
        valid_sets=[train_data, val_data],
        valid_names=["train", "val"],
        callbacks=[lgb.early_stopping(early_stopping, verbose=False)],
    )
    return model


def train_xgboost(X_train, y_train, X_val, y_val, params):
    import xgboost as xgb

    # XGBoost doesn't support categorical natively in all versions; encode them
    X_train_enc = X_train.copy()
    X_val_enc = X_val.copy()

    for col in X_train_enc.select_dtypes(include=["category"]).columns:
        X_train_enc[col] = X_train_enc[col].cat.codes
        X_val_enc[col] = X_val_enc[col].cat.codes

    dtrain = xgb.DMatrix(X_train_enc, label=y_train)
    dval = xgb.DMatrix(X_val_enc, label=y_val)

    default_params = {
        "objective": "binary:logistic",
        "eval_metric": "auc",
        "max_depth": 6,
        "learning_rate": 0.05,
        "subsample": 0.8,
        "colsample_bytree": 0.8,
        "seed": 42,
    }
    default_params.update(params)

    num_rounds = int(params.get("num_rounds", 100))
    early_stopping = int(params.get("early_stopping_rounds", 10))

    evals = [(dtrain, "train"), (dval, "val")]
    model = xgb.train(
        default_params,
        dtrain,
        num_boost_round=num_rounds,
        evals=evals,
        early_stopping_rounds=early_stopping,
        verbose_eval=False,
    )
    return model


def evaluate_model(model, X_test, y_test, model_type):
    import numpy as np
    from sklearn.metrics import (
        roc_auc_score, log_loss, accuracy_score,
        precision_score, recall_score, f1_score
    )

    if model_type == "lightgbm":
        y_prob = model.predict(X_test, num_iteration=model.best_iteration)
    else:
        # XGBoost
        import xgboost as xgb
        # Re-encode categorical for XGBoost prediction
        X_test_enc = X_test.copy()
        for col in X_test_enc.select_dtypes(include=["category"]).columns:
            X_test_enc[col] = X_test_enc[col].cat.codes
        dtest = xgb.DMatrix(X_test_enc)
        y_prob = model.predict(dtest)

    y_pred = (np.array(y_prob) >= 0.5).astype(int)

    metrics = {
        "auc": float(roc_auc_score(y_test, y_prob)),
        "logloss": float(log_loss(y_test, y_prob)),
        "accuracy": float(accuracy_score(y_test, y_pred)),
        "precision": float(precision_score(y_test, y_pred, zero_division=0)),
        "recall": float(recall_score(y_test, y_pred, zero_division=0)),
        "f1": float(f1_score(y_test, y_pred, zero_division=0)),
    }
    return metrics


def save_model(model, model_type, output_path):
    if model_type == "lightgbm":
        model.save_model(output_path)
    else:
        model.save_model(output_path)


def run_training(args, params, minio_client, temp_dir):
    """Core training logic shared by both local and K8s modes."""
    # Load data
    train_data = load_json(args.train)
    val_data = load_json(args.val)
    test_data = load_json(args.test)

    print(f"[Train] Loaded {len(train_data)} train, {len(val_data)} val, {len(test_data)} test samples")

    # Prepare features
    X_train, y_train, feature_cols, cat_cols = prepare_data(train_data, args.label_col)
    X_val, y_val, _, _ = prepare_data(val_data, args.label_col)
    X_test, y_test, _, _ = prepare_data(test_data, args.label_col)

    print(f"[Train] Features: {len(feature_cols)}, Categorical: {cat_cols}")

    # Train
    if args.model_type == "lightgbm":
        model = train_lightgbm(X_train, y_train, X_val, y_val, params, cat_cols)
    else:
        model = train_xgboost(X_train, y_train, X_val, y_val, params)

    # Evaluate
    metrics = evaluate_model(model, X_test, y_test, args.model_type)
    print(f"[Train] Metrics: {json.dumps(metrics, indent=2)}")

    # Save
    os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
    save_model(model, args.model_type, args.output)
    print(f"[Train] Model saved to {args.output}")

    with open(args.metrics_output, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)
    print(f"[Train] Metrics saved to {args.metrics_output}")

    # Upload to MinIO if in K8s mode
    if minio_client:
        output_bucket_obj = os.environ.get("OUTPUT_MINIO_PATH")
        metrics_bucket_obj = os.environ.get("METRICS_MINIO_PATH")
        if output_bucket_obj:
            upload_to_minio(minio_client, args.output, output_bucket_obj)
        if metrics_bucket_obj:
            upload_to_minio(minio_client, args.metrics_output, metrics_bucket_obj)

    return metrics


def main():
    parser = argparse.ArgumentParser(description="MModelX Model Training")
    parser.add_argument("--train", help="Path to train JSON")
    parser.add_argument("--val", help="Path to val JSON")
    parser.add_argument("--test", help="Path to test JSON")
    parser.add_argument("--model-type", choices=["lightgbm", "xgboost"], help="Model type")
    parser.add_argument("--label-col", help="Label column name")
    parser.add_argument("--params", default="{}", help="Model params JSON string")
    parser.add_argument("--output", help="Output model file path")
    parser.add_argument("--metrics-output", help="Output metrics JSON path")
    args = parser.parse_args()

    # Detect mode: if train path provided via CLI, use local mode; otherwise use K8s env mode
    minio_client = None
    temp_dir = None

    if args.train:
        # Local mode - use CLI args directly
        params = json.loads(args.params)
        return run_training(args, params, None, None)

    # K8s mode - read from environment variables
    print("[Train] Running in K8s mode (reading from environment variables)")

    minio_client = get_minio_client()
    dataset_name = os.environ.get("DATASET_NAME")
    dataset_version = os.environ.get("DATASET_VERSION")
    model_type = os.environ.get("MODEL_TYPE", "lightgbm")
    label_col = os.environ.get("LABEL_COL", "is_churned")
    params = json.loads(os.environ.get("PARAMS_JSON", "{}"))

    if not dataset_name or not dataset_version:
        print("[Train] ERROR: DATASET_NAME and DATASET_VERSION env vars are required")
        return 1

    # Create temp directory for downloaded data
    temp_dir = tempfile.mkdtemp(prefix="mmodelx_train_")

    try:
        # Download data from MinIO
        train_path = os.path.join(temp_dir, "train.json")
        val_path = os.path.join(temp_dir, "val.json")
        test_path = os.path.join(temp_dir, "test.json")

        # Check if actual MinIO paths are provided via env vars
        env_train_path = os.environ.get("TRAIN_PATH")
        env_val_path = os.environ.get("VAL_PATH")
        env_test_path = os.environ.get("TEST_PATH")

        if env_train_path and env_val_path and env_test_path:
            # Use actual MinIO paths from dataset version record
            download_from_minio(minio_client, env_train_path, train_path)
            download_from_minio(minio_client, env_val_path, val_path)
            download_from_minio(minio_client, env_test_path, test_path)
        else:
            # Fallback: construct path from dataset name + version
            version_path = dataset_version[1:] if dataset_version.startswith("v") else dataset_version
            sample_prefix = f"samples/{dataset_name}/{version_path}"
            download_from_minio(minio_client, f"{sample_prefix}/train.json", train_path)
            download_from_minio(minio_client, f"{sample_prefix}/val.json", val_path)
            download_from_minio(minio_client, f"{sample_prefix}/test.json", test_path)

        # Set up args for training
        args.train = train_path
        args.val = val_path
        args.test = test_path
        args.model_type = model_type
        args.label_col = label_col
        args.output = os.environ.get("OUTPUT_PATH", "/tmp/model.txt")
        args.metrics_output = os.environ.get("METRICS_OUTPUT_PATH", "/tmp/metrics.json")

        run_training(args, params, minio_client, temp_dir)

    finally:
        # Cleanup temp directory
        import shutil
        if temp_dir and os.path.exists(temp_dir):
            shutil.rmtree(temp_dir, ignore_errors=True)

    return 0


if __name__ == "__main__":
    sys.exit(main())
