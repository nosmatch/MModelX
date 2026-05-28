<template>
  <div class="tuning-page">
    <el-row :gutter="24">
      <!-- 左侧：配置面板 -->
      <el-col :span="10">
        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>调优配置</span>
            </div>
          </template>

          <el-form :model="tuneForm" label-width="120px" class="tune-form">
            <!-- 实验名称 -->
            <el-form-item label="实验名称" required>
              <el-input v-model="tuneForm.experimentName" placeholder="请输入新实验名称，调优将创建新实验" />
            </el-form-item>

            <!-- 数据集选择 -->
            <el-form-item label="数据集" required>
              <el-select
                v-model="tuneForm.datasetName"
                placeholder="选择数据集"
                clearable
                style="width: 100%; margin-bottom: 8px"
                @change="handleDatasetChange"
              >
                <el-option
                  v-for="ds in datasetList"
                  :key="ds.name"
                  :label="ds.name + (ds.description ? ' (' + ds.description + ')' : '')"
                  :value="ds.name"
                />
              </el-select>
            </el-form-item>

            <!-- 数据集版本 -->
            <el-form-item label="版本" required>
              <el-select
                v-model="tuneForm.datasetVersion"
                placeholder="选择版本或手动输入"
                clearable
                filterable
                allow-create
                default-first-option
                style="width: 100%"
              >
                <el-option
                  v-for="v in versionList"
                  :key="v"
                  :label="v"
                  :value="v"
                />
              </el-select>
            </el-form-item>

            <!-- 模型类型 -->
            <el-form-item label="模型类型">
              <el-radio-group v-model="tuneForm.modelType" @change="handleModelTypeChange">
                <el-radio-button label="lightgbm">LightGBM</el-radio-button>
                <el-radio-button label="xgboost">XGBoost</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-divider content-position="left">搜索配置</el-divider>

            <!-- 搜索轮数 -->
            <el-form-item label="搜索轮数">
              <el-slider v-model="tuneForm.nTrials" :min="5" :max="100" :step="5" show-input />
            </el-form-item>

            <!-- 优化目标 -->
            <el-form-item label="优化目标">
              <el-select v-model="tuneForm.metric" style="width: 150px; margin-right: 12px">
                <el-option label="AUC" value="auc" />
                <el-option label="LogLoss" value="logloss" />
                <el-option label="RMSE" value="rmse" />
              </el-select>
              <el-radio-group v-model="tuneForm.direction">
                <el-radio-button label="maximize">最大化</el-radio-button>
                <el-radio-button label="minimize">最小化</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-divider content-position="left">参数配置</el-divider>

            <!-- 参数范围列表 -->
            <div class="param-ranges">
              <div
                v-for="(param, index) in tuneForm.paramRanges"
                :key="index"
                class="param-range-item"
              >
                <el-switch
                  v-model="param.enabled"
                  size="small"
                  style="margin-right: 8px"
                />
                <el-input
                  v-model="param.name"
                  placeholder="参数名"
                  size="small"
                  style="width: 140px; margin-right: 8px"
                  :disabled="!param.enabled"
                />
                <el-select
                  v-model="param.type"
                  placeholder="类型"
                  size="small"
                  style="width: 90px; margin-right: 8px"
                  :disabled="!param.enabled"
                >
                  <el-option label="整数" value="int" />
                  <el-option label="浮点" value="float" />
                  <el-option label="枚举" value="categorical" />
                </el-select>
                <template v-if="param.type === 'categorical'">
                  <el-input
                    v-model="param.choicesStr"
                    placeholder="选项,逗号分隔"
                    size="small"
                    style="width: 160px; margin-right: 8px"
                    :disabled="!param.enabled"
                  />
                </template>
                <template v-else>
                  <el-input-number
                    v-model="param.min"
                    placeholder="最小"
                    size="small"
                    :step="param.type === 'int' ? 1 : 0.01"
                    style="width: 100px; margin-right: 8px"
                    :disabled="!param.enabled"
                  />
                  <span class="range-separator">~</span>
                  <el-input-number
                    v-model="param.max"
                    placeholder="最大"
                    size="small"
                    :step="param.type === 'int' ? 1 : 0.01"
                    style="width: 100px; margin-left: 8px; margin-right: 8px"
                    :disabled="!param.enabled"
                  />
                </template>
                <el-button
                  size="small"
                  type="danger"
                  :icon="Delete"
                  circle
                  @click="removeParamRange(index)"
                />
              </div>
              <el-button
                size="small"
                type="primary"
                :icon="Plus"
                @click="addParamRange"
                style="margin-top: 8px"
              >
                添加参数
              </el-button>
            </div>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :icon="Search"
                :loading="trainingStore.loading.tuning || isTuningRunning"
                @click="handleStartTuning"
              >
                {{ isTuningRunning ? '调优中...' : '开始调优' }}
              </el-button>
              <el-button size="large" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 历史调优任务 -->
        <el-card class="history-card" shadow="never" style="margin-top: 16px">
          <template #header>
            <div class="card-header">
              <span>历史调优任务</span>
              <el-button size="small" :icon="Refresh" @click="loadTuningHistory">刷新</el-button>
            </div>
          </template>
          <el-table :data="tuningHistory" size="small" style="width: 100%">
            <el-table-column prop="experimentName" label="实验名称" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="currentTrial" label="进度" width="100">
              <template #default="{ row }">
                <span>{{ row.currentTrial || 0 }}/{{ row.nTrials }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-button size="small" :icon="View" circle @click="selectHistoryJob(row)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：结果展示 -->
      <el-col :span="14">
        <el-card class="result-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>调优结果</span>
              <el-tag v-if="tuneResult" type="success">已完成</el-tag>
              <el-tag v-else-if="isTuningRunning" type="warning">调优中</el-tag>
              <el-tag v-else type="info">等待开始</el-tag>
            </div>
          </template>

          <!-- 空状态 -->
          <div v-if="!isTuningRunning && !tuneResult" class="empty-result">
            <el-empty description="配置参数后点击开始调优">
              <template #image>
                <el-icon :size="64" color="#c0c4cc"><Setting /></el-icon>
              </template>
            </el-empty>
          </div>

          <!-- 调优中 -->
          <div v-else-if="isTuningRunning" class="tuning-progress">
            <div class="progress-header">
              <el-progress
                :percentage="Math.round((currentTuningJob.currentTrial / currentTuningJob.nTrials) * 100)"
                :format="() => currentTuningJob.currentTrial + '/' + currentTuningJob.nTrials"
                :stroke-width="18"
                status="active"
              />
              <p class="progress-text">
                正在执行第 {{ currentTuningJob.currentTrial + 1 }} 轮搜索，共 {{ currentTuningJob.nTrials }} 轮
              </p>
            </div>

            <el-divider content-position="left">当前最佳</el-divider>
            <div v-if="currentTuningJob.bestScore" class="current-best">
              <el-statistic title="最佳得分" :value="currentTuningJob.bestScore" :precision="4" />
              <div class="best-params-preview">
                <el-tag
                  v-for="(value, key) in bestParamsFormatted"
                  :key="key"
                  size="small"
                  type="success"
                >
                  {{ key }}: {{ value }}
                </el-tag>
              </div>
            </div>
            <el-empty v-else description="等待第一轮结果..." :image-size="60" />

            <el-divider content-position="left">Trial 列表</el-divider>
            <el-table :data="trialList" size="small" max-height="400" v-loading="trialsLoading">
              <el-table-column prop="trialIndex" label="#" width="50" align="center" />
              <el-table-column label="参数" min-width="200">
                <template #default="{ row }">
                  <span class="param-preview">{{ formatParams(row.params) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="score" label="得分" width="100" align="center">
                <template #default="{ row }">
                  <span :class="{ 'best-score': row.isBest }">
                    {{ row.score != null ? row.score.toFixed(4) : '-' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getTrialStatusType(row.status)" size="small">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 调优完成 -->
          <div v-else-if="tuneResult" class="tune-result">
            <!-- 最佳结果卡片 -->
            <el-row :gutter="16" class="result-stats">
              <el-col :span="8">
                <div class="result-stat best-score">
                  <div class="stat-label">最佳得分</div>
                  <div class="stat-value">{{ tuneResult.bestScore?.toFixed(4) }}</div>
                  <div class="stat-metric">{{ tuneResult.metric }}</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="result-stat">
                  <div class="stat-label">优化方向</div>
                  <div class="stat-value">{{ tuneResult.direction === 'maximize' ? '最大化' : '最小化' }}</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="result-stat">
                  <div class="stat-label">搜索轮数</div>
                  <div class="stat-value">{{ tuneResult.nTrials || 20 }}</div>
                </div>
              </el-col>
            </el-row>

            <el-divider content-position="left">最佳参数</el-divider>
            <div class="best-params">
              <el-tag
                v-for="(value, key) in tuneResult.bestParams"
                :key="key"
                size="large"
                type="success"
                effect="dark"
                class="param-tag"
              >
                {{ key }}: {{ typeof value === 'number' ? value.toFixed(4) : value }}
              </el-tag>
            </div>

            <el-divider content-position="left">最终评估指标</el-divider>
            <div class="final-metrics">
              <el-tag
                v-for="(value, key) in tuneResult.finalMetrics"
                :key="key"
                size="large"
                type="primary"
                class="metric-tag"
              >
                {{ key }}: {{ typeof value === 'number' ? value.toFixed(4) : value }}
              </el-tag>
            </div>

            <el-divider content-position="left">模型路径</el-divider>
            <el-input v-model="tuneResult.modelPath" readonly>
              <template #append>
                <el-button @click="copyModelPath">复制</el-button>
              </template>
            </el-input>

            <div class="result-actions">
              <el-button type="primary" @click="handleRegisterModel">
                <el-icon><Box /></el-icon>
                注册模型
              </el-button>
              <el-button @click="handleNewTuning">
                <el-icon><Refresh /></el-icon>
                新的调优
              </el-button>
            </div>

            <el-divider content-position="left">调优配置</el-divider>
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="实验名称">{{ tuneResult.experimentName }}</el-descriptions-item>
              <el-descriptions-item label="数据集">{{ tuneResult.datasetName }} @ {{ tuneResult.datasetVersion }}</el-descriptions-item>
              <el-descriptions-item label="模型类型">{{ tuneResult.modelType }}</el-descriptions-item>
              <el-descriptions-item label="优化目标">{{ tuneResult.metric }} ({{ tuneResult.direction === 'maximize' ? '最大化' : '最小化' }})</el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">Trial 详情</el-divider>
            <el-table :data="trialList" size="small" max-height="500" v-loading="trialsLoading">
              <el-table-column prop="trialIndex" label="#" width="50" align="center" />
              <el-table-column label="参数" min-width="250">
                <template #default="{ row }">
                  <div class="param-detail">
                    <el-tag
                      v-for="(value, key) in row.params"
                      :key="key"
                      size="small"
                      class="param-tag-inline"
                    >
                      {{ key }}: {{ typeof value === 'number' ? value.toFixed(4) : value }}
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="score" label="得分" width="100" align="center">
                <template #default="{ row }">
                  <span :class="{ 'best-score': row.isBest }">
                    {{ row.score != null ? row.score.toFixed(4) : '-' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="指标" min-width="150">
                <template #default="{ row }">
                  <span class="param-preview">{{ formatParams(row.metrics) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="getTrialStatusType(row.status)" size="small">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="elapsedTimeMs" label="耗时(ms)" width="100" align="center">
                <template #default="{ row }">
                  {{ row.elapsedTimeMs != null ? row.elapsedTimeMs : '-' }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Setting, Box, Refresh, View, Plus, Delete } from '@element-plus/icons-vue'
import { useTrainingStore } from '@/stores/training'
import { listDatasets, listDatasetVersions } from '@/api/modules/samples'
import { registerModel } from '@/api/modules/training'

const trainingStore = useTrainingStore()

const datasetList = ref([])
const versionList = ref([])

const defaultLightGBMRanges = () => [
  { name: 'num_leaves', type: 'int', min: 31, max: 127, enabled: true },
  { name: 'learning_rate', type: 'float', min: 0.01, max: 0.2, enabled: true },
  { name: 'feature_fraction', type: 'float', min: 0.5, max: 1.0, enabled: true },
  { name: 'bagging_fraction', type: 'float', min: 0.5, max: 1.0, enabled: true },
  { name: 'bagging_freq', type: 'int', min: 1, max: 10, enabled: true },
]

const defaultXGBoostRanges = () => [
  { name: 'max_depth', type: 'int', min: 3, max: 13, enabled: true },
  { name: 'learning_rate', type: 'float', min: 0.01, max: 0.2, enabled: true },
  { name: 'subsample', type: 'float', min: 0.5, max: 1.0, enabled: true },
  { name: 'colsample_bytree', type: 'float', min: 0.5, max: 1.0, enabled: true },
]

const defaultForm = () => ({
  experimentName: '',
  datasetName: '',
  datasetVersion: '',
  modelType: 'lightgbm',
  nTrials: 20,
  metric: 'auc',
  direction: 'maximize',
  paramRanges: defaultLightGBMRanges(),
})

const tuneForm = ref(defaultForm())

// 调优状态
const tuningJobId = ref(null)
const isTuningRunning = ref(false)
const currentTuningJob = ref({})
const trialList = ref([])
const trialsLoading = ref(false)
const tuneResult = ref(null)
const tuningPollTimer = ref(null)

// 历史记录
const tuningHistory = ref([])

// 最佳参数格式化
const bestParamsFormatted = computed(() => {
  if (!currentTuningJob.value.bestParams) return {}
  try {
    return typeof currentTuningJob.value.bestParams === 'object'
      ? currentTuningJob.value.bestParams
      : JSON.parse(currentTuningJob.value.bestParams)
  } catch {
    return {}
  }
})

const loadDatasets = async () => {
  try {
    const response = await listDatasets()
    const isSuccess = response.code === '200' || response.code === 200
    if (isSuccess) {
      datasetList.value = response.data || []
    }
  } catch (error) {
    console.error('加载数据集列表失败:', error)
  }
}

const handleDatasetChange = async (datasetName) => {
  tuneForm.value.datasetVersion = ''
  versionList.value = []
  if (!datasetName) return

  try {
    const response = await listDatasetVersions(datasetName)
    const isSuccess = response.code === '200' || response.code === 200
    if (isSuccess) {
      const versions = response.data || []
      versionList.value = versions.length > 0 ? versions : ['v1.0']
    }
  } catch (error) {
    console.error('加载版本列表失败:', error)
    versionList.value = ['v1.0']
  }
}

const handleModelTypeChange = (type) => {
  if (type === 'lightgbm') {
    tuneForm.value.paramRanges = defaultLightGBMRanges()
  } else if (type === 'xgboost') {
    tuneForm.value.paramRanges = defaultXGBoostRanges()
  }
}

const addParamRange = () => {
  tuneForm.value.paramRanges.push({
    name: '',
    type: 'float',
    min: 0,
    max: 1,
    enabled: true,
    choicesStr: ''
  })
}

const removeParamRange = (index) => {
  tuneForm.value.paramRanges.splice(index, 1)
}

const buildParamRanges = () => {
  return tuneForm.value.paramRanges
    .filter(p => p.enabled && p.name)
    .map(p => {
      const range = {
        name: p.name,
        type: p.type,
        enabled: p.enabled
      }
      if (p.type === 'categorical' && p.choicesStr) {
        range.choices = p.choicesStr.split(',').map(s => s.trim()).filter(s => s)
      } else {
        if (p.min !== undefined && p.min !== null) range.min = Number(p.min)
        if (p.max !== undefined && p.max !== null) range.max = Number(p.max)
      }
      return range
    })
}

const handleStartTuning = async () => {
  if (!tuneForm.value.experimentName || !tuneForm.value.datasetName || !tuneForm.value.datasetVersion) {
    ElMessage.warning('请填写实验名称、数据集和数据集版本')
    return
  }

  const paramRanges = buildParamRanges()
  if (paramRanges.length === 0) {
    ElMessage.warning('请至少配置一个调优参数')
    return
  }

  // 重置状态
  stopTuningPoll()
  tuneResult.value = null
  trialList.value = []
  currentTuningJob.value = {}

  try {
    const config = {
      experimentName: tuneForm.value.experimentName,
      datasetName: tuneForm.value.datasetName,
      datasetVersion: tuneForm.value.datasetVersion,
      model: {
        type: tuneForm.value.modelType,
        params: {}
      },
      optunaConfig: {
        nTrials: tuneForm.value.nTrials,
        metric: tuneForm.value.metric,
        direction: tuneForm.value.direction,
        paramRanges: paramRanges
      }
    }

    ElMessage.info('开始超参数调优，请稍候...')
    const jobId = await trainingStore.submitTuningJob(config)
    tuningJobId.value = jobId
    isTuningRunning.value = true
    ElMessage.success(`调优任务已提交, 任务ID: ${jobId}`)

    // 开始轮询
    startTuningPoll(jobId)
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

const startTuningPoll = (jobId) => {
  stopTuningPoll()

  tuningPollTimer.value = setInterval(async () => {
    try {
      // 获取调优任务状态
      const job = await trainingStore.fetchTuningJob(jobId)
      currentTuningJob.value = job || {}

      // 获取 trial 列表
      trialsLoading.value = true
      const trials = await trainingStore.fetchTuningTrials(jobId)
      trialList.value = trials || []
      trialsLoading.value = false

      // 检查是否完成
      if (job.status === 'SUCCESS' || job.status === 'FAILED') {
        stopTuningPoll()
        isTuningRunning.value = false

        if (job.status === 'SUCCESS') {
          ElMessage.success('超参数调优完成！')
          tuneResult.value = {
            bestParams: job.bestParams || {},
            bestScore: job.bestScore,
            metric: job.metric,
            direction: job.direction,
            finalMetrics: job.finalMetrics || {},
            modelPath: job.modelPath
          }
        } else {
          ElMessage.error('超参数调优失败: ' + (job.errorMessage || '未知错误'))
        }

        // 刷新历史记录
        loadTuningHistory()
      }
    } catch (error) {
      console.error('轮询调优状态失败:', error)
      trialsLoading.value = false
    }
  }, 3000)
}

const stopTuningPoll = () => {
  if (tuningPollTimer.value) {
    clearInterval(tuningPollTimer.value)
    tuningPollTimer.value = null
  }
}

const loadTuningHistory = async () => {
  try {
    const data = await trainingStore.fetchTuningJobs()
    tuningHistory.value = data || []
  } catch (error) {
    console.error('加载调优历史失败:', error)
  }
}

const selectHistoryJob = async (row) => {
  tuningJobId.value = row.id
  currentTuningJob.value = row

  if (row.status === 'PENDING' || row.status === 'RUNNING') {
    isTuningRunning.value = true
    tuneResult.value = null
    startTuningPoll(row.id)
  } else if (row.status === 'SUCCESS' || row.status === 'FAILED') {
    isTuningRunning.value = false

    const bestParams = parseJsonField(row.bestParams) || {}
    const finalMetrics = parseJsonField(row.finalMetrics) || {}

    tuneResult.value = {
      bestParams,
      bestScore: row.bestScore,
      metric: row.metric,
      direction: row.direction,
      finalMetrics,
      modelPath: row.modelPath,
      nTrials: row.nTrials || 20,
      experimentName: row.experimentName,
      modelType: row.modelType,
      datasetName: row.datasetName,
      datasetVersion: row.datasetVersion,
      status: row.status
    }

    // 加载 trial 列表
    try {
      trialsLoading.value = true
      const trials = await trainingStore.fetchTuningTrials(row.id)
      trialList.value = (trials || []).map(t => ({
        ...t,
        params: parseJsonField(t.params),
        metrics: parseJsonField(t.metrics)
      }))
      trialsLoading.value = false
    } catch (e) {
      console.error('加载 trial 列表失败:', e)
      trialsLoading.value = false
    }
  } else {
    isTuningRunning.value = false
    tuneResult.value = null
  }
}

const handleReset = () => {
  tuneForm.value = defaultForm()
  versionList.value = []
  stopTuningPoll()
  isTuningRunning.value = false
  tuneResult.value = null
  trialList.value = []
  currentTuningJob.value = {}
  tuningJobId.value = null
}

const handleNewTuning = () => {
  handleReset()
}

const copyModelPath = () => {
  if (tuneResult.value?.modelPath) {
    navigator.clipboard.writeText(tuneResult.value.modelPath)
    ElMessage.success('路径已复制')
  }
}

const handleRegisterModel = async () => {
  if (!tuneResult.value) return

  try {
    const model = {
      name: tuneForm.value.experimentName,
      version: 'v' + new Date().toISOString().slice(0, 19).replace(/[-:T]/g, ''),
      experimentId: tuneForm.value.experimentName,
      modelType: tuneForm.value.modelType,
      modelPath: tuneResult.value.modelPath,
      performance: tuneResult.value.bestScore
    }
    await registerModel(model)
    await trainingStore.fetchModels()
    ElMessage.success('模型已注册')
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

const parseJsonField = (field) => {
  if (!field) return null
  if (typeof field === 'object') return field
  try {
    return JSON.parse(field)
  } catch {
    return null
  }
}

const formatParams = (params) => {
  if (!params) return '-'
  try {
    const obj = typeof params === 'object' ? params : JSON.parse(params)
    return Object.entries(obj).map(([k, v]) => `${k}=${typeof v === 'number' ? v.toFixed(3) : v}`).join(', ')
  } catch {
    return String(params)
  }
}

const getStatusType = (status) => {
  const map = { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

const getTrialStatusType = (status) => {
  const map = { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

onMounted(() => {
  loadDatasets()
  loadTuningHistory()
})

onUnmounted(() => {
  stopTuningPoll()
})
</script>

<style scoped lang="scss">
.tuning-page {
  padding: 24px;
}

.config-card,
.result-card,
.history-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
  }
}

.tune-form {
  .range-separator {
    text-align: center;
    line-height: 32px;
    color: $text-muted;
  }

  .param-ranges {
    .param-range-item {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
      padding: 8px 12px;
      background: $bg-gray;
      border-radius: $radius-sm;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

.empty-result {
  padding: 60px 0;
  text-align: center;
}

.tuning-progress {
  .progress-header {
    margin-bottom: 20px;

    .progress-text {
      text-align: center;
      color: $text-muted;
      margin-top: 8px;
      font-size: 14px;
    }
  }

  .current-best {
    margin-bottom: 16px;

    .best-params-preview {
      margin-top: 12px;
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
  }

  .param-preview {
    font-size: 12px;
    color: $text-muted;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: block;
    max-width: 200px;
  }

  .best-score {
    color: #67c23a;
    font-weight: 600;
  }
}

.tune-result {
  .result-stats {
    margin-bottom: 20px;
  }

  .result-stat {
    padding: 16px;
    background: $bg-gray;
    border-radius: $radius-sm;
    text-align: center;

    .stat-label {
      font-size: 13px;
      color: $text-muted;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: $text-primary;
    }

    .stat-metric {
      font-size: 12px;
      color: $text-muted;
      margin-top: 4px;
    }

    &.best-score {
      background: linear-gradient(135deg, #ecf5ff 0%, #f0f9ff 100%);

      .stat-value {
        color: #409eff;
      }
    }
  }

  .best-params,
  .final-metrics {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 16px;

    .param-tag,
    .metric-tag {
      margin-right: 8px;
      margin-bottom: 8px;
    }
  }

  .result-actions {
    margin-top: 24px;
    display: flex;
    gap: 12px;
    justify-content: center;
  }

  .param-detail {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .param-tag-inline {
    margin-right: 4px;
    margin-bottom: 4px;
  }
}

@media (max-width: 768px) {
  .tuning-page {
    padding: 16px;
  }
}
</style>
