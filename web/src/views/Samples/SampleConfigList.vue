<template>
  <div class="sample-config-list">
    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="left-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索配置名称或描述"
          clearable
          style="width: 280px; margin-right: 12px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filterStatus"
          placeholder="筛选状态"
          clearable
          style="width: 140px; margin-right: 12px"
        >
          <el-option label="激活" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
          <el-option label="归档" value="ARCHIVED" />
        </el-select>

        <el-select
          v-model="filterLabelType"
          placeholder="标签类型"
          clearable
          style="width: 140px"
        >
          <el-option label="二分类" value="BINARY" />
          <el-option label="多分类" value="MULTICLASS" />
          <el-option label="回归" value="REGRESSION" />
        </el-select>
      </div>

      <div class="right-actions">
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleCreate">创建配置</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="samplesStore.loading.configs"
      :data="pagedConfigs"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="name" label="配置名称" min-width="180" fixed="left">
        <template #default="{ row }">
          <div class="config-name">
            <el-icon class="icon" color="#409eff"><SetUp /></el-icon>
            <span class="name">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="labelType" label="标签类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="getLabelTypeType(row.labelType)">
            {{ getLabelTypeLabel(row.labelType) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="splitStrategy" label="划分策略" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ getSplitStrategyLabel(row.splitStrategy) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="比例" width="140" align="center">
        <template #default="{ row }">
          <span class="ratio-text">
            {{ formatRatio(row.trainRatio) }} / {{ formatRatio(row.valRatio) }} / {{ formatRatio(row.testRatio) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="特征视图" width="120" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ (row.featureViews || []).length }} 个</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.description || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="updatedAt" label="更新时间" width="160" align="center">
        <template #default="{ row }">
          <span>{{ formatDate(row.updatedAt) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-tooltip content="编辑" placement="top">
              <el-button size="small" type="primary" :icon="Edit" circle @click.stop="handleEdit(row)" />
            </el-tooltip>
            <el-tooltip content="构建样本" placement="top">
              <el-button size="small" type="success" :icon="VideoPlay" circle @click.stop="handleBuild(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button size="small" type="danger" :icon="Delete" circle @click.stop="handleDelete(row)" />
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页器 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="displayConfigs.length"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <!-- 创建/编辑对话框 -->
    <sample-config-dialog
      v-if="showDialog"
      :visible="showDialog"
      :mode="dialogMode"
      :config-data="currentConfig"
      @close="handleDialogClose"
      @save="handleDialogSave"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, SetUp, Edit, VideoPlay, Delete } from '@element-plus/icons-vue'
import { useSamplesStore } from '@/stores/samples'
import { formatDate } from '@/utils/date'
import {
  ActiveStatusColors,
  ActiveStatusLabels,
  LabelTypeColors,
  LabelTypeLabels,
  SplitStrategyLabels
} from '@/constants/status'
import SampleConfigDialog from './SampleConfigDialog.vue'

const router = useRouter()
const samplesStore = useSamplesStore()

const searchKeyword = ref('')
const filterStatus = ref('')
const filterLabelType = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const showDialog = ref(false)
const dialogMode = ref('create')
const currentConfig = ref(null)

const displayConfigs = computed(() => {
  let configs = samplesStore.configs

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    configs = configs.filter(c =>
      c.name.toLowerCase().includes(keyword) ||
      (c.description && c.description.toLowerCase().includes(keyword))
    )
  }

  if (filterStatus.value) {
    configs = configs.filter(c => c.status === filterStatus.value)
  }

  if (filterLabelType.value) {
    configs = configs.filter(c => c.labelType === filterLabelType.value)
  }

  return configs
})

const pagedConfigs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return displayConfigs.value.slice(start, start + pageSize.value)
})

const loadConfigs = async () => {
  try {
    await samplesStore.fetchConfigs()
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

const handleRefresh = () => {
  loadConfigs()
  ElMessage.success('刷新成功')
}

const handleCreate = () => {
  dialogMode.value = 'create'
  currentConfig.value = null
  showDialog.value = true
}

const handleEdit = (row) => {
  dialogMode.value = 'edit'
  currentConfig.value = { ...row }
  showDialog.value = true
}

const handleBuild = (row) => {
  router.push({ name: 'SampleBuild', query: { configId: row.id } })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除样本配置 "${row.name}" 吗？`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await samplesStore.deleteConfig(row.id)
    ElMessage.success('删除成功')
    await loadConfigs()
  } catch (error) {
    if (error === 'cancel') return
    // 错误已由 request.js 拦截器统一提示
  }
}

const handleDialogClose = () => {
  showDialog.value = false
  currentConfig.value = null
}

const handleDialogSave = async (data) => {
  try {
    if (dialogMode.value === 'create') {
      await samplesStore.createConfig(data)
      ElMessage.success('创建成功')
    } else {
      await samplesStore.updateConfig(currentConfig.value.id, data)
      ElMessage.success('更新成功')
    }
    showDialog.value = false
    await loadConfigs()
  } catch (error) {
    // 错误已由 request.js 拦截器统一提示
  }
}

// 状态/标签类型/划分策略映射（统一从 constants/status.js 取）
const getStatusType = (status) => ActiveStatusColors[status] || 'info'
const getStatusLabel = (status) => ActiveStatusLabels[status] || status
const getLabelTypeType = (type) => LabelTypeColors[type] || 'info'
const getLabelTypeLabel = (type) => LabelTypeLabels[type] || type
const getSplitStrategyLabel = (strategy) => SplitStrategyLabels[strategy] || strategy

const formatRatio = (ratio) => {
  if (ratio === undefined || ratio === null) return '0'
  return Math.round(ratio * 100) + '%'
}

// 监听搜索/筛选条件变化，自动回到第 1 页
watch([searchKeyword, filterStatus, filterLabelType], () => {
  currentPage.value = 1
})

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped lang="scss">
.sample-config-list {
  padding: 24px;
  background: $bg-white;
  border-radius: $radius-md;
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

.config-name {
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

.ratio-text {
  font-size: 12px;
  color: $text-secondary;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 6px;
}

@media (max-width: 768px) {
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
