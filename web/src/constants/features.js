/**
 * 特征工程相关常量和类型定义
 *
 * @author MModelX Team
 * @since 2026-05-20
 */

// ==================== 数据源类型 ====================

/**
 * 数据源类型枚举
 */
export const DataSourceTypes = Object.freeze({
  POSTGRESQL: 'postgresql',
  MYSQL: 'mysql',
  API: 'api',
  REDIS: 'redis',
  KAFKA: 'kafka',
  MINIO: 'minio',
  CLICKHOUSE: 'clickhouse'
})

/**
 * 数据源类型显示名称映射
 */
export const DataSourceTypeLabels = Object.freeze({
  [DataSourceTypes.POSTGRESQL]: 'PostgreSQL',
  [DataSourceTypes.MYSQL]: 'MySQL',
  [DataSourceTypes.API]: 'HTTP API',
  [DataSourceTypes.REDIS]: 'Redis',
  [DataSourceTypes.KAFKA]: 'Kafka',
  [DataSourceTypes.MINIO]: 'MinIO',
  [DataSourceTypes.CLICKHOUSE]: 'ClickHouse'
})

/**
 * 数据源类型选项（用于表单选择器）
 */
export const DataSourceTypeOptions = Object.freeze(
  Object.entries(DataSourceTypeLabels).map(([value, label]) => ({
    value,
    label
  }))
)

// ==================== 特征视图状态 ====================

/**
 * 特征视图状态枚举
 */
export const FeatureViewStatus = Object.freeze({
  DRAFT: 'DRAFT',           // 草稿
  ACTIVE: 'ACTIVE',         // 激活
  DEPRECATED: 'DEPRECATED', // 弃用
  ARCHIVED: 'ARCHIVED'      // 归档
})

/**
 * 特征视图状态显示名称映射
 */
export const FeatureViewStatusLabels = Object.freeze({
  [FeatureViewStatus.DRAFT]: '草稿',
  [FeatureViewStatus.ACTIVE]: '激活',
  [FeatureViewStatus.DEPRECATED]: '弃用',
  [FeatureViewStatus.ARCHIVED]: '归档'
})

/**
 * 特征视图状态颜色映射（Element Plus tag type）
 */
export const FeatureViewStatusColors = Object.freeze({
  [FeatureViewStatus.DRAFT]: 'info',
  [FeatureViewStatus.ACTIVE]: 'success',
  [FeatureViewStatus.DEPRECATED]: 'warning',
  [FeatureViewStatus.ARCHIVED]: 'danger'
})

/**
 * 特征视图状态选项（排除 ARCHIVED，后端 list 接口不返回已归档视图）
 */
export const FeatureViewStatusOptions = Object.freeze(
  Object.entries(FeatureViewStatusLabels)
    .filter(([value]) => value !== FeatureViewStatus.ARCHIVED)
    .map(([value, label]) => ({
      value,
      label,
      color: FeatureViewStatusColors[value]
    }))
)

// ==================== 特征数据类型 ====================

/**
 * 特征数据类型枚举
 */
export const FeatureDataTypes = Object.freeze({
  INT64: 'INT64',
  FLOAT64: 'FLOAT64',
  STRING: 'STRING',
  BOOLEAN: 'BOOLEAN',
  ARRAY: 'ARRAY',
  MAP: 'MAP'
})

/**
 * 特征数据类型显示名称
 */
export const FeatureDataTypeLabels = Object.freeze({
  [FeatureDataTypes.INT64]: '整数',
  [FeatureDataTypes.FLOAT64]: '浮点数',
  [FeatureDataTypes.STRING]: '字符串',
  [FeatureDataTypes.BOOLEAN]: '布尔值',
  [FeatureDataTypes.ARRAY]: '数组',
  [FeatureDataTypes.MAP]: '映射'
})

/**
 * 特征数据类型选项
 */
export const FeatureDataTypeOptions = Object.freeze(
  Object.entries(FeatureDataTypeLabels).map(([value, label]) => ({
    value,
    label
  }))
)

// ==================== 时间窗口选项 ====================

/**
 * 特征时间窗口选项
 */
export const TimeWindowOptions = Object.freeze([
  { value: '', label: '无' },
  { value: '1h', label: '1小时' },
  { value: '6h', label: '6小时' },
  { value: '12h', label: '12小时' },
  { value: '1d', label: '1天' },
  { value: '3d', label: '3天' },
  { value: '7d', label: '7天' },
  { value: '14d', label: '14天' },
  { value: '30d', label: '30天' },
  { value: '60d', label: '60天' },
  { value: '90d', label: '90天' }
])

// ==================== Transform 表达式类型 ====================

/**
 * Transform 表达式类型枚举
 */
