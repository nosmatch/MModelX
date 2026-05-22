<template>
  <div class="feature-view-detail">
    <!-- 返回按钮 -->
    <div class="back-nav">
      <el-button :icon="ArrowLeft" @click="goBack">返回列表</el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 详情内容 -->
    <div v-else-if="viewDetail">
      <!-- 头部信息 -->
      <el-card class="header-card">
        <div class="view-header">
          <div class="header-left">
            <h2 class="view-name">{{ viewDetail.name }}</h2>
            <p class="view-description">{{ viewDetail.description || '暂无描述' }}</p>
          </div>
          <div class="header-right">
            <el-tag :type="getStatusType(viewDetail.status)" size="large">
              {{ getStatusLabel(viewDetail.status) }}
            </el-tag>
          </div>
        </div>

        <el-divider />

        <el-descriptions :column="3" border>
          <el-descriptions-item label="实体类型">
            <el-tag>{{ viewDetail.entity }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="数据源">
            <el-tag type="info">{{ viewDetail.datasourceName || '未配置' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="TTL">
            {{ viewDetail.ttl }} 天
          </el-descriptions-item>
          <el-descriptions-item label="特征数量">
            {{ viewDetail.features?.length || 0 }} 个
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(viewDetail.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(viewDetail.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="primary" :icon="Edit" @click="handleEdit">
          编辑视图
        </el-button>
        <el-button :icon="Operation" @click="handleCompute">
          计算特征
        </el-button>
        <el-button :icon="Files" @click="handleMaterialize">
          物化到Redis
        </el-button>
        <el-button :icon="View" @click="handleOnlineQuery">
          在线查询
        </el-button>
        <el-button :icon="DataAnalysis" @click="handleVisualize">
          可视化
        </el-button>
        <el-button :icon="Delete" type="danger" @click="handleDelete">
          删除
        </el-button>
      </div>

      <!-- 特征列表 -->
      <el-card class="features-card">
        <template #header>
          <div class="card-header">
            <span>特征列表</span>
            <el-button size="small" :icon="Plus" @click="handleAddFeature">
              添加特征
            </el-button>
          </div>
        </template>

        <el-table
          :data="viewDetail.features"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="name" label="特征名称" width="200" />
          <el-table-column prop="transformExpr" label="Transform表达式" min-width="200" />
          <el-table-column prop="dtype" label="数据类型" width="120">
            <template #default="{ row }">
              <el-tag :type="getDataTypeTagType(row.dtype)" size="small">
                {{ row.dtype }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="defaultValue" label="默认值" width="120" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row, $index }">
              <el-button
                size="small"
                :icon="Edit"
                @click="handleEditFeature(row, $index)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                @click="handleDeleteFeature(row, $index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 数据源配置 -->
      <el-card class="datasource-card">
        <template #header>
          <span>数据源配置</span>
        </template>

        <pre class="config-json">{{ formatConfig(viewDetail.sourceConfig) }}</pre>
      </el-card>
    </div>

    <!-- 错误状态 -->
    <el-empty v-else description="未找到特征视图" />

    <!-- 添加/编辑特征对话框 -->
    <feature-dialog
      v-if="showFeatureDialog"
      v-model:visible="showFeatureDialog"
      :mode="featureDialogMode"
      :feature-view-name="viewDetail?.name"
      :feature-data="currentFeatureData"
      @close="showFeatureDialog = false"
      @success="handleFeatureSuccess"
    />
  </div>
</template>

<script setup>
/**
 * 特征视图详情页
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Edit,
  Operation,
  Files,
  View,
  DataAnalysis,
  Delete,
  Plus
} from '@element-plus/icons-vue'
import { useFeaturesStore } from '@/stores/features'
import FeatureDialog from './FeatureDialog.vue'

// ==================== 路由和Store ====================
const route = useRoute()
const router = useRouter()
const featuresStore = useFeaturesStore()

// ==================== 响应式数据 ====================
const loading = ref(true)
const viewDetail = ref(null)

// 特征对话框
const showFeatureDialog = ref(false)
const featureDialogMode = ref('create')
const currentFeatureData = ref(null)

// ==================== 方法 ====================
/**
 * 加载视图详情
 */
const loadViewDetail = async () => {
  try {
    loading.value = true
    const viewName = route.params.name

    await featuresStore.fetchView(viewName)
    viewDetail.value = featuresStore.currentView
  } catch (error) {
    ElMessage.error('加载视图详情失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

/**
 * 返回列表
 */
const goBack = () => {
  router.push({ name: 'FeatureViewList' })
}

/**
 * 编辑视图
 */
const handleEdit = () => {
  ElMessage.info('编辑功能开发中')
}

/**
 * 计算特征
 */
const handleCompute = () => {
  router.push({
    name: 'FeatureCompute',
    query: { view: viewDetail.value.name }
  })
}

/**
 * 物化到Redis
 */
const handleMaterialize = () => {
  router.push({
    name: 'FeatureMaterialize',
    query: { view: viewDetail.value.name }
  })
}

/**
 * 在线查询
 */
const handleOnlineQuery = () => {
  router.push({
    name: 'OnlineFeatureQuery',
    query: { view: viewDetail.value.name }
  })
}

/**
 * 可视化
 */
const handleVisualize = () => {
  router.push({
    name: 'FeatureVisualization',
    query: { view: viewDetail.value.name }
  })
}

/**
 * 删除视图
 */
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除特征视图 "${viewDetail.value.name}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await featuresStore.deleteView(viewDetail.value.name)
    ElMessage.success('删除成功')
    goBack()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

/**
 * 添加特征
 */
const handleAddFeature = () => {
  featureDialogMode.value = 'create'
  currentFeatureData.value = null
  showFeatureDialog.value = true
}

/**
 * 编辑特征
 */
const handleEditFeature = (row, index) => {
  featureDialogMode.value = 'edit'
  currentFeatureData.value = { ...row }
  showFeatureDialog.value = true
}

/**
 * 删除特征
 */
const handleDeleteFeature = async (row, index) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除特征 "${row.name}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await featuresApi.deleteFeatureDefinition(viewDetail.value.name, row.name)
    ElMessage.success('删除成功')
    // 刷新详情
    await loadViewDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  }
}

/**
 * 特征操作成功后刷新
 */
const handleFeatureSuccess = async () => {
  await loadViewDetail()
}

/**
 * 格式化配置
 */
const formatConfig = (configStr) => {
  try {
    if (typeof configStr === 'string') {
      const config = JSON.parse(configStr)
      return JSON.stringify(config, null, 2)
    }
    return JSON.stringify(configStr, null, 2)
  } catch (error) {
    return configStr
  }
}

/**
 * 获取状态类型
 */
const getStatusType = (status) => {
  const types = {
    DRAFT: 'info',
    ACTIVE: 'success',
    DEPRECATED: 'warning',
    ARCHIVED: 'danger'
  }
  return types[status] || 'info'
}

/**
 * 获取状态标签
 */
const getStatusLabel = (status) => {
  const labels = {
    DRAFT: '草稿',
    ACTIVE: '激活',
    DEPRECATED: '弃用',
    ARCHIVED: '归档'
  }
  return labels[status] || status
}

/**
 * 获取数据类型标签类型
 */
const getDataTypeTagType = (dtype) => {
  const types = {
    INT64: '',
    FLOAT64: 'success',
    STRING: 'warning',
    BOOLEAN: 'info'
  }
  return types[dtype] || 'info'
}

/**
 * 格式化日期时间
 */
const formatDateTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadViewDetail()
})
</script>

<style scoped lang="scss">
.feature-view-detail {
  padding: 24px;
}

.back-nav {
  margin-bottom: 20px;
}

.loading-container {
  padding: 40px;
}

.header-card {
  margin-bottom: 20px;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  .header-left {
    flex: 1;

    .view-name {
      font-size: 28px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 8px 0;
    }

    .view-description {
      font-size: 14px;
      color: #606266;
      margin: 0;
    }
  }

  .header-right {
    margin-left: 20px;
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.features-card,
.datasource-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.config-json {
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
}

// 响应式
@media (max-width: 768px) {
  .feature-view-detail {
    padding: 16px;
  }

  .view-header {
    flex-direction: column;

    .header-right {
      margin-left: 0;
      margin-top: 16px;
    }
  }

  .action-buttons {
    flex-direction: column;

    button {
      width: 100%;
    }
  }
}
</style>
