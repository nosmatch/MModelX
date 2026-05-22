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

/**
 * 获取数据源的表名列表
 * @param {number} id - 数据源ID
 * @returns {Promise<{code: string, message: string, data: Array<string>}>}
 */
export function getDataSourceTables(id) {
  return request({ url: `/datasources/${id}/tables`, method: 'get' })
}

/**
 * 获取指定表的列信息
 * @param {number} id - 数据源ID
 * @param {string} table - 表名
 * @returns {Promise<{code: string, message: string, data: Array<{name: string, type: string, nullable: string}>}>}
 */
export function getDataSourceColumns(id, table) {
  return request({ url: `/datasources/${id}/tables/${encodeURIComponent(table)}/columns`, method: 'get' })
}
