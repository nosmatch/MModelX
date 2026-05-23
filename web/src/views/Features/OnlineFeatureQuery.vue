<template>
  <div class="online-feature-query">
    <!-- 查询表单 -->
    <el-card class="query-card">
      <template #header>
        <div class="card-header">
          <span>查询参数</span>
          <div class="header-actions">
            <el-button size="small" :icon="Clock" @click="showHistory = true">
              查询历史 ({{ queryHistory.length }})
            </el-button>
            <el-button size="small" :icon="Star" @click="showBookmarkDialog = true">
              收藏夹 ({{ bookmarks.length }})
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="queryForm" label-width="120px" @submit.prevent="handleQuery">
        <el-row :gutter="20">
          <!-- 实体类型 -->
          <el-col :span="8">
            <el-form-item label="实体类型">
              <el-select
                v-model="queryForm.entityType"
                placeholder="选择或输入实体类型"
                filterable
                allow-create
                style="width: 100%"
              >
                <el-option
                  v-for="type in commonEntityTypes"
                  :key="type.value"
                  :label="type.label"
                  :value="type.value"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <!-- 实体ID -->
          <el-col :span="8">
            <el-form-item label="实体ID">
              <el-input
                v-model="queryForm.entityId"
                placeholder="输入实体ID"
                clearable
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>

          <!-- 查询按钮 -->
          <el-col :span="8">
            <el-form-item label-width="0" class="query-btn-item">
              <el-button
                type="primary"
                :icon="Search"
                :loading="querying"
                :disabled="!canQuery"
                @click="handleQuery"
              >
                查询特征
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 特征选择 -->
        <el-form-item label="选择特征" class="feature-select-item">
          <div class="feature-selector">
            <!-- 顶部对齐行 -->
            <div class="selector-top">
              <div class="top-left">
                <el-select
                  v-model="selectedFeatureView"
                  placeholder="选择特征视图（可选）"
                  clearable
                  style="width: 100%"
                  @change="handleViewChange"
                >
                  <el-option
                    v-for="view in activeViews"
                    :key="view.name"
                    :label="view.name"
                    :value="view.name"
                  >
                    <div class="view-option">
                      <span class="name">{{ view.name }}</span>
                      <el-tag size="small" type="info">{{ view.entity }}</el-tag>
                      <span class="count">{{ view.features?.length || 0 }} 特征</span>
                    </div>
                  </el-option>
                </el-select>
              </div>
              <div class="top-right">
                <span class="section-title">快捷操作</span>
              </div>
            </div>

            <!-- 底部内容行 -->
            <div class="selector-bottom">
              <div class="selector-left">
                <el-transfer
                  v-model="queryForm.featureNames"
                  :data="availableFeatures"
                  :titles="['可选特征', '已选特征']"
                  filterable
                  filter-placeholder="搜索特征"
                  style="width: 100%"
                  class="feature-transfer"
                >
                  <template #default="{ option }">
                    <div class="transfer-item">
                      <span class="feature-name">{{ option.label }}</span>
                      <el-tag size="small" :type="getDataTypeTagType(option.dtype)">
                        {{ option.dtype }}
                      </el-tag>
                    </div>
                  </template>
                </el-transfer>
              </div>

              <div class="selector-right">
                <div class="quick-select">
                  <!-- 已选特征预览 -->
                  <div class="feature-group">
                    <div class="group-title">
                      已选特征
                      <el-tag size="small" type="success">{{ queryForm.featureNames.length }}</el-tag>
                    </div>
                    <div class="selected-features-list">
                      <el-tag
                        v-for="featureName in queryForm.featureNames.slice(0, 15)"
                        :key="featureName"
                        size="small"
                        closable
                        @close="removeFeature(featureName)"
                        style="margin: 2px"
                      >
                        {{ featureName }}
                      </el-tag>
                      <el-tag
                        v-if="queryForm.featureNames.length > 15"
                        size="small"
                        type="info"
                        style="margin: 2px"
                      >
                        +{{ queryForm.featureNames.length - 15 }}
                      </el-tag>
                      <div v-if="queryForm.featureNames.length === 0" class="no-selection">
                        暂无选择
                      </div>
                    </div>
                  </div>

                  <!-- 选择操作 -->
                  <div class="select-actions">
                    <el-button size="small" @click="selectAllFeatures">全选</el-button>
                    <el-button size="small" @click="clearAllFeatures">清空</el-button>
                    <el-button size="small" type="primary" @click="saveAsBookmark">
                      保存为收藏
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 查询结果 -->
    <el-card v-if="queryResult" class="result-card">
      <template #header>
        <div class="card-header">
          <span>查询结果</span>
          <div class="header-actions">
            <el-tag v-if="queryResult.fromCache" type="success" size="small">
              来自缓存
            </el-tag>
            <el-tag type="info" size="small">
              耗时: {{ queryResult.elapsedTime }}ms
            </el-tag>
            <el-button size="small" :icon="Download" @click="exportResult">
              导出
            </el-button>
          </div>
        </div>
      </template>

      <!-- 结果统计 -->
      <el-row :gutter="16" class="result-stats">
        <el-col :span="8">
          <div class="stat-card-mini">
            <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">
              <el-icon :size="24"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">实体</div>
              <div class="stat-value">{{ queryResult.entityType }}:{{ queryResult.entityId }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-card-mini">
            <div class="stat-icon" style="background: #f0f9ff; color: #67c23a;">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">特征数量</div>
              <div class="stat-value highlight">{{ Object.keys(queryResult.features).length }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-card-mini">
            <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">
              <el-icon :size="24"><Timer /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">查询时间</div>
              <div class="stat-value">{{ formatDateTime(queryResult.timestamp) }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 特征数据表格 -->
      <el-table
        :data="featureTableData"
        stripe
        border
        style="width: 100%; margin-top: 16px"
        max-height="500"
      >
        <el-table-column prop="name" label="特征名称" width="200" fixed>
          <template #default="{ row }">
            <div class="feature-name-cell">
              <el-icon class="icon" :color="getFeatureColor(row.dtype)">
                <component :is="getFeatureIcon(row.dtype)" />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="value" label="特征值" min-width="200">
          <template #default="{ row }">
            <div v-if="row.value === null" class="null-value">NULL</div>
            <div v-else-if="row.dtype === 'BOOLEAN'" class="boolean-value">
              <el-tag :type="row.value ? 'success' : 'info'" size="small">
                {{ row.value ? 'TRUE' : 'FALSE' }}
              </el-tag>
            </div>
            <div v-else-if="row.dtype === 'STRING'" class="string-value">
              {{ row.value }}
            </div>
            <div v-else-if="Array.isArray(row.value)" class="array-value">
              <el-tag
                v-for="(item, index) in row.value.slice(0, 5)"
                :key="index"
                size="small"
                style="margin-right: 4px"
              >
                {{ item }}
              </el-tag>
              <span v-if="row.value.length > 5" class="more-hint">
                +{{ row.value.length - 5 }}
              </span>
            </div>
            <div v-else class="number-value">
              {{ formatNumber(row.value) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="dtype" label="数据类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDataTypeTagType(row.dtype)" size="small">
              {{ row.dtype }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.description || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :icon="DocumentCopy"
              @click="copyFeatureValue(row)"
            >
              复制
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- JSON视图 -->
      <el-collapse style="margin-top: 16px">
        <el-collapse-item title="查看JSON格式" name="json">
          <pre class="json-view">{{ JSON.stringify(queryResult.features, null, 2) }}</pre>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 空状态 -->
    <el-card v-else class="empty-card">
      <el-empty description="请输入查询参数并点击查询按钮">
        <template #image>
          <el-icon :size="80" color="#c0c4cc">
            <Search />
          </el-icon>
        </template>
      </el-empty>
    </el-card>

    <!-- 查询历史对话框 -->
    <el-dialog
      v-model="showHistory"
      title="查询历史"
      width="800px"
    >
      <div class="history-list">
        <div
          v-for="(item, index) in queryHistory"
          :key="index"
          class="history-item"
          @click="restoreFromHistory(item)"
        >
          <div class="item-header">
            <div class="entity-info">
              <el-tag size="small">{{ item.entityType }}</el-tag>
              <span class="entity-id">{{ item.entityId }}</span>
            </div>
            <div class="item-actions">
              <el-button
                size="small"
                :icon="Delete"
                type="danger"
                @click.stop="deleteHistoryItem(index)"
              >
                删除
              </el-button>
            </div>
          </div>
          <div class="item-body">
            <span class="feature-count">{{ item.featureNames.length }} 个特征</span>
            <span class="query-time">{{ formatDateTime(item.timestamp) }}</span>
          </div>
        </div>

        <div v-if="queryHistory.length === 0" class="history-empty">
          <el-empty description="暂无查询历史" />
        </div>
      </div>

      <template #footer>
        <el-button @click="clearHistory">清空历史</el-button>
        <el-button type="primary" @click="showHistory = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 收藏夹对话框 -->
    <el-dialog
      v-model="showBookmarkDialog"
      title="收藏夹"
      width="800px"
    >
      <div class="bookmark-list">
        <div
          v-for="(bookmark, index) in bookmarks"
          :key="index"
          class="bookmark-item"
          @click="restoreFromBookmark(bookmark)"
        >
          <div class="bookmark-header">
            <div class="bookmark-name">
              <el-icon class="icon" color="#f5a623"><Star /></el-icon>
              {{ bookmark.name }}
            </div>
            <div class="bookmark-actions">
              <el-button
                size="small"
                type="primary"
                @click.stop="restoreFromBookmark(bookmark)"
              >
                使用
              </el-button>
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                @click.stop="deleteBookmark(index)"
              >
                删除
              </el-button>
            </div>
          </div>
          <div class="bookmark-body">
            <div class="bookmark-info">
              <span class="label">实体：</span>
              <el-tag size="small">{{ bookmark.entityType }}</el-tag>
            </div>
            <div class="bookmark-features">
              <el-tag
                v-for="feature in bookmark.featureNames"
                :key="feature"
                size="small"
                style="margin: 2px"
              >
                {{ feature }}
              </el-tag>
            </div>
          </div>
        </div>

        <div v-if="bookmarks.length === 0" class="bookmark-empty">
          <el-empty description="暂无收藏" />
        </div>
      </div>

      <template #footer>
        <el-button @click="showBookmarkDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 保存收藏对话框 -->
    <el-dialog
      v-model="showSaveBookmarkDialog"
      title="保存为收藏"
      width="500px"
    >
      <el-form :model="bookmarkForm" label-width="80px">
        <el-form-item label="名称">
          <el-input
            v-model="bookmarkForm.name"
            placeholder="输入收藏名称"
            clearable
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showSaveBookmarkDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmSaveBookmark">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 在线特征查询工具
 *
 * 功能：
 * - 实时查询Redis中的在线特征
 * - 支持多特征批量查询
 * - 查询历史记录
 * - 收藏夹功能
 * - 结果导出
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Key,
  Clock,
  Star,
  Download,
  DocumentCopy,
  Delete,
  TrendCharts,
  Calendar,
  User,
  Timer
} from '@element-plus/icons-vue'
import { useFeaturesStore } from '@/stores/features'

// ==================== localStorage 工具 ====================
const loadFromStorage = (key, defaultValue) => {
  try {
    const stored = localStorage.getItem(key)
    return stored ? JSON.parse(stored) : defaultValue
  } catch {
    return defaultValue
  }
}

const saveToStorage = (key, value) => {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (e) {
    console.warn('保存到 localStorage 失败:', e)
  }
}

// ==================== Store ====================
const featuresStore = useFeaturesStore()

// ==================== 响应式数据 ====================
// 查询表单
const queryForm = ref({
  entityType: 'user_id',
  entityId: '',
  featureNames: []
})

// 查询状态
const querying = ref(false)
const queryResult = ref(null)
const selectedFeatureView = ref('')

// 可用特征列表
const availableFeatures = ref([])

// 常用实体类型
const commonEntityTypes = ref([
  { value: 'user_id', label: '用户ID' },
  { value: 'item_id', label: '商品ID' },
  { value: 'shop_id', label: '店铺ID' },
  { value: 'session_id', label: '会话ID' }
])

// 查询历史（localStorage 持久化）
const QUERY_HISTORY_KEY = 'mmodelx_query_history'
const queryHistory = ref(loadFromStorage(QUERY_HISTORY_KEY, []))
const showHistory = ref(false)

// 收藏夹（localStorage 持久化）
const BOOKMARKS_KEY = 'mmodelx_bookmarks'
const bookmarks = ref(loadFromStorage(BOOKMARKS_KEY, []))
const showBookmarkDialog = ref(false)
const showSaveBookmarkDialog = ref(false)
const bookmarkForm = ref({
  name: ''
})

// ==================== 计算属性 ====================
/**
 * 激活的特征视图
 */
const activeViews = computed(() => {
  return featuresStore.activeViews
})

/**
 * 是否可以查询
 */
const canQuery = computed(() => {
  return queryForm.value.entityType &&
         queryForm.value.entityId &&
         queryForm.value.featureNames.length > 0
})

/**
 * 特征表格数据
 */
const featureTableData = computed(() => {
  if (!queryResult.value) return []

  return Object.entries(queryResult.value.features).map(([name, value]) => {
    // 查找特征元数据
    const feature = findFeatureMetadata(name)

    return {
      name,
      value,
      dtype: feature?.dtype || inferDataType(value),
      description: feature?.description || ''
    }
  })
})

// ==================== 方法 ====================
/**
 * 处理特征视图变化
 */
const handleViewChange = (viewName) => {
  queryForm.value.featureNames = []

  if (!viewName) {
    loadAllFeatures()
    return
  }

  const view = activeViews.value.find(v => v.name === viewName)
  if (view && view.features) {
    availableFeatures.value = view.features.map(f => ({
      key: f.name,
      label: f.name,
      dtype: f.dtype,
      disabled: false
    }))
  }
}

/**
 * 加载所有特征（跨所有视图）
 */
const loadAllFeatures = () => {
  const allFeatures = []
  const seen = new Set()
  for (const view of activeViews.value) {
    if (view.features) {
      for (const f of view.features) {
        if (!seen.has(f.name)) {
          seen.add(f.name)
          allFeatures.push({
            key: f.name,
            label: f.name,
            dtype: f.dtype,
            disabled: false
          })
        }
      }
    }
  }
  availableFeatures.value = allFeatures
}

/**
 * 查找特征元数据
 */
const findFeatureMetadata = (featureName) => {
  for (const view of activeViews.value) {
    if (view.features) {
      const feature = view.features.find(f => f.name === featureName)
      if (feature) return feature
    }
  }
  return null
}

/**
 * 推断数据类型
 */
const inferDataType = (value) => {
  if (value === null) return 'NULL'
  if (typeof value === 'boolean') return 'BOOLEAN'
  if (typeof value === 'number') return Number.isInteger(value) ? 'INT64' : 'FLOAT64'
  if (Array.isArray(value)) return 'ARRAY'
  if (typeof value === 'object') return 'MAP'
  return 'STRING'
}

/**
 * 执行查询
 */
const handleQuery = async () => {
  if (!canQuery.value) {
    ElMessage.warning('请填写完整的查询参数')
    return
  }

  try {
    querying.value = true

    const startTime = Date.now()

    // 调用API查询在线特征
    const features = await featuresStore.fetchOnlineFeatures({
      entityType: queryForm.value.entityType,
      entityId: queryForm.value.entityId,
      featureNames: queryForm.value.featureNames.join(',')
    })

    const elapsedTime = Date.now() - startTime

    // 保存查询结果
    queryResult.value = {
      entityType: queryForm.value.entityType,
      entityId: queryForm.value.entityId,
      features,
      fromCache: elapsedTime < 50, // 假设小于50ms来自缓存
      elapsedTime,
      timestamp: Date.now()
    }

    // 添加到查询历史
    addToHistory()

    ElMessage.success(`查询成功，获取到 ${Object.keys(features).length} 个特征`)
  } catch (error) {
    ElMessage.error('查询失败: ' + error.message)
    queryResult.value = null
  } finally {
    querying.value = false
  }
}

/**
 * 添加到查询历史
 */
const addToHistory = () => {
  queryHistory.value.unshift({
    entityType: queryForm.value.entityType,
    entityId: queryForm.value.entityId,
    featureNames: [...queryForm.value.featureNames],
    timestamp: Date.now()
  })

  // 最多保留50条历史
  if (queryHistory.value.length > 50) {
    queryHistory.value = queryHistory.value.slice(0, 50)
  }

  saveToStorage(QUERY_HISTORY_KEY, queryHistory.value)
}

/**
 * 从历史恢复
 */
const restoreFromHistory = (item) => {
  queryForm.value.entityType = item.entityType
  queryForm.value.entityId = item.entityId
  queryForm.value.featureNames = [...item.featureNames]

  showHistory.value = false
  ElMessage.success('已从历史恢复查询条件')
}

/**
 * 从收藏夹恢复
 */
const restoreFromBookmark = (bookmark) => {
  queryForm.value.entityType = bookmark.entityType
  queryForm.value.entityId = ''
  queryForm.value.featureNames = [...bookmark.featureNames]

  showBookmarkDialog.value = false
  ElMessage.success('已从收藏夹恢复查询条件')
}

/**
 * 删除历史记录
 */
const deleteHistoryItem = (index) => {
  queryHistory.value.splice(index, 1)
  saveToStorage(QUERY_HISTORY_KEY, queryHistory.value)
}

/**
 * 清空历史
 */
const clearHistory = () => {
  queryHistory.value = []
  saveToStorage(QUERY_HISTORY_KEY, queryHistory.value)
  ElMessage.success('查询历史已清空')
}

/**
 * 保存为收藏
 */
const saveAsBookmark = () => {
  if (!canQuery.value) {
    ElMessage.warning('请先配置查询条件')
    return
  }

  bookmarkForm.value.name = `${queryForm.value.entityType} 特征查询`
  showSaveBookmarkDialog.value = true
}

/**
 * 确认保存收藏
 */
const confirmSaveBookmark = () => {
  if (!bookmarkForm.value.name) {
    ElMessage.warning('请输入收藏名称')
    return
  }

  bookmarks.value.push({
    name: bookmarkForm.value.name,
    entityType: queryForm.value.entityType,
    featureNames: [...queryForm.value.featureNames]
  })

  saveToStorage(BOOKMARKS_KEY, bookmarks.value)
  showSaveBookmarkDialog.value = false
  ElMessage.success('已保存到收藏夹')
}

/**
 * 删除收藏
 */
const deleteBookmark = (index) => {
  bookmarks.value.splice(index, 1)
  saveToStorage(BOOKMARKS_KEY, bookmarks.value)
}

/**
 * 全选特征
 */
const selectAllFeatures = () => {
  queryForm.value.featureNames = availableFeatures.value.map(f => f.key)
}

/**
 * 清空特征选择
 */
const clearAllFeatures = () => {
  queryForm.value.featureNames = []
}

/**
 * 移除单个已选特征
 */
const removeFeature = (featureName) => {
  const idx = queryForm.value.featureNames.indexOf(featureName)
  if (idx !== -1) {
    queryForm.value.featureNames.splice(idx, 1)
  }
}

/**
 * 复制特征值
 */
const copyFeatureValue = (row) => {
  const value = typeof row.value === 'object' ? JSON.stringify(row.value) : String(row.value)

  navigator.clipboard.writeText(value).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

/**
 * 导出结果
 */
const exportResult = () => {
  if (!queryResult.value) return

  const data = {
    entity: `${queryResult.value.entityType}:${queryResult.value.entityId}`,
    queryTime: formatDateTime(queryResult.value.timestamp),
    features: queryResult.value.features
  }

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = `feature-${queryForm.value.entityType}-${queryForm.value.entityId}-${Date.now()}.json`
  link.click()

  URL.revokeObjectURL(url)
  ElMessage.success('结果已导出')
}

/**
 * 获取数据类型标签类型
 */
const getDataTypeTagType = (dtype) => {
  const types = {
    INT64: '',
    FLOAT64: 'success',
    STRING: 'warning',
    BOOLEAN: 'info',
    ARRAY: 'danger',
    MAP: 'danger'
  }
  return types[dtype] || 'info'
}

/**
 * 获取特征图标
 */
const getFeatureIcon = (dtype) => {
  const icons = {
    INT64: TrendCharts,
    FLOAT64: TrendCharts,
    STRING: Calendar,
    BOOLEAN: TrendCharts,
    ARRAY: TrendCharts,
    MAP: TrendCharts
  }
  return icons[dtype] || TrendCharts
}

/**
 * 获取特征颜色
 */
const getFeatureColor = (dtype) => {
  const colors = {
    INT64: '#409eff',
    FLOAT64: '#67c23a',
    STRING: '#e6a23c',
    BOOLEAN: '#909399',
    ARRAY: '#f56c6c',
    MAP: '#f56c6c'
  }
  return colors[dtype] || '#909399'
}

/**
 * 格式化数字
 */
const formatNumber = (value) => {
  if (Number.isInteger(value)) {
    return value.toLocaleString()
  }
  return value.toFixed(2)
}

/**
 * 格式化日期时间
 */
const formatDateTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await featuresStore.fetchViews()
  loadAllFeatures()
})
</script>

<style scoped lang="scss">
.online-feature-query {
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.query-btn-item {
  display: flex;
  align-items: flex-end;
}

.feature-select-item {
  :deep(.el-form-item__label) {
    align-self: flex-start;
    padding-top: 8px;
  }
}

.feature-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .selector-top {
    display: flex;
    gap: 20px;
    align-items: center;

    .top-left {
      flex: 1;
      min-width: 0;
    }

    .top-right {
      width: 280px;
      flex-shrink: 0;
    }

    .section-title {
      font-weight: 600;
      color: $text-primary;
      font-size: 14px;
    }
  }

  .selector-bottom {
    display: flex;
    gap: 20px;
    align-items: stretch;
    min-height: 380px;

    .selector-left {
      flex: 1;
      min-width: 0;

      .feature-transfer {
        height: 100%;

        :deep(.el-transfer-panel) {
          width: 42%;
        }

        :deep(.el-transfer__buttons) {
          width: 16%;
          padding: 0 8px;
        }

        :deep(.el-transfer) {
          height: 100%;

          .el-transfer-panel {
            height: 100%;
          }
        }
      }
    }

    .selector-right {
      width: 280px;
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
    }
  }
}

.view-option {
  display: flex;
  align-items: center;
  gap: 8px;

  .name {
    font-weight: 500;
  }

  .count {
    color: $text-muted;
    font-size: 12px;
  }
}

.transfer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;

  .feature-name {
    flex: 1;
  }
}

.quick-select {
  padding: 16px;
  background: $bg-gray;
  border-radius: $radius-sm;
  height: 100%;
  display: flex;
  flex-direction: column;

  .feature-group {
    margin-bottom: 16px;
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;

    .group-title {
      font-size: 13px;
      color: $text-secondary;
      margin-bottom: 8px;
      display: flex;
      align-items: center;
      gap: 6px;
    }
  }

  .selected-features-list {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    overflow-y: auto;
    padding: 4px;
    flex: 1;
    min-height: 0;
    align-content: flex-start;

    .no-selection {
      color: $text-muted;
      font-size: 13px;
      padding: 8px 0;
    }
  }

  .select-actions {
    display: flex;
    gap: 8px;
    padding-top: 12px;
    border-top: 1px solid $border-color;
    margin-top: auto;
    flex-shrink: 0;
  }
}

.result-card {
  margin-top: 20px;
}

.result-stats {
  margin-bottom: 16px;

  .stat-card-mini {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    background: $bg-white;
    border: 1px solid $border-color;
    border-radius: $radius-md;
    transition: all 0.3s;

    &:hover {
      box-shadow: $shadow-hover;
      transform: translateY(-1px);
    }

    .stat-icon {
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 10px;
      flex-shrink: 0;
    }

    .stat-info {
      min-width: 0;

      .stat-label {
        font-size: 12px;
        color: $text-muted;
        margin-bottom: 4px;
      }

      .stat-value {
        font-size: 14px;
        font-weight: 600;
        color: $text-primary;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;

        &.highlight {
          color: #409eff;
          font-size: 18px;
        }
      }
    }
  }
}

.feature-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.null-value {
  color: $text-placeholder;
  font-style: italic;
}

.boolean-value,
.string-value,
.number-value {
  font-weight: 500;
}

.array-value {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;

  .more-hint {
    color: $text-muted;
    font-size: 12px;
  }
}

.json-view {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: $radius-sm;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
}

.empty-card {
  margin-top: 20px;
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-list,
.bookmark-list {
  max-height: 500px;
  overflow-y: auto;
}

.history-item,
.bookmark-item {
  padding: 16px;
  margin-bottom: 12px;
  background: $bg-gray;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: #e6e8eb;
  }
}

.item-header,
.bookmark-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.bookmark-actions {
  display: flex;
  gap: 8px;
}

.entity-info {
  display: flex;
  align-items: center;
  gap: 8px;

  .entity-id {
    font-weight: 600;
    color: $text-primary;
  }
}

.item-body,
.bookmark-body {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: $text-secondary;

  .feature-count {
    color: #409eff;
    font-weight: 500;
  }
}

.bookmark-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: $text-primary;
}

.bookmark-info {
  display: flex;
  align-items: center;
  gap: 8px;

  .label {
    color: $text-secondary;
  }
}

.bookmark-features {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
}

.history-empty,
.bookmark-empty {
  padding: 40px 0;
}

// 响应式
@media (max-width: 1200px) {
  .feature-selector {
    .selector-top {
      flex-direction: column;
      gap: 12px;

      .top-right {
        width: 100%;
      }
    }

    .selector-bottom {
      flex-direction: column;
      min-height: auto;

      .selector-right {
        width: 100%;
      }
    }
  }
}

@media (max-width: 768px) {
  .online-feature-query {
    padding: 16px;
  }

  .result-stats {
    flex-direction: column;
    gap: 12px;
  }

  .quick-select {
    .checkbox-list {
      max-height: 200px;
    }
  }
}
</style>
