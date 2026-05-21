-- MModelX Feature Engineering - Alter feature_views table
-- Version: 4
-- Description: Add missing columns to feature_views table
-- Author: MModelX Team
-- Date: 2026-05-20

-- Add data_source_type column
ALTER TABLE feature_views ADD COLUMN IF NOT EXISTS data_source_type VARCHAR(50);

-- Add data_source_config column
ALTER TABLE feature_views ADD COLUMN IF NOT EXISTS data_source_config TEXT;

-- Add status column with default value
ALTER TABLE feature_views ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'DRAFT';

-- Add last_computed_time column
ALTER TABLE feature_views ADD COLUMN IF NOT EXISTS last_computed_time TIMESTAMP;

-- Create index on status
CREATE INDEX IF NOT EXISTS idx_feature_views_status ON feature_views(status);

-- Create index on data_source_type
CREATE INDEX IF NOT EXISTS idx_feature_views_datasource_type ON feature_views(data_source_type);

-- Add comments
COMMENT ON COLUMN feature_views.data_source_type IS '数据源类型：postgresql, api, redis, kafka, minio, localfile';
COMMENT ON COLUMN feature_views.data_source_config IS '数据源配置（JSON格式）';
COMMENT ON COLUMN feature_views.status IS '状态：DRAFT, ACTIVE, DEPRECATED, ARCHIVED';
COMMENT ON COLUMN feature_views.last_computed_time IS '最后计算时间';

-- Update existing records to have default status
UPDATE feature_views SET status = 'DRAFT' WHERE status IS NULL;
