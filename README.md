# MModelX

MModelX 是面向 100GB 数据规模的端到端机器学习平台，覆盖特征工程、样本工程、模型训练、推理服务与 K8s 部署。

## 当前实现基线

- Java 8
- Spring Boot 2.7.18
- Maven 多模块单体架构（主入口 `platform-api`）

## 快速入口

- Java 版说明与启动指南：[`README_JAVA.md`](README_JAVA.md)
- 当前架构文档：[`docs/architecture.md`](docs/architecture.md)
- 技术基线决议：[`docs/baseline-decision.md`](docs/baseline-decision.md)

## 启动

```bash
docker-compose up -d
mvn clean install
cd platform-api && mvn spring-boot:run
curl http://localhost:8080/api/health
```
