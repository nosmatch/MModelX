import { defineStore } from 'pinia'
import * as trainingApi from '@/api/modules/training'

/**
 * 训练管理状态管理 Store
 *
 * 管理训练模块的全局状态，包括：
 * - 训练任务列表
 * - 实验列表
 * - 模型列表
 * - 当前选中的任务/实验/模型
 * - 加载状态和错误状态
 */
export const useTrainingStore = defineStore('training', {
  state: () => ({
    // 训练任务列表
    jobs: [],

    // 运行中的任务列表
    runningJobs: [],

    // 实验列表
    experiments: [],

    // 模型列表
    models: [],

    // 当前选中的训练任务
    currentJob: null,

    // 当前选中的实验
    currentExperiment: null,

    // 当前选中的模型
    currentModel: null,

    // 加载状态
    loading: {
      jobs: false,
      runningJobs: false,
      experiments: false,
      models: false,
      training: false,
      tuning: false
    },

    // 错误信息
    error: null,

    // 训练任务轮询定时器
    jobPollTimer: null
  }),

  getters: {
    /**
     * 获取成功的训练任务
     */
    successfulJobs: (state) => {
      return state.jobs.filter(job => job.status === 'SUCCESS')
    },

    /**
     * 获取失败的训练任务
     */
    failedJobs: (state) => {
      return state.jobs.filter(job => job.status === 'FAILED')
    },

    /**
     * 获取待处理的训练任务
     */
    pendingJobs: (state) => {
      return state.jobs.filter(job => job.status === 'PENDING')
    },

    /**
     * 获取生产环境模型
     */
    productionModels: (state) => {
      return state.models.filter(model => model.stage === 'Production')
    },

    /**
     * 检查是否正在加载
     */
    isLoading: (state) => {
      return Object.values(state.loading).some(value => value === true)
    },

    /**
     * 获取任务统计
     */
    jobStats: (state) => {
      const total = state.jobs.length
      const running = state.jobs.filter(j => j.status === 'RUNNING').length
      const success = state.jobs.filter(j => j.status === 'SUCCESS').length
      const failed = state.jobs.filter(j => j.status === 'FAILED').length
      const pending = state.jobs.filter(j => j.status === 'PENDING').length
      return { total, running, success, failed, pending }
    }
  },

  actions: {
    /**
     * 获取训练任务列表
     */
    async fetchJobs() {
      try {
        this.loading.jobs = true
        this.error = null

        const response = await trainingApi.listTrainingJobs()

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.jobs = response.data || []
        } else {
          throw new Error(response.message || '获取训练任务列表失败')
        }
      } catch (error) {
        console.error('获取训练任务列表失败:', error)
        this.error = error.message || '获取训练任务列表失败'
        throw error
      } finally {
        this.loading.jobs = false
      }
    },

    /**
     * 获取运行中的任务列表
     */
    async fetchRunningJobs() {
      try {
        this.loading.runningJobs = true
        this.error = null

        const response = await trainingApi.listRunningJobs()

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.runningJobs = response.data || []
        } else {
          throw new Error(response.message || '获取运行中任务失败')
        }
      } catch (error) {
        console.error('获取运行中任务失败:', error)
        this.error = error.message || '获取运行中任务失败'
        throw error
      } finally {
        this.loading.runningJobs = false
      }
    },

    /**
     * 获取单个任务状态
     * @param {number} jobId - 任务ID
     */
    async fetchJobStatus(jobId) {
      try {
        const response = await trainingApi.getJobStatus(jobId)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.currentJob = response.data
          // 更新列表中的对应任务
          const index = this.jobs.findIndex(j => j.id === jobId)
          if (index !== -1) {
            this.jobs[index] = response.data
          }
          return response.data
        } else {
          throw new Error(response.message || '获取任务状态失败')
        }
      } catch (error) {
        console.error('获取任务状态失败:', error)
        this.error = error.message || '获取任务状态失败'
        throw error
      }
    },

    /**
     * 提交异步训练任务
     * @param {Object} config - 训练配置
     * @returns {Promise<number>} 任务ID
     */
    async submitJob(config) {
      try {
        this.loading.training = true
        this.error = null

        const response = await trainingApi.submitTrainingJob(config)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          // 刷新任务列表
          await this.fetchJobs()
          return response.data
        } else {
          throw new Error(response.message || '提交训练任务失败')
        }
      } catch (error) {
        console.error('提交训练任务失败:', error)
        this.error = error.message || '提交训练任务失败'
        throw error
      } finally {
        this.loading.training = false
      }
    },

    /**
     * 同步训练
     * @param {Object} config - 训练配置
     */
    async trainSync(config) {
      try {
        this.loading.training = true
        this.error = null

        const response = await trainingApi.trainSync(config)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          return response.data
        } else {
          throw new Error(response.message || '训练失败')
        }
      } catch (error) {
        console.error('训练失败:', error)
        this.error = error.message || '训练失败'
        throw error
      } finally {
        this.loading.training = false
      }
    },

    /**
     * 开始轮询任务状态
     * @param {number} jobId - 任务ID
     * @param {Function} [onUpdate] - 状态更新回调
     * @param {Function} [onComplete] - 完成回调
     */
    startJobPolling(jobId, onUpdate, onComplete) {
      // 清除已有定时器
      this.stopJobPolling()

      this.jobPollTimer = setInterval(async () => {
        try {
          const job = await this.fetchJobStatus(jobId)
          if (onUpdate) onUpdate(job)

          // 任务完成或失败时停止轮询
          if (job.status === 'SUCCESS' || job.status === 'FAILED') {
            this.stopJobPolling()
            if (onComplete) onComplete(job)
          }
        } catch (error) {
          console.error('轮询任务状态失败:', error)
        }
      }, 3000)
    },

    /**
     * 停止轮询任务状态
     */
    stopJobPolling() {
      if (this.jobPollTimer) {
        clearInterval(this.jobPollTimer)
        this.jobPollTimer = null
      }
    },

    /**
     * 获取实验列表
     */
    async fetchExperiments() {
      try {
        this.loading.experiments = true
        this.error = null

        const response = await trainingApi.listExperiments()

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.experiments = response.data || []
        } else {
          throw new Error(response.message || '获取实验列表失败')
        }
      } catch (error) {
        console.error('获取实验列表失败:', error)
        this.error = error.message || '获取实验列表失败'
        throw error
      } finally {
        this.loading.experiments = false
      }
    },

    /**
     * 获取实验详情
     * @param {string} name - 实验名称
     */
    async fetchExperiment(name) {
      try {
        const response = await trainingApi.getExperiment(name)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.currentExperiment = response.data
          return response.data
        } else {
          throw new Error(response.message || '获取实验详情失败')
        }
      } catch (error) {
        console.error('获取实验详情失败:', error)
        this.error = error.message || '获取实验详情失败'
        throw error
      }
    },

    /**
     * 创建实验
     * @param {Object} experiment - 实验数据
     */
    async createExperiment(experiment) {
      try {
        const response = await trainingApi.createExperiment(experiment)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          await this.fetchExperiments()
          return response.data
        } else {
          throw new Error(response.message || '创建实验失败')
        }
      } catch (error) {
        console.error('创建实验失败:', error)
        this.error = error.message || '创建实验失败'
        throw error
      }
    },

    /**
     * 获取模型列表
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
     * 获取生产环境模型
     * @param {string} name - 模型名称
     */
    async fetchProductionModel(name) {
      try {
        const response = await trainingApi.getProductionModel(name)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          return response.data
        } else {
          throw new Error(response.message || '获取生产模型失败')
        }
      } catch (error) {
        console.error('获取生产模型失败:', error)
        throw error
      }
    },

    /**
     * 转换模型阶段
     * @param {string} name - 模型名称
     * @param {string} version - 模型版本
     * @param {string} stage - 目标阶段
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
     * 超参数调优
     * @param {Object} config - 训练配置
     */
    async hyperparameterTuning(config) {
      try {
        this.loading.tuning = true
        this.error = null

        const response = await trainingApi.hyperparameterTuning(config)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          return response.data
        } else {
          throw new Error(response.message || '超参数调优失败')
        }
      } catch (error) {
        console.error('超参数调优失败:', error)
        this.error = error.message || '超参数调优失败'
        throw error
      } finally {
        this.loading.tuning = false
      }
    },

    /**
     * 清空错误信息
     */
    clearError() {
      this.error = null
    },

    /**
     * 重置所有状态
     */
    resetState() {
      this.jobs = []
      this.runningJobs = []
      this.experiments = []
      this.models = []
      this.currentJob = null
      this.currentExperiment = null
      this.currentModel = null
      this.error = null
      this.stopJobPolling()
    }
  }
})

export default useTrainingStore
