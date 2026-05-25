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
                v-model="selectedDataset"
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
              <el-radio-group v-model="tuneForm.modelType">
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

            <el-divider content-position="left">参数搜索空间</el-divider>

            <!-- LightGBM 参数空间 -->
            <template v-if="tuneForm.modelType === 'lightgbm'">
              <el-form-item label="num_leaves">
                <el-row :gutter="8">
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.num_leaves.min" :min="2" :max="256" placeholder="最小" />
                  </el-col>
                  <el-col :span="2" class="range-separator">~</el-col>
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.num_leaves.max" :min="2" :max="256" placeholder="最大" />
                  </el-col>
                </el-row>
              </el-form-item>

              <el-form-item label="learning_rate">
                <el-row :gutter="8">
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.learning_rate.min" :min="0.001" :max="1" :step="0.001" placeholder="最小" />
                  </el-col>
                  <el-col :span="2" class="range-separator">~</el-col>
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.learning_rate.max" :min="0.001" :max="1" :step="0.001" placeholder="最大" />
                  </el-col>
                </el-row>
              </el-form-item>

              <el-form-item label="feature_fraction">
                <el-row :gutter="8">
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.feature_fraction.min" :min="0.1" :max="1" :step="0.05" placeholder="最小" />
                  </el-col>
                  <el-col :span="2" class="range-separator">~</el-col>
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.feature_fraction.max" :min="0.1" :max="1" :step="0.05" placeholder="最大" />
                  </el-col>
                </el-row>
              </el-form-item>
            </template>

            <!-- XGBoost 参数空间 -->
            <template v-else>
              <el-form-item label="max_depth">
                <el-row :gutter="8">
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.max_depth.min" :min="1" :max="20" placeholder="最小" />
                  </el-col>
                  <el-col :span="2" class="range-separator">~</el-col>
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.max_depth.max" :min="1" :max="20" placeholder="最大" />
                  </el-col>
                </el-row>
              </el-form-item>

              <el-form-item label="learning_rate">
                <el-row :gutter="8">
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.learning_rate.min" :min="0.001" :max="1" :step="0.001" placeholder="最小" />
                  </el-col>
                  <el-col :span="2" class="range-separator">~</el-col>
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.learning_rate.max" :min="0.001" :max="1" :step="0.001" placeholder="最大" />
                  </el-col>
                </el-row>
              </el-form-item>

              <el-form-item label="subsample">
                <el-row :gutter="8">
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.subsample.min" :min="0.1" :max="1" :step="0.05" placeholder="最小" />
                  </el-col>
                  <el-col :span="2" class="range-separator">~</el-col>
                  <el-col :span="11">
                    <el-input-number v-model="tuneForm.searchSpace.subsample.max" :min="0.1" :max="1" :step="0.05" placeholder="最大" />
                  </el-col>
                </el-row>
              </el-form-item>
            </template>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :icon="Search"
                :loading="trainingStore.loading.tuning"
                @click="handleStartTuning"
              >
                开始调优
              </el-button>
              <el-button size="large" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：结果展示 -->
      <el-col :span="14">
        <el-card class="result-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>调优结果</span>
              <el-tag v-if="tuneResult" type="success">已完成</el-tag>
              <el-tag v-else type="info">等待开始</el-tag>
            </div>
          </template>

          <div v-if="!tuneResult" class="empty-result">
            <el-empty description="配置参数后点击开始调优">
              <template #image>
                <el-icon :size="64" color="#c0c4cc"><Setting /></el-icon>
              </template>
            </el-empty>
          </div>

          <div v-else class="tune-result">
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
                  <div class="stat-value">{{ tuneForm.nTrials }}</div>
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
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Setting, Box, Refresh } from '@element-plus/icons-vue'
import { useTrainingStore } from '@/stores/training'
import { listDatasets, listDatasetVersions } from '@/api/modules/samples'

const trainingStore = useTrainingStore()

const datasetList = ref([])
const versionList = ref([])
const selectedDataset = ref('')

const defaultForm = () => ({
  experimentName: '',
  datasetVersion: '',
  modelType: 'lightgbm',
  nTrials: 20,
  metric: 'auc',
  direction: 'maximize',
  searchSpace: {
    num_leaves: { min: 20, max: 150 },
    learning_rate: { min: 0.01, max: 0.3 },
    feature_fraction: { min: 0.5, max: 1.0 },
    bagging_fraction: { min: 0.5, max: 1.0 },
    max_depth: { min: 3, max: 15 },
    subsample: { min: 0.5, max: 1.0 },
    colsample_bytree: { min: 0.5, max: 1.0 }
  }
})

const tuneForm = ref(defaultForm())
const tuneResult = ref(null)

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

const handleStartTuning = async () => {
  if (!tuneForm.value.experimentName || !tuneForm.value.datasetVersion) {
    ElMessage.warning('请填写实验名称和数据集版本')
    return
  }

  try {
    // 构建配置
    const config = {
      experimentName: tuneForm.value.experimentName,
      datasetVersion: tuneForm.value.datasetVersion,
      model: {
        type: tuneForm.value.modelType,
        params: {}
      },
      optunaConfig: {
        nTrials: tuneForm.value.nTrials,
        metric: tuneForm.value.metric,
        direction: tuneForm.value.direction
      }
    }

    ElMessage.info('开始超参数调优，请稍候...')
    const result = await trainingStore.hyperparameterTuning(config)
    tuneResult.value = result
    ElMessage.success('超参数调优完成！')
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

const handleReset = () => {
  tuneForm.value = defaultForm()
  selectedDataset.value = ''
  versionList.value = []
  tuneResult.value = null
}

const handleNewTuning = () => {
  tuneResult.value = null
  tuneForm.value.experimentName = ''
  selectedDataset.value = ''
  tuneForm.value.datasetVersion = ''
  versionList.value = []
}

onMounted(() => {
  loadDatasets()
})

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
    await trainingStore.fetchModels()
    ElMessage.success('模型已注册')
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}
</script>

<style scoped lang="scss">
.tuning-page {
  padding: 24px;
}

.config-card,
.result-card {
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
}

.empty-result {
  padding: 60px 0;
  text-align: center;
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
}

@media (max-width: 768px) {
  .tuning-page {
    padding: 16px;
  }
}
</style>
