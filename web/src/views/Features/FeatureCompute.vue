<template>
  <div class="feature-compute">
    <!-- 计算配置步骤 -->
    <el-steps :active="currentStep" align-center class="steps">
      <el-step title="选择视图" description="选择要计算的特征视图" />
      <el-step title="配置参数" description="配置计算参数" />
      <el-step title="执行计算" description="运行计算任务" />
      <el-step title="完成" description="查看计算结果" />
    </el-steps>

    <!-- 步骤1：选择特征视图 -->
    <div v-show="currentStep === 0" class="step-content">
      <el-card class="config-card">
        <template #header>
          <div class="card-header">
            <span>选择特征视图</span>
          </div>
        </template>

        <el-form :model="computeForm" label-width="120px">
          <el-form-item label="特征视图">
            <el-select
              v-model="computeForm.featureViewName"
              placeholder="请选择特征视图"
              style="width: 100%"
              @change="handleFeatureViewChange"
            >
              <el-option
                v-for="view in activeViews"
                :key="view.name"
                :label="`${view.name} (${view.entity})`"
                :value="view.name"
              >
                <div class="view-option">
                  <span class="view-name">{{ view.name }}</span>
                  <el-tag size="small" type="info">{{ view.entity }}</el-tag>
                  <span class="view-desc">{{ view.description }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="数据源类型">
            <el-tag v-if="selectedView?.datasourceType">{{ DataSourceTypeLabels[selectedView.datasourceType] || selectedView.datasourceType }}</el-tag>
            <span v-else-if="selectedView" class="placeholder">未配置</span>
            <span v-else class="placeholder">请先选择特征视图</span>
          </el-form-item>

          <el-form-item label="特征数量">
            <el-tag v-if="selectedView" type="success">{{ selectedView.features?.length || 0 }} 个特征</el-tag>
            <span v-else class="placeholder">-</span>
          </el-form-item>

          <el-form-item label="视图描述">
            <span v-if="selectedView">{{ selectedView.description || '-' }}</span>
            <span v-else class="placeholder">-</span>
          </el-form-item>
        </el-form>

        <div class="step-actions">
          <el-button type="primary" :disabled="!computeForm.featureViewName" @click="nextStep">
            下一步
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤2：配置参数 -->
    <div v-show="currentStep === 1" class="step-content">
      <el-card class="config-card">
        <template #header>
          <div class="card-header">
            <span>配置计算参数</span>
          </div>
        </template>

        <el-form :model="computeForm" label-width="140px">
          <el-form-item label="分区日期">
            <el-date-picker
              v-model="computeForm.partitionDate"
              type="date"
              placeholder="选择分区日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
            />
            <div class="form-tip">计算指定日期的数据分区</div>
          </el-form-item>

          <el-form-item label="输入路径">
            <el-input
              v-model="computeForm.inputPath"
              placeholder="可选，留空则使用默认路径"
              clearable
            />
            <div class="form-tip">自定义输入数据路径，通常留空</div>
          </el-form-item>

          <el-form-item label="输出路径">
            <el-input
              v-model="computeForm.outputPath"
              placeholder="可选，留空则使用默认路径"
              clearable
            />
            <div class="form-tip">自定义输出数据路径，通常留空</div>
          </el-form-item>

          <el-form-item label="并行度">
            <el-input-number
              v-model="computeForm.parallelism"
              :min="1"
              :max="10"
              :step="1"
            />
            <div class="form-tip">并行计算任务数（1-10）</div>
          </el-form-item>

          <el-form-item label="批量模式">
            <el-switch v-model="computeForm.batchMode" />
            <div class="form-tip">是否批量计算所有特征视图</div>
          </el-form-item>

          <el-form-item v-if="computeForm.batchMode" label="包含视图">
            <el-select
              v-model="computeForm.batchViews"
              multiple
              placeholder="选择要批量计算的视图"
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
        </el-form>

        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="startCompute">开始计算</el-button>
        </div>
      </el-card>

      <!-- 特征预览 -->
      <el-card v-if="selectedView" class="preview-card">
        <template #header>
          <div class="card-header">
            <span>特征预览</span>
            <el-tag size="small">{{ selectedView.features?.length || 0 }} 个特征</el-tag>
          </div>
        </template>

        <el-table
          :data="selectedView.features"
          size="small"
          max-height="300"
        >
          <el-table-column prop="name" label="特征名称" width="180" />
          <el-table-column prop="transformExpr" label="Transform表达式" min-width="200" />
          <el-table-column prop="dtype" label="数据类型" width="100" />
          <el-table-column prop="defaultValue" label="默认值" width="100" />
        </el-table>
      </el-card>
    </div>

    <!-- 步骤3：执行计算 -->
    <div v-show="currentStep === 2" class="step-content">
      <el-alert
        v-if="computeStatus?.status === 'running'"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #title>
          <span>计算任务进行中，请勿关闭或刷新页面</span>
        </template>
      </el-alert>

      <el-card class="compute-card">
        <template #header>
          <div class="card-header">
            <span>计算进度</span>
            <el-tag v-if="computeStatus" :type="getStatusTagType(computeStatus.status)">
              {{ getStatusLabel(computeStatus.status) }}
            </el-tag>
          </div>
        </template>

        <!-- 进度条（同步阻塞 API：仅展示运行/成功/失败 三种状态，不显示伪造的进度数值） -->
        <div class="progress-section">
          <el-progress
            :percentage="computeStatus?.status === 'success' ? 100 : (computeStatus?.status === 'failed' ? 100 : 50)"
            :status="computeStatus?.status === 'failed' ? 'exception' : (computeStatus?.status === 'success' ? 'success' : '')"
            :stroke-width="20"
            :indeterminate="computeStatus?.status === 'running'"
            :duration="3"
          >
            <template #default>
              <span class="progress-text">
                {{ computeStatus?.status === 'running' ? '同步计算中…' :
                   computeStatus?.status === 'success' ? '完成' :
                   computeStatus?.status === 'failed' ? '失败' : '' }}
              </span>
            </template>
          </el-progress>
        </div>

        <!-- 计算时间统计（仅显示真实可观测的耗时） -->
        <div v-if="computeStatus" class="statistics-section">
          <el-row :gutter="16">
            <el-col :span="12">
              <div class="stat-card">
                <div class="stat-label">已耗时</div>
                <div class="stat-value">{{ computeStatus.elapsedTime || 0 }}s</div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="stat-card">
                <div class="stat-label">特征数</div>
                <div class="stat-value">{{ selectedView?.features?.length || 0 }}</div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 操作按钮 -->
        <div class="compute-actions">
          <el-button
            v-if="computeStatus?.status === 'running'"
            type="danger"
            :icon="CircleClose"
            @click="handleCancelCompute"
          >
            取消计算
          </el-button>
          <el-button
            v-if="computeStatus?.status === 'failed'"
            type="warning"
            :icon="RefreshRight"
            @click="retryCompute"
          >
            重试
          </el-button>
          <el-button
            v-if="computeStatus?.status === 'success'"
            type="primary"
            :icon="View"
            @click="viewResults"
          >
            查看结果
          </el-button>
          <el-button
            :icon="Download"
            @click="downloadLogs"
          >
            下载日志
          </el-button>
        </div>
      </el-card>

      <!-- 实时日志 -->
      <el-card class="logs-card">
        <template #header>
          <div class="card-header">
            <span>计算日志</span>
            <div class="header-actions">
              <el-checkbox v-model="autoScroll">自动滚动</el-checkbox>
              <el-button size="small" :icon="Delete" @click="clearLogs">清空</el-button>
            </div>
          </div>
        </template>

        <div ref="logContainer" class="log-container">
          <div
            v-for="(log, index) in computeLogs"
            :key="index"
            :class="['log-line', `log-${log.level}`]"
          >
            <span class="log-time">{{ formatLogTime(log.timestamp) }}</span>
            <span class="log-level">{{ log.level.toUpperCase() }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
          <div v-if="computeLogs.length === 0" class="log-empty">
            <el-empty description="暂无日志">
              <template #image>
                <el-icon :size="48" color="#6b7280">
                  <Document />
                </el-icon>
              </template>
              <template #description>
                <div style="color: #9ca3af; font-size: 13px;">
                  <p>等待日志输出...</p>
                  <p v-if="computeStatus?.status === 'running'" style="margin-top: 8px;">
                    计算任务正在执行中
                  </p>
                </div>
              </template>
            </el-empty>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 步骤4：完成 -->
    <div v-show="currentStep === 3" class="step-content">
      <el-result
        :icon="computeFinalStatus === 'success' ? 'success' : 'error'"
        :title="computeFinalStatus === 'success' ? '计算完成' : '计算失败'"
        :sub-title="computeFinalMessage"
      >
        <template #extra>
          <div class="result-actions">
            <el-button type="primary" :icon="View" @click="viewResults">
              查看结果
            </el-button>
            <el-button :icon="Position" @click="materializeFeatures">
              物化到Redis
            </el-button>
            <el-button :icon="RefreshLeft" @click="resetCompute">
              重新计算
            </el-button>
          </div>

          <!-- 计算摘要（仅显示真实可观测的字段） -->
          <el-card v-if="computeResult" class="result-summary">
            <template #header>
              <span>计算摘要</span>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="特征视图">{{ computeResult.featureViewName }}</el-descriptions-item>
              <el-descriptions-item label="分区日期">{{ computeResult.partitionDate }}</el-descriptions-item>
              <el-descriptions-item label="特征数">{{ computeResult.totalFeatures }}</el-descriptions-item>
              <el-descriptions-item label="计算耗时">{{ computeResult.elapsedTime }}s</el-descriptions-item>
              <el-descriptions-item label="输出路径" :span="2">{{ computeResult.outputPath }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
/**
 * 特征计算页面
 *
 * 功能：
 * - 选择特征视图
 * - 配置计算参数
 * - 启动计算任务
 * - 显示计算进度
 * - 查看计算日志
 * - 计算结果统计
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleClose,
  RefreshRight,
  View,
  Download,
  Delete,
  Position,
  RefreshLeft
} from '@element-plus/icons-vue'
import { useFeaturesStore } from '@/stores/features'
import { DataSourceTypeLabels } from '@/constants/features'

// ==================== 路由和Store ====================
const router = useRouter()
const featuresStore = useFeaturesStore()

// ==================== 响应式数据 ====================
const currentStep = ref(0)
const logContainer = ref(null)
const autoScroll = ref(true)

// 计算表单
const computeForm = ref({
  featureViewName: '',
  partitionDate: new Date().toISOString().split('T')[0], // 默认今天
  inputPath: '',
  outputPath: '',
  parallelism: 1,
  batchMode: false,
  batchViews: []
})

// 计算状态
const computeStatus = ref(null)
const computeLogs = ref([])

const computeFinalStatus = ref('')
const computeFinalMessage = ref('')
const computeResult = ref(null)

// 后端为同步阻塞 API：仅用一个定时器维护"已耗时"显示
let elapsedTimer = null

// ==================== 计算属性 ====================
/**
 * 激活状态的特征视图
 */
const activeViews = computed(() => {
  return featuresStore.activeViews
})

/**
 * 选中的特征视图
 */
const selectedView = computed(() => {
  if (!computeForm.value.featureViewName) return null
  return featuresStore.views.find(v => v.name === computeForm.value.featureViewName)
})

// ==================== 方法 ====================
/**
 * 处理特征视图变化
 */
const handleFeatureViewChange = async () => {
  // 重置状态
  computeLogs.value = []
  computeStatus.value = null

  // 获取视图完整详情（包含特征列表）
  const viewName = computeForm.value.featureViewName
  if (viewName) {
    try {
      await featuresStore.fetchView(viewName)
      // 将完整详情更新到 views 列表中，使 selectedView 能获取到 features
      const index = featuresStore.views.findIndex(v => v.name === viewName)
      if (index !== -1 && featuresStore.currentView) {
        featuresStore.views[index] = featuresStore.currentView
      }
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
 * 下一步
 */
const nextStep = () => {
  if (currentStep.value < 3) {
    currentStep.value++
  }
}

/**
 * 上一步
 */
const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

/**
 * 校验特征视图配置是否完整
 */
const validateViewConfig = () => {
  const view = selectedView.value
  if (!view) return '请先选择特征视图'
  if (!view.features || view.features.length === 0) {
    return `特征视图 "${view.name}" 没有配置任何特征，请先添加特征`
  }
  if (!view.sourceConfig) {
    return `特征视图 "${view.name}" 没有配置数据源，请先编辑视图配置数据源`
  }
  let config = null
  try {
    config = JSON.parse(view.sourceConfig)
  } catch (e) {
    return `特征视图 "${view.name}" 的数据源配置格式错误`
  }
  if (!config.table) {
    return `特征视图 "${view.name}" 缺少数据表配置，请先编辑视图选择数据表`
  }
  if (!config.entityColumn) {
    return `特征视图 "${view.name}" 缺少实体字段配置，请先编辑视图选择实体字段`
  }
  if (!config.dateColumn) {
    return `特征视图 "${view.name}" 缺少日期字段配置，请先编辑视图选择日期字段`
  }
  return null
}

/**
 * 启动 elapsedTime 计时器（仅用于显示真实耗时）
 */
const startElapsedTimer = () => {
  if (elapsedTimer) clearInterval(elapsedTimer)
  elapsedTimer = setInterval(() => {
    if (!computeStatus.value || !computeStatus.value.startTime) return
    const elapsed = (Date.now() - computeStatus.value.startTime) / 1000
    computeStatus.value.elapsedTime = elapsed.toFixed(1)
  }, 500)
}

/**
 * 停止 elapsedTime 计时器
 */
const stopElapsedTimer = () => {
  if (elapsedTimer) {
    clearInterval(elapsedTimer)
    elapsedTimer = null
  }
}

/**
 * 开始计算（同步阻塞 API）
 */
const startCompute = async () => {
  // 前置校验
  const errorMsg = validateViewConfig()
  if (errorMsg) {
    ElMessage.warning(errorMsg)
    return
  }

  try {
    currentStep.value = 2

    // 初始化计算状态
    computeStatus.value = {
      status: 'running',
      startTime: Date.now(),
      elapsedTime: 0
    }
    computeLogs.value = []
    addLog('info', `已提交计算请求：特征视图 ${selectedView.value.name}（${selectedView.value.features?.length || 0} 个特征）`)
    addLog('info', '后端正在同步执行计算，请耐心等待…')

    // 启动耗时计时器
    startElapsedTimer()

    // 构造 FeatureDefinition
    let sourceConfig = null
    try {
      if (selectedView.value.sourceConfig) {
        sourceConfig = JSON.parse(selectedView.value.sourceConfig)
      }
    } catch (e) {
      console.warn('解析 sourceConfig 失败:', e)
    }

    const definition = {
      featureView: selectedView.value.name,
      entity: selectedView.value.entity,
      features: selectedView.value.features || [],
      source: {
        type: selectedView.value.datasourceType || 'postgresql',
        config: sourceConfig || {}
      }
    }

    // 调用实际 API（同步阻塞，后端执行实际计算）
    const response = await featuresStore.computeFeatures(
      definition,
      computeForm.value.inputPath || null,
      computeForm.value.outputPath || null
    )

    // API 返回成功 → 完成计算
    stopElapsedTimer()
    completeCompute('success', response)
  } catch (error) {
    stopElapsedTimer()
    addLog('error', '计算失败: ' + (error.message || '未知错误'))
    completeCompute('failed', null, error.message || '未知错误')
  }
}

/**
 * 添加日志
 */
const addLog = (level, message) => {
  computeLogs.value.push({
    level,
    message,
    timestamp: Date.now()
  })

  // 自动滚动到底部
  if (autoScroll.value && logContainer.value) {
    nextTick(() => {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    })
  }
}

/**
 * 完成计算
 * @param {string} status - 'success' | 'failed' | 'cancelled'
 * @param {Object} response - API 响应（成功时）
 * @param {string} errorMsg - 错误信息（失败时）
 */
const completeCompute = (status, response = null, errorMsg = '') => {
  if (!computeStatus.value) return
  computeStatus.value.status = status
  // 锁定最终耗时
  const elapsed = ((Date.now() - computeStatus.value.startTime) / 1000).toFixed(1)
  computeStatus.value.elapsedTime = elapsed

  if (status === 'success') {
    addLog('success', `计算任务完成，耗时 ${elapsed}s`)
    computeFinalStatus.value = 'success'
    computeFinalMessage.value = '特征计算已成功完成'

    // 优先使用 API 响应中的真实输出路径，没有则给一个推断值
    const responseData = response?.data || {}
    computeResult.value = {
      featureViewName: computeForm.value.featureViewName,
      partitionDate: computeForm.value.partitionDate,
      totalFeatures: selectedView.value?.features?.length || 0,
      outputPath: responseData.outputPath
        || computeForm.value.outputPath
        || `minio://features/${computeForm.value.featureViewName}/${computeForm.value.partitionDate.replace(/-/g, '')}/`,
      elapsedTime: elapsed
    }
  } else if (status === 'cancelled') {
    computeFinalStatus.value = 'failed'
    computeFinalMessage.value = '计算任务已取消'
  } else {
    computeFinalStatus.value = 'failed'
    computeFinalMessage.value = errorMsg
      ? `特征计算失败: ${errorMsg}`
      : '特征计算失败，请检查日志获取详细信息'
  }

  stopElapsedTimer()

  // 延迟进入完成页面
  setTimeout(() => {
    currentStep.value = 3
  }, 600)
}

/**
 * 取消计算
 *
 * 注意：后端是同步阻塞 API，前端无法真正中止已发出的请求。
 * 这里只是中断 UI 上的计时和等待状态，后端可能仍在继续执行。
 */
const handleCancelCompute = async () => {
  try {
    await ElMessageBox.confirm(
      '后端为同步阻塞接口，取消仅会终止页面上的等待状态，无法中止后端实际计算。是否继续？',
      '确认取消',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '继续等待',
        type: 'warning'
      }
    )

    stopElapsedTimer()
    addLog('warning', '页面端已停止等待（后端可能仍在执行）')
    completeCompute('cancelled')
  } catch (error) {
    // 用户取消操作（点击"继续等待"）
  }
}

/**
 * 重试计算
 */
const retryCompute = () => {
  stopElapsedTimer()
  currentStep.value = 1
  computeLogs.value = []
  computeStatus.value = null
  computeResult.value = null
  computeFinalStatus.value = ''
  computeFinalMessage.value = ''
}

/**
 * 查看结果
 */
const viewResults = () => {
  router.push({
    name: 'FeatureViewDetail',
    params: { name: computeForm.value.featureViewName }
  })
}

/**
 * 物化到Redis
 */
const materializeFeatures = async () => {
  try {
    await featuresStore.materializeFeatures(computeForm.value.featureViewName)
    ElMessage.success('特征物化成功')
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

/**
 * 重新计算
 */
const resetCompute = () => {
  stopElapsedTimer()
  currentStep.value = 0
  computeLogs.value = []
  computeStatus.value = null
  computeResult.value = null
  computeFinalStatus.value = ''
  computeFinalMessage.value = ''
}

/**
 * 下载日志
 */
const downloadLogs = () => {
  const logText = computeLogs.value.map(log =>
    `[${formatLogTime(log.timestamp)}] [${log.level.toUpperCase()}] ${log.message}`
  ).join('\n')

  const blob = new Blob([logText], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = `feature-compute-${Date.now()}.log`
  link.click()

  URL.revokeObjectURL(url)
  ElMessage.success('日志已下载')
}

/**
 * 清空日志
 */
const clearLogs = () => {
  computeLogs.value = []
}

/**
 * 获取状态标签类型
 */
const getStatusTagType = (status) => {
  const types = {
    running: 'warning',
    success: 'success',
    failed: 'danger',
    cancelled: 'info'
  }
  return types[status] || 'info'
}

/**
 * 获取状态标签
 */
const getStatusLabel = (status) => {
  const labels = {
    running: '运行中',
    success: '成功',
    failed: '失败',
    cancelled: '已取消'
  }
  return labels[status] || status
}

/**
 * 格式化日志时间
 */
const formatLogTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
}

// ==================== 生命周期 ====================

/**
 * 页面刷新前警告
 */
const handleBeforeUnload = (e) => {
  if (computeStatus.value?.status === 'running') {
    e.preventDefault()
    e.returnValue = '计算任务进行中，确定要离开吗？'
    return e.returnValue
  }
}

onMounted(async () => {
  // 加载特征视图列表（用于下拉选择）
  if (featuresStore.views.length === 0) {
    try {
      await featuresStore.fetchViews()
    } catch (error) {
      console.error('加载特征视图列表失败:', error)
    }
  }

  // 监听页面刷新/关闭事件
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  stopElapsedTimer()
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped lang="scss">
.feature-compute {
  padding: 24px;
}

.steps {
  margin-bottom: 32px;
}

.step-content {
  max-width: 900px;
  margin: 0 auto;
}

.config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.view-option {
  display: flex;
  align-items: center;
  gap: 8px;

  .view-name {
    font-weight: 500;
  }

  .view-desc {
    color: $text-muted;
    font-size: 12px;
  }
}

.placeholder {
  color: $text-placeholder;
}

.form-tip {
  font-size: 12px;
  color: $text-muted;
  margin-top: 4px;
  line-height: 1.5;
}

.preview-card {
  margin-top: 20px;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

.compute-card {
  margin-bottom: 20px;
}

.progress-section {
  margin-bottom: 32px;
  padding: 20px;
  background: $bg-gray;
  border-radius: $radius-sm;

  .progress-text {
    font-weight: 600;
  }
}

.compute-timeline {
  margin-bottom: 24px;

  .timeline-content {
    .step-title {
      font-weight: 600;
      font-size: 15px;
      color: $text-primary;
      margin-bottom: 4px;
    }

    .step-description {
      font-size: 13px;
      color: $text-secondary;
      margin-bottom: 4px;
    }

    .step-time {
      font-size: 12px;
      color: $text-muted;
    }
  }
}

.statistics-section {
  margin-bottom: 24px;
  padding: 20px;
  background: $bg-gray;
  border-radius: $radius-sm;

  .stat-card {
    text-align: center;
    padding: 16px;
    background: $bg-white;
    border-radius: $radius-sm;

    .stat-label {
      font-size: 13px;
      color: $text-secondary;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: #409eff;
    }
  }
}

.compute-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.logs-card {
  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .log-container {
    height: 300px;
    overflow-y: auto;
    background: #1e1e1e;
    border-radius: $radius-sm;
    padding: 12px;
    font-family: 'Courier New', monospace;
    font-size: 13px;

    .log-line {
      padding: 4px 0;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-all;

      &.log-info {
        color: #60a5fa;
      }

      &.log-success {
        color: #4ade80;
      }

      &.log-warning {
        color: #fbbf24;
      }

      &.log-error {
        color: #f87171;
      }

      &.log-debug {
        color: #9ca3af;
      }

      .log-time {
        color: #9ca3af;
        margin-right: 8px;
      }

      .log-level {
        margin-right: 8px;
        font-weight: 600;
      }

      .log-message {
        color: #e5e7eb;
      }
    }

    .log-empty {
      color: #6b7280;
      text-align: center;
      padding: 40px 0;
    }
  }
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.result-summary {
  max-width: 800px;
  margin: 0 auto;
}

// 响应式
@media (max-width: 768px) {
  .step-actions,
  .compute-actions,
  .result-actions {
    flex-direction: column;

    button {
      width: 100%;
    }
  }

  .statistics-section {
    :deep(.el-col) {
      margin-bottom: 12px;
    }
  }
}
</style>
