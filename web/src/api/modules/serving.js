import request from '@/utils/request'

/**
 * Serving/模型部署 API 模块
 */

/**
 * 获取服务状态
 */
export function getServingStatus() {
  return request({
    url: '/serving/status',
    method: 'get'
  })
}

/**
 * 获取服务健康状态
 */
export function getServingHealth() {
  return request({
    url: '/serving/health',
    method: 'get'
  })
}

/**
 * 单条预测
 * @param {Object} data - 预测请求
 */
export function predict(data) {
  return request({
    url: '/serving/predict',
    method: 'post',
    data
  })
}

/**
 * 快速预测
 * @param {Object} params - 查询参数 {modelName, entityType, entityId}
 */
export function quickPredict(params) {
  return request({
    url: '/serving/predict',
    method: 'get',
    params
  })
}

/**
 * A/B 测试预测
 * @param {Object} data - 预测请求
 * @param {Object} params - {modelA, modelB, ratio}
 */
export function abTestPredict(data, params) {
  return request({
    url: '/serving/predict/abtest',
    method: 'post',
    data,
    params
  })
}

/**
 * 获取模型版本信息
 * @param {string} modelName - 模型名称
 */
export function getModelVersionInfo(modelName) {
  return request({
    url: `/serving/models/${encodeURIComponent(modelName)}/info`,
    method: 'get'
  })
}

/**
 * 热加载模型
 * @param {string} modelName - 模型名称
 * @param {string} version - 模型版本
 */
export function reloadModel(modelName, version) {
  return request({
    url: `/serving/models/${encodeURIComponent(modelName)}/${encodeURIComponent(version)}/reload`,
    method: 'post'
  })
}

/**
 * 卸载模型
 * @param {string} modelName - 模型名称
 * @param {string} version - 模型版本
 */
export function unloadModel(modelName, version) {
  return request({
    url: `/serving/models/${encodeURIComponent(modelName)}/${encodeURIComponent(version)}`,
    method: 'delete'
  })
}

/**
 * 清空模型缓存
 */
export function clearModelCache() {
  return request({
    url: '/serving/models/cache',
    method: 'delete'
  })
}

export default {
  getServingStatus,
  getServingHealth,
  predict,
  quickPredict,
  abTestPredict,
  getModelVersionInfo,
  reloadModel,
  unloadModel,
  clearModelCache
}
