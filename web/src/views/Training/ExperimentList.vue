<template>
  <div class="experiment-list">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="left-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索实验名称"
          clearable
          style="width: 260px; margin-right: 12px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filterStatus"
          placeholder="状态"
          clearable
          style="width: 120px; margin-right: 12px"
        >
          <el-option label="运行中" value="RUNNING" />
          <el-option label="已完成" value="COMPLETED" />
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
        <el-button type="primary" :icon="Plus" @click="handleCreate">创建实验</el-button>
      </div>
    </div>

    <!-- 实验表格 -->
    <el-table
      v-loading="trainingStore.loading.experiments"
      :data="displayExperiments"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="name" label="实验名称" min-width="180" fixed="left">
        <template #default="{ row }">
          <div class="experiment-name">
            <el-icon class="icon" color="#409eff"><Collection /></el-icon>
            <span class="name">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.description || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="modelType" label="模型类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.modelType === 'lightgbm' ? 'success' : 'warning'">
            {{ row.modelType === 'lightgbm' ? 'LightGBM' : 'XGBoost' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="datasetVersion" label="数据集版本" width="130" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.datasetVersion || '-' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="评估指标" width="150" align="center">
        <template #default="{ row }">
          <div v-if="row.metrics" class="metrics-cell">
            <el-tag
              v-for="(value, key) in formatMetrics(row.metrics)"
              :key="key"
              size="small"
              class="metric-tag"
            >
              {{ key }}: {{ value }}
            </el-tag>
          </div>
          <span v-else class="text-muted">-</span>
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

      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-tooltip content="查看详情" placement="top">
              <el-button size="small" :icon="View" circle @click.stop="handleView(row)" />
            </el-tooltip>
            <el-tooltip content="删除实验" placement="top">
              <el-button size="small" type="danger" :icon="Delete" circle @click.stop="handleDelete(row)" />
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 实验详情对话框 -->
    <el-dialog v-model="showDetail" title="实验详情" width="600px">
      <div v-if="selectedExperiment" class="experiment-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="实验ID">{{ selectedExperiment.id }}</el-descriptions-item>
          <el-descriptions-item label="实验名称">{{ selectedExperiment.name }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ selectedExperiment.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模型类型">{{ selectedExperiment.modelType }}</el-descriptions-item>
          <el-descriptions-item label="数据集版本">{{ selectedExperiment.datasetVersion }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedExperiment.status)">
              {{ getStatusLabel(selectedExperiment.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(selectedExperiment.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(selectedExperiment.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider v-if="selectedExperiment.params" content-position="left">参数</el-divider>
        <div v-if="selectedExperiment.params" class="params-section">
          <el-tag
            v-for="(value, key) in selectedExperiment.params"
            :key="key"
            size="large"
            class="param-tag"
          >
            {{ key }}: {{ typeof value === 'number' ? value.toFixed(4) : value }}
          </el-tag>
        </div>

        <el-divider v-if="selectedExperiment.metrics" content-position="left">评估指标</el-divider>
        <div v-if="selectedExperiment.metrics" class="metrics-section">
          <el-tag
            v-for="(value, key) in selectedExperiment.metrics"
            :key="key"
            size="large"
            type="success"
            class="metric-tag"
          >
            {{ key }}: {{ typeof value === 'number' ? value.toFixed(4) : value }}
          </el-tag>
        </div>
      </div>
    </el-dialog>

    <!-- 创建实验对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建实验" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="实验名称" required>
          <el-input v-model="createForm.name" placeholder="请输入实验名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" placeholder="实验描述" />
        </el-form-item>
        <el-form-item label="数据集版本" required>
          <el-input v-model="createForm.datasetVersion" placeholder="例如: dataset_v1.0" />
        </el-form-item>
        <el-form-item label="模型类型">
          <el-radio-group v-model="createForm.modelType">
            <el-radio-button label="lightgbm">LightGBM</el-radio-button>
            <el-radio-button label="xgboost">XGBoost</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateSubmit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Plus, View, Collection, Delete
} from '@element-plus/icons-vue'
import { useTrainingStore } from '@/stores/training'
import { formatDate } from '@/utils/date'
import { ExperimentStatusColors, ExperimentStatusLabels } from '@/constants/status'

const trainingStore = useTrainingStore()

const searchKeyword = ref('')
const filterStatus = ref('')
const filterModelType = ref('')
const showDetail = ref(false)
const selectedExperiment = ref(null)
const showCreateDialog = ref(false)
const creating = ref(false)

const createForm = ref({
  name: '',
  description: '',
  datasetVersion: '',
  modelType: 'lightgbm'
})

const displayExperiments = computed(() => {
  let experiments = trainingStore.experiments

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    experiments = experiments.filter(e =>
      (e.name && e.name.toLowerCase().includes(keyword)) ||
      (e.description && e.description.toLowerCase().includes(keyword))
    )
  }

  if (filterStatus.value) {
    experiments = experiments.filter(e => e.status === filterStatus.value)
  }

  if (filterModelType.value) {
    experiments = experiments.filter(e => e.modelType === filterModelType.value)
  }

  return experiments
})

const loadExperiments = async () => {
  try {
    await trainingStore.fetchExperiments()
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

const handleRefresh = () => {
  loadExperiments()
  ElMessage.success('刷新成功')
}

const handleView = (row) => {
  selectedExperiment.value = row
  showDetail.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除实验 "${row.name}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await trainingStore.deleteExperiment(row.name)
    ElMessage.success('删除成功')
    await loadExperiments()
  } catch (error) {
    if (error !== 'cancel') {
      // 错误已由 request.js 拦截器统一提示
    }
  }
}

const handleCreate = () => {
  createForm.value = { name: '', description: '', datasetVersion: '', modelType: 'lightgbm' }
  showCreateDialog.value = true
}

const handleCreateSubmit = async () => {
  if (!createForm.value.name || !createForm.value.datasetVersion) {
    ElMessage.warning('请填写必填项')
    return
  }

  creating.value = true
  try {
    await trainingStore.createExperiment(createForm.value)
    ElMessage.success('实验创建成功')
    showCreateDialog.value = false
    await loadExperiments()
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  } finally {
    creating.value = false
  }
}

// 状态映射（统一从 constants/status.js 取）
const getStatusType = (status) => ExperimentStatusColors[status] || 'info'
const getStatusLabel = (status) => ExperimentStatusLabels[status] || status

const formatMetrics = (metrics) => {
  if (!metrics) return {}
  const result = {}
  for (const [key, value] of Object.entries(metrics)) {
    result[key] = typeof value === 'number' ? value.toFixed(4) : value
  }
  return result
}

onMounted(() => {
  loadExperiments()
})
</script>

<style scoped lang="scss">
.experiment-list {
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

.experiment-name {
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

.metrics-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}

.metric-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.experiment-detail {
  .params-section,
  .metrics-section {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .param-tag,
  .metric-tag {
    margin-right: 8px;
    margin-bottom: 8px;
  }
}

.text-muted {
  color: #909399;
  font-size: 13px;
}

@media (max-width: 768px) {
  .experiment-list {
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
