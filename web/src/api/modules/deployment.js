import request from '@/utils/request'

/**
 * K8s 模型部署 API 模块
 */

/**
 * 部署模型到 K8s
 * @param {Object} data - 部署请求
 */
export function deployModel(data) {
  return request({
    url: '/deployment/deploy',
    method: 'post',
    data
  })
}

/**
 * 下线部署
 * @param {number} id - 部署ID
 */
export function undeploy(id) {
  return request({
    url: `/deployment/${id}`,
    method: 'delete'
  })
}

/**
 * 扩缩容
 * @param {number} id - 部署ID
 * @param {number} replicas - 目标副本数
 */
export function scaleDeployment(id, replicas) {
  return request({
    url: `/deployment/${id}/scale`,
    method: 'put',
    params: { replicas }
  })
}

/**
 * 获取部署状态
 * @param {number} id - 部署ID
 */
export function getDeploymentStatus(id) {
  return request({
    url: `/deployment/${id}/status`,
    method: 'get'
  })
}

/**
 * 获取部署详情
 * @param {number} id - 部署ID
 */
export function getDeployment(id) {
  return request({
    url: `/deployment/${id}`,
    method: 'get'
  })
}

/**
 * 列出所有部署
 */
export function listDeployments() {
  return request({
    url: '/deployment',
    method: 'get'
  })
}

/**
 * 按 namespace 列出部署
 * @param {string} namespace - 命名空间
 */
export function listDeploymentsByNamespace(namespace) {
  return request({
    url: `/deployment/namespace/${encodeURIComponent(namespace)}`,
    method: 'get'
  })
}

/**
 * 获取 Pod 列表
 * @param {number} id - 部署ID
 */
export function getPods(id) {
  return request({
    url: `/deployment/${id}/pods`,
    method: 'get'
  })
}

/**
 * 获取 Pod 日志
 * @param {number} id - 部署ID
 * @param {string} podName - Pod 名称
 * @param {number} tailLines - 日志行数
 */
export function getPodLogs(id, podName, tailLines = 100) {
  return request({
    url: `/deployment/${id}/pods/${encodeURIComponent(podName)}/logs`,
    method: 'get',
    params: { tailLines }
  })
}

/**
 * 重启 Pod
 * @param {number} id - 部署ID
 * @param {string} podName - Pod 名称
 */
export function restartPod(id, podName) {
  return request({
    url: `/deployment/${id}/pods/${encodeURIComponent(podName)}`,
    method: 'delete'
  })
}

/**
 * 列出所有 namespace（数据库中）
 */
export function listNamespaces() {
  return request({
    url: '/deployment/namespaces',
    method: 'get'
  })
}

/**
 * 列出集群中的 namespace
 */
export function listClusterNamespaces() {
  return request({
    url: '/deployment/namespaces/cluster',
    method: 'get'
  })
}

export default {
  deployModel,
  undeploy,
  scaleDeployment,
  getDeploymentStatus,
  getDeployment,
  listDeployments,
  listDeploymentsByNamespace,
  getPods,
  getPodLogs,
  restartPod,
  listNamespaces,
  listClusterNamespaces
}
