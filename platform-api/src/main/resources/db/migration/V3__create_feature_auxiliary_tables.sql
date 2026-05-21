-- MModelX Feature Engineering - Auxiliary Tables
-- Version: 3
-- Description: Create materialization_history, query_history, and favorites tables
-- Author: MModelX Team
-- Date: 2026-05-20

-- 1. Create materialization_history table
-- 特征物化历史记录表
CREATE TABLE IF NOT EXISTS materialization_history (
    id BIGSERIAL PRIMARY KEY,
    feature_view_name VARCHAR(100) NOT NULL,
    feature_view_id BIGINT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    feature_count INTEGER,
    entity_count BIGINT,
    source_path VARCHAR(500),
    redis_key_prefix VARCHAR(200),
    error_message TEXT,
    operator VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT chk_materialization_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'PARTIAL'))
);

-- Create indexes for materialization_history
CREATE INDEX IF NOT EXISTS idx_materialization_feature_view_name ON materialization_history(feature_view_name);
CREATE INDEX IF NOT EXISTS idx_materialization_status ON materialization_history(status);
CREATE INDEX IF NOT EXISTS idx_materialization_started_at ON materialization_history(started_at);
CREATE INDEX IF NOT EXISTS idx_materialization_operator ON materialization_history(operator);

-- Add comments
COMMENT ON TABLE materialization_history IS '特征物化历史记录表';
COMMENT ON COLUMN materialization_history.id IS '主键ID';
COMMENT ON COLUMN materialization_history.feature_view_name IS '特征视图名称';
COMMENT ON COLUMN materialization_history.feature_view_id IS '特征视图ID';
COMMENT ON COLUMN materialization_history.started_at IS '物化开始时间';
COMMENT ON COLUMN materialization_history.completed_at IS '物化完成时间';
COMMENT ON COLUMN materialization_history.status IS '物化状态：PENDING/RUNNING/SUCCESS/FAILED/PARTIAL';
COMMENT ON COLUMN materialization_history.feature_count IS '物化特征数量';
COMMENT ON COLUMN materialization_history.entity_count IS '物化实体数量';
COMMENT ON COLUMN materialization_history.source_path IS 'MinIO源文件路径';
COMMENT ON COLUMN materialization_history.redis_key_prefix IS 'Redis目标键前缀';
COMMENT ON COLUMN materialization_history.error_message IS '错误信息';
COMMENT ON COLUMN materialization_history.operator IS '操作者';
COMMENT ON COLUMN materialization_history.created_at IS '创建时间';
COMMENT ON COLUMN materialization_history.updated_at IS '更新时间';

-- 2. Create query_history table
-- 在线特征查询历史记录表
CREATE TABLE IF NOT EXISTS query_history (
    id BIGSERIAL PRIMARY KEY,
    query_name VARCHAR(100),
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    feature_names TEXT,
    query_result TEXT,
    status VARCHAR(20) NOT NULL,
    duration_ms BIGINT,
    error_message TEXT,
    queried_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_query_status CHECK (status IN ('SUCCESS', 'FAILED', 'TIMEOUT', 'PARTIAL'))
);

-- Create indexes for query_history
CREATE INDEX IF NOT EXISTS idx_query_entity_type ON query_history(entity_type);
CREATE INDEX IF NOT EXISTS idx_query_entity_id ON query_history(entity_id);
CREATE INDEX IF NOT EXISTS idx_query_status ON query_history(status);
CREATE INDEX IF NOT EXISTS idx_query_created_at ON query_history(created_at);
CREATE INDEX IF NOT EXISTS idx_query_queried_by ON query_history(queried_by);
CREATE INDEX IF NOT EXISTS idx_query_name ON query_history(query_name);

