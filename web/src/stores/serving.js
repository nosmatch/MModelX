import { defineStore } from 'pinia'
import * as servingApi from '@/api/modules/serving'
import * as trainingApi from '@/api/modules/training'

/**
 * 模型部署/Serving 状态管理 Store
 */
export const useServingStore = defineStore('serving', {
  state: () => ({
    // 模型列表
    models: [],

    // 服务状态
    serviceStatus: null,

    // 健康状态
    healthStatus: null,

    // 模型缓存信息
    modelCacheInfo: [],

    // 预测结果
    predictionResult: null,

    // 加载状态
    loading: {
      models: false,
      status: false,
      predict: false,
      reload: false
    },

    // 错误信息
    error: null
  }),

  getters: {
    /**
     * 生产环境模型
     */
    productionModels: (state) => {
      return state.models.filter(m => m.stage === 'Production')
    },

    /**
     * 暂存环境模型
     */
    stagingModels: (state) => {
      return state.models.filter(m => m.stage === 'Staging')
    },

    /**
     * 归档模型
     */
    archivedModels: (state) => {
      return state.models.filter(m => m.stage === 'Archived')
    },

    /**
     * 服务是否健康
     */
    isHealthy: (state) => {
      return state.healthStatus?.status === 'healthy'
    }
  },

  actions: {
    /**
     * 获取模型列表（从 training 模块获取）
     */
    async fetchModels() {
      try {
        this.loading.models = true
        this.error = null

        const response = await trainingApi.listModels()

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.models = response.data || []
        } else {
          throw new Error(response.message || '获取模型列表失败')
        }
      } catch (error) {
        console.error('获取模型列表失败:', error)
        this.error = error.message || '获取模型列表失败'
        throw error
      } finally {
        this.loading.models = false
      }
    },

    /**
     * 获取服务状态
     */
    async fetchServiceStatus() {
      try {
        this.loading.status = true

        const response = await servingApi.getServingStatus()

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.serviceStatus = response.data
          this.modelCacheInfo = response.data?.modelCache?.cachedModels || []
        }
      } catch (error) {
        console.error('获取服务状态失败:', error)
      } finally {
        this.loading.status = false
      }
    },

    /**
     * 获取健康状态
     */
    async fetchHealthStatus() {
      try {
        const response = await servingApi.getServingHealth()

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.healthStatus = response.data
        }
      } catch (error) {
        console.error('获取健康状态失败:', error)
        this.healthStatus = { status: 'unhealthy' }
      }
    },

    /**
     * 转换模型阶段
     */
    async transitionModelStage(name, version, stage) {
      try {
        const response = await trainingApi.transitionModelStage(name, version, stage)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          await this.fetchModels()
          return true
        } else {
          throw new Error(response.message || '转换模型阶段失败')
        }
      } catch (error) {
        console.error('转换模型阶段失败:', error)
        this.error = error.message || '转换模型阶段失败'
        throw error
      }
    },

    /**
     * 热加载模型
     */
    async hotReloadModel(modelName, version) {
      try {
        this.loading.reload = true

        const response = await servingApi.reloadModel(modelName, version)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          await this.fetchServiceStatus()
          return true
        } else {
          throw new Error(response.message || '热加载模型失败')
        }
      } catch (error) {
        console.error('热加载模型失败:', error)
        this.error = error.message || '热加载模型失败'
        throw error
      } finally {
        this.loading.reload = false
      }
    },

    /**
     * 执行预测
     */
    async doPredict(requestData) {
      try {
        this.loading.predict = true
        this.predictionResult = null

        const response = await servingApi.predict(requestData)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.predictionResult = response.data
          return response.data
        } else {
          throw new Error(response.message || '预测失败')
        }
      } catch (error) {
        console.error('预测失败:', error)
        this.error = error.message || '预测失败'
        throw error
      } finally {
        this.loading.predict = false
      }
    },

    /**
     * A/B 测试预测
     */
    async doAbTestPredict(data, modelA, modelB, ratio) {
      try {
        this.loading.predict = true
        this.predictionResult = null

        const response = await servingApi.abTestPredict(data, { modelA, modelB, ratio })

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.predictionResult = response.data
          return response.data
        } else {
          throw new Error(response.message || 'A/B测试预测失败')
        }
      } catch (error) {
        console.error('A/B测试预测失败:', error)
        this.error = error.message || 'A/B测试预测失败'
        throw error
      } finally {
        this.loading.predict = false
      }
    },

    /**
     * 清空模型缓存
     */
    async clearCache() {
      try {
        const response = await servingApi.clearModelCache()

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          await this.fetchServiceStatus()
          return true
        }
      } catch (error) {
        console.error('清空缓存失败:', error)
        throw error
      }
    },

    /**
     * 清空错误信息
     */
    clearError() {
      this.error = null
    }
  }
})

export default useServingStore
