<template>
  <div class="feature-view-list">
    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="left-actions">
        <!-- 搜索框 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索特征视图名称或描述"
          clearable
          style="width: 300px; margin-right: 12px"
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <!-- 状态筛选 -->
        <el-select
          v-model="filterStatus"
          placeholder="筛选状态"
          clearable
          style="width: 150px; margin-right: 12px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="option in FeatureViewStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>

        <!-- 实体类型筛选 -->
        <el-select
          v-model="filterEntity"
          placeholder="筛选实体类型"
          clearable
          style="width: 150px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="entity in entityTypes"
            :key="entity"
            :label="entity"
            :value="entity"
          />
        </el-select>
      </div>

      <div class="right-actions">
        <!-- 刷新按钮 -->
        <el-button
          :icon="Refresh"
          @click="handleRefresh"
        >
          刷新
        </el-button>

        <!-- 批量定义特征按钮 -->
        <el-button
          :icon="DocumentAdd"
          @click="handleBatchDefine"
        >
          批量定义特征
        </el-button>

        <!-- 创建新视图按钮 -->
        <el-button
          type="primary"
          :icon="Plus"
          @click="handleCreate"
        >
          创建特征视图
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="featuresStore.loading.views"
      :data="displayViews"
      stripe
      style="width: 100%"
      @row-click="handleRowClick"
    >
      <!-- 视图名称 -->
      <el-table-column
        prop="name"
        label="视图名称"
        min-width="180"
        fixed="left"
      >
        <template #default="{ row }">
          <div class="view-name">
            <el-icon class="icon"><Document /></el-icon>
            <span class="name">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>

      <!-- 实体类型 -->
      <el-table-column
        prop="entity"
        label="实体类型"
        width="120"
      >
        <template #default="{ row }">
          <el-tag type="info" size="small">
            {{ row.entity }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 数据源 -->
      <el-table-column
        prop="datasourceName"
        label="数据源"
        width="150"
      >
        <template #default="{ row }">
          <el-tag v-if="row.datasourceName" size="small" type="primary">
            {{ row.datasourceName }}
          </el-tag>
          <span v-else-if="row.datasourceId" class="text-muted">ID:{{ row.datasourceId }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <!-- 数据表 -->
      <el-table-column
        label="数据表"
        width="150"
      >
        <template #default="{ row }">
          <span v-if="getTableName(row)">{{ getTableName(row) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <!-- TTL -->
      <el-table-column
        prop="ttl"
        label="TTL"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <span>{{ row.ttl }}天</span>
        </template>
      </el-table-column>

      <!-- 状态 -->
      <el-table-column
        prop="status"
        label="状态"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            :type="FeatureViewStatusColors[row.status]"
            size="small"
          >
            {{ FeatureViewStatusLabels[row.status] }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 描述 -->
      <el-table-column
        prop="description"
        label="描述"
        min-width="200"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span>{{ row.description || '-' }}</span>
        </template>
      </el-table-column>

      <!-- 更新时间 -->
      <el-table-column
        prop="updatedAt"
        label="更新时间"
        width="160"
        align="center"
      >
        <template #default="{ row }">
          <span>{{ formatDate(row.updatedAt) }}</span>
        </template>
      </el-table-column>

      <!-- 操作列 -->
      <el-table-column
        label="操作"
        width="140"
        fixed="right"
        align="center"
      >
        <template #default="{ row }">
          <div class="action-buttons">
            <el-tooltip content="查看详情" placement="top"
            >
              <el-button
                size="small"
                :icon="View"
                circle
                @click.stop="handleView(row)"
              />
            </el-tooltip>

            <el-tooltip content="编辑" placement="top"
            >
              <el-button
                size="small"
                type="primary"
                :icon="Edit"
                circle
                @click.stop="handleEdit(row)"
              />
            </el-tooltip>

            <el-tooltip content="删除" placement="top"
            >
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                circle
                @click.stop="handleDelete(row)"
              />
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
        :total="featuresStore.pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 创建/编辑对话框 -->
    <feature-view-dialog
      v-if="showDialog"
      :visible="showDialog"
      :mode="dialogMode"
      :view-data="currentViewData"
      @close="handleDialogClose"
      @save="handleDialogSave"
    />
  </div>
</template>

<script setup>
/**
 * 特征视图列表页
 *
 * 功能：
 * - 展示所有特征视图
 * - 搜索和筛选
 * - 创建/编辑/删除操作
 * - 分页功能
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Document,
  DocumentAdd,
  View,
  Edit,
  Delete
} from '@element-plus/icons-vue'
import { useFeaturesStore } from '@/stores/features'
import {
  FeatureViewStatusOptions,
  FeatureViewStatusLabels,
  FeatureViewStatusColors
} from '@/constants/features'
import FeatureViewDialog from './FeatureViewDialog.vue'

// ==================== 路由 ====================
const router = useRouter()

// ==================== Store ====================
const featuresStore = useFeaturesStore()

// ==================== 响应式数据 ====================
// 搜索关键词
const searchKeyword = ref('')

// 筛选条件
const filterStatus = ref('')
const filterEntity = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(20)

// 对话框
const showDialog = ref(false)
const dialogMode = ref('create') // 'create' | 'edit'
const currentViewData = ref(null)

// ==================== 计算属性 ====================
/**
 * 显示的特征视图列表（应用搜索和筛选）
 */
const displayViews = computed(() => {
  let views = featuresStore.views

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    views = views.filter(view =>
      view.name.toLowerCase().includes(keyword) ||
      (view.description && view.description.toLowerCase().includes(keyword))
    )
  }

  // 状态筛选
  if (filterStatus.value) {
    views = views.filter(view => view.status === filterStatus.value)
  }

  // 实体类型筛选
  if (filterEntity.value) {
    views = views.filter(view => view.entity === filterEntity.value)
  }

  return views
})

/**
 * 获取所有实体类型（去重）
 */
const entityTypes = computed(() => {
  return featuresStore.entityTypes
})

// ==================== 方法 ====================
/**
 * 加载特征视图列表
 */
const loadViews = async () => {
  try {
    await featuresStore.fetchViews({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value,
      entity: filterEntity.value
    })
  } catch (error) {
    ElMessage.error('加载特征视图列表失败: ' + error.message)
  }
}

/**
 * 处理搜索输入（带防抖）
 */
let searchDebounceTimer = null
const handleSearch = () => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  searchDebounceTimer = setTimeout(() => {
    // 搜索在本地进行，不需要重新请求后端
  }, 300)
}

/**
 * 处理筛选条件变化
 */
const handleFilterChange = () => {
  currentPage.value = 1
  loadViews()
}

/**
 * 处理刷新
 */
const handleRefresh = () => {
  loadViews()
  ElMessage.success('刷新成功')
}

/**
 * 批量定义特征
 */
const handleBatchDefine = () => {
  router.push({
    name: 'FeatureDefinitionConfig'
  })
}

/**
 * 处理创建新视图
 */
const handleCreate = () => {
  dialogMode.value = 'create'
  currentViewData.value = null
  showDialog.value = true
}

/**
 * 处理查看详情
 */
const handleView = (row) => {
  router.push({
    name: 'FeatureViewDetail',
    params: { name: row.name }
  })
}

/**
 * 处理编辑
 */
const handleEdit = (row) => {
  dialogMode.value = 'edit'
  currentViewData.value = { ...row }
  showDialog.value = true
}

/**
 * 处理删除
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除特征视图 "${row.name}" 吗？删除后将进入归档状态。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await featuresStore.deleteView(row.name)
    ElMessage.success('删除成功')

    // 重新加载列表
    await loadViews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

/**
 * 处理行点击（查看详情）
 */
const handleRowClick = (row) => {
  handleView(row)
}

/**
 * 处理分页变化
 */
const handlePageChange = (page) => {
  currentPage.value = page
  loadViews()
}

/**
 * 处理每页条数变化
 */
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadViews()
}

/**
 * 处理对话框关闭
 */
const handleDialogClose = () => {
  showDialog.value = false
  currentViewData.value = null
}

/**
 * 处理对话框保存
 */
const handleDialogSave = async (data) => {
  try {
    if (dialogMode.value === 'create') {
      await featuresStore.createView(data)
      ElMessage.success('创建成功')
    } else {
      await featuresStore.updateView(currentViewData.value.name, data)
      ElMessage.success('更新成功')
    }

    showDialog.value = false
    await loadViews()
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  }
}

/**
 * 从 sourceConfig 中解析数据表名
 */
const getTableName = (row) => {
  if (!row.sourceConfig) return null
  try {
    const config = typeof row.sourceConfig === 'string'
      ? JSON.parse(row.sourceConfig)
      : row.sourceConfig
    return config.table || null
  } catch (e) {
    return null
  }
}

/**
 * 格式化日期
 */
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

// ==================== 生命周期 ====================
onMounted(() => {
  loadViews()
})

// 监听筛选条件变化
watch([filterStatus, filterEntity], () => {
  currentPage.value = 1
})
</script>

<style scoped lang="scss">
.feature-view-list {
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

.view-name {
  display: flex;
  align-items: center;

  .icon {
    margin-right: 8px;
    color: #409eff;
  }

  .name {
    font-weight: 500;
    color: $text-primary;
  }
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

/* 表格行悬停效果 */
:deep(.el-table__body tr:hover > td) {
  cursor: pointer;
}

/* 响应式布局 */
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
