<template>
  <div class="dataset-list-page">
    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="left-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索数据集名称"
          clearable
          style="width: 280px"
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="right-actions">
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
      </div>
    </div>

    <!-- 数据集表格 -->
    <el-table
      v-loading="samplesStore.loading.datasets"
      :data="displayDatasets"
      stripe
      style="width: 100%"
      @expand-change="handleExpandChange"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="versions-panel">
            <el-table :data="row.versions || []" size="small" border>
              <el-table-column prop="version" label="版本号" width="100" />
              <el-table-column prop="sampleCount" label="样本数" width="100" align="right" />
              <el-table-column prop="featureCount" label="特征数" width="100" align="right" />
              <el-table-column prop="splitInfo" label="划分信息" min-width="200">
                <template #default="{ row: v }">
                  <span v-if="v.splitInfo">{{ formatSplitInfo(v.splitInfo) }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="160">
                <template #default="{ row: v }">
                  {{ formatDate(v.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160" align="center">
                <template #default="{ row: v }">
                  <el-button size="small" text type="primary" @click="previewDataset(row.name, v.version)">
                    预览
                  </el-button>
                  <el-button size="small" text type="primary" @click="downloadDataset(row.name, v.version)">
                    下载
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="name" label="数据集名称" min-width="200">
        <template #default="{ row }">
          <div class="dataset-name">
            <el-icon class="icon" color="#67c23a"><FolderOpened /></el-icon>
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

      <el-table-column prop="sampleCount" label="总样本数" width="110" align="right">
        <template #default="{ row }">
          {{ formatNumber(row.sampleCount) }}
        </template>
      </el-table-column>

      <el-table-column prop="featureCount" label="特征数" width="90" align="right">
        <template #default="{ row }">
          {{ row.featureCount || '-' }}
        </template>
      </el-table-column>

      <el-table-column prop="versionCount" label="版本数" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.versions?.length || 0 }} 个</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.description || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" text @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页器 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="totalDatasets"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>

  <DatasetPreviewDialog
    v-model="previewVisible"
    :dataset-name="previewDatasetName"
    :version="previewVersion"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, FolderOpened } from '@element-plus/icons-vue'
import { useSamplesStore } from '@/stores/samples'
import DatasetPreviewDialog from './DatasetPreviewDialog.vue'

const samplesStore = useSamplesStore()

const previewVisible = ref(false)
const previewDatasetName = ref('')
const previewVersion = ref('')

const previewDataset = (name, version) => {
  previewDatasetName.value = name
  previewVersion.value = version
  previewVisible.value = true
}

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

const displayDatasets = computed(() => {
  let datasets = samplesStore.datasets

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    datasets = datasets.filter(d =>
      d.name.toLowerCase().includes(keyword) ||
      (d.description && d.description.toLowerCase().includes(keyword))
    )
  }

  return datasets
})

const totalDatasets = computed(() => samplesStore.datasets.length)

const loadDatasets = async () => {
  try {
    await samplesStore.fetchDatasets({
      page: currentPage.value,
      pageSize: pageSize.value
    })
  } catch (error) {
    ElMessage.error('加载数据集失败: ' + error.message)
  }
}

let searchDebounceTimer = null
const handleSearch = () => {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
  searchDebounceTimer = setTimeout(() => {}, 300)
}

const handleRefresh = () => {
  loadDatasets()
  ElMessage.success('刷新成功')
}

const handleExpandChange = (row, expandedRows) => {
  // 展开时加载版本列表（如果需要）
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除数据集 "${row.name}" 吗？此操作不可恢复。`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    // TODO: 调用删除 API
    ElMessage.success('删除成功')
    await loadDatasets()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const downloadDataset = (name, version) => {
  ElMessage.info(`下载数据集 ${name} v${version}`)
  // TODO: 实现下载
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadDatasets()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadDatasets()
}

const getLabelTypeType = (type) => {
  const types = { BINARY: 'danger', MULTICLASS: 'warning', REGRESSION: 'success' }
  return types[type] || 'info'
}

const getLabelTypeLabel = (type) => {
  const labels = { BINARY: '二分类', MULTICLASS: '多分类', REGRESSION: '回归' }
  return labels[type] || type
}

const formatNumber = (num) => {
  if (num === undefined || num === null) return '-'
  return num.toLocaleString()
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const formatSplitInfo = (splitInfo) => {
  if (typeof splitInfo === 'string') {
    try {
      splitInfo = JSON.parse(splitInfo)
    } catch {
      return splitInfo
    }
  }
  if (!splitInfo) return '-'
  const parts = []
  if (splitInfo.train) parts.push(`训练: ${splitInfo.train}`)
  if (splitInfo.val) parts.push(`验证: ${splitInfo.val}`)
  if (splitInfo.test) parts.push(`测试: ${splitInfo.test}`)
  return parts.join(' / ')
}

onMounted(() => {
  loadDatasets()
})
</script>

<style scoped lang="scss">
.dataset-list-page {
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

.dataset-name {
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

.versions-panel {
  padding: 12px 24px;
  background: $bg-gray;
  border-radius: $radius-sm;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 12px;

    .left-actions,
    .right-actions {
      width: 100%;
    }
  }
}
</style>
