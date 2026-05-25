<template>
  <div class="serving-page">
    <!-- 健康状态 -->
    <div class="health-bar">
      <el-tag v-if="healthStatus?.status === 'healthy'" type="success" effect="dark">
        <el-icon><CircleCheck /></el-icon> 服务正常
      </el-tag>
      <el-tag v-else type="danger" effect="dark">
        <el-icon><CircleClose /></el-icon> 服务异常
      </el-tag>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <stat-card :icon="Box" tone="primary" :value="models.length" label="总模型数" />
      </el-col>
      <el-col :span="6">
        <stat-card :icon="CircleCheck" tone="success" :value="productionModels.length" label="生产环境" />
      </el-col>
      <el-col :span="6">
        <stat-card :icon="Timer" tone="warning" :value="stagingModels.length" label="暂存环境" />
      </el-col>
      <el-col :span="6">
        <stat-card :icon="Cpu" tone="success" :value="modelCacheInfo.length" label="缓存模型" />
      </el-col>
    </el-row>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" type="border-card" class="serving-tabs">
      <!-- 模型注册表 -->
      <el-tab-pane label="模型注册表" name="registry">
        <div class="tab-toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索模型名称"
            clearable
            style="width: 260px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="filterStage" placeholder="阶段筛选" clearable style="width: 140px; margin-left: 12px">
            <el-option label="全部" value="" />
            <el-option label="生产" value="Production" />
            <el-option label="暂存" value="Staging" />
            <el-option label="归档" value="Archived" />
          </el-select>
          <el-button :icon="Refresh" style="margin-left: auto" @click="refreshData">
            刷新
          </el-button>
        </div>

        <el-table v-loading="servingStore.loading.models" :data="filteredModels" stripe>
          <el-table-column prop="name" label="模型名称" min-width="180">
            <template #default="{ row }">
              <div class="model-name">
                <el-icon color="#409eff"><Box /></el-icon>
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="120" />
          <el-table-column prop="modelType" label="框架" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.modelType === 'lightgbm' ? 'success' : 'warning'">
                {{ row.modelType === 'lightgbm' ? 'LightGBM' : 'XGBoost' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="stage" label="阶段" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getStageType(row.stage)" size="small" effect="dark">
                {{ getStageLabel(row.stage) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="performance" label="性能指标" width="120" align="center">
            <template #default="{ row }">
              <span v-if="row.performance">{{ row.performance.toFixed(4) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="modelPath" label="模型路径" min-width="200" show-overflow-tooltip />
          <el-table-column prop="updatedAt" label="更新时间" width="160" />
          <el-table-column label="操作" width="220" fixed="right" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="handleDeploy(row)">
                部署
              </el-button>
              <el-button
                v-if="row.stage !== 'Production'"
                size="small"
                type="success"
                @click="handlePromote(row)"
              >
                上线
              </el-button>
              <el-button size="small" @click="handleTestPredict(row)">
                测试
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- A/B 测试 -->
      <el-tab-pane label="A/B 测试" name="abtest">
        <el-row :gutter="24">
          <el-col :span="10">
            <el-card title="A/B 测试配置">
              <template #header>
                <div class="card-header">A/B 测试配置</div>
              </template>
              <el-form :model="abTestForm" label-width="100px">
                <el-form-item label="模型 A">
                  <el-select v-model="abTestForm.modelA" placeholder="选择模型A" style="width: 100%">
                    <el-option
                      v-for="m in productionModels"
                      :key="m.name + ':' + m.version"
                      :label="m.name + ' (' + m.version + ')'"
                      :value="m.name"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="模型 B">
                  <el-select v-model="abTestForm.modelB" placeholder="选择模型B" style="width: 100%">
                    <el-option
                      v-for="m in models"
                      :key="m.name + ':' + m.version"
                      :label="m.name + ' (' + m.version + ')'"
                      :value="m.name"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="流量分配">
                  <div class="ratio-display">
                    <span class="ratio-label">模型 A: {{ abTestForm.ratio }}%</span>
                    <el-slider v-model="abTestForm.ratio" :min="0" :max="100" show-input />
                    <span class="ratio-label">模型 B: {{ 100 - abTestForm.ratio }}%</span>
                  </div>
                </el-form-item>
                <el-form-item label="实体类型">
                  <el-input v-model="abTestForm.entityType" placeholder="例如: user" />
                </el-form-item>
                <el-form-item label="实体ID">
                  <el-input v-model="abTestForm.entityId" placeholder="例如: 12345" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="servingStore.loading.predict" @click="handleAbTest">
                    执行 A/B 测试
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card>
              <template #header>
                <div class="card-header">测试结果</div>
              </template>
              <div v-if="!servingStore.predictionResult" class="empty-result">
                <el-empty description="配置参数后执行测试" />
              </div>
              <div v-else class="result-content">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="选中模型">
                    {{ servingStore.predictionResult.modelInfo?.modelName }}
                  </el-descriptions-item>
                  <el-descriptions-item label="预测结果">
                    <el-tag size="large" type="primary">
                      {{ servingStore.predictionResult.prediction?.toFixed(4) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="延迟">
                    {{ servingStore.predictionResult.latency }}ms
                  </el-descriptions-item>
                  <el-descriptions-item label="实体">
                    {{ servingStore.predictionResult.entityType }}: {{ servingStore.predictionResult.entityId }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 在线预测 -->
      <el-tab-pane label="在线预测" name="predict">
        <el-row :gutter="24">
          <el-col :span="10">
            <el-card>
              <template #header>
                <div class="card-header">在线预测</div>
              </template>
              <el-form :model="predictForm" label-width="100px">
                <el-form-item label="模型名称">
                  <el-select v-model="predictForm.modelName" placeholder="选择模型" style="width: 100%">
                    <el-option
                      v-for="m in productionModels"
                      :key="m.name"
                      :label="m.name + ' (' + m.version + ')'"
                      :value="m.name"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="实体类型">
                  <el-input v-model="predictForm.entityType" placeholder="例如: user" />
                </el-form-item>
                <el-form-item label="实体ID">
                  <el-input v-model="predictForm.entityId" placeholder="例如: 12345" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="servingStore.loading.predict" @click="handlePredict">
                    执行预测
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card>
              <template #header>
                <div class="card-header">预测结果</div>
              </template>
              <div v-if="!servingStore.predictionResult" class="empty-result">
                <el-empty description="配置参数后执行预测" />
              </div>
              <div v-else class="result-content">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="预测值">
                    <el-tag size="large" type="success" effect="dark">
                      {{ servingStore.predictionResult.prediction?.toFixed(4) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="延迟">
                    {{ servingStore.predictionResult.latency }}ms
                  </el-descriptions-item>
                  <el-descriptions-item label="时间戳">
                    {{ formatDate(servingStore.predictionResult.timestamp) }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 服务监控 -->
      <el-tab-pane label="服务监控" name="monitor">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">服务状态</div>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="服务名称">
                  {{ serviceStatus?.service || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="版本">
                  {{ serviceStatus?.version || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="serviceStatus?.status === 'running' ? 'success' : 'danger'">
                    {{ serviceStatus?.status || 'unknown' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="缓存大小">
                  {{ serviceStatus?.modelCache?.cacheSize || 0 }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">模型缓存</div>
              </template>
              <div v-if="modelCacheInfo.length === 0" class="empty-cache">
                <el-empty description="暂无缓存模型" />
              </div>
              <div v-else class="cache-list">
                <el-tag
                  v-for="model in modelCacheInfo"
                  :key="model"
                  size="large"
                  type="info"
                  class="cache-tag"
                >
                  {{ model }}
                </el-tag>
              </div>
              <div class="cache-actions">
                <el-button type="danger" size="small" @click="handleClearCache">
                  清空缓存
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 部署对话框 -->
    <el-dialog v-model="showDeployDialog" title="部署模型" width="500px">
      <el-form :model="deployForm" label-width="100px">
        <el-form-item label="模型名称">
          <el-input v-model="deployForm.modelName" disabled />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="deployForm.version" disabled />
        </el-form-item>
        <el-form-item label="目标环境">
          <el-select v-model="deployForm.targetStage" style="width: 100%">
            <el-option label="生产环境" value="Production" />
            <el-option label="暂存环境" value="Staging" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDeployDialog = false">取消</el-button>
        <el-button type="primary" :loading="servingStore.loading.reload" @click="confirmDeploy">
          确认部署
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Box, CircleCheck, CircleClose, Cpu, Search, Refresh, Timer
} from '@element-plus/icons-vue'
import { useServingStore } from '@/stores/serving'
import { formatDateTime as formatDate } from '@/utils/date'
import { ModelStageColors, ModelStageLabels } from '@/constants/status'
import StatCard from '@/components/StatCard.vue'

const servingStore = useServingStore()
const activeTab = ref('registry')

// 搜索和筛选
const searchKeyword = ref('')
const filterStage = ref('')

// 表单
const abTestForm = ref({
  modelA: '',
  modelB: '',
  ratio: 50,
  entityType: 'user',
  entityId: ''
})

const predictForm = ref({
  modelName: '',
  entityType: 'user',
  entityId: ''
})

// 部署对话框
const showDeployDialog = ref(false)
const deployForm = ref({
  modelName: '',
  version: '',
  targetStage: 'Production'
})

// 计算属性
const models = computed(() => servingStore.models)
const productionModels = computed(() => servingStore.productionModels)
const healthStatus = computed(() => servingStore.healthStatus)
const serviceStatus = computed(() => servingStore.serviceStatus)
const modelCacheInfo = computed(() => servingStore.modelCacheInfo)

const filteredModels = computed(() => {
  let result = models.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(m => m.name?.toLowerCase().includes(kw))
  }
  if (filterStage.value) {
    result = result.filter(m => m.stage === filterStage.value)
  }
  return result
})

// 阶段映射（统一从 constants/status.js 取，与 ModelList 保持一致）
const getStageType = (stage) => ModelStageColors[stage] || 'info'
const getStageLabel = (stage) => ModelStageLabels[stage] || stage

// 刷新数据
const refreshData = async () => {
  try {
    await servingStore.fetchModels()
    await servingStore.fetchServiceStatus()
    await servingStore.fetchHealthStatus()
    ElMessage.success('刷新成功')
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

// 部署
const handleDeploy = (row) => {
  deployForm.value = {
    modelName: row.name,
    version: row.version,
    targetStage: 'Production'
  }
  showDeployDialog.value = true
}

const confirmDeploy = async () => {
  try {
    await servingStore.hotReloadModel(deployForm.value.modelName, deployForm.value.version)
    ElMessage.success('模型部署成功')
    showDeployDialog.value = false
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

// 上线
const handlePromote = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认将模型 ${row.name} (${row.version}) 升级到生产环境?`,
      '确认上线',
      { type: 'warning' }
    )
    await servingStore.transitionModelStage(row.name, row.version, 'Production')
    ElMessage.success('模型已上线')
  } catch (error) {
    if (error === 'cancel') return
    // 错误已由 request.js 拦截器统一提示
  }
}

// 测试预测
const handleTestPredict = (row) => {
  predictForm.value.modelName = row.name
  activeTab.value = 'predict'
}

// 预测
const handlePredict = async () => {
  if (!predictForm.value.modelName || !predictForm.value.entityId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await servingStore.doPredict({
      modelName: predictForm.value.modelName,
      entityType: predictForm.value.entityType,
      entityId: predictForm.value.entityId,
      includeDetails: true
    })
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

// A/B 测试
const handleAbTest = async () => {
  if (!abTestForm.value.modelA || !abTestForm.value.modelB || !abTestForm.value.entityId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await servingStore.doAbTestPredict(
      {
        modelName: abTestForm.value.modelA,
        entityType: abTestForm.value.entityType,
        entityId: abTestForm.value.entityId,
        includeDetails: true
      },
      abTestForm.value.modelA,
      abTestForm.value.modelB,
      abTestForm.value.ratio / 100
    )
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

// 清空缓存
const handleClearCache = async () => {
  try {
    await ElMessageBox.confirm('确认清空所有模型缓存?', '警告', { type: 'warning' })
    await servingStore.clearCache()
    ElMessage.success('缓存已清空')
  } catch (error) {
    if (error === 'cancel') return
    // 错误已由 request.js 拦截器统一提示
  }
}

// 生命周期
onMounted(() => {
  refreshData()
})
</script>

<style scoped lang="scss">
.serving-page {
  padding: 24px;
}

.health-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.stats-row {
  margin-bottom: 24px;
}

.serving-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__content) {
    padding: 20px;
    background: #fff;
    border: 1px solid var(--el-border-color-light);
    border-top: none;
  }
}

.tab-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.model-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header {
  font-weight: 600;
}

.empty-result,
.empty-cache {
  padding: 40px 0;
}

.result-content {
  padding: 10px 0;
}

.ratio-display {
  display: flex;
  align-items: center;
  gap: 16px;

  .ratio-label {
    font-size: 14px;
    color: $text-primary;
    white-space: nowrap;
  }

  :deep(.el-slider) {
    flex: 1;
  }
}

.cache-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;

  .cache-tag {
    margin-bottom: 8px;
  }
}

.cache-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
