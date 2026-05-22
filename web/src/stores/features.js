import { defineStore } from 'pinia'
import * as featuresApi from '@/api/modules/features'

/**
 * 特征工程状态管理 Store
 *
 * 管理特征工程的全局状态，包括：
 * - 特征视图列表
 * - 当前选中的特征视图
 * - 特征定义列表
 * - 加载状态和错误状态
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
export const useFeaturesStore = defineStore('features', {
  state: () => ({
    // 特征视图列表
    views: [],

    // 当前选中的特征视图
    currentView: null,

    // 特征定义列表
    definitions: [],

    // 加载状态
    loading: {
      views: false,
      currentView: false,
      definitions: false,
      computing: false,
      materializing: false
    },

    // 错误信息
    error: null,

    // 分页信息
    pagination: {
      page: 1,
      pageSize: 20,
      total: 0
    },

    // 筛选条件
    filters: {
      status: '',
      entity: '',
      keyword: ''
    }
  }),

  getters: {
    /**
     * 获取激活状态的特征视图
     */
    activeViews: (state) => {
      return state.views.filter(view => view.status === 'ACTIVE')
    },

    /**
     * 获取草稿状态的特征视图
     */
    draftViews: (state) => {
      return state.views.filter(view => view.status === 'DRAFT')
    },

    /**
     * 根据实体类型筛选视图
     */
    viewsByEntity: (state) => (entityType) => {
      return state.views.filter(view => view.entity === entityType)
    },

    /**
     * 检查是否正在加载
     */
    isLoading: (state) => {
      return Object.values(state.loading).some(value => value === true)
    },

    /**
     * 获取当前视图的特征数量
     */
    currentViewFeatureCount: (state) => {
      return state.currentView?.features?.length || 0
    },

    /**
     * 获取所有实体类型列表（去重）
     */
    entityTypes: (state) => {
      const entities = state.views.map(view => view.entity)
      return [...new Set(entities)]
    }
  },

  actions: {
    /**
     * 获取特征视图列表
     * @param {Object} params - 查询参数
     * @param {number} [params.page] - 页码
     * @param {number} [params.pageSize] - 每页条数
     * @param {string} [params.status] - 状态筛选
     * @param {string} [params.entity] - 实体类型筛选
     */
    async fetchViews(params = {}) {
      try {
        this.loading.views = true
        this.error = null

        const queryParams = {
          page: params.page || this.pagination.page,
          pageSize: params.pageSize || this.pagination.pageSize,
          status: params.status || this.filters.status,
          entity: params.entity || this.filters.entity
        }

        const response = await featuresApi.listFeatureViews(queryParams)
        console.log('[fetchViews] response:', response)

        const isSuccess = response.code === '200' || response.code === 200
        if (isSuccess) {
          // 兼容两种后端返回格式：直接数组 或 {items, total} 对象
          const rawData = response.data
          if (Array.isArray(rawData)) {
            this.views = rawData
            this.pagination.total = rawData.length
          } else {
            this.views = rawData?.items || []
            this.pagination.total = rawData?.total || 0
          }
          this.pagination.page = queryParams.page
          this.pagination.pageSize = queryParams.pageSize
          console.log('[fetchViews] views loaded:', this.views.length, this.views)
        } else {
          throw new Error(response.message || '获取特征视图列表失败')
        }
      } catch (error) {
        console.error('获取特征视图列表失败:', error)
        this.error = error.message || '获取特征视图列表失败'
        throw error
      } finally {
        this.loading.views = false
      }
    },

    /**
     * 获取单个特征视图详情
     * @param {string} name - 特征视图名称
     */
    async fetchView(name) {
      try {
        this.loading.currentView = true
        this.error = null

        const response = await featuresApi.getFeatureView(name)

        if (response.code === '200') {
          this.currentView = response.data
        } else {
          throw new Error(response.message || '获取特征视图详情失败')
        }
      } catch (error) {
        console.error('获取特征视图详情失败:', error)
        this.error = error.message || '获取特征视图详情失败'
        throw error
      } finally {
        this.loading.currentView = false
      }
    },

    /**
     * 选择特征视图
     * @param {Object|string} view - 视图对象或视图名称
     */
    selectView(view) {
      if (typeof view === 'string') {
        const found = this.views.find(v => v.name === view)
        this.currentView = found || null
      } else {
        this.currentView = view
      }
    },

    /**
     * 创建新特征视图
     * @param {Object} data - 特征视图数据
     * @returns {Promise<number>} 返回创建的特征视图ID
     */
    async createView(data) {
      try {
        this.loading.views = true
        this.error = null

        const response = await featuresApi.registerFeatureView(data)

        if (response.code === '200') {
          // 刷新列表
          await this.fetchViews()
          return response.data.id
        } else {
          throw new Error(response.message || '创建特征视图失败')
        }
      } catch (error) {
        console.error('创建特征视图失败:', error)
        this.error = error.message || '创建特征视图失败'
        throw error
      } finally {
        this.loading.views = false
      }
    },

    /**
     * 更新特征视图
     * @param {string} name - 特征视图名称
     * @param {Object} data - 更新数据
     */
    async updateView(name, data) {
      try {
        this.loading.views = true
        this.error = null

        const response = await featuresApi.updateFeatureView(name, data)

        if (response.code === '200') {
          // 更新本地状态
          const index = this.views.findIndex(v => v.name === name)
          if (index !== -1) {
            this.views[index] = { ...this.views[index], ...data }
          }

          // 如果更新的是当前视图，也更新当前视图
          if (this.currentView && this.currentView.name === name) {
            this.currentView = { ...this.currentView, ...data }
          }
        } else {
          throw new Error(response.message || '更新特征视图失败')
        }
      } catch (error) {
        console.error('更新特征视图失败:', error)
        this.error = error.message || '更新特征视图失败'
        throw error
      } finally {
        this.loading.views = false
      }
    },

    /**
     * 删除特征视图（软删除）
     * @param {string} name - 特征视图名称
     */
    async deleteView(name) {
      try {
        this.loading.views = true
        this.error = null

        const response = await featuresApi.deleteFeatureView(name)

        if (response.code === '200') {
          // 从列表中移除
          this.views = this.views.filter(v => v.name !== name)

          // 如果删除的是当前视图，清空当前视图
          if (this.currentView && this.currentView.name === name) {
            this.currentView = null
          }
        } else {
          throw new Error(response.message || '删除特征视图失败')
        }
      } catch (error) {
        console.error('删除特征视图失败:', error)
        this.error = error.message || '删除特征视图失败'
        throw error
      } finally {
        this.loading.views = false
      }
    },

    /**
     * 计算特征
     * @param {Object} definition - 特征定义
     * @param {string} [inputPath] - 输入路径
     * @param {string} [outputPath] - 输出路径
     */
    async computeFeatures(definition, inputPath, outputPath) {
      try {
        this.loading.computing = true
        this.error = null

        const response = await featuresApi.computeFeatures(definition, inputPath, outputPath)

        if (response.code !== '200') {
          throw new Error(response.message || '特征计算失败')
        }

        return response
      } catch (error) {
        console.error('特征计算失败:', error)
        this.error = error.message || '特征计算失败'
        throw error
      } finally {
        this.loading.computing = false
      }
    },

    /**
     * 批量计算特征
     * @param {Array<Object>} definitions - 特征定义列表
     */
    async batchComputeFeatures(definitions) {
      try {
        this.loading.computing = true
        this.error = null

        const response = await featuresApi.batchComputeFeatures(definitions)

        if (response.code !== '200') {
          throw new Error(response.message || '批量特征计算失败')
        }

        return response
      } catch (error) {
        console.error('批量特征计算失败:', error)
        this.error = error.message || '批量特征计算失败'
        throw error
      } finally {
        this.loading.computing = false
      }
    },

    /**
     * 物化特征到Redis
     * @param {string} featureViewName - 特征视图名称
     */
    async materializeFeatures(featureViewName) {
      try {
        this.loading.materializing = true
        this.error = null

        const response = await featuresApi.materializeFeatures(featureViewName)

        if (response.code !== '200') {
          throw new Error(response.message || '特征物化失败')
        }

        return response
      } catch (error) {
        console.error('特征物化失败:', error)
        this.error = error.message || '特征物化失败'
        throw error
      } finally {
        this.loading.materializing = false
      }
    },

    /**
     * 获取在线特征
     * @param {Object} params - 查询参数
     * @param {string} params.entityType - 实体类型
     * @param {string} params.entityId - 实体ID
     * @param {string} params.featureNames - 特征名称列表（逗号分隔）
     */
    async fetchOnlineFeatures(params) {
      try {
        const response = await featuresApi.getOnlineFeatures(params)

        if (response.code === '200') {
          return response.data
        } else {
          throw new Error(response.message || '获取在线特征失败')
        }
      } catch (error) {
        console.error('获取在线特征失败:', error)
        this.error = error.message || '获取在线特征失败'
        throw error
      }
    },

    /**
     * 测试数据源连接
     * @param {Object} config - 数据源配置
     */
    async testConnection(config) {
      try {
        const response = await featuresApi.testDataSource(config)

        if (response.code === '200') {
          return response.data.connected
        } else {
          throw new Error(response.message || '测试连接失败')
        }
      } catch (error) {
        console.error('测试数据源连接失败:', error)
        throw error
      }
    },

    /**
     * 设置筛选条件
     * @param {Object} filters - 筛选条件
     */
    setFilters(filters) {
      this.filters = { ...this.filters, ...filters }
    },

    /**
     * 重置筛选条件
     */
    resetFilters() {
      this.filters = {
        status: '',
        entity: '',
        keyword: ''
      }
    },

    /**
     * 设置分页
     * @param {Object} pagination - 分页信息
     */
    setPagination(pagination) {
      this.pagination = { ...this.pagination, ...pagination }
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
      this.views = []
      this.currentView = null
      this.definitions = []
      this.error = null
      this.pagination = {
        page: 1,
        pageSize: 20,
        total: 0
      }
      this.filters = {
        status: '',
        entity: '',
        keyword: ''
      }
    }
  }
})

export default useFeaturesStore