export const TransformTypes = Object.freeze({
  // 聚合操作
  SUM: 'sum',
  AVG: 'avg',
  COUNT: 'count',
  MAX: 'max',
  MIN: 'min',
  DISTINCT_COUNT: 'distinct_count',

  // 数学变换
  LOG: 'log',
  LOG1P: 'log1p',
  SQRT: 'sqrt',
  ABS: 'abs',
  POWER: 'power',
  ROUND: 'round',

  // 时间窗口
  LAST: 'last',
  FIRST: 'first',
  LAG: 'lag',

  // 条件过滤
  WHERE: 'where',
  FILTER: 'filter'
})

/**
 * Transform 表达式显示名称
 */
export const TransformTypeLabels = Object.freeze({
  [TransformTypes.SUM]: '求和',
  [TransformTypes.AVG]: '平均值',
  [TransformTypes.COUNT]: '计数',
  [TransformTypes.MAX]: '最大值',
  [TransformTypes.MIN]: '最小值',
  [TransformTypes.DISTINCT_COUNT]: '去重计数',
  [TransformTypes.LOG]: '对数',
  [TransformTypes.LOG1P]: '对数(1+x)',
  [TransformTypes.SQRT]: '平方根',
  [TransformTypes.ABS]: '绝对值',
  [TransformTypes.POWER]: '幂次方',
  [TransformTypes.ROUND]: '四舍五入',
  [TransformTypes.LAST]: '最新值',
  [TransformTypes.FIRST]: '最早值',
  [TransformTypes.LAG]: '滞后值',
  [TransformTypes.WHERE]: '条件过滤',
  [TransformTypes.FILTER]: '条件筛选'
})

/**
 * Transform 表达式分组（用于分类显示）
 */
export const TransformTypeGroups = Object.freeze({
  AGGREGATION: 'aggregation',
  MATH: 'math',
  TIME_WINDOW: 'time_window',
  CONDITION: 'condition'
})

/**
 * Transform 表达式分组名称
 */
export const TransformTypeGroupLabels = Object.freeze({
  [TransformTypeGroups.AGGREGATION]: '聚合操作',
  [TransformTypeGroups.MATH]: '数学变换',
  [TransformTypeGroups.TIME_WINDOW]: '时间窗口',
  [TransformTypeGroups.CONDITION]: '条件过滤'
})

/**
 * Transform 表达式分类映射
 */
export const TransformTypeCategories = Object.freeze({
  [TransformTypes.SUM]: TransformTypeGroups.AGGREGATION,
  [TransformTypes.AVG]: TransformTypeGroups.AGGREGATION,
  [TransformTypes.COUNT]: TransformTypeGroups.AGGREGATION,
  [TransformTypes.MAX]: TransformTypeGroups.AGGREGATION,
  [TransformTypes.MIN]: TransformTypeGroups.AGGREGATION,
  [TransformTypes.DISTINCT_COUNT]: TransformTypeGroups.AGGREGATION,

  [TransformTypes.LOG]: TransformTypeGroups.MATH,
  [TransformTypes.LOG1P]: TransformTypeGroups.MATH,
  [TransformTypes.SQRT]: TransformTypeGroups.MATH,
  [TransformTypes.ABS]: TransformTypeGroups.MATH,
  [TransformTypes.POWER]: TransformTypeGroups.MATH,
  [TransformTypes.ROUND]: TransformTypeGroups.MATH,

  [TransformTypes.LAST]: TransformTypeGroups.TIME_WINDOW,
  [TransformTypes.FIRST]: TransformTypeGroups.TIME_WINDOW,
  [TransformTypes.LAG]: TransformTypeGroups.TIME_WINDOW,

  [TransformTypes.WHERE]: TransformTypeGroups.CONDITION,
  [TransformTypes.FILTER]: TransformTypeGroups.CONDITION
})

/**
 * Transform 表达式选项列表
 */
export const TransformTypeOptions = Object.freeze(
  Object.entries(TransformTypeLabels).map(([value, label]) => ({
    value,
    label,
    group: TransformTypeCategories[value]
  }))
)

// ==================== 验证规则 ====================

/**
 * 正则表达式验证规则
 */
export const ValidationPatterns = Object.freeze({
  // 特征视图名称：3-50字符，仅允许字母、数字、下划线
  FEATURE_VIEW_NAME: /^[a-zA-Z0-9_]{3,50}$/,

  // 特征名称：3-100字符，仅允许字母、数字、下划线
  FEATURE_NAME: /^[a-zA-Z0-9_]{3,100}$/,

  // 实体类型：2-50字符
  ENTITY_TYPE: /^[a-zA-Z0-9_]{2,50}$/,

  // JSON字符串验证
  JSON_STRING: /^[\s\S]*$/

  // URL验证（用于API数据源）
  // URL: /^https?:\/\/.+/
})

/**
 * 验证错误提示消息
 */
