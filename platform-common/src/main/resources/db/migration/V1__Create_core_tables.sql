-- ================================================================
-- MModelX 核心表结构创建
-- 版本: V1
-- 描述: 创建用户、特征、样本、训练、部署相关核心表
-- 作者: MModelX Team
-- 日期: 2026-05-20
-- ================================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'ENGINEER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- 用户表注释
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.role IS '角色: ENGINEER, MLOPS, ADMIN';
COMMENT ON COLUMN users.status IS '状态: ACTIVE, INACTIVE, LOCKED';

-- 2. 特征视图表
CREATE TABLE IF NOT EXISTS feature_views (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    entity VARCHAR(50) NOT NULL,
    ttl VARCHAR(20) NOT NULL,
    description TEXT,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 特征视图表注释
COMMENT ON TABLE feature_views IS '特征视图表，管理相关特征的分组';
COMMENT ON COLUMN feature_views.entity IS '实体键，如 user_id, item_id';
COMMENT ON COLUMN feature_views.ttl IS '特征过期时间，如 7d, 30d';

-- 3. 特征定义表
CREATE TABLE IF NOT EXISTS features (
    id BIGSERIAL PRIMARY KEY,
    feature_view_id BIGINT NOT NULL REFERENCES feature_views(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    dtype VARCHAR(20) NOT NULL,
    description TEXT,
    source_type VARCHAR(20) NOT NULL,
    source_path TEXT,
    config JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(feature_view_id, name)
);

-- 特征定义表注释
COMMENT ON TABLE features IS '特征定义表，存储具体的特征配置';
COMMENT ON COLUMN features.dtype IS '数据类型: INT64, FLOAT64, STRING, BOOLEAN';
COMMENT ON COLUMN features.source_type IS '源数据类型: PARQUET, DATABASE, API';
COMMENT ON COLUMN features.status IS '状态: DRAFT, COMPUTING, ACTIVE, FAILED';

-- 4. 数据集表
CREATE TABLE IF NOT EXISTS datasets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    description TEXT,
    feature_view_ids BIGINT[] NOT NULL,
    label_column VARCHAR(100) NOT NULL,
    time_column VARCHAR(100),
    sample_count BIGINT,
    positive_count BIGINT,
    negative_count BIGINT,
    split_ratio JSONB,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    point_in_time_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    quality_score VARCHAR(10),
    status VARCHAR(20) NOT NULL DEFAULT 'BUILDING',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, version)
);

-- 数据集表注释
COMMENT ON TABLE datasets IS '数据集表，管理训练样本集';
COMMENT ON COLUMN datasets.split_ratio IS '数据集划分比例，如 [0.7, 0.15, 0.15]';
COMMENT ON COLUMN datasets.point_in_time_enabled IS '是否启用Point-in-time正确性';
COMMENT ON COLUMN datasets.quality_score IS '质量评级: A+, A, B+, B, C';
COMMENT ON COLUMN datasets.status IS '状态: BUILDING, COMPLETED, FAILED';

-- 5. 实验表
CREATE TABLE IF NOT EXISTS experiments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    dataset_id BIGINT REFERENCES datasets(id),
    model_type VARCHAR(20) NOT NULL,
    hyperparameters JSONB,
    metric_name VARCHAR(50) NOT NULL DEFAULT 'auc',
    metric_value DECIMAL(10, 4),
    optimization_direction VARCHAR(10) NOT NULL DEFAULT 'MAXIMIZE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    mlflow_run_id VARCHAR(255),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 实验表注释
COMMENT ON TABLE experiments IS '实验表，管理机器学习实验';
COMMENT ON COLUMN experiments.model_type IS '模型类型: LIGHTGBM, XGBOOST, TENSORFLOW';
COMMENT ON COLUMN experiments.metric_name IS '优化指标名称: auc, f1, accuracy';
COMMENT ON COLUMN experiments.optimization_direction IS '优化方向: MAXIMIZE, MINIMIZE';
COMMENT ON COLUMN experiments.status IS '状态: PENDING, RUNNING, COMPLETED, FAILED, STOPPED';

-- 6. 模型表
CREATE TABLE IF NOT EXISTS models (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    experiment_id BIGINT REFERENCES experiments(id),
    file_path TEXT NOT NULL,
    file_size BIGINT,
    framework VARCHAR(20) NOT NULL,
    model_type VARCHAR(20) NOT NULL,
    hyperparameters JSONB,
    metrics JSONB,
    mlflow_run_id VARCHAR(255),
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, version)
);

-- 模型表注释
COMMENT ON TABLE models IS '模型表，管理训练好的模型';
COMMENT ON COLUMN models.framework IS '框架: LIGHTGBM, XGBOOST, TENSORFLOW, PYTORCH';
COMMENT ON COLUMN models.file_path IS '模型文件在MinIO中的存储路径';

-- 7. 模型部署表
CREATE TABLE IF NOT EXISTS deployments (
    id BIGSERIAL PRIMARY KEY,
    model_id BIGINT NOT NULL REFERENCES models(id),
    environment VARCHAR(20) NOT NULL,
    traffic_percentage INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'STOPPED',
    endpoint_url VARCHAR(255),
    current_qps INTEGER NOT NULL DEFAULT 0,
    avg_latency_ms INTEGER NOT NULL DEFAULT 0,
    error_rate DECIMAL(5, 4) NOT NULL DEFAULT 0,
    deployed_at TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 模型部署表注释
COMMENT ON TABLE deployments IS '模型部署表，管理模型的生产部署';
COMMENT ON COLUMN deployments.environment IS '环境: DEVELOPMENT, STAGING, PRODUCTION';
COMMENT ON COLUMN deployments.traffic_percentage IS '流量分配百分比，用于A/B测试';
COMMENT ON COLUMN deployments.status IS '状态: STOPPED, DEPLOYING, RUNNING, FAILED';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_features_view_id ON features(feature_view_id);
CREATE INDEX IF NOT EXISTS idx_features_status ON features(status);
CREATE INDEX IF NOT EXISTS idx_datasets_name_version ON datasets(name, version);
CREATE INDEX IF NOT EXISTS idx_datasets_status ON datasets(status);
CREATE INDEX IF NOT EXISTS idx_experiments_dataset_id ON experiments(dataset_id);
CREATE INDEX IF NOT EXISTS idx_experiments_status ON experiments(status);
CREATE INDEX IF NOT EXISTS idx_models_name_version ON models(name, version);
CREATE INDEX IF NOT EXISTS idx_deployments_model_id ON deployments(model_id);
CREATE INDEX IF NOT EXISTS idx_deployments_environment ON deployments(environment);