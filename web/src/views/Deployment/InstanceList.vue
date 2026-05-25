<template>
  <div class="instance-list">
    <div class="instance-toolbar">
      <el-button :icon="Refresh" size="small" @click="refreshPods">
        刷新
      </el-button>
    </div>

    <el-table v-loading="deploymentStore.loading.pods" :data="pods" stripe>
      <el-table-column prop="name" label="Pod 名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="namespace" label="Namespace" width="120" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small" effect="dark">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phase" label="阶段" width="100" align="center" />
      <el-table-column prop="podIp" label="Pod IP" width="130" />
      <el-table-column prop="nodeName" label="节点" width="150" show-overflow-tooltip />
      <el-table-column prop="restartCount" label="重启次数" width="100" align="center" />
      <el-table-column prop="ready" label="就绪" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.ready === 'True' ? 'success' : 'danger'" size="small">
            {{ row.ready === 'True' ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="启动时间" width="160">
        <template #default="{ row }">
          {{ formatDate(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="handleViewLogs(row)">
            日志
          </el-button>
          <el-button size="small" type="warning" @click="handleRestart(row)">
            重启
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="pods.length === 0 && !deploymentStore.loading.pods" description="暂无 Pod" />

    <!-- 日志对话框 -->
    <el-dialog
      v-model="showLogDialog"
      :title="`Pod 日志 - ${selectedPod?.name || ''}`"
      width="80%"
      top="5vh"
    >
      <div class="log-toolbar">
        <el-input-number v-model="logTailLines" :min="10" :max="1000" :step="10" size="small">
          <template #suffix>行</template>
        </el-input-number>
        <el-button size="small" :icon="Refresh" @click="refreshLogs">
          刷新
        </el-button>
      </div>
      <el-input
        v-loading="deploymentStore.loading.logs"
        v-model="deploymentStore.podLogs"
        type="textarea"
        :rows="25"
        readonly
        class="log-content"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useDeploymentStore } from '@/stores/deployment'

const props = defineProps({
  deploymentId: {
    type: Number,
    default: null
  },
  deploymentName: {
    type: String,
    default: ''
  }
})

const deploymentStore = useDeploymentStore()

const showLogDialog = ref(false)
const selectedPod = ref(null)
const logTailLines = ref(100)

const pods = computed(() => deploymentStore.pods)

const refreshPods = async () => {
  if (!props.deploymentId) return
  try {
    await deploymentStore.fetchPods(props.deploymentId)
  } catch (error) {
    ElMessage.error('刷新 Pod 列表失败: ' + error.message)
  }
}

const handleViewLogs = (row) => {
  selectedPod.value = row
  showLogDialog.value = true
  fetchLogs()
}

const fetchLogs = async () => {
  if (!props.deploymentId || !selectedPod.value) return
  try {
    await deploymentStore.fetchPodLogs(props.deploymentId, selectedPod.value.name, logTailLines.value)
  } catch (error) {
    ElMessage.error('获取日志失败: ' + error.message)
  }
}

const refreshLogs = () => {
  fetchLogs()
}

const handleRestart = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认重启 Pod ${row.name}?`,
      '确认重启',
      { type: 'warning' }
    )
    await deploymentStore.restartPod(props.deploymentId, row.name)
    ElMessage.success('重启成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重启失败: ' + error.message)
    }
  }
}

const getStatusType = (status) => {
  const types = {
    Running: 'success',
    Pending: 'warning',
    Failed: 'danger',
    Succeeded: 'primary'
  }
  return types[status] || 'info'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 监听 deploymentId 变化，自动加载 Pod
watch(() => props.deploymentId, (newId) => {
  if (newId) {
    refreshPods()
  }
}, { immediate: true })
</script>

<style scoped lang="scss">
.instance-list {
  padding: 0;
}

.instance-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.log-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.log-content {
  :deep(.el-textarea__inner) {
    font-family: 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.6;
    background: #1a1a2e;
    color: #e0e0e0;
    border: none;
    border-radius: 4px;
    padding: 16px;
  }
}
</style>
