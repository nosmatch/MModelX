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
            <el-form-item label=" ">
              <el-button
                type="primary"
                :icon="Search"
                :loading="querying"
                :disabled="!canQuery"
                @click="handleQuery"
                style="width: 100%"
              >
                查询特征
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 特征选择 -->
        <el-form-item label="选择特征">
          <div class="feature-selector">
            <div class="selector-left">
              <el-select
                v-model="selectedFeatureView"
                placeholder="选择特征视图（可选）"
                clearable
                style="width: 100%; margin-bottom: 12px"
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

              <el-transfer
                v-model="queryForm.featureNames"
                :data="availableFeatures"
                :titles="['可选特征', '已选特征']"
                filterable
                filter-placeholder="搜索特征"
                style="width: 100%"
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
                <div class="section-title">快捷选择</div>

                <!-- 常用特征 -->
                <div class="feature-group">
                  <div class="group-title">常用特征</div>
                  <el-checkbox-group v-model="queryForm.featureNames" class="checkbox-list">
                    <el-checkbox
                      v-for="feature in commonFeatures"
                      :key="feature.name"
                      :value="feature.name"
                    >
                      <span class="checkbox-label">{{ feature.label }}</span>
                      <el-tag size="small" type="info">{{ feature.dtype }}</el-tag>
                    </el-checkbox>
                  </el-checkbox-group>
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
      <div class="result-stats">
        <div class="stat-item">
          <span class="label">实体：</span>
          <span class="value">{{ queryResult.entityType }}:{{ queryResult.entityId }}</span>
        </div>
        <div class="stat-item">
          <span class="label">特征数量：</span>
          <span class="value highlight">{{ Object.keys(queryResult.features).length }}</span>
        </div>
        <div class="stat-item">
          <span class="label">查询时间：</span>
          <span class="value">{{ formatDateTime(queryResult.timestamp) }}</span>
        </div>
      </div>

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
        >
          <div class="bookmark-header">
            <div class="bookmark-name">
              <el-icon class="icon" color="#f5a623"><Star /></el-icon>
              {{ bookmark.name }}
            </div>
            <el-button
              size="small"
              type="danger"
              :icon="Delete"
              @click="deleteBookmark(index)"
            >
              删除
            </el-button>
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
import { ref, computed } from 'vue'
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
  Calendar
} from '@element-plus/icons-vue'
import { useFeaturesStore } from '@/stores/features'

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

// 常用特征
const commonFeatures = ref([
  { name: 'total_orders', label: '总订单数', dtype: 'INT64' },
  { name: 'avg_order_value', label: '平均订单金额', dtype: 'FLOAT64' },
  { name: 'last_order_days', label: '最后订单天数', dtype: 'INT64' },
  { name: 'is_active', label: '是否活跃', dtype: 'BOOLEAN' },
  { name: 'user_level', label: '用户等级', dtype: 'STRING' }
])

// 常用实体类型
const commonEntityTypes = ref([
  { value: 'user_id', label: '用户ID' },
  { value: 'item_id', label: '商品ID' },
  { value: 'shop_id', label: '店铺ID' },
  { value: 'session_id', label: '会话ID' }
])

// 查询历史
const queryHistory = ref([])
const showHistory = ref(false)

// 收藏夹
const bookmarks = ref([
  {
    name: '用户基础特征',
    entityType: 'user_id',
    featureNames: ['total_orders', 'avg_order_value', 'last_order_days']
  }
])
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
  if (!viewName) {
    availableFeatures.value = []
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

    // 模拟数据（用于演示）
    queryResult.value = {
      entityType: queryForm.value.entityType,
      entityId: queryForm.value.entityId,
      features: {
        total_orders: 42,
        avg_order_value: 158.5,
        last_order_days: 3,
        is_active: true,
        user_level: 'VIP',
        favorite_categories: ['electronics', 'books'],
        registration_date: '2023-01-15'
      },
      fromCache: false,
      elapsedTime: 125,
      timestamp: Date.now()
    }
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
 * 删除历史记录
 */
const deleteHistoryItem = (index) => {
  queryHistory.value.splice(index, 1)
}

/**
 * 清空历史
 */
const clearHistory = () => {
  queryHistory.value = []
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

  showSaveBookmarkDialog.value = false
  ElMessage.success('已保存到收藏夹')
}

/**
 * 删除收藏
 */
const deleteBookmark = (index) => {
  bookmarks.value.splice(index, 1)
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

.feature-selector {
  display: flex;
  gap: 20px;

  .selector-left {
    flex: 1;
  }

  .selector-right {
    width: 280px;
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
    color: #909399;
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
  background: #f5f7fa;
  border-radius: 4px;

  .section-title {
    font-weight: 600;
    color: #303133;
    margin-bottom: 12px;
  }

  .feature-group {
    margin-bottom: 16px;

    .group-title {
      font-size: 13px;
      color: #606266;
      margin-bottom: 8px;
    }
  }

  .checkbox-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 300px;
    overflow-y: auto;

    :deep(.el-checkbox) {
      margin-right: 0;
      white-space: nowrap;
    }

    .checkbox-label {
      margin-right: 8px;
    }
  }

  .select-actions {
    display: flex;
    gap: 8px;
    padding-top: 12px;
    border-top: 1px solid #dcdfe6;
  }
}

.result-card {
  margin-top: 20px;
}

.result-stats {
  display: flex;
  gap: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;

  .stat-item {
    display: flex;
    align-items: center;

    .label {
      color: #606266;
      font-size: 14px;
    }

    .value {
      margin-left: 8px;
      font-weight: 600;
      color: #303133;

      &.highlight {
        color: #409eff;
        font-size: 16px;
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
  color: #c0c4cc;
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
    color: #909399;
    font-size: 12px;
  }
}

.json-view {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
}

.empty-card {
  margin-top: 20px;
  min-height: 400px;
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
  background: #f5f7fa;
  border-radius: 4px;
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

.entity-info {
  display: flex;
  align-items: center;
  gap: 8px;

  .entity-id {
    font-weight: 600;
    color: #303133;
  }
}

.item-body,
.bookmark-body {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #606266;

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
  color: #303133;
}

.bookmark-info {
  display: flex;
  align-items: center;
  gap: 8px;

  .label {
    color: #606266;
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
    flex-direction: column;

    .selector-right {
      width: 100%;
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
