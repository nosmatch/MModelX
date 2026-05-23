import { defineStore } from 'pinia'
import * as samplesApi from '@/api/modules/samples'

/**
 * 样本工程状态管理 Store
 */
export const useSamplesStore = defineStore('samples', {
  state: () => ({
    configs: [],
    datasets: [],
    currentBuildJob: null,
    loading: {
      configs: false,
      datasets: false,
      building: false
    },
    error: null,
    pagination: {
      page: 1,
      pageSize: 20,
      total: 0
    }
  }),

  getters: {
    activeConfigs: (state) => {
      return state.configs.filter(c => c.status === 'ACTIVE')
    },

    configCount: (state) => state.configs.length,
    datasetCount: (state) => state.datasets.length,

    isLoading: (state) => {
      return Object.values(state.loading).some(v => v === true)
    }
  },

  actions: {
    async fetchConfigs(params = {}) {
      try {
        this.loading.configs = true
        this.error = null

        const response = await samplesApi.listSampleConfigs(params)
        if (response.code === '200' || response.code === 200) {
          const rawData = response.data
          if (Array.isArray(rawData)) {
            this.configs = rawData
            this.pagination.total = rawData.length
          } else {
            this.configs = rawData?.items || []
            this.pagination.total = rawData?.total || 0
          }
        } else {
          throw new Error(response.message || '获取样本配置失败')
        }
      } catch (error) {
        console.error('获取样本配置失败:', error)
        this.error = error.message || '获取样本配置失败'
        throw error
      } finally {
        this.loading.configs = false
      }
    },

    async createConfig(data) {
      try {
        this.loading.configs = true
        const response = await samplesApi.createSampleConfig(data)
        if (response.code === '200') {
          await this.fetchConfigs()
          return response.data
        } else {
          throw new Error(response.message || '创建失败')
        }
      } catch (error) {
        console.error('创建样本配置失败:', error)
        throw error
      } finally {
        this.loading.configs = false
      }
    },

    async updateConfig(id, data) {
      try {
        this.loading.configs = true
        const response = await samplesApi.updateSampleConfig(id, data)
        if (response.code === '200') {
          await this.fetchConfigs()
          return response.data
        } else {
          throw new Error(response.message || '更新失败')
        }
      } catch (error) {
        console.error('更新样本配置失败:', error)
        throw error
      } finally {
        this.loading.configs = false
      }
    },

    async deleteConfig(id) {
      try {
        this.loading.configs = true
        const response = await samplesApi.deleteSampleConfig(id)
        if (response.code === '200') {
          this.configs = this.configs.filter(c => c.id !== id)
        } else {
          throw new Error(response.message || '删除失败')
        }
      } catch (error) {
        console.error('删除样本配置失败:', error)
        throw error
      } finally {
        this.loading.configs = false
      }
    },

    async buildSample(config) {
      try {
        this.loading.building = true
        const response = await samplesApi.buildSample(config)
        if (response.code === '200') {
          this.currentBuildJob = { jobId: response.data, progress: 0, status: 'RUNNING' }
          return response.data
        } else {
          throw new Error(response.message || '构建失败')
        }
      } catch (error) {
        console.error('样本构建失败:', error)
        throw error
      } finally {
        this.loading.building = false
      }
    },

    async fetchBuildStatus(jobId) {
      try {
        const response = await samplesApi.getBuildJobStatus(jobId)
        if (response.code === '200' || response.code === 200) {
          this.currentBuildJob = {
            jobId,
            progress: response.data.progress || 0,
            status: response.data.status,
            errorMessage: response.data.errorMessage
          }
          return response.data
        }
      } catch (error) {
        console.error('获取构建状态失败:', error)
      }
      return null
    },

    async fetchDatasets(params = {}) {
      try {
        this.loading.datasets = true
        this.error = null
        const response = await samplesApi.listDatasets(params)
        if (response.code === '200' || response.code === 200) {
          const rawData = response.data
          this.datasets = Array.isArray(rawData) ? rawData : (rawData?.items || [])
        } else {
          throw new Error(response.message || '获取数据集失败')
        }
      } catch (error) {
        console.error('获取数据集失败:', error)
        this.error = error.message || '获取数据集失败'
        throw error
      } finally {
        this.loading.datasets = false
      }
    },

    clearError() {
      this.error = null
    }
  }
})

export default useSamplesStore
