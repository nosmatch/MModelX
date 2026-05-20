import request from '@/utils/request'

// 获取系统状态摘要
export function getSummary() {
  return request({
    url: '/overview/summary',
    method: 'get'
  })
}

// 获取关键指标趋势
export function getMetrics(params) {
  return request({
    url: '/overview/metrics',
    method: 'get',
    params
  })
}

// 获取运行中任务
export function getTasks() {
  return request({
    url: '/overview/tasks',
    method: 'get'
  })
}

// 获取资源使用情况
export function getResources() {
  return request({
    url: '/overview/resources',
    method: 'get'
  })
}