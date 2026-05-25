<template>
  <div class="datasource-list" style="padding:24px">
    <!-- 工具栏 -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <el-input v-model="keyword" placeholder="搜索名称或描述" clearable style="width:260px"
        @input="onSearch" />
      <el-button type="primary" :icon="Plus" @click="openCreate">创建数据源</el-button>
    </div>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="连接信息" min-width="200">
        <template #default="{ row }">
          <span class="conn-info">{{ connInfo(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : row.status === 'ERROR' ? 'danger' : 'info'" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后测试" width="160" align="center">
        <template #default="{ row }">
          <span v-if="row.lastTestedAt">
            <el-icon :color="row.lastTestResult ? '#67c23a' : '#f56c6c'">
              <component :is="row.lastTestResult ? 'CircleCheck' : 'CircleClose'" />
            </el-icon>
            {{ formatDate(row.lastTestedAt) }}
          </span>
          <span v-else style="color:#999">未测试</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right" align="center">
        <template #default="{ row }">
          <el-button size="small" :loading="testingId === row.id" @click="onTest(row)">测试</el-button>
          <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'ACTIVE'" size="small" @click="onToggle(row)">禁用</el-button>
          <el-button v-else size="small" type="success" @click="onToggle(row)">启用</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 弹窗 -->
    <DataSourceDialog
      v-if="dialogVisible"
      :visible="dialogVisible"
      :mode="dialogMode"
      :data="dialogData"
      @close="dialogVisible = false"
      @save="onSave"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import {
  listDataSources, createDataSource, updateDataSource,
  deleteDataSource, testConnection, enableDataSource, disableDataSource
} from '@/api/modules/datasources'
import DataSourceDialog from './DataSourceDialog.vue'
import { formatDate } from '@/utils/date'

const TYPE_LABELS = {
  postgresql: 'PostgreSQL', mysql: 'MySQL', redis: 'Redis',
  kafka: 'Kafka', minio: 'MinIO', api: 'HTTP API', local_file: '本地文件'
}
const STATUS_LABELS = { ACTIVE: '启用', DISABLED: '禁用', ERROR: '错误' }

const loading = ref(false)
const list = ref([])
const keyword = ref('')
const testingId = ref(null)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const dialogData = ref(null)

const typeLabel = (t) => TYPE_LABELS[t] || t
const statusLabel = (s) => STATUS_LABELS[s] || s
const connInfo = (row) => {
  if (row.type === 'api') return row.host || '-'
  if (row.type === 'local_file') return '-'
  const parts = [row.host, row.port ? `:${row.port}` : '', row.databaseName ? `/${row.databaseName}` : '']
  return parts.join('') || '-'
}

async function load() {
  loading.value = true
  try {
    const res = await listDataSources()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function onSearch() {
  // 本地过滤（后端已返回全量激活数据）
  // 如需服务端搜索可改为调用 searchDataSources
}

function openCreate() {
  dialogMode.value = 'create'
  dialogData.value = null
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  dialogData.value = { ...row }
  dialogVisible.value = true
}

async function onSave(payload) {
  try {
    if (dialogMode.value === 'create') {
      await createDataSource(payload)
      ElMessage.success('创建成功')
    } else {
      await updateDataSource(dialogData.value.id, payload)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    // request.js 已统一弹错误
  }
}

async function onTest(row) {
  testingId.value = row.id
  try {
    const res = await testConnection(row.id)
    ElMessage[res.data ? 'success' : 'error'](res.data ? '连接成功' : '连接失败')
    await load()
  } finally {
    testingId.value = null
  }
}

async function onToggle(row) {
  try {
    if (row.status === 'ACTIVE') {
      await disableDataSource(row.id)
      ElMessage.success('已禁用')
    } else {
      await enableDataSource(row.id)
      ElMessage.success('已启用')
    }
    await load()
  } catch (e) {}
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除数据源 "${row.name}"？`, '确认删除', { type: 'warning' })
  try {
    await deleteDataSource(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) {}
}

onMounted(load)
</script>

<style scoped>
.conn-info { font-family: monospace; font-size: 12px; color: #606266; }
</style>
