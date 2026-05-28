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
 * 提交超参数调优任务
 * @param {Object} config - 训练配置
 * @returns {Promise<{code: string, message: string, data: number}>}
 */
export function submitTuningJob(config) {
  return request({
    url: '/training/tune-jobs',
    method: 'post',
    data: config
  })
}

/**
 * 获取调优任务状态
 * @param {number} id - 调优任务ID
 * @returns {Promise<{code: string, message: string, data: Object}>}
 */
export function getTuningJob(id) {
  return request({
    url: `/training/tune-jobs/${id}`,
    method: 'get'
  })
}

/**
 * 获取调优任务 trial 列表
 * @param {number} id - 调优任务ID
 * @returns {Promise<{code: string, message: string, data: Array}>}
 */
export function getTuningTrials(id) {
  return request({
    url: `/training/tune-jobs/${id}/trials`,
    method: 'get'
  })
}

/**
 * 列出所有调优任务
 * @returns {Promise<{code: string, message: string, data: Array}>}
 */
export function listTuningJobs() {
  return request({
    url: '/training/tune-jobs',
    method: 'get'
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
 * 删除实验
 * @param {string} name - 实验名称
 * @returns {Promise<{code: string, message: string}>}
 */
export function deleteExperiment(name) {
  return request({
    url: `/training/experiments/${encodeURIComponent(name)}`,
    method: 'delete'
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

/**
 * 删除模型（包括 MinIO 文件）
 * @param {string} name - 模型名称
 * @param {string} version - 模型版本
 * @returns {Promise<{code: string, message: string}>}
 */
export function deleteModel(name, version) {
  return request({
    url: `/training/models/${encodeURIComponent(name)}/${encodeURIComponent(version)}`,
    method: 'delete'
  })
}

/**
 * 获取训练任务日志
 * @param {number} id - 任务ID
 * @returns {Promise<{code: string, message: string, data: string}>}
 */
export function getJobLogs(id) {
  return request({
    url: `/training/jobs/${id}/logs`,
    method: 'get'
  })
}

/**
 * 重新训练（基于已有任务的配置）
 * @param {number} id - 原任务ID
 * @returns {Promise<{code: string, message: string, data: number}>}
 */
export function retryTrainingJob(id) {
  return request({
    url: `/training/jobs/${id}/retry`,
    method: 'post'
  })
}

/**
 * 删除训练任务
 * @param {number} id - 任务ID
 * @returns {Promise<{code: string, message: string}>}
 */
export function deleteTrainingJob(id) {
  return request({
    url: `/training/jobs/${id}`,
    method: 'delete'
  })
}

export default {
  trainSync,
  submitTrainingJob,
  retryTrainingJob,
  deleteTrainingJob,
  getJobStatus,
  getJobLogs,
  listTrainingJobs,
  listRunningJobs,
  submitTuningJob,
  getTuningJob,
  getTuningTrials,
  listTuningJobs,
  createExperiment,
  getExperiment,
  listExperiments,
  deleteExperiment,
  registerModel,
  transitionModelStage,
  getProductionModel,
  listModels,
  deleteModel
}
