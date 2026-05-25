import json
import logging
import os
import time
from contextlib import asynccontextmanager
from typing import Any, Dict, List, Optional

import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from model_loader import load_model, load_model_config, predict, unload_model

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger(__name__)

# 全局模型实例
model: Optional[Any] = None
model_config: Optional[Dict[str, Any]] = None


class PredictRequest(BaseModel):
    features: Dict[str, Any]
    include_details: bool = False


class PredictResponse(BaseModel):
    predictions: List[float]
    model_name: str
    model_version: str
    framework: str
    latency_ms: float


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_name: Optional[str] = None
    model_version: Optional[str] = None
    framework: Optional[str] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理：启动时加载模型"""
    global model, model_config
    logger.info("=" * 50)
    logger.info("Starting MModelX Inference Service")
    logger.info("=" * 50)

    try:
        # 1. 从 ConfigMap 读取模型配置
        config_path = "/etc/model-config/model.json"
        if os.path.exists(config_path):
            model_config = load_model_config(config_path)
        else:
            # 降级：从环境变量读取
            logger.warning("ConfigMap not found, using environment variables")
            model_config = {
                "modelName": os.getenv("MODEL_NAME", "unknown"),
                "modelVersion": os.getenv("MODEL_VERSION", "unknown"),
                "framework": os.getenv("MODEL_FRAMEWORK", "lightgbm"),
                "filePath": os.getenv("MODEL_PATH", ""),
                "minioEndpoint": os.getenv("MINIO_ENDPOINT", "http://localhost:9002"),
                "minioBucket": os.getenv("MINIO_BUCKET", "mmodelx"),
            }

        # 2. 下载并加载模型
        model = load_model(model_config)
        logger.info("Model loaded successfully: %s v%s (%s)",
                    model_config.get("modelName"),
                    model_config.get("modelVersion"),
                    model_config.get("framework"))

    except Exception as e:
        logger.error("Failed to load model: %s", e, exc_info=True)
        # 即使加载失败也继续启动，但健康检查会返回不健康

    yield

    # 清理
    if model is not None:
        unload_model(model)
        logger.info("Model unloaded")


app = FastAPI(
    title="MModelX Inference Service",
    description="Model inference API for MModelX platform",
    version="1.0.0",
    lifespan=lifespan
)


@app.post("/predict", response_model=PredictResponse)
async def do_predict(request: PredictRequest):
    """执行模型预测"""
    global model, model_config

    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    start_time = time.time()

    try:
        results = predict(model, request.features, model_config)
        latency_ms = (time.time() - start_time) * 1000

        # 确保返回列表
        if isinstance(results, (int, float)):
            results = [float(results)]
        elif isinstance(results, np.ndarray):
            results = results.tolist()

        logger.info("Prediction completed: latency=%.2fms", latency_ms)

        return PredictResponse(
            predictions=results,
            model_name=model_config.get("modelName", "unknown"),
            model_version=model_config.get("modelVersion", "unknown"),
            framework=model_config.get("framework", "unknown"),
            latency_ms=round(latency_ms, 2)
        )

    except Exception as e:
        logger.error("Prediction failed: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@app.get("/health", response_model=HealthResponse)
async def health():
    """健康检查"""
    global model, model_config
    loaded = model is not None
    return HealthResponse(
        status="healthy" if loaded else "unhealthy",
        model_loaded=loaded,
        model_name=model_config.get("modelName") if model_config else None,
        model_version=model_config.get("modelVersion") if model_config else None,
        framework=model_config.get("framework") if model_config else None
    )


@app.get("/")
async def root():
    """根路径"""
    return {
        "service": "MModelX Inference Service",
        "version": "1.0.0",
        "endpoints": ["/predict", "/health"]
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