export const ValidationMessages = Object.freeze({
  FEATURE_VIEW_NAME_REQUIRED: '请输入特征视图名称',
  FEATURE_VIEW_NAME_INVALID: '名称长度3-50字符，仅允许字母、数字、下划线',
  ENTITY_TYPE_REQUIRED: '请输入实体类型',
  ENTITY_TYPE_INVALID: '实体类型长度2-50字符，仅允许字母、数字、下划线',
  FEATURE_NAME_REQUIRED: '请输入特征名称',
  FEATURE_NAME_INVALID: '特征名称长度3-100字符，仅允许字母、数字、下划线',
  DATA_SOURCE_TYPE_REQUIRED: '请选择数据源类型',
  DATA_SOURCE_CONFIG_REQUIRED: '请配置数据源信息',
  TTL_REQUIRED: '请输入TTL时间',
  TTL_MIN: 'TTL时间不能小于1天',
  TTL_MAX: 'TTL时间不能大于365天',
  TRANSFORM_EXPR_REQUIRED: '请选择Transform表达式',
  DATA_TYPE_REQUIRED: '请选择数据类型'
})

/**
 * 默认值配置
 */
export const DefaultValues = Object.freeze({
  TTL: 30,              // 默认TTL 30天
  PAGE_SIZE: 20,        // 默认每页20条
  MAX_PAGE_SIZE: 100,   // 最大每页100条
  MIN_TTL: 1,           // 最小TTL 1天
  MAX_TTL: 365          // 最大TTL 365天
})

// ==================== 常用实体类型 ====================

/**
 * 常用实体类型列表
 */
export const CommonEntityTypes = Object.freeze([
  { value: 'user_id', label: '用户ID' },
  { value: 'item_id', label: '商品ID' },
  { value: 'shop_id', label: '店铺ID' },
  { value: 'category_id', label: '分类ID' },
  { value: 'session_id', label: '会话ID' },
  { value: 'device_id', label: '设备ID' }
])

// ==================== 数据源配置模板 ====================

/**
 * PostgreSQL 数据源配置模板
 */
export const PostgreSQLConfigTemplate = Object.freeze({
  table: '',
  entityColumn: 'id',
  dateColumn: 'created_at'
})

/**
 * API 数据源配置模板
 */
export const ApiConfigTemplate = Object.freeze({
  url: 'https://api.example.com/data',
  method: 'GET',
  entityField: 'user_id',
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  },
  params: {}
})

/**
 * Redis 数据源配置模板
 */
export const RedisConfigTemplate = Object.freeze({
  keyPattern: 'user:*',
  entityField: 'user_id',
  dataStructure: 'hash'
})

/**
 * Kafka 数据源配置模板
 */
export const KafkaConfigTemplate = Object.freeze({
  topic: 'user-events',
  entityField: 'user_id',
  maxPollRecords: 1000,
  consumerGroup: 'feature-computation-group'
})

// ==================== 其他常量 ====================

/**
 * 特征视图名称前缀（用于自动生成）
 */
export const FeatureViewNamePrefixes = Object.freeze({
  USER: 'user_features',
  ITEM: 'item_features',
  SHOP: 'shop_features',
  SESSION: 'session_features'
})

/**
 * 特征计算进度状态
 */
export const ComputeStatus = Object.freeze({
  PENDING: 'pending',
  RUNNING: 'running',
  SUCCESS: 'success',
  FAILED: 'failed',
  CANCELLED: 'cancelled'
})

/**
 * 特征物化进度状态
 */
export const MaterializeStatus = Object.freeze({
  PENDING: 'pending',
  RUNNING: 'running',
  SUCCESS: 'success',
  FAILED: 'failed',
  PARTIAL: 'partial'  // 部分成功
})

/**
 * 日期格式化选项
 */
export const DateFormats = Object.freeze({
  DATE: 'YYYY-MM-DD',
  DATETIME: 'YYYY-MM-DD HH:mm:ss',
  TIME: 'HH:mm:ss'
})

// 导出所有常量的默认对象
export default {
  DataSourceTypes,
  DataSourceTypeLabels,
  DataSourceTypeOptions,
  FeatureViewStatus,
  FeatureViewStatusLabels,
  FeatureViewStatusColors,
  FeatureViewStatusOptions,
  FeatureDataTypes,
  FeatureDataTypeLabels,
  FeatureDataTypeOptions,
  TimeWindowOptions,
  TransformTypes,
  TransformTypeLabels,
  TransformTypeGroups,
  TransformTypeGroupLabels,
  TransformTypeCategories,
  TransformTypeOptions,
  ValidationPatterns,
  ValidationMessages,
  DefaultValues,
  CommonEntityTypes,
  PostgreSQLConfigTemplate,
  ApiConfigTemplate,
  RedisConfigTemplate,
  KafkaConfigTemplate,
  FeatureViewNamePrefixes,
  ComputeStatus,
  MaterializeStatus,
  DateFormats
}