-- Add comments
COMMENT ON TABLE query_history IS '在线特征查询历史记录表';
COMMENT ON COLUMN query_history.id IS '主键ID';
COMMENT ON COLUMN query_history.query_name IS '查询名称（用户自定义）';
COMMENT ON COLUMN query_history.entity_type IS '实体类型';
COMMENT ON COLUMN query_history.entity_id IS '实体ID';
COMMENT ON COLUMN query_history.feature_names IS '查询的特征名称列表（JSON格式）';
COMMENT ON COLUMN query_history.query_result IS '查询结果（JSON格式）';
COMMENT ON COLUMN query_history.status IS '查询状态：SUCCESS/FAILED/TIMEOUT/PARTIAL';
COMMENT ON COLUMN query_history.duration_ms IS '查询耗时（毫秒）';
COMMENT ON COLUMN query_history.error_message IS '错误信息';
COMMENT ON COLUMN query_history.queried_by IS '查询者';
COMMENT ON COLUMN query_history.created_at IS '创建时间';

-- 3. Create favorites table
-- 特征查询收藏夹表
CREATE TABLE IF NOT EXISTS favorites (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    entity_type VARCHAR(50) NOT NULL,
    feature_names TEXT NOT NULL,
    favorite_type VARCHAR(50) NOT NULL,
    feature_view_name VARCHAR(100),
    sort_order INTEGER,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT chk_favorite_type CHECK (favorite_type IN ('FEATURE_QUERY', 'FEATURE_VIEW', 'DATASET'))
);

-- Create indexes for favorites
CREATE INDEX IF NOT EXISTS idx_favorite_created_by ON favorites(created_by);
CREATE INDEX IF NOT EXISTS idx_favorite_type ON favorites(favorite_type);
CREATE INDEX IF NOT EXISTS idx_favorite_entity_type ON favorites(entity_type);
CREATE INDEX IF NOT EXISTS idx_favorite_feature_view_name ON favorites(feature_view_name);
CREATE INDEX IF NOT EXISTS idx_favorite_name ON favorites(name);
CREATE INDEX IF NOT EXISTS idx_favorite_sort_order ON favorites(sort_order);

-- Add unique constraint
CREATE UNIQUE INDEX IF NOT EXISTS idx_favorite_unique_name ON favorites(created_by, name);

-- Add comments
COMMENT ON TABLE favorites IS '特征查询收藏夹表';
COMMENT ON COLUMN favorites.id IS '主键ID';
COMMENT ON COLUMN favorites.name IS '收藏名称';
COMMENT ON COLUMN favorites.description IS '收藏描述';
COMMENT ON COLUMN favorites.entity_type IS '实体类型';
COMMENT ON COLUMN favorites.feature_names IS '特征名称列表（JSON格式）';
COMMENT ON COLUMN favorites.favorite_type IS '收藏类型：FEATURE_QUERY/FEATURE_VIEW/DATASET';
COMMENT ON COLUMN favorites.feature_view_name IS '关联的特征视图名称';
COMMENT ON COLUMN favorites.sort_order IS '排序序号';
COMMENT ON COLUMN favorites.is_default IS '是否默认';
COMMENT ON COLUMN favorites.created_by IS '创建者';
COMMENT ON COLUMN favorites.created_at IS '创建时间';
COMMENT ON COLUMN favorites.updated_at IS '更新时间';

-- 4. Foreign key constraints (if feature_views table exists)
-- Add foreign key to feature_views if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'feature_views') THEN
        ALTER TABLE materialization_history
        DROP CONSTRAINT IF EXISTS fk_materialization_feature_view;

        ALTER TABLE materialization_history
        ADD CONSTRAINT fk_materialization_feature_view
        FOREIGN KEY (feature_view_id)
        REFERENCES feature_views(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        ALTER TABLE query_history
        DROP CONSTRAINT IF EXISTS fk_query_user;

        ALTER TABLE query_history
        ADD CONSTRAINT fk_query_user
        FOREIGN KEY (queried_by)
        REFERENCES users(username)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

        ALTER TABLE favorites
        DROP CONSTRAINT IF EXISTS fk_favorite_user;

        ALTER TABLE favorites
        ADD CONSTRAINT fk_favorite_user
        FOREIGN KEY (created_by)
        REFERENCES users(username)
        ON DELETE CASCADE
        ON UPDATE CASCADE;
    END IF;
END $$;
