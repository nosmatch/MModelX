<template>
  <div class="sample-build-page">
    <!-- 返回按钮 -->
    <div class="back-nav">
      <el-button text :icon="ArrowLeft" @click="goBack">返回</el-button>
    </div>

    <el-row :gutter="24">
      <!-- 左侧：配置选择 -->
      <el-col :span="8">
        <el-card class="config-card">
          <template #header>
            <span>选择配置</span>
          </template>

          <el-select
            v-model="selectedConfigId"
            placeholder="选择样本配置"
            style="width: 100%"
            @change="handleConfigChange"
          >
            <el-option
              v-for="config in activeConfigs"
              :key="config.id"
              :label="config.name"
              :value="config.id"
            />
          </el-select>

          <div v-if="selectedConfig" class="config-preview">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="标签类型">
                {{ getLabelTypeLabel(selectedConfig.labelType) }}
              </el-descriptions-item>
              <el-descriptions-item label="划分策略">
                {{ getSplitStrategyLabel(selectedConfig.splitStrategy) }}
              </el-descriptions-item>
              <el-descriptions-item label="比例">
                {{ formatRatio(selectedConfig.trainRatio) }} / {{ formatRatio(selectedConfig.valRatio) }} / {{ formatRatio(selectedConfig.testRatio) }}
              </el-descriptions-item>
              <el-descriptions-item label="实体字段">
                {{ selectedConfig.entityColumn }}
              </el-descriptions-item>
              <el-descriptions-item label="特征视图">
                {{ (selectedConfig.featureViews || []).join(', ') || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="负采样">
                {{ selectedConfig.negativeSamplingRatio || 0 }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <el-empty v-else description="请先选择一个样本配置" />
        </el-card>
      </el-col>

      <!-- 右侧：构建执行 -->
      <el-col :span="16">
        <el-card class="build-card">
          <template #header>
            <span>执行构建</span>
          </template>

          <div v-if="!selectedConfig" class="build-placeholder">
            <el-empty description="选择左侧配置后开始构建" />
          </div>

          <div v-else-if="!isBuilding && !buildResult" class="build-ready">
            <div class="build-info">
              <el-icon :size="48" color="#409eff"><VideoPlay /></el-icon>
              <p class="build-title">准备构建样本</p>
              <p class="build-desc">
                将执行 Point-in-time join、样本质量检查、数据集划分和版本注册
              </p>
            </div>
            <el-button type="primary" size="large" :icon="VideoPlay" @click="startBuild">
              开始构建
            </el-button>
          </div>

          <div v-else-if="isBuilding" class="build-progress">
            <el-progress
              :percentage="buildProgress"
              :status="buildProgress === 100 ? 'success' : ''"
              :stroke-width="20"
              striped
              striped-flow
            />
            <p class="progress-status">{{ buildStatusText }}</p>
            <el-timeline class="build-timeline">
              <el-timeline-item
                v-for="(step, index) in buildSteps"
                :key="index"
                :type="step.status === 'done' ? 'success' : step.status === 'running' ? 'primary' : ''"
                :icon="step.status === 'done' ? CircleCheck : undefined"
              >
                {{ step.label }}
                <span v-if="step.status === 'running'" class="step-running">执行中...</span>
              </el-timeline-item>
            </el-timeline>
          </div>

          <div v-else-if="buildResult" class="build-result">
            <el-result
              :icon="buildResult.success ? 'success' : 'error'"
              :title="buildResult.success ? '构建成功' : '构建失败'"
              :sub-title="buildResult.message"
            >
              <template #extra>
                <el-button @click="resetBuild">重新构建</el-button>
                <el-button v-if="buildResult.success" type="primary" @click="goToDatasets">
                  查看数据集
                </el-button>
              </template>
            </el-result>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, VideoPlay, CircleCheck } from '@element-plus/icons-vue'
import { useSamplesStore } from '@/stores/samples'
import { LabelTypeLabels, SplitStrategyLabels } from '@/constants/status'

const router = useRouter()
const route = useRoute()
const samplesStore = useSamplesStore()

const selectedConfigId = ref(null)
const isBuilding = ref(false)
const buildProgress = ref(0)
const buildStatusText = ref('准备中...')
const buildResult = ref(null)

const buildSteps = ref([
  { label: 'Point-in-time join', status: 'pending' },
  { label: '合并特征与标签', status: 'pending' },
  { label: '负采样', status: 'pending' },
  { label: '样本质量检查', status: 'pending' },
  { label: '数据集划分', status: 'pending' },
  { label: '注册版本', status: 'pending' }
])

const activeConfigs = computed(() => samplesStore.activeConfigs)

const selectedConfig = computed(() => {
  return samplesStore.configs.find(c => c.id === selectedConfigId.value)
})

const handleConfigChange = () => {
  buildResult.value = null
  resetSteps()
}

const resetSteps = () => {
  buildSteps.value = buildSteps.value.map(s => ({ ...s, status: 'pending' }))
}

const startBuild = async () => {
  if (!selectedConfig.value) {
    ElMessage.warning('请先选择样本配置')
    return
  }

  isBuilding.value = true
  buildProgress.value = 0
  buildResult.value = null
  resetSteps()

  try {
    const response = await samplesStore.buildSample(selectedConfig.value)
    const jobId = response

    // 轮询任务状态
    await pollBuildStatus(jobId)
  } catch (error) {
    isBuilding.value = false
    buildResult.value = {
      success: false,
      message: error.message || '构建失败'
    }
    // 错误已由 request.js 拦截器统一提示
  }
}

const pollBuildStatus = async (jobId) => {
  const maxRetries = 60
  const interval = 2000

  for (let retry = 0; retry < maxRetries; retry++) {
    const status = await samplesStore.fetchBuildStatus(jobId)
    if (!status) {
      await delay(interval)
      continue
    }

    const progress = status.progress || 0
    buildProgress.value = progress
    buildStatusText.value = status.currentStep || '执行中...'

    // 更新步骤状态
    const stepIndex = Math.min(
      Math.floor((progress / 100) * buildSteps.value.length),
      buildSteps.value.length - 1
    )
    buildSteps.value.forEach((step, idx) => {
      if (idx < stepIndex) step.status = 'done'
      else if (idx === stepIndex) step.status = 'running'
      else step.status = 'pending'
    })

    if (status.status === 'SUCCESS') {
      buildSteps.value.forEach(s => { s.status = 'done' })
      buildProgress.value = 100
      buildStatusText.value = '构建完成'
      isBuilding.value = false
      buildResult.value = {
        success: true,
        message: `样本构建完成，Job ID: ${jobId}，样本数: ${status.entityCount || 0}`
      }
      ElMessage.success('样本构建成功')
      return
    }

    if (status.status === 'FAILED') {
      isBuilding.value = false
      buildResult.value = {
        success: false,
        message: status.errorMessage || '构建失败，请查看后端日志'
      }
      ElMessage.error('构建失败: ' + (status.errorMessage || '未知错误'))
      return
    }

    await delay(interval)
  }

  // 超时
  isBuilding.value = false
  buildResult.value = {
    success: false,
    message: '构建超时，请手动检查任务状态'
  }
  ElMessage.warning('构建轮询超时')
}

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const resetBuild = () => {
  buildResult.value = null
  buildProgress.value = 0
  resetSteps()
}

const goBack = () => {
  router.push({ name: 'SampleConfigList' })
}

const goToDatasets = () => {
  router.push({ name: 'DatasetList' })
}

// 标签类型/划分策略映射（统一从 constants/status.js 取）
const getLabelTypeLabel = (type) => LabelTypeLabels[type] || type
const getSplitStrategyLabel = (strategy) => SplitStrategyLabels[strategy] || strategy

const formatRatio = (ratio) => {
  if (ratio === undefined || ratio === null) return '0%'
  return Math.round(ratio * 100) + '%'
}

onMounted(() => {
  if (samplesStore.configs.length === 0) {
    samplesStore.fetchConfigs()
  }

  const configId = route.query.configId
  if (configId) {
    selectedConfigId.value = Number(configId)
  }
})
</script>

<style scoped lang="scss">
.sample-build-page {
  padding: 24px;
}

.back-nav {
  margin-bottom: 20px;
}

.config-card {
  .config-preview {
    margin-top: 20px;
  }
}

.build-card {
  min-height: 500px;
}

.build-placeholder,
.build-ready {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.build-info {
  text-align: center;
  margin-bottom: 32px;

  .build-title {
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
    margin: 16px 0 8px;
  }

  .build-desc {
    font-size: 14px;
    color: $text-secondary;
    margin: 0;
  }
}

.build-progress {
  padding: 20px 0;

  .progress-status {
    text-align: center;
    margin-top: 16px;
    font-size: 14px;
    color: $text-secondary;
  }
}

.build-timeline {
  margin-top: 32px;

  .step-running {
    margin-left: 8px;
    color: #409eff;
    font-size: 12px;
  }
}

.build-result {
  padding: 40px 0;
}
</style>
