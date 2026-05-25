<template>
  <div class="deployment-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title">K8s 部署管理</h3>
        <p class="page-desc">管理模型在 Kubernetes 集群中的部署实例</p>
      </div>
      <div class="header-right">
        <el-button :icon="Refresh" @click="refreshData">
          刷新
        </el-button>
      </div>
    </div>

    <!-- Namespace 选择器 + 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">
              <el-icon :size="24"><Box /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ deployments.length }}</div>
              <div class="stat-label">总部署数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f0f9ff; color: #67c23a;">
              <el-icon :size="24"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ runningCount }}</div>
              <div class="stat-label">运行中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">
              <el-icon :size="24"><Loading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ deployingCount }}</div>
              <div class="stat-label">部署中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #fef0f0; color: #f56c6c;">
              <el-icon :size="24"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ failedCount }}</div>
              <div class="stat-label">失败/停止</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-select
        v-model="currentNamespace"
        placeholder="选择 Namespace"
        clearable
        style="width: 200px"
        @change="handleNamespaceChange"
      >
        <el-option label="全部 Namespace" value="" />
        <el-option
          v-for="ns in namespaces"
          :key="ns.name"
          :label="ns.displayName || ns.name"
          :value="ns.name"
        />
      </el-select>

      <el-input
        v-model="searchKeyword"
        placeholder="搜索模型名称"
        clearable
        style="width: 260px; margin-left: 12px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-button
        type="primary"
        :icon="Plus"
        style="margin-left: auto"
        @click="showDeployDialog = true"
      >
        新建部署
      </el-button>
    </div>

    <!-- 部署列表 -->
    <el-card v-loading="deploymentStore.loading.deployments" shadow="never">
      <el-table :data="filteredDeployments" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="模型" min-width="180">
          <template #default="{ row }">
            <div class="model-info">
              <el-icon color="#409eff"><Box /></el-icon>
              <div>
                <div class="model-name">{{ row.model?.name || '-' }}</div>
                <div class="model-version">{{ row.model?.version || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="namespace" label="Namespace" width="120" />
        <el-table-column prop="deploymentName" label="Deployment" min-width="200" show-overflow-tooltip />
        <el-table-column label="副本" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getReplicaType(row)">
              {{ row.readyReplicas || 0 }}/{{ row.replicas }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="image" label="镜像" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" effect="dark">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="K8s 状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getK8sStatusType(row.k8sStatus)" size="small">
              {{ row.k8sStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deployedAt" label="部署时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.deployedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewPods(row)">
              Pod
            </el-button>
            <el-button size="small" type="primary" @click="handleScale(row)">
              扩缩容
            </el-button>
            <el-button
              v-if="row.status !== 'STOPPED'"
              size="small"
              type="danger"
              @click="handleUndeploy(row)"
            >
              下线
            </el-button>
            <el-button
              v-else
              size="small"
              type="success"
              @click="handleRedeploy(row)"
            >
              重新部署
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="filteredDeployments.length === 0" description="暂无部署实例" />
    </el-card>

    <!-- 新建部署对话框 -->
    <el-dialog v-model="showDeployDialog" title="新建部署" width="600px">
      <el-form :model="deployForm" label-width="120px">
        <el-form-item label="模型" required>
          <el-select v-model="deployForm.modelId" placeholder="选择模型" style="width: 100%">
            <el-option
              v-for="m in modelList"
              :key="m.id"
              :label="`${m.name} (${m.version})`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Namespace" required>
          <el-select v-model="deployForm.namespace" placeholder="选择 Namespace" style="width: 100%">
            <el-option
              v-for="ns in namespaces"
              :key="ns.name"
              :label="`${ns.displayName || ns.name} (${ns.businessLine})`"
              :value="ns.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="副本数">
          <el-input-number v-model="deployForm.replicas" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="镜像">
          <el-input v-model="deployForm.image" placeholder="默认使用 mmodelx-inference:latest" />
        </el-form-item>
        <el-form-item label="资源限制">
          <el-row :gutter="12">
            <el-col :span="12">
              <el-input v-model="deployForm.cpuRequest" placeholder="CPU 请求">
                <template #prepend>CPU</template>
              </el-input>
            </el-col>
            <el-col :span="12">
              <el-input v-model="deployForm.memoryRequest" placeholder="内存请求">
                <template #prepend>内存</template>
              </el-input>
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDeployDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="deploymentStore.loading.deploy"
          @click="confirmDeploy"
        >
          确认部署
        </el-button>
      </template>
    </el-dialog>

    <!-- 扩缩容对话框 -->
    <el-dialog v-model="showScaleDialog" title="扩缩容" width="400px">
      <div class="scale-dialog-content">
        <p>Deployment: {{ scaleForm.deploymentName }}</p>
        <el-slider v-model="scaleForm.replicas" :min="1" :max="20" show-stops />
        <div class="scale-value">副本数: {{ scaleForm.replicas }}</div>
      </div>
      <template #footer>
        <el-button @click="showScaleDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="deploymentStore.loading.scale"
          @click="confirmScale"
        >
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- Pod 列表抽屉 -->
    <el-drawer v-model="showPodDrawer" :title="`Pod 列表 - ${selectedDeployment?.deploymentName || ''}`" size="60%">
      <InstanceList
        :deployment-id="selectedDeployment?.id"
        :deployment-name="selectedDeployment?.deploymentName"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Box, CircleCheck, Loading, Warning, Search, Refresh, Plus
} from '@element-plus/icons-vue'
import { useDeploymentStore } from '@/stores/deployment'
import { useTrainingStore } from '@/stores/training'
import InstanceList from './InstanceList.vue'

const deploymentStore = useDeploymentStore()
const trainingStore = useTrainingStore()

// 搜索和筛选
const searchKeyword = ref('')
const currentNamespace = ref('')

// 部署对话框
const showDeployDialog = ref(false)
const deployForm = ref({
  modelId: null,
  namespace: '',
  replicas: 2,
  image: '',
  cpuRequest: '500m',
  memoryRequest: '512Mi',
  cpuLimit: '2000m',
  memoryLimit: '2Gi'
})

// 扩缩容对话框
const showScaleDialog = ref(false)
const scaleForm = ref({
  id: null,
  deploymentName: '',
  replicas: 1
})

// Pod 抽屉
const showPodDrawer = ref(false)
const selectedDeployment = ref(null)

// 计算属性
const deployments = computed(() => deploymentStore.deployments)
const namespaces = computed(() => deploymentStore.namespaces)
const modelList = computed(() => trainingStore.models)

const filteredDeployments = computed(() => {
  let result = deploymentStore.filteredDeployments
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(d =>
      (d.model?.name || '').toLowerCase().includes(kw) ||
      (d.deploymentName || '').toLowerCase().includes(kw) ||
      (d.namespace || '').toLowerCase().includes(kw)
    )
  }
  return result
})

const runningCount = computed(() =>
  deployments.value.filter(d => d.status === 'RUNNING').length
)
const deployingCount = computed(() =>
  deployments.value.filter(d => d.status === 'DEPLOYING').length
)
const failedCount = computed(() =>
  deployments.value.filter(d => d.status === 'FAILED' || d.status === 'STOPPED').length
)

// 方法
const refreshData = async () => {
  try {
    await Promise.all([
      deploymentStore.fetchNamespaces(),
      deploymentStore.fetchDeployments(),
      trainingStore.fetchModels()
    ])
    ElMessage.success('刷新成功')
  } catch (error) {
    ElMessage.error('刷新失败: ' + error.message)
  }
}

const handleNamespaceChange = async (val) => {
  deploymentStore.setCurrentNamespace(val)
  if (val) {
    await deploymentStore.fetchDeploymentsByNamespace(val)
  } else {
    await deploymentStore.fetchDeployments()
  }
}

const confirmDeploy = async () => {
  if (!deployForm.value.modelId || !deployForm.value.namespace) {
    ElMessage.warning('请选择模型和 Namespace')
    return
  }
  try {
    const request = {
      modelId: deployForm.value.modelId,
      namespace: deployForm.value.namespace,
      replicas: deployForm.value.replicas,
      image: deployForm.value.image || undefined,
      cpuRequest: deployForm.value.cpuRequest,
      memoryRequest: deployForm.value.memoryRequest,
      cpuLimit: deployForm.value.cpuLimit,
      memoryLimit: deployForm.value.memoryLimit
    }
    await deploymentStore.deployModel(request)
    ElMessage.success('部署成功')
    showDeployDialog.value = false
    resetDeployForm()
  } catch (error) {
    ElMessage.error('部署失败: ' + error.message)
  }
}

const handleUndeploy = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认下线部署 ${row.deploymentName}?`,
      '确认下线',
      { type: 'warning' }
    )
    await deploymentStore.undeploy(row.id)
    ElMessage.success('下线成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('下线失败: ' + error.message)
    }
  }
}

const handleScale = (row) => {
  scaleForm.value = {
    id: row.id,
    deploymentName: row.deploymentName,
    replicas: row.replicas
  }
  showScaleDialog.value = true
}

const confirmScale = async () => {
  try {
    await deploymentStore.scaleDeployment(scaleForm.value.id, scaleForm.value.replicas)
    ElMessage.success('扩缩容成功')
    showScaleDialog.value = false
  } catch (error) {
    ElMessage.error('扩缩容失败: ' + error.message)
  }
}

const handleViewPods = (row) => {
  selectedDeployment.value = row
  showPodDrawer.value = true
}

const handleRedeploy = (row) => {
  deployForm.value = {
    modelId: row.model?.id,
    namespace: row.namespace,
    replicas: row.replicas,
    image: row.image,
    cpuRequest: row.cpuRequest,
    memoryRequest: row.memoryRequest,
    cpuLimit: row.cpuLimit,
    memoryLimit: row.memoryLimit
  }
  showDeployDialog.value = true
}

const resetDeployForm = () => {
  deployForm.value = {
    modelId: null,
    namespace: '',
    replicas: 2,
    image: '',
    cpuRequest: '500m',
    memoryRequest: '512Mi',
    cpuLimit: '2000m',
    memoryLimit: '2Gi'
  }
}

// 状态映射
const getStatusType = (status) => {
  const types = {
    RUNNING: 'success',
    DEPLOYING: 'warning',
    STOPPED: 'info',
    FAILED: 'danger'
  }
  return types[status] || 'info'
}

const getStatusLabel = (status) => {
  const labels = {
    RUNNING: '运行中',
    DEPLOYING: '部署中',
    STOPPED: '已停止',
    FAILED: '失败'
  }
  return labels[status] || status
}

const getK8sStatusType = (status) => {
  const types = {
    Running: 'success',
    Scaling: 'warning',
    Deploying: 'primary',
    Stopped: 'info',
    NotFound: 'danger'
  }
  return types[status] || 'info'
}

const getReplicaType = (row) => {
  const ready = row.readyReplicas || 0
  const total = row.replicas || 1
  if (ready >= total) return 'success'
  if (ready > 0) return 'warning'
  return 'danger'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  refreshData()
})
</script>

<style scoped lang="scss">
.deployment-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .page-title {
    margin: 0 0 8px 0;
    font-size: 20px;
    font-weight: 600;
    color: $text-primary;
  }

  .page-desc {
    margin: 0;
    font-size: 14px;
    color: $text-muted;
  }
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    margin-right: 16px;
  }

  .stat-info {
    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: $text-primary;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: $text-muted;
      margin-top: 4px;
    }
  }
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.model-info {
  display: flex;
  align-items: center;
  gap: 8px;

  .model-name {
    font-weight: 500;
    color: $text-primary;
  }

  .model-version {
    font-size: 12px;
    color: $text-muted;
  }
}

.scale-dialog-content {
  text-align: center;
  padding: 20px 0;

  p {
    margin-bottom: 20px;
    color: $text-primary;
  }

  .scale-value {
    margin-top: 16px;
    font-size: 18px;
    font-weight: 600;
    color: #409eff;
  }
}
</style>
