import { defineStore } from 'pinia'
import * as deploymentApi from '@/api/modules/deployment'

/**
 * K8s 模型部署状态管理 Store
 */
export const useDeploymentStore = defineStore('deployment', {
  state: () => ({
    // 部署列表
    deployments: [],

    // Namespace 列表
    namespaces: [],

    // 当前选中的 namespace
    currentNamespace: '',

    // 部署状态详情
    deploymentStatus: null,

    // Pod 列表
    pods: [],

    // Pod 日志
    podLogs: '',

    // 加载状态
    loading: {
      deployments: false,
      namespaces: false,
      deploy: false,
      undeploy: false,
      scale: false,
      pods: false,
      logs: false,
      status: false
    },

    // 错误信息
    error: null
  }),

  getters: {
    /**
     * 运行中的部署
     */
    runningDeployments: (state) => {
      return state.deployments.filter(d => d.status === 'RUNNING' || d.k8sStatus === 'Running')
    },

    /**
     * 按 namespace 筛选的部署
     */
    filteredDeployments: (state) => {
      if (!state.currentNamespace) {
        return state.deployments
      }
      return state.deployments.filter(d => d.namespace === state.currentNamespace)
    }
  },

  actions: {
    /**
     * 获取 Namespace 列表
     */
    async fetchNamespaces() {
      try {
        this.loading.namespaces = true
        this.error = null

        const response = await deploymentApi.listNamespaces()

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.namespaces = response.data || []
        } else {
          throw new Error(response.message || '获取 Namespace 失败')
        }
      } catch (error) {
        console.error('获取 Namespace 失败:', error)
        this.error = error.message || '获取 Namespace 失败'
        throw error
      } finally {
        this.loading.namespaces = false
      }
    },

    /**
     * 获取部署列表
     */
    async fetchDeployments() {
      try {
        this.loading.deployments = true
        this.error = null

        const response = await deploymentApi.listDeployments()

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.deployments = response.data || []
        } else {
          throw new Error(response.message || '获取部署列表失败')
        }
      } catch (error) {
        console.error('获取部署列表失败:', error)
        this.error = error.message || '获取部署列表失败'
        throw error
      } finally {
        this.loading.deployments = false
      }
    },

    /**
     * 按 namespace 获取部署列表
     */
    async fetchDeploymentsByNamespace(namespace) {
      try {
        this.loading.deployments = true
        this.error = null

        const response = await deploymentApi.listDeploymentsByNamespace(namespace)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.deployments = response.data || []
        } else {
          throw new Error(response.message || '获取部署列表失败')
        }
      } catch (error) {
        console.error('获取部署列表失败:', error)
        this.error = error.message || '获取部署列表失败'
        throw error
      } finally {
        this.loading.deployments = false
      }
    },

    /**
     * 部署模型
     */
    async deployModel(requestData) {
      try {
        this.loading.deploy = true
        this.error = null

        const response = await deploymentApi.deployModel(requestData)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          await this.fetchDeployments()
          return response.data
        } else {
          throw new Error(response.message || '部署失败')
        }
      } catch (error) {
        console.error('部署失败:', error)
        this.error = error.message || '部署失败'
        throw error
      } finally {
        this.loading.deploy = false
      }
    },

    /**
     * 下线部署
     */
    async undeploy(id) {
      try {
        this.loading.undeploy = true
        this.error = null

        const response = await deploymentApi.undeploy(id)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          await this.fetchDeployments()
          return true
        } else {
          throw new Error(response.message || '下线失败')
        }
      } catch (error) {
        console.error('下线失败:', error)
        this.error = error.message || '下线失败'
        throw error
      } finally {
        this.loading.undeploy = false
      }
    },

    /**
     * 扩缩容
     */
    async scaleDeployment(id, replicas) {
      try {
        this.loading.scale = true
        this.error = null

        const response = await deploymentApi.scaleDeployment(id, replicas)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          await this.fetchDeployments()
          return true
        } else {
          throw new Error(response.message || '扩缩容失败')
        }
      } catch (error) {
        console.error('扩缩容失败:', error)
        this.error = error.message || '扩缩容失败'
        throw error
      } finally {
        this.loading.scale = false
      }
    },

    /**
     * 获取部署状态
     */
    async fetchDeploymentStatus(id) {
      try {
        this.loading.status = true
        this.error = null

        const response = await deploymentApi.getDeploymentStatus(id)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.deploymentStatus = response.data
          return response.data
        } else {
          throw new Error(response.message || '获取部署状态失败')
        }
      } catch (error) {
        console.error('获取部署状态失败:', error)
        this.error = error.message || '获取部署状态失败'
        throw error
      } finally {
        this.loading.status = false
      }
    },

    /**
     * 获取 Pod 列表
     */
    async fetchPods(id) {
      try {
        this.loading.pods = true
        this.error = null

        const response = await deploymentApi.getPods(id)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.pods = response.data || []
          return response.data
        } else {
          throw new Error(response.message || '获取 Pod 列表失败')
        }
      } catch (error) {
        console.error('获取 Pod 列表失败:', error)
        this.error = error.message || '获取 Pod 列表失败'
        throw error
      } finally {
        this.loading.pods = false
      }
    },

    /**
     * 获取 Pod 日志
     */
    async fetchPodLogs(id, podName, tailLines = 100) {
      try {
        this.loading.logs = true
        this.error = null

        const response = await deploymentApi.getPodLogs(id, podName, tailLines)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          this.podLogs = response.data || ''
          return response.data
        } else {
          throw new Error(response.message || '获取 Pod 日志失败')
        }
      } catch (error) {
        console.error('获取 Pod 日志失败:', error)
        this.error = error.message || '获取 Pod 日志失败'
        throw error
      } finally {
        this.loading.logs = false
      }
    },

    /**
     * 重启 Pod
     */
    async restartPod(id, podName) {
      try {
        this.error = null

        const response = await deploymentApi.restartPod(id, podName)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          await this.fetchPods(id)
          return true
        } else {
          throw new Error(response.message || '重启 Pod 失败')
        }
      } catch (error) {
        console.error('重启 Pod 失败:', error)
        this.error = error.message || '重启 Pod 失败'
        throw error
      }
    },

    /**
     * 设置当前 namespace
     */
    setCurrentNamespace(namespace) {
      this.currentNamespace = namespace
    },

    /**
     * 清空错误信息
     */
    clearError() {
      this.error = null
    }
  }
})

export default useDeploymentStore
