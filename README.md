# MModelX - 机器学习平台

面向 100GB 数据规模的端到端机器学习平台，覆盖特征工程、样本工程、模型训练到推理服务的全流程。

## 核心特性

- **特征工程**：声明式特征定义，自动计算与注册，支持离线/在线双写
- **样本工程**：Point-in-time correct join，数据集版本化管理
- **模型训练**：实验追踪、超参搜索、模型注册一体化
- **推理服务**：模型热加载、A/B 测试、低延迟预测

## 技术栈

| 组件 | 用途 |
|------|------|
| Prefect | DAG 编排调度 |
| Polars | 数据处理 |
| Feast | 特征存储 |
| MLflow | 实验追踪与模型注册 |
| DVC | 数据版本管理 |
| FastAPI | 推理服务 |
| Docker | 容器化部署 |

## 快速开始

```bash
# 启动基础设施
docker-compose up -d

# 安装依赖
pip install -e .

# 运行特征计算
python -m platform.features compute --config features/definitions/user_features.yaml

# 启动训练
python -m platform.training train --config training/configs/ctr_model.yaml

# 启动推理服务
uvicorn platform.serving.api:app --host 0.0.0.0 --port 8000
```

## 文档

- [技术架构](docs/architecture.md)
- [开发计划](docs/development-plan.md)

## 项目状态

🚧 开发中 - 当前处于第一阶段（基础设施搭建）

## License

MIT
