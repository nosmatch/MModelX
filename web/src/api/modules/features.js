import request from '@/utils/request'

/**
 * 特征工程API模块
 * 封装所有特征工程相关的后端API接口
 *
 * @author MModelX Team
 * @since 2026-05-20
 */

/**
 * 注册特征视图
 * @param {Object} data - 特征视图数据
 * @param {string} data.name - 视图名称（必填，3-50字符，字母数字下划线）
 * @param {string} data.entity - 实体类型（必填，如：user_id, item_id）
 * @param {number} data.ttl - TTL时间，单位天（可选，默认30天）
 * @param {string} data.description - 描述（可选）
 * @param {string} data.dataSourceType - 数据源类型（postgresql/api/redis/kafka等）
 * @param {string} data.dataSourceConfig - 数据源配置（JSON字符串）
 * @returns {Promise<{code: string, message: string, data: {id: number}}>}
 */
export function registerFeatureView(data) {
  return request({
    url: '/features/views',
    method: 'post',
    data
  })
}

/**
 * 获取特征视图详情
 * @param {string} name - 特征视图名称
 * @returns {Promise<{code: string, message: string, data: Object}>}
 */
export function getFeatureView(name) {
  return request({
    url: `/features/views/${encodeURIComponent(name)}`,
    method: 'get'
  })
}

/**
 * 列出所有特征视图
 * @param {Object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.pageSize=20] - 每页条数
 * @param {string} [params.status] - 状态筛选（DRAFT/ACTIVE/DEPRECATED/ARCHIVED）
 * @param {string} [params.entity] - 实体类型筛选
 * @returns {Promise<{code: string, message: string, data: {items: Array, total: number}}>}
 */
export function listFeatureViews(params = {}) {
  return request({
    url: '/features/views',
    method: 'get',
    params
  })
}

/**
 * 更新特征视图
 * @param {string} name - 特征视图名称
 * @param {Object} data - 更新数据
 * @param {string} [data.entity] - 实体类型
 * @param {number} [data.ttl] - TTL时间
 * @param {string} [data.description] - 描述
 * @returns {Promise<{code: string, message: string, data: Object}>}
 */
export function updateFeatureView(name, data) {
  return request({
    url: `/features/views/${encodeURIComponent(name)}`,
    method: 'put',
    data
  })
}

/**
 * 删除特征视图（软删除，状态改为ARCHIVED）
 * @param {string} name - 特征视图名称
 * @returns {Promise<{code: string, message: string}>}
 */
export function deleteFeatureView(name) {
  return request({
    url: `/features/views/${encodeURIComponent(name)}`,
    method: 'delete'
  })
}

/**
 * 注册特征定义
 * @param {Object} definition - 特征定义数据
 * @param {string} definition.featureView - 特征视图名称
 * @param {string} definition.entity - 实体类型
 * @param {Array} definition.features - 特征规格列表
 * @param {Object} definition.source - 数据源配置
 * @returns {Promise<{code: string, message: string, data: Object}>}
 */
export function registerFeatureDefinition(definition) {
  return request({
    url: '/features/definitions',
    method: 'post',
    data: definition
  })
}

/**
 * 计算特征
 * @param {Object} data - 计算请求参数
 * @param {Object} data.definition - 特征定义
 * @param {string} [data.inputPath] - 输入路径
 * @param {string} [data.outputPath] - 输出路径
 * @returns {Promise<{code: string, message: string}>}
 */
export function computeFeatures(data) {
  return request({
    url: '/features/compute',
    method: 'post',
    data
  })
}

/**
 * 批量计算特征
 * @param {Array<Object>} definitions - 特征定义列表
 * @returns {Promise<{code: string, message: string}>}
 */
export function batchComputeFeatures(definitions) {
  return request({
    url: '/features/compute/batch',
    method: 'post',
    data: { definitions }
  })
}

/**
 * 物化特征到Redis（将MinIO中的离线特征写入在线存储）
 * @param {string} featureViewName - 特征视图名称
 * @returns {Promise<{code: string, message: string}>}
 */
export function materializeFeatures(featureViewName) {
  return request({
    url: `/features/materialize/${encodeURIComponent(featureViewName)}`,
    method: 'post'
  })
}

/**
 * 获取在线特征（从Redis实时查询）
 * @param {Object} params - 查询参数
 * @param {string} params.entityType - 实体类型（必填）
 * @param {string} params.entityId - 实体ID（必填）
 * @param {string} params.featureNames - 特征名称列表，逗号分隔（必填）
 * @returns {Promise<{code: string, message: string, data: Object}>}
 */
export function getOnlineFeatures(params) {
  return request({
    url: '/features/online',
    method: 'get',
    params
  })
}

/**
 * 测试数据源连接
 * @param {Object} config - 测试配置
 * @param {string} config.type - 数据源类型（api/redis/kafka等）
 * @param {string} config.config - 配置信息（JSON字符串）
 * @returns {Promise<{code: string, message: string, data: {connected: boolean}}>}
 */
export function testDataSource(config) {
  return request({
    url: '/features/test-connection',
    method: 'post',
    data: config
  })
}

/**
 * 获取特征视图的特征定义列表
 * @param {string} featureViewName - 特征视图名称
 * @returns {Promise<{code: string, message: string, data: Array}>}
 */
export function getFeatureDefinitions(featureViewName) {
  return request({
    url: `/features/views/${encodeURIComponent(featureViewName)}/definitions`,
    method: 'get'
  })
}

/**
 * 删除特征定义
 * @param {string} featureViewName - 特征视图名称
 * @param {string} featureName - 特征名称
 * @returns {Promise<{code: string, message: string}>}
 */
export function deleteFeatureDefinition(featureViewName, featureName) {
  return request({
    url: `/features/views/${encodeURIComponent(featureViewName)}/definitions/${encodeURIComponent(featureName)}`,
    method: 'delete'
  })
}

// 导出所有API函数的默认对象，便于解构使用
export default {
  registerFeatureView,
  getFeatureView,
  listFeatureViews,
  updateFeatureView,
  deleteFeatureView,
  registerFeatureDefinition,
  computeFeatures,
  batchComputeFeatures,
  materializeFeatures,
  getOnlineFeatures,
  testDataSource,
  getFeatureDefinitions,
  deleteFeatureDefinition
}
