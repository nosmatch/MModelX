<template>
  <div class="training-overview">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <stat-card :icon="List" tone="primary" :value="jobStats.total" label="总任务" />
      </el-col>
      <el-col :span="6">
        <stat-card :icon="VideoPlay" tone="success" :value="jobStats.running + jobStats.pending" label="运行中" />
      </el-col>
      <el-col :span="6">
        <stat-card :icon="CircleCheck" tone="success" :value="jobStats.success" label="成功" />
      </el-col>
      <el-col :span="6">
        <stat-card :icon="CircleClose" tone="danger" :value="jobStats.failed" label="失败" />
      </el-col>
    </el-row>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="left-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索任务名称或实验名"
          clearable
          style="width: 260px; margin-right: 12px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filterStatus"
          placeholder="筛选状态"
          clearable
          style="width: 120px; margin-right: 12px"
        >
          <el-option label="等待中" value="PENDING" />
          <el-option label="运行中" value="RUNNING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>

        <el-select
          v-model="filterModelType"
          placeholder="模型类型"
          clearable
          style="width: 120px"
        >
          <el-option label="LightGBM" value="lightgbm" />
          <el-option label="XGBoost" value="xgboost" />
        </el-select>
      </div>

      <div class="right-actions">
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleNewTraining">新建任务</el-button>
      </div>
    </div>

    <!-- 训练任务表格 -->
    <el-table
      v-loading="trainingStore.loading.jobs"
      :data="displayJobs"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="jobName" label="任务名称" min-width="200" fixed="left">
        <template #default="{ row }">
          <div class="job-name">
            <el-icon class="icon" color="#409eff"><Cpu /></el-icon>
            <span class="name">{{ row.jobName }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="experimentName" label="实验名称" min-width="150">
        <template #default="{ row }">
          <span>{{ row.experimentName }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="modelType" label="模型类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.modelType === 'lightgbm' ? 'success' : 'warning'">
            {{ row.modelType === 'lightgbm' ? 'LightGBM' : 'XGBoost' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            <el-icon v-if="row.status === 'RUNNING'" class="is-loading"><Loading /></el-icon>
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="进度" width="180" align="center">
        <template #default="{ row }">
          <el-progress
            v-if="row.status === 'RUNNING' || row.status === 'PENDING'"
            :percentage="row.progress || 0"
            :status="row.status === 'FAILED' ? 'exception' : ''"
          />
          <span v-else-if="row.status === 'SUCCESS'" class="text-success">
            <el-icon><CircleCheck /></el-icon> 完成
          </span>
          <span v-else-if="row.status === 'FAILED'" class="text-danger">
            <el-icon><CircleClose /></el-icon> 失败
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column prop="currentStep" label="当前步骤" min-width="120">
        <template #default="{ row }">
          <span>{{ row.currentStep || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="datasetVersion" label="数据集版本" width="130">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.datasetVersion || '-' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="elapsedTimeMs" label="耗时" width="100" align="center">
        <template #default="{ row }">
          <span>{{ formatElapsedTime(row.elapsedTimeMs) }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
        <template #default="{ row }">
          <span>{{ formatDate(row.createdAt) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-tooltip content="查看详情" placement="top">
              <el-button size="small" :icon="View" circle @click.stop="handleViewJob(row)" />
            </el-tooltip>
            <el-tooltip
              v-if="row.status === 'RUNNING' || row.status === 'PENDING'"
              content="刷新状态"
              placement="top"
            >
              <el-button size="small" type="primary" :icon="Refresh" circle @click.stop="handleRefreshJob(row)" />
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 任务详情对话框 -->
    <el-dialog
      v-model="showJobDetail"
      title="训练任务详情"
      width="600px"
    >
      <div v-if="selectedJob" class="job-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ selectedJob.id }}</el-descriptions-item>
          <el-descriptions-item label="任务名称">{{ selectedJob.jobName }}</el-descriptions-item>
          <el-descriptions-item label="实验名称">{{ selectedJob.experimentName }}</el-descriptions-item>
          <el-descriptions-item label="模型类型">{{ selectedJob.modelType }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedJob.status)">
              {{ getStatusLabel(selectedJob.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="进度">{{ selectedJob.progress }}%</el-descriptions-item>
          <el-descriptions-item label="当前步骤">{{ selectedJob.currentStep }}</el-descriptions-item>
          <el-descriptions-item label="总步骤">{{ selectedJob.totalSteps }}</el-descriptions-item>
          <el-descriptions-item label="数据集版本">{{ selectedJob.datasetVersion }}</el-descriptions-item>
          <el-descriptions-item label="模型版本">{{ selectedJob.modelVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ formatElapsedTime(selectedJob.elapsedTimeMs) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(selectedJob.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider v-if="selectedJob.metrics" content-position="left">评估指标</el-divider>
        <div v-if="selectedJob.metrics" class="metrics-section">
          <el-tag
            v-for="(value, key) in selectedJob.metrics"
            :key="key"
            size="large"
            class="metric-tag"
          >
            {{ key }}: {{ typeof value === 'number' ? value.toFixed(4) : value }}
          </el-tag>
        </div>

        <el-divider v-if="selectedJob.errorMessage" content-position="left">错误信息</el-divider>
        <el-alert
          v-if="selectedJob.errorMessage"
          :title="selectedJob.errorMessage"
          type="error"
          :closable="false"
        />
      </div>
    </el-dialog>

    <!-- 训练配置对话框 -->
    <training-config-dialog
      v-if="showConfigDialog"
      :visible="showConfigDialog"
      :mode="dialogMode"
      @close="showConfigDialog = false"
      @submit="handleTrainingSubmit"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  List, VideoPlay, CircleCheck, CircleClose,
  Search, Refresh, Plus, View, Cpu, Loading
} from '@element-plus/icons-vue'
import { useTrainingStore } from '@/stores/training'
import { formatDate, formatElapsed as formatElapsedTime } from '@/utils/date'
import { JobStatusColors, JobStatusLabels } from '@/constants/status'
import StatCard from '@/components/StatCard.vue'
import TrainingConfigDialog from './TrainingConfigDialog.vue'

const router = useRouter()
const trainingStore = useTrainingStore()

// 搜索和筛选
const searchKeyword = ref('')
const filterStatus = ref('')
const filterModelType = ref('')

// 对话框
const showConfigDialog = ref(false)
const dialogMode = ref('create')
const showJobDetail = ref(false)
const selectedJob = ref(null)

// 自动刷新定时器
let autoRefreshTimer = null

// 计算属性
const jobStats = computed(() => trainingStore.jobStats)

const displayJobs = computed(() => {
  let jobs = trainingStore.jobs

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    jobs = jobs.filter(j =>
      (j.jobName && j.jobName.toLowerCase().includes(keyword)) ||
      (j.experimentName && j.experimentName.toLowerCase().includes(keyword))
    )
  }

  if (filterStatus.value) {
    jobs = jobs.filter(j => j.status === filterStatus.value)
  }

  if (filterModelType.value) {
    jobs = jobs.filter(j => j.modelType === filterModelType.value)
  }

  return jobs
})

// 方法
const loadJobs = async () => {
  try {
    await trainingStore.fetchJobs()
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

const handleRefresh = () => {
  loadJobs()
  ElMessage.success('刷新成功')
}

const handleNewTraining = () => {
  dialogMode.value = 'create'
  showConfigDialog.value = true
}

const handleHyperTune = () => {
  dialogMode.value = 'tune'
  showConfigDialog.value = true
}

const handleViewExperiments = () => {
  router.push('/training/experiments')
}

const handleViewJob = (row) => {
  selectedJob.value = row
  showJobDetail.value = true
}

const handleRefreshJob = async (row) => {
  try {
    await trainingStore.fetchJobStatus(row.id)
    ElMessage.success('状态已更新')
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

const handleTrainingSubmit = async (config, isAsync) => {
  try {
    if (isAsync) {
      const jobId = await trainingStore.submitJob(config)
      ElMessage.success(`训练任务已提交, 任务ID: ${jobId}`)

      // 开始轮询
      trainingStore.startJobPolling(jobId, (job) => {
        console.log('Job update:', job.status, job.progress)
      }, (job) => {
        if (job.status === 'SUCCESS') {
          ElMessage.success(`训练任务完成: ${job.jobName}`)
        } else {
          ElMessage.error(`训练任务失败: ${job.jobName}`)
        }
      })
    } else {
      ElMessage.info('开始同步训练...')
      const modelPath = await trainingStore.trainSync(config)
      ElMessage.success('训练完成, 模型路径: ' + modelPath)
    }

    showConfigDialog.value = false
    await loadJobs()
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

// 状态映射（统一从 constants/status.js 取）
const getStatusType = (status) => JobStatusColors[status] || 'info'
const getStatusLabel = (status) => JobStatusLabels[status] || status

// 生命周期
onMounted(() => {
  loadJobs()

  // 每10秒自动刷新
  autoRefreshTimer = setInterval(() => {
    if (trainingStore.runningJobs.length > 0) {
      loadJobs()
    }
  }, 10000)
})

onUnmounted(() => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
  }
  trainingStore.stopJobPolling()
})
</script>

<style scoped lang="scss">
.training-overview {
  padding: 24px;
}

.stats-row {
  margin-bottom: 24px;
}

.quick-actions {
  margin-bottom: 24px;
}

.action-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }

  .action-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px;
    text-align: center;
  }

  .action-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-top: 12px;
  }

  .action-desc {
    font-size: 13px;
    color: $text-muted;
    margin-top: 4px;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: $bg-gray;
  border-radius: $radius-sm;

  .left-actions,
  .right-actions {
    display: flex;
    align-items: center;
  }
}

.job-name {
  display: flex;
  align-items: center;

  .icon {
    margin-right: 8px;
  }

  .name {
    font-weight: 500;
    color: $text-primary;
  }
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 6px;
}

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}

.job-detail {
  .metrics-section {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
  }

  .metric-tag {
    margin-right: 8px;
    margin-bottom: 8px;
  }
}

.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .training-overview {
    padding: 16px;
  }

  .toolbar {
    flex-direction: column;
    gap: 12px;

    .left-actions,
    .right-actions {
      width: 100%;
      flex-wrap: wrap;
    }
  }
}
</style>
