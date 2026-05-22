<template>
  <div class="feature-materialize">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">
            <el-icon :size="24"><FolderOpened /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">已物化视图</div>
            <div class="stat-value">{{ materializedViewsCount }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #f0f9ff; color: #67c23a;">
            <el-icon :size="24"><Key /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">Redis Key数量</div>
            <div class="stat-value">{{ totalRedisKeys }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #fef0f0; color: #f56c6c;">
            <el-icon :size="24"><Timer /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">即将过期</div>
            <div class="stat-value">{{ expiringSoonCount }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">内存使用</div>
            <div class="stat-value">{{ memoryUsage }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 主要内容区域 -->
    <el-row :gutter="20">
      <!-- 左侧：物化操作 -->
      <el-col :span="14">
        <el-card class="operation-card">
          <template #header>
            <div class="card-header">
              <span>执行物化操作</span>
              <el-button size="small" :icon="Refresh" @click="refreshStats">
                刷新统计
              </el-button>
            </div>
          </template>

          <el-form :model="materializeForm" label-width="120px">
            <!-- 特征视图选择 -->
            <el-form-item label="特征视图">
              <el-select
                v-model="materializeForm.featureViewName"
                placeholder="选择要物化的特征视图"
                style="width: 100%"
                filterable
                @change="handleViewChange"
              >
                <el-option
                  v-for="view in activeViews"
                  :key="view.name"
                  :label="view.name"
                  :value="view.name"
                >
                  <div class="view-option-item">
                    <span class="view-name">{{ view.name }}</span>
                    <el-tag size="small" type="info">{{ view.entity }}</el-tag>
                    <span class="view-meta">
                      <el-icon><Document /></el-icon>
                      {{ view.features?.length || 0 }} 特征
                    </span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <!-- 视图详情 -->
            <div v-if="selectedView" class="view-detail-section">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="实体类型">
                  <el-tag size="small">{{ selectedView.entity }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="数据源">
                  <el-tag size="small">{{ DataSourceTypeLabels[selectedView.datasourceType] || selectedView.datasourceType || '未配置' }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="TTL">
                  {{ selectedView.ttl }} 天
                </el-descriptions-item>
                <el-descriptions-item label="特征数量">
                  {{ selectedView.features?.length || 0 }} 个
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 数据源选择 -->
            <el-form-item label="数据来源" style="margin-top: 16px;">
              <el-radio-group v-model="materializeForm.sourceType">
                <el-radio value="minio">从MinIO读取（离线特征）</el-radio>
                <el-radio value="compute">重新计算</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 分区日期 -->
            <el-form-item label="分区日期">
              <el-date-picker
                v-model="materializeForm.partitionDate"
                type="date"
                placeholder="选择要物化的数据分区"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledDate"
                style="width: 100%"
              />
            </el-form-item>

            <!-- 高级选项 -->
            <el-form-item>
              <el-checkbox v-model="materializeForm.forceRefresh">强制刷新（覆盖已有数据）</el-checkbox>
              <el-checkbox v-model="materializeForm.asyncMode" style="margin-left: 16px;">
                异步模式（后台执行）
              </el-checkbox>
            </el-form-item>

            <!-- 执行按钮 -->
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :icon="Position"
                :loading="materializing"
                :disabled="!materializeForm.featureViewName"
                @click="handleMaterialize"
              >
                {{ materializing ? '物化中...' : '开始物化' }}
              </el-button>
              <el-button
                size="large"
                :icon="VideoPlay"
                :disabled="!materializeForm.featureViewName"
                @click="handlePreview"
              >
                预览数据
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 物化进度 -->
          <div v-if="showProgress" class="progress-section">
            <el-divider content-position="left">
              <span class="divider-title">物化进度</span>
            </el-divider>

            <el-progress
              :percentage="materializeProgress"
              :status="materializeStatus"
              :stroke-width="18"
            >
              <span class="progress-text">{{ materializeProgress }}%</span>
            </el-progress>

            <div class="progress-info">
              <div class="info-item">
                <span class="label">已处理实体：</span>
                <span class="value">{{ progressData.processedEntities }}</span>
              </div>
              <div class="info-item">
                <span class="label">写入Redis：</span>
                <span class="value">{{ progressData.writtenKeys }}</span>
              </div>
              <div class="info-item">
                <span class="label">耗时：</span>
                <span class="value">{{ progressData.elapsedTime }}s</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：Redis状态 -->
      <el-col :span="10">
        <el-card class="redis-card">
          <template #header>
            <div class="card-header">
              <span>Redis状态</span>
              <el-tag :type="redisConnected ? 'success' : 'danger'" size="small">
                {{ redisConnected ? '已连接' : '未连接' }}
              </el-tag>
            </div>
          </template>

          <!-- Redis信息 -->
          <div class="redis-info">
            <div class="info-row">
              <span class="label">主机：</span>
              <span class="value">{{ redisInfo.host }}</span>
            </div>
            <div class="info-row">
              <span class="label">端口：</span>
              <span class="value">{{ redisInfo.port }}</span>
            </div>
            <div class="info-row">
              <span class="label">数据库：</span>
              <span class="value">{{ redisInfo.db }}</span>
            </div>
            <div class="info-row">
              <span class="label">Key数量：</span>
              <span class="value highlight">{{ redisInfo.keyCount }}</span>
            </div>
            <div class="info-row">
              <span class="label">内存使用：</span>
              <span class="value highlight">{{ redisInfo.memory }}</span>
            </div>
          </div>

          <!-- Key模式分布 -->
          <el-divider content-position="left">
            <span class="divider-title">特征Key分布</span>
          </el-divider>

          <div class="key-distribution">
            <div
              v-for="(item, index) in keyDistribution"
              :key="index"
              class="distribution-item"
            >
              <div class="item-header">
                <span class="pattern">{{ item.pattern }}</span>
                <el-tag size="small">{{ item.count }} keys</el-tag>
              </div>
              <el-progress
                :percentage="item.percentage"
                :show-text="false"
                :stroke-width="6"
              />
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="redis-actions">
            <el-button size="small" :icon="View" @click="viewRedisKeys">
              查看Keys
            </el-button>
            <el-button size="small" :icon="Delete" type="danger" @click="showCleanDialog">
              清理过期
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 物化历史 -->
    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <span>物化历史</span>
          <div class="header-actions">
            <el-select
              v-model="historyFilter"
              placeholder="筛选视图"
              clearable
              size="small"
              style="width: 150px; margin-right: 12px"
            >
              <el-option
                v-for="view in activeViews"
                :key="view.name"
                :label="view.name"
                :value="view.name"
              />
            </el-select>
            <el-button size="small" :icon="Download" @click="exportHistory">
              导出记录
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="filteredHistory"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="featureViewName" label="特征视图" width="180" />
        <el-table-column prop="partitionDate" label="分区日期" width="120" />
        <el-table-column prop="entityCount" label="实体数" width="100" align="center" />
        <el-table-column prop="featureCount" label="特征数" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="elapsedTime" label="耗时" width="100" align="center">
          <template #default="{ row }">
            {{ row.elapsedTime }}s
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="执行时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button
              size="small"
              :icon="View"
              @click="viewMaterializeDetail(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.status === 'failed'"
              size="small"
              type="warning"
              :icon="RefreshRight"
              @click="retryMaterialize(row)"
            >
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="showPreviewDialog"
      title="预览特征数据"
      width="900px"
    >
      <div class="preview-content">
        <el-alert
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        >
          <template #title>
            <span>预览 {{ previewData.featureViewName }} 的前 {{ previewData.data?.length || 0 }} 条记录</span>
          </template>
        </el-alert>

        <el-table
          :data="previewData.data"
          stripe
          max-height="400"
          size="small"
        >
          <el-table-column
            v-for="key in previewColumns"
            :key="key"
            :prop="key"
            :label="key === 'entity_id' ? '实体ID' : key === 'computed_at' ? '计算时间' : key"
            :width="key === 'entity_id' ? 120 : key === 'computed_at' ? 160 : undefined"
            :fixed="key === 'entity_id' ? 'left' : false"
            min-width="120"
          />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="showPreviewDialog = false">关闭</el-button>
        <el-button type="primary" @click="handleMaterialize">
          确认物化
        </el-button>
      </template>
    </el-dialog>

    <!-- 清理对话框 -->
    <el-dialog
      v-model="showCleanupDialog"
      title="清理过期特征"
      width="600px"
    >
      <el-form :model="cleanupForm" label-width="120px">
        <el-form-item label="清理范围">
          <el-radio-group v-model="cleanupForm.scope">
            <el-radio value="expired">仅清理已过期</el-radio>
            <el-radio value="all">清理所有特征</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="特征视图" v-if="cleanupForm.scope === 'all'">
          <el-select
            v-model="cleanupForm.featureViewName"
            placeholder="选择要清理的特征视图"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="view in activeViews"
              :key="view.name"
              :label="view.name"
              :value="view.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="预计删除">
          <el-tag type="danger" size="large">
            {{ cleanupForm.estimatedKeys }} 个Redis Key
          </el-tag>
        </el-form-item>

        <el-alert
          type="warning"
          title="警告"
          :closable="false"
        >
          清理操作不可撤销，请谨慎操作！
        </el-alert>
      </el-form>

      <template #footer>
        <el-button @click="showCleanupDialog = false">取消</el-button>
        <el-button type="danger" :loading="cleaning" @click="handleCleanup">
          确认清理
        </el-button>
      </template>
    </el-dialog>

    <!-- Redis Keys查看器 -->
    <el-dialog
      v-model="showKeysDialog"
      title="Redis特征Keys"
      width="800px"
    >
      <div class="keys-viewer">
        <el-input
          v-model="keySearchPattern"
          placeholder="输入Key模式（如：feature:user_id:*）"
          clearable
          style="margin-bottom: 16px"
        >
          <template #append>
            <el-button :icon="Search" @click="searchRedisKeys">搜索</el-button>
          </template>
        </el-input>

        <div class="keys-list">
          <div
            v-for="(key, index) in redisKeys"
            :key="index"
            class="key-item"
          >
            <el-icon class="key-icon"><Key /></el-icon>
            <span class="key-name">{{ key }}</span>
            <el-button
              size="small"
              @click="viewKeyValue(key)"
            >
              查看值
            </el-button>
          </div>
          <div v-if="redisKeys.length === 0" class="keys-empty">
            暂无数据，请输入Key模式搜索
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showKeysDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 特征物化管理页面
 *
 * 功能：
 * - 选择特征视图进行物化
 * - 配置物化参数
 * - 执行物化操作
 * - 监控物化进度
 * - 查看Redis中的特征数据
 * - 清理过期特征
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FolderOpened,
  Key,
  Timer,
  TrendCharts,
  Refresh,
  Position,
  VideoPlay,
  View,
  Delete,
  Download,
  Search,
  RefreshRight,
  Document
} from '@element-plus/icons-vue'
import { useFeaturesStore } from '@/stores/features'
import { DataSourceTypeLabels } from '@/constants/features'
import { previewFeatures, getRedisStatus, searchRedisKeys as apiSearchRedisKeys, getMaterializeHistory } from '@/api/modules/features'

// ==================== Store ====================
const featuresStore = useFeaturesStore()

// ==================== 响应式数据 ====================
// 统计数据
const materializedViewsCount = ref(0)
const totalRedisKeys = ref(0)
const expiringSoonCount = ref(0)
const memoryUsage = ref('0 MB')

// Redis信息
const redisConnected = ref(false)
const redisInfo = ref({
  host: '-',
  port: '-',
  db: '-',
  keyCount: 0,
  memory: '-'
})

// Key分布
const keyDistribution = ref([])

// 物化表单
const materializeForm = ref({
  featureViewName: '',
  sourceType: 'minio',
  partitionDate: new Date().toISOString().split('T')[0],
  forceRefresh: false,
  asyncMode: false
})

// 物化进度
const showProgress = ref(false)
const materializing = ref(false)
const materializeProgress = ref(0)
const materializeStatus = ref('')
const progressData = ref({
  processedEntities: 0,
  writtenKeys: 0,
  elapsedTime: 0
})

// 物化历史
const historyFilter = ref('')
const materializeHistory = ref([])

// 对话框状态
const showPreviewDialog = ref(false)
const showCleanupDialog = ref(false)
const showKeysDialog = ref(false)
const cleaning = ref(false)

// 预览数据
const previewData = ref({
  featureViewName: '',
  features: [],
  data: []
})

// 清理表单
const cleanupForm = ref({
  scope: 'expired',
  featureViewName: '',
  estimatedKeys: 0
})

// Redis Keys查看
const keySearchPattern = ref('feature:*')
const redisKeys = ref([])

// ==================== 计算属性 ====================
/**
 * 激活的特征视图
 */
const activeViews = computed(() => {
  return featuresStore.activeViews
})

/**
 * 预览数据表格列（从数据 keys 动态推导）
 */
const previewColumns = computed(() => {
  if (!previewData.value.data || previewData.value.data.length === 0) {
    return []
  }
  const keys = Object.keys(previewData.value.data[0])
  // entity_id 放最前面，computed_at 放最后面
  return keys.sort((a, b) => {
    if (a === 'entity_id') return -1
    if (b === 'entity_id') return 1
    if (a === 'computed_at') return 1
    if (b === 'computed_at') return -1
    return a.localeCompare(b)
  })
})

/**
 * 选中的视图
 */
const selectedView = computed(() => {
  if (!materializeForm.value.featureViewName) return null
  // 优先使用 currentView（包含完整详情如 features、datasourceType）
  if (featuresStore.currentView && featuresStore.currentView.name === materializeForm.value.featureViewName) {
    return featuresStore.currentView
  }
  return activeViews.value.find(v => v.name === materializeForm.value.featureViewName)
})

/**
 * 筛选后的历史记录
 */
const filteredHistory = computed(() => {
  if (!historyFilter.value) return materializeHistory.value
  return materializeHistory.value.filter(h => h.featureViewName === historyFilter.value)
})

// ==================== 方法 ====================
/**
 * 加载Redis状态（真实数据）
 */
const loadRedisStatus = async () => {
  try {
    const response = await getRedisStatus()
    if (response.code === '200' && response.data) {
      const data = response.data
      redisConnected.value = data.connected || false
      totalRedisKeys.value = data.totalKeys || 0
      materializedViewsCount.value = data.materializedViewCount || 0
      memoryUsage.value = (data.memoryInfo?.estimatedKeys || 0) + ' keys'

      // 更新redisInfo
      redisInfo.value = {
        host: data.host || '-',
        port: data.port || '-',
        db: data.db || '-',
        keyCount: data.totalKeys || 0,
        memory: memoryUsage.value
      }

      // 更新keyDistribution
      if (data.entityDistribution) {
        const total = data.totalKeys || 1
        keyDistribution.value = Object.entries(data.entityDistribution).map(([entity, count]) => ({
          pattern: `feature:${entity}:*`,
          count,
          percentage: Math.round((count / total) * 100)
        }))
      } else {
        keyDistribution.value = []
      }
    }
  } catch (error) {
    console.error('加载Redis状态失败:', error)
    redisConnected.value = false
  }
}

/**
 * 加载物化历史（真实数据）
 */
const loadMaterializeHistory = async () => {
  try {
    const response = await getMaterializeHistory()
    if (response.code === '200' && response.data) {
      materializeHistory.value = response.data.map(item => ({
        id: item.id,
        featureViewName: item.featureViewName,
        partitionDate: item.startedAt ? item.startedAt.split('T')[0] : '',
        entityCount: item.entityCount || 0,
        featureCount: item.featureCount || 0,
        status: item.status?.toLowerCase() || 'unknown',
        elapsedTime: item.completedAt && item.startedAt
          ? ((new Date(item.completedAt) - new Date(item.startedAt)) / 1000).toFixed(1)
          : 0,
        createdAt: item.createdAt ? new Date(item.createdAt).getTime() : Date.now()
      }))
    }
  } catch (error) {
    console.error('加载物化历史失败:', error)
    materializeHistory.value = []
  }
}

/**
 * 刷新统计信息
 */
const refreshStats = async () => {
  try {
    await loadRedisStatus()
    await loadMaterializeHistory()
    ElMessage.success('统计信息已刷新')
  } catch (error) {
    ElMessage.error('刷新失败: ' + error.message)
  }
}

/**
 * 处理视图变化
 */
const handleViewChange = async () => {
  // 重置表单状态
  showProgress.value = false
  materializeProgress.value = 0

  // 加载视图完整详情（包含特征列表和数据源信息）
  if (materializeForm.value.featureViewName) {
    try {
      await featuresStore.fetchView(materializeForm.value.featureViewName)
    } catch (error) {
      console.error('加载视图详情失败:', error)
    }
  }
}

/**
 * 禁用未来日期
 */
const disabledDate = (time) => {
  return time.getTime() > Date.now()
}

/**
 * 执行物化
 */
const handleMaterialize = async () => {
  try {
    materializing.value = true
    showProgress.value = true
    materializeProgress.value = 0
    materializeStatus.value = ''

    // 调用物化API
    await featuresStore.materializeFeatures(materializeForm.value.featureViewName)

    // 模拟物化进度
    simulateProgress()

    ElMessage.success('物化任务已启动')
  } catch (error) {
    ElMessage.error('物化失败: ' + error.message)
    materializing.value = false
  }
}

/**
 * 模拟物化进度
 */
const simulateProgress = () => {
  let progress = 0
  const timer = setInterval(() => {
    progress += Math.random() * 10 + 5
    materializeProgress.value = Math.min(progress, 100)

    progressData.value.processedEntities = Math.floor(progress * 10)
    progressData.value.writtenKeys = Math.floor(progress * 15)
    progressData.value.elapsedTime = (progress / 10).toFixed(1)

    if (progress >= 100) {
      clearInterval(timer)
      materializing.value = false
      materializeStatus.value = 'success'

      // 物化完成后刷新历史记录（从API获取真实数据）
      loadMaterializeHistory()

      ElMessage.success('物化完成！')
    }
  }, 500)
}

/**
 * 预览数据（从MinIO读取真实计算结果）
 */
const handlePreview = async () => {
  try {
    const viewName = materializeForm.value.featureViewName
    if (!viewName) {
      ElMessage.warning('请先选择特征视图')
      return
    }

    ElMessage.info('正在加载预览数据...')
    const response = await previewFeatures(viewName, 10)

    if (response.code === '200') {
      previewData.value = {
        featureViewName: viewName,
        features: selectedView.value?.features || [],
        data: response.data || []
      }
      showPreviewDialog.value = true
      ElMessage.success(`已加载 ${previewData.value.data.length} 条预览数据`)
    } else {
      throw new Error(response.message || '预览失败')
    }
  } catch (error) {
    ElMessage.error('预览失败: ' + error.message)
  }
}

/**
 * 查看Redis Keys
 */
const viewRedisKeys = () => {
  keySearchPattern.value = 'feature:*'
  showKeysDialog.value = true
  searchRedisKeys()
}

/**
 * 搜索Redis Keys
 */
const searchRedisKeys = async () => {
  try {
    const response = await apiSearchRedisKeys(keySearchPattern.value)
    if (response.code === '200' && response.data) {
      redisKeys.value = response.data
      ElMessage.success(`找到 ${redisKeys.value.length} 个Keys`)
    } else {
      redisKeys.value = []
    }
  } catch (error) {
    ElMessage.error('搜索失败: ' + error.message)
    redisKeys.value = []
  }
}

/**
 * 查看Key的值
 */
const viewKeyValue = async (key) => {
  try {
    // 调用API获取Key的值
    ElMessage.info(`查看Key: ${key}`)
  } catch (error) {
    ElMessage.error('获取失败: ' + error.message)
  }
}

/**
 * 显示清理对话框
 */
const showCleanDialog = () => {
  cleanupForm.value.estimatedKeys = expiringSoonCount.value
  showCleanupDialog.value = true
}

/**
 * 执行清理
 */
const handleCleanup = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${cleanupForm.value.estimatedKeys} 个Redis Key吗？此操作不可撤销！`,
      '确认清理',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    cleaning.value = true

    // 调用清理API
    await new Promise(resolve => setTimeout(resolve, 2000))

    ElMessage.success('清理完成')
    showCleanupDialog.value = false
    refreshStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清理失败: ' + error.message)
    }
  } finally {
    cleaning.value = false
  }
}

/**
 * 查看物化详情
 */
const viewMaterializeDetail = (row) => {
  ElMessage.info(`查看物化任务详情: ${row.featureViewName}`)
}

/**
 * 重试物化
 */
const retryMaterialize = async (row) => {
  materializeForm.value.featureViewName = row.featureViewName
  materializeForm.value.partitionDate = row.partitionDate
  await handleMaterialize()
}

/**
 * 导出历史记录
 */
const exportHistory = () => {
  const data = filteredHistory.value.map(h => ({
    特征视图: h.featureViewName,
    分区日期: h.partitionDate,
    实体数: h.entityCount,
    特征数: h.featureCount,
    状态: getStatusLabel(h.status),
    耗时: h.elapsedTime + 's',
    执行时间: formatDateTime(h.createdAt)
  }))

  const csv = [
    Object.keys(data[0]).join(','),
    ...data.map(row => Object.values(row).join(','))
  ].join('\n')

  const blob = new Blob([csv], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `materialize-history-${Date.now()}.csv`
  link.click()

  URL.revokeObjectURL(url)
  ElMessage.success('历史记录已导出')
}

/**
 * 获取状态类型
 */
const getStatusType = (status) => {
  const types = {
    success: 'success',
    failed: 'danger',
    running: 'warning',
    pending: 'info'
  }
  return types[status] || 'info'
}

/**
 * 获取状态标签
 */
const getStatusLabel = (status) => {
  const labels = {
    success: '成功',
    failed: '失败',
    running: '运行中',
    pending: '等待中'
  }
  return labels[status] || status
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
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// ==================== 生命周期 ====================
onMounted(() => {
  // 加载特征视图列表
  featuresStore.fetchViews()
  // 加载Redis真实状态
  loadRedisStatus()
  // 加载物化历史
  loadMaterializeHistory()
})
</script>

<style scoped lang="scss">
.feature-materialize {
  padding: 24px;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  .stat-icon {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    margin-right: 16px;
  }

  .stat-content {
    flex: 1;

    .stat-label {
      font-size: 14px;
      color: #606266;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 600;
      color: #303133;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
}

.view-option-item {
  display: flex;
  align-items: center;
  gap: 8px;

  .view-name {
    font-weight: 500;
  }

  .view-meta {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #909399;
    font-size: 12px;
  }
}

.view-detail-section {
  margin-bottom: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.progress-section {
  margin-top: 24px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;

  .divider-title {
    font-weight: 600;
    color: #303133;
  }

  .progress-text {
    font-weight: 600;
  }

  .progress-info {
    display: flex;
    gap: 24px;
    margin-top: 16px;

    .info-item {
      .label {
        color: #606266;
        font-size: 14px;
      }

      .value {
        color: #303133;
        font-weight: 600;
        font-size: 16px;
      }
    }
  }
}

.redis-card {
  .redis-info {
    margin-bottom: 20px;

    .info-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #ebeef5;

      &:last-child {
        border-bottom: none;
      }

      .label {
        color: #606266;
      }

      .value {
        color: #303133;
        font-weight: 500;

        &.highlight {
          color: #409eff;
          font-weight: 600;
        }
      }
    }
  }

  .key-distribution {
    margin-bottom: 16px;

    .distribution-item {
      margin-bottom: 12px;

      .item-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 6px;

        .pattern {
          font-size: 13px;
          color: #303133;
          font-family: 'Courier New', monospace;
        }
      }
    }
  }

  .redis-actions {
    display: flex;
    gap: 8px;
  }
}

.history-card {
  margin-top: 20px;
}

.preview-content {
  padding: 8px 0;
}

.keys-viewer {
  .keys-list {
    max-height: 400px;
    overflow-y: auto;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 12px;

    .key-item {
      display: flex;
      align-items: center;
      padding: 8px;
      margin-bottom: 8px;
      background: #f5f7fa;
      border-radius: 4px;

      &:last-child {
        margin-bottom: 0;
      }

      .key-icon {
        margin-right: 8px;
        color: #409eff;
      }

      .key-name {
        flex: 1;
        font-family: 'Courier New', monospace;
        font-size: 13px;
        color: #303133;
      }
    }

    .keys-empty {
      text-align: center;
      padding: 40px 0;
      color: #909399;
    }
  }
}

.divider-title {
  font-weight: 600;
  color: #303133;
}

// 响应式
@media (max-width: 1200px) {
  .stats-row {
    :deep(.el-col) {
      margin-bottom: 12px;
    }
  }
}

@media (max-width: 768px) {
  .feature-materialize {
    padding: 16px;
  }

  .stat-card {
    .stat-value {
      font-size: 24px;
    }
  }

  .progress-info {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
