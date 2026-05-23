import request from '@/utils/request'

/**
 * 样本工程API模块
 */

export function listSampleConfigs(params = {}) {
  return request({
    url: '/samples/configs',
    method: 'get',
    params
  })
}

export function createSampleConfig(data) {
  return request({
    url: '/samples/configs',
    method: 'post',
    data
  })
}

export function updateSampleConfig(id, data) {
  return request({
    url: `/samples/configs/${id}`,
    method: 'put',
    data
  })
}

export function deleteSampleConfig(id) {
  return request({
    url: `/samples/configs/${id}`,
    method: 'delete'
  })
}

export function buildSample(data) {
  return request({
    url: '/samples/build',
    method: 'post',
    data
  })
}

export function getBuildJobStatus(jobId) {
  return request({
    url: `/samples/build/${jobId}/status`,
    method: 'get'
  })
}

export function pointInTimeJoin(data) {
  return request({
    url: '/samples/join',
    method: 'post',
    data
  })
}

export function validateSample(data) {
  return request({
    url: '/samples/validate',
    method: 'post',
    data
  })
}

export function listDatasets(params = {}) {
  return request({
    url: '/samples/datasets',
    method: 'get',
    params
  })
}

export function getDataset(name, version) {
  return request({
    url: `/samples/datasets/${name}/${version}`,
    method: 'get'
  })
}

export function listDatasetVersions(name) {
  return request({
    url: `/samples/datasets/${name}/versions`,
    method: 'get'
  })
}

export function deleteDataset(name, version) {
  return request({
    url: `/samples/datasets/${name}/${version}`,
    method: 'delete'
  })
}

export function compareDatasets(name, version1, version2) {
  return request({
    url: `/samples/datasets/${name}/compare`,
    method: 'get',
    params: { version1, version2 }
  })
}

export function previewDataset(name, version, split, limit) {
  return request({
    url: `/samples/datasets/${name}/${version}/preview`,
    method: 'get',
    params: { split, limit }
  })
}

export default {
  listSampleConfigs,
  createSampleConfig,
  updateSampleConfig,
  deleteSampleConfig,
  buildSample,
  getBuildJobStatus,
  pointInTimeJoin,
  validateSample,
  listDatasets,
  getDataset,
  listDatasetVersions,
  deleteDataset,
  compareDatasets,
  previewDataset
}
