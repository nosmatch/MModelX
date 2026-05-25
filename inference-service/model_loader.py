import json
import logging
import os
from typing import Any, Dict, List

import numpy as np

logger = logging.getLogger(__name__)


def load_model_config(config_path: str = "/etc/model-config/model.json") -> Dict[str, Any]:
    """从 ConfigMap 挂载的文件中读取模型配置"""
    logger.info("Loading model config from: %s", config_path)
    with open(config_path, "r", encoding="utf-8") as f:
        config = json.load(f)
    logger.info("Model config loaded: model=%s, framework=%s", config.get("modelName"), config.get("framework"))
    return config


def download_from_minio(config: Dict[str, Any], local_path: str = "/tmp/model") -> str:
    """从 MinIO 下载模型文件到本地"""
    try:
        from minio import Minio
    except ImportError:
        logger.warning("minio package not installed, skipping download")
        return local_path

    endpoint = config.get("minioEndpoint", os.getenv("MINIO_ENDPOINT", "http://localhost:9002"))
    access_key = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
    secret_key = os.getenv("MINIO_SECRET_KEY", "minioadmin")
    bucket = config.get("minioBucket", "mmodelx")
    file_path = config.get("filePath", "")

    # 去掉 http:// 前缀用于 MinIO 客户端
    endpoint_clean = endpoint.replace("http://", "").replace("https://", "")
    secure = endpoint.startswith("https")

    client = Minio(endpoint_clean, access_key=access_key, secret_key=secret_key, secure=secure)

    os.makedirs(local_path, exist_ok=True)
    local_file = os.path.join(local_path, os.path.basename(file_path))

    logger.info("Downloading model from MinIO: bucket=%s, path=%s -> %s", bucket, file_path, local_file)
    client.fget_object(bucket, file_path, local_file)
    logger.info("Model downloaded successfully")
    return local_file


def load_model(config: Dict[str, Any]) -> Any:
    """根据框架类型加载模型"""
    framework = config.get("framework", "").lower()
    model_file = config.get("localModelPath")

    if not model_file or not os.path.exists(model_file):
        # 尝试从 MinIO 下载
        model_file = download_from_minio(config)

    logger.info("Loading model with framework: %s, file: %s", framework, model_file)

    if framework == "xgboost":
        import xgboost as xgb
        model = xgb.Booster()
        model.load_model(model_file)
        logger.info("XGBoost model loaded")
        return model

    elif framework == "lightgbm":
        import lightgbm as lgb
        model = lgb.Booster(model_file=model_file)
        logger.info("LightGBM model loaded")
        return model

    else:
        raise ValueError(f"Unsupported framework: {framework}")


def predict(model: Any, features: Dict[str, Any], config: Dict[str, Any]) -> List[float]:
    """使用加载的模型进行预测"""
    framework = config.get("framework", "").lower()

    # 将特征字典转换为 numpy 数组
    # 假设特征已经按正确顺序排列，或者可以通过特征名对齐
    feature_values = []
    for key, value in sorted(features.items()):
        if isinstance(value, (int, float)):
            feature_values.append(float(value))
        else:
            feature_values.append(0.0)

    x = np.array([feature_values])

    if framework == "xgboost":
        import xgboost as xgb
        dmatrix = xgb.DMatrix(x)
        preds = model.predict(dmatrix)
        return preds.tolist()

    elif framework == "lightgbm":
        preds = model.predict(x)
        return preds.tolist()

    else:
        raise ValueError(f"Unsupported framework for prediction: {framework}")


def unload_model(model: Any) -> None:
    """释放模型资源"""
    logger.info("Unloading model")
    del model
    import gc
    gc.collect()
