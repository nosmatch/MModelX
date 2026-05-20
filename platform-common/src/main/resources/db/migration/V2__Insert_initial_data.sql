-- ================================================================
-- MModelX 初始数据插入
-- 版本: V2
-- 描述: 插入默认管理员用户和示例配置数据
-- 作者: MModelX Team
-- 日期: 2026-05-20
-- ================================================================

-- 插入默认管理员用户
-- 密码: admin123 (BCrypt加密后的值，实际生产环境应该使用更强的密码)
INSERT INTO users (username, password, email, role, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@mmodelx.com', 'ADMIN', 'ACTIVE'),
('engineer', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'engineer@mmodelx.com', 'ENGINEER', 'ACTIVE'),
('mlops', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'mlops@mmodelx.com', 'MLOPS', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- 插入示例特征视图
INSERT INTO feature_views (name, entity, ttl, description, created_by) VALUES
('user_behavior', 'user_id', '7d', '用户行为特征视图，包含用户点击、浏览、购买等行为特征', 1),
('item_features', 'item_id', '30d', '商品特征视图，包含商品类别、价格、销量等特征', 1),
('context_features', 'context_id', '1d', '上下文特征视图，包含时间、设备、地理位置等特征', 1),
('user_profile', 'user_id', '30d', '用户画像特征视图，包含用户人口统计学特征', 1)
ON CONFLICT (name) DO NOTHING;

-- 插入示例特征定义
INSERT INTO features (feature_view_id, name, dtype, description, source_type, source_path, config, status, created_by) VALUES
-- 用户行为特征
(1, 'click_count_7d', 'INT64', '用户7日点击次数', 'PARQUET', 's3://features/user_behavior/',
 '{"aggregation": "count", "window": "7d"}', 'ACTIVE', 1),
(1, 'avg_order_amount_30d', 'FLOAT64', '用户30日平均订单金额', 'PARQUET', 's3://features/user_behavior/',
 '{"aggregation": "avg", "window": "30d"}', 'ACTIVE', 1),
(1, 'last_login_days', 'INT64', '距离上次登录天数', 'PARQUET', 's3://features/user_behavior/',
'{"aggregation": "diff", "window": "max"}', 'ACTIVE', 1),

-- 商品特征
(2, 'item_category', 'STRING', '商品类别', 'PARQUET', 's3://features/item_master/',
'{"type": "category"}', 'ACTIVE', 1),
(2, 'item_price', 'FLOAT64', '商品价格', 'PARQUET', 's3://features/item_master/',
'{"type": "price"}', 'ACTIVE', 1),
(2, 'item_sales_rank_30d', 'INT64', '商品30日销量排名', 'PARQUET', 's3://features/item_sales/',
'{"aggregation": "rank", "window": "30d"}', 'ACTIVE', 1),

-- 上下文特征
(3, 'hour_of_day', 'INT64', '一天中的小时(0-23)', 'PARQUET', 's3://features/context/',
'{"type": "hour"}', 'ACTIVE', 1),
(3, 'day_of_week', 'INT64', '一周中的天数(0-6)', 'PARQUET', 's3://features/context/',
'{"type": "day"}', 'ACTIVE', 1),
(3, 'is_weekend', 'BOOLEAN', '是否周末', 'PARQUET', 's3://features/context/',
'{"type": "weekend"}', 'ACTIVE', 1),

-- 用户画像特征
(4, 'user_age', 'INT64', '用户年龄', 'PARQUET', 's3://features/user_profile/',
'{"type": "age"}', 'ACTIVE', 1),
(4, 'user_gender', 'STRING', '用户性别', 'PARQUET', 's3://features/user_profile/',
'{"type": "gender"}', 'ACTIVE', 1),
(4, 'user_tier', 'STRING', '用户等级', 'PARQUET', 's3://features/user_profile/',
'{"type": "tier"}', 'ACTIVE', 1)
ON CONFLICT DO NOTHING;

-- 插入示例数据集
INSERT INTO datasets (name, version, description, feature_view_ids, label_column, time_column,
sample_count, positive_count, negative_count, split_ratio, quality_score, status, created_by) VALUES
('ctr_dataset', 'v1.0', 'CTR预测数据集', ARRAY[1, 3], 'is_click', 'event_time',
1200000, 240000, 960000, '[0.7, 0.15, 0.15]', 'A+', 'COMPLETED', 1),
('ranking_dataset', 'v1.0', '商品排名数据集', ARRAY[1, 2, 3], 'relevance_score', 'event_time',
500000, 150000, 350000, '[0.7, 0.15, 0.15]', 'A', 'COMPLETED', 1),
('regression_dataset', 'v1.0', '销量预测数据集', ARRAY[2, 4], 'sales_amount', 'event_time',
800000, 800000, 0, '[0.8, 0.1, 0.1]', 'B+', 'COMPLETED', 1)
ON CONFLICT (name, version) DO NOTHING;

-- 插入示例实验
INSERT INTO experiments (name, description, dataset_id, model_type, hyperparameters,
metric_name, metric_value, optimization_direction, status, mlflow_run_id, created_by) VALUES
('ctr_baseline_v1', 'CTR预测基线模型', 1, 'LIGHTGBM',
'{"num_leaves": 31, "learning_rate": 0.1, "max_depth": 7}', 'auc', 0.823, 'MAXIMIZE', 'COMPLETED', 'run_001', 1),
('ctr_optimized_v1', 'CTR预测优化模型', 1, 'LIGHTGBM',
'{"num_leaves": 63, "learning_rate": 0.05, "max_depth": 10}', 'auc', 0.852, 'MAXIMIZE', 'COMPLETED', 'run_002', 1),
('ranking_model_v1', '商品排名模型', 2, 'XGBOOST',
'{"max_depth": 6, "learning_rate": 0.1, "n_estimators": 100}', 'ndcg', 0.823, 'MAXIMIZE', 'COMPLETED', 'run_003', 1),
('regression_v1', '销量预测模型', 3, 'LIGHTGBM',
'{"num_leaves": 127, "learning_rate": 0.1, "objective": "regression"}', 'rmse', 12.5, 'MINIMIZE', 'COMPLETED', 'run_004', 1)
ON CONFLICT DO NOTHING;

-- 插入示例模型
INSERT INTO models (name, version, experiment_id, file_path, file_size, framework, model_type,
hyperparameters, metrics, mlflow_run_id, created_by) VALUES
('ctr_model', 'v1.0', 1, 's3://models/ctr_model_v1.pkl', 5242880, 'LIGHTGBM', 'LIGHTGBM',
'{"num_leaves": 31, "learning_rate": 0.1, "max_depth": 7}',
'{"auc": 0.823, "f1": 0.801, "logloss": 0.345}', 'run_001', 1),
('ctr_model', 'v1.1', 2, 's3://models/ctr_model_v1.1.pkl', 5242880, 'LIGHTGBM', 'LIGHTGBM',
'{"num_leaves": 63, "learning_rate": 0.05, "max_depth": 10}',
'{"auc": 0.852, "f1": 0.812, "logloss": 0.321}', 'run_002', 1),
('ranking_model', 'v1.0', 3, 's3://models/ranking_model_v1.pkl', 4194304, 'XGBOOST', 'XGBOOST',
'{"max_depth": 6, "learning_rate": 0.1, "n_estimators": 100}',
'{"ndcg": 0.823, "mrr": 0.801}', 'run_003', 1),
('sales_prediction', 'v1.0', 4, 's3://models/sales_model_v1.pkl', 6291456, 'LIGHTGBM', 'LIGHTGBM',
'{"num_leaves": 127, "learning_rate": 0.1, "objective": "regression"}',
'{"rmse": 12.5, "mae": 8.3, "r2": 0.85}', 'run_004', 1)
ON CONFLICT (name, version) DO NOTHING;

-- 插入示例部署记录
INSERT INTO deployments (model_id, environment, traffic_percentage, status, created_by) VALUES
(1, 'DEVELOPMENT', 100, 'RUNNING', 1),
(2, 'STAGING', 100, 'RUNNING', 1),
(3, 'PRODUCTION', 0, 'STOPPED', 1),
(4, 'PRODUCTION', 100, 'RUNNING', 1)
ON CONFLICT DO NOTHING;

-- 创建序列（用于自动生成ID）
CREATE SEQUENCE IF NOT EXISTS users_seq START 1;
CREATE SEQUENCE IF NOT EXISTS feature_views_seq START 1;
CREATE SEQUENCE IF NOT EXISTS features_seq START 1;
CREATE SEQUENCE IF NOT EXISTS datasets_seq START 1;
CREATE SEQUENCE IF NOT EXISTS experiments_seq START 1;
CREATE SEQUENCE IF NOT EXISTS models_seq START 1;
CREATE SEQUENCE IF NOT EXISTS deployments_seq START 1;

-- 添加表注释和约束
COMMENT ON TABLE deployments IS '模型部署表';
COMMENT ON COLUMN deployments.environment IS '环境: DEVELOPMENT, STAGING, PRODUCTION';
COMMENT ON COLUMN deployments.traffic_percentage IS '流量分配百分比，用于A/B测试';
COMMENT ON COLUMN deployments.status IS '状态: STOPPED, DEPLOYING, RUNNING, FAILED';

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为所有表添加更新时间触发器
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_feature_views_updated_at BEFORE UPDATE ON feature_views
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_features_updated_at BEFORE UPDATE ON features
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_datasets_updated_at BEFORE UPDATE ON datasets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_experiments_updated_at BEFORE UPDATE ON experiments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_models_updated_at BEFORE UPDATE ON models
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_deployments_updated_at BEFORE UPDATE ON deployments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMIT;