import request from '@/utils/request'

/**
 * 训练管理API模块
 * 封装所有模型训练相关的后端API接口
 */

/**
 * 同步训练模型
 * @param {Object} config - 训练配置
 * @returns {Promise<{code: string, message: string, data: string}>}
 */
export function trainSync(config) {
  return request({
    url: '/training/train',
    method: 'post',
    data: config
  })
}

/**
 * 提交异步训练任务
 * @param {Object} config - 训练配置
 * @returns {Promise<{code: string, message: string, data: number}>}
 */
export function submitTrainingJob(config) {
  return request({
    url: '/training/jobs',
    method: 'post',
    data: config
  })
}

/**
 * 获取训练任务状态
 * @param {number} id - 任务ID
 * @returns {Promise<{code: string, message: string, data: Object}>}
 */
export function getJobStatus(id) {
  return request({
    url: `/training/jobs/${id}`,
    method: 'get'
  })
}

/**
 * 列出所有训练任务
 * @returns {Promise<{code: string, message: string, data: Array}>}
 */
export function listTrainingJobs() {
  return request({
    url: '/training/jobs',
    method: 'get'
  })
}

/**
 * 列出运行中的训练任务
 * @returns {Promise<{code: string, message: string, data: Array}>}
 */
export function listRunningJobs() {
  return request({
    url: '/training/jobs/running',
    method: 'get'
  })
}

/**
 * 超参数调优
 * @param {Object} config - 训练配置
 * @returns {Promise<{code: string, message: string, data: Object}>}
 */
export function hyperparameterTuning(config) {
  return request({
    url: '/training/tune',
    method: 'post',
    data: config
  })
}

/**
 * 创建实验
 * @param {Object} experiment - 实验数据
 * @returns {Promise<{code: string, message: string, data: number}>}
 */
export function createExperiment(experiment) {
  return request({
    url: '/training/experiments',
    method: 'post',
    data: experiment
  })
}

/**
 * 获取实验详情
 * @param {string} name - 实验名称
 * @returns {Promise<{code: string, message: string, data: Object}>}
 */
export function getExperiment(name) {
  return request({
    url: `/training/experiments/${encodeURIComponent(name)}`,
    method: 'get'
  })
}

/**
 * 列出所有实验
 * @returns {Promise<{code: string, message: string, data: Array}>}
 */
export function listExperiments() {
  return request({
    url: '/training/experiments',
    method: 'get'
  })
}

/**
 * 注册模型
 * @param {Object} model - 模型数据
 * @returns {Promise<{code: string, message: string, data: number}>}
 */
export function registerModel(model) {
  return request({
    url: '/training/models',
    method: 'post',
    data: model
  })
}

/**
 * 转换模型阶段
 * @param {string} name - 模型名称
 * @param {string} version - 模型版本
 * @param {string} stage - 目标阶段
 * @returns {Promise<{code: string, message: string}>}
 */
export function transitionModelStage(name, version, stage) {
  return request({
    url: `/training/models/${encodeURIComponent(name)}/${encodeURIComponent(version)}/stage`,
    method: 'put',
    params: { stage }
  })
}

/**
 * 获取生产环境模型
 * @param {string} name - 模型名称
 * @returns {Promise<{code: string, message: string, data: Object}>}
 */
export function getProductionModel(name) {
  return request({
    url: `/training/models/${encodeURIComponent(name)}/production`,
    method: 'get'
  })
}

/**
 * 列出所有模型
 * @returns {Promise<{code: string, message: string, data: Array}>}
 */
export function listModels() {
  return request({
    url: '/training/models',
    method: 'get'
  })
}

export default {
  trainSync,
  submitTrainingJob,
  getJobStatus,
  listTrainingJobs,
  listRunningJobs,
  hyperparameterTuning,
  createExperiment,
  getExperiment,
  listExperiments,
  registerModel,
  transitionModelStage,
  getProductionModel,
  listModels
}
