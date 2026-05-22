import request from '@/utils/request'

export function listDataSources() {
  return request({ url: '/datasources', method: 'get' })
}

export function getDataSource(id) {
  return request({ url: `/datasources/${id}`, method: 'get' })
}

export function createDataSource(data) {
  return request({ url: '/datasources', method: 'post', data })
}

export function updateDataSource(id, data) {
  return request({ url: `/datasources/${id}`, method: 'put', data })
}

export function deleteDataSource(id) {
  return request({ url: `/datasources/${id}`, method: 'delete' })
}

export function testConnection(id) {
  return request({ url: `/datasources/${id}/test`, method: 'post' })
}

export function enableDataSource(id) {
  return request({ url: `/datasources/${id}/enable`, method: 'post' })
}

export function disableDataSource(id) {
  return request({ url: `/datasources/${id}/disable`, method: 'post' })
}

export function getDataSourceUsage(id) {
  return request({ url: `/datasources/${id}/usage`, method: 'get' })
}
