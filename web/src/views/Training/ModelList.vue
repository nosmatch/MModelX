<template>
  <div class="model-list">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="left-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索模型名称"
          clearable
          style="width: 260px; margin-right: 12px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filterStage"
          placeholder="阶段"
          clearable
          style="width: 120px; margin-right: 12px"
        >
          <el-option label="暂存" value="Staging" />
          <el-option label="生产" value="Production" />
          <el-option label="归档" value="Archived" />
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
      </div>
    </div>

    <!-- 模型表格 -->
    <el-table
      v-loading="trainingStore.loading.models"
      :data="displayModels"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="name" label="模型名称" min-width="180" fixed="left">
        <template #default="{ row }">
          <div class="model-name">
            <el-icon class="icon" color="#409eff"><Box /></el-icon>
            <span class="name">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="version" label="版本" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.version || '-' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="modelType" label="模型类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.modelType === 'lightgbm' ? 'success' : 'warning'">
            {{ row.modelType === 'lightgbm' ? 'LightGBM' : 'XGBoost' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="stage" label="阶段" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStageType(row.stage)" size="small">
            {{ getStageLabel(row.stage) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="performance" label="性能指标" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.performance" class="performance-value">
            {{ row.performance.toFixed(4) }}
          </span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <el-table-column prop="experimentId" label="实验ID" width="100" align="center">
        <template #default="{ row }">
          <span class="text-muted">{{ row.experimentId || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="modelPath" label="模型路径" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="text-muted">{{ row.modelPath || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
        <template #default="{ row }">
          <span>{{ formatDate(row.createdAt) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-tooltip
              v-if="row.stage !== 'Production'"
              content="提升到生产环境"
              placement="top"
            >
              <el-button
                size="small"
                type="success"
                :icon="Promotion"
                circle
                @click.stop="handlePromote(row)"
              />
            </el-tooltip>
            <el-tooltip
              v-if="row.stage !== 'Archived'"
              content="归档模型"
              placement="top"
            >
              <el-button
                size="small"
                type="warning"
                :icon="FolderRemove"
                circle
                @click.stop="handleArchive(row)"
              />
            </el-tooltip>
            <el-tooltip content="查看详情" placement="top">
              <el-button size="small" :icon="View" circle @click.stop="handleView(row)" />
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 模型详情对话框 -->
    <el-dialog v-model="showDetail" title="模型详情" width="600px">
      <div v-if="selectedModel" class="model-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模型ID">{{ selectedModel.id }}</el-descriptions-item>
          <el-descriptions-item label="模型名称">{{ selectedModel.name }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ selectedModel.version }}</el-descriptions-item>
          <el-descriptions-item label="实验ID">{{ selectedModel.experimentId }}</el-descriptions-item>
          <el-descriptions-item label="模型类型">{{ selectedModel.modelType }}</el-descriptions-item>
          <el-descriptions-item label="阶段">
            <el-tag :type="getStageType(selectedModel.stage)">
              {{ getStageLabel(selectedModel.stage) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="性能指标">
            {{ selectedModel.performance ? selectedModel.performance.toFixed(4) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(selectedModel.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">模型路径</el-divider>
        <el-input v-model="selectedModel.modelPath" readonly>
          <template #append>
            <el-button @click="copyPath">复制</el-button>
          </template>
        </el-input>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, View, Box, Promotion, FolderRemove
} from '@element-plus/icons-vue'
import { useTrainingStore } from '@/stores/training'
import { formatDate } from '@/utils/date'
import { ModelStageColors, ModelStageLabels } from '@/constants/status'

const trainingStore = useTrainingStore()

const searchKeyword = ref('')
const filterStage = ref('')
const filterModelType = ref('')
const showDetail = ref(false)
const selectedModel = ref(null)

const displayModels = computed(() => {
  let models = trainingStore.models

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    models = models.filter(m =>
      m.name && m.name.toLowerCase().includes(keyword)
    )
  }

  if (filterStage.value) {
    models = models.filter(m => m.stage === filterStage.value)
  }

  if (filterModelType.value) {
    models = models.filter(m => m.modelType === filterModelType.value)
  }

  return models
})

const loadModels = async () => {
  try {
    await trainingStore.fetchModels()
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

const handleRefresh = () => {
  loadModels()
  ElMessage.success('刷新成功')
}

const handleView = (row) => {
  selectedModel.value = row
  showDetail.value = true
}

const handlePromote = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定将模型 "${row.name}" (版本 ${row.version}) 提升到生产环境吗？`,
      '确认操作',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await trainingStore.transitionModelStage(row.name, row.version, 'Production')
    ElMessage.success('模型已提升到生产环境')
    await loadModels()
  } catch (error) {
    if (error === 'cancel') return
    // 错误已由 request.js 拦截器统一提示
  }
}

const handleArchive = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定将模型 "${row.name}" (版本 ${row.version}) 归档吗？`,
      '确认操作',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await trainingStore.transitionModelStage(row.name, row.version, 'Archived')
    ElMessage.success('模型已归档')
    await loadModels()
  } catch (error) {
    if (error === 'cancel') return
    // 错误已由 request.js 拦截器统一提示
  }
}

const copyPath = () => {
  if (selectedModel.value?.modelPath) {
    navigator.clipboard.writeText(selectedModel.value.modelPath)
    ElMessage.success('路径已复制')
  }
}

// 阶段映射（统一从 constants/status.js 取）
const getStageType = (stage) => ModelStageColors[stage] || 'info'
const getStageLabel = (stage) => ModelStageLabels[stage] || stage

onMounted(() => {
  loadModels()
})
</script>

<style scoped lang="scss">
.model-list {
  padding: 24px;
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

.model-name {
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

.performance-value {
  color: #67c23a;
  font-weight: 600;
}

.text-muted {
  color: #909399;
  font-size: 13px;
}

.model-detail {
  padding: 10px 0;
}

@media (max-width: 768px) {
  .model-list {
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
