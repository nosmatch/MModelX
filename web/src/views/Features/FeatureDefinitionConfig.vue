<template>
  <div class="feature-definition-config">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <h2 class="page-title">特征定义配置</h2>
      </div>
      <div class="header-right">
        <el-select
          v-model="selectedViewName"
          placeholder="选择特征视图"
          clearable
          style="width: 260px; margin-right: 12px"
        >
          <el-option
            v-for="view in viewOptions"
            :key="view.value"
            :label="view.label"
            :value="view.value"
          >
            <div class="view-option">
              <span>{{ view.label }}</span>
              <el-tag size="small" type="info">{{ view.entity }}</el-tag>
            </div>
          </el-option>
        </el-select>
        <el-button
          type="primary"
          :icon="Check"
          :loading="saving"
          :disabled="!canSave"
          @click="handleSave"
        >
          保存到特征视图
        </el-button>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="left-actions">
        <h3 class="title">特征定义</h3>
        <span class="subtitle">配置特征的变换表达式和数据类型</span>
      </div>
      <div class="right-actions">
        <el-button
          :icon="Plus"
          type="primary"
          @click="handleAddFeature"
        >
          添加特征
        </el-button>
        <el-button
          :icon="Upload"
          @click="showImportDialog = true"
        >
          批量导入
        </el-button>
        <el-button
          :icon="Download"
          @click="handleExport"
        >
          导出配置
        </el-button>
      </div>
    </div>

    <!-- 特征列表表格 -->
    <el-table
      :data="features"
      border
      stripe
      style="width: 100%"
      :row-class-name="getRowClassName"
    >
      <!-- 序号 -->
      <el-table-column
        type="index"
        label="序号"
        width="60"
        align="center"
      />

      <!-- 特征名称 -->
      <el-table-column
        prop="name"
        label="特征名称"
        min-width="180"
      >
        <template #default="{ row }">
          <el-input
            v-model="row.name"
            placeholder="特征名称"
            clearable
            @blur="validateFeatureName(row)"
          >
            <template #prefix>
              <el-icon><Key /></el-icon>
            </template>
          </el-input>
        </template>
      </el-table-column>

      <!-- Transform表达式 -->
      <el-table-column
        prop="transformExpr"
        label="Transform表达式"
        min-width="200"
      >
        <template #default="{ row }">
          <div class="transform-input">
            <el-select
              v-model="row.transformType"
              placeholder="选择操作"
              style="width: 130px"
              @change="handleTransformTypeChange(row)"
            >
              <el-option-group
                v-for="group in groupedTransformOptions"
                :key="group.label"
                :label="group.label"
              >
                <el-option
                  v-for="option in group.options"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-option-group>
            </el-select>
            <el-input
              v-model="row.transformColumn"
              placeholder="字段名"
              style="flex: 1; margin-left: 8px"
              @input="updateTransformExpr(row)"
            />
          </div>
          <div class="expression-preview">
            <el-tag size="small" type="info">
              {{ row.transformExpr || '未配置' }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <!-- 时间窗口 -->
      <el-table-column
        prop="timeWindow"
        label="时间窗口"
        width="130"
      >
        <template #header>
          <el-tooltip content="聚合类特征（sum/avg/count等）建议设置，不选则使用全部历史数据">
            <span>时间窗口 <el-icon><QuestionFilled /></el-icon></span>
          </el-tooltip>
        </template>
        <template #default="{ row }">
          <el-select
            v-model="row.timeWindow"
            placeholder="选择窗口"
            clearable
          >
            <el-option
              v-for="opt in TimeWindowOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </template>
      </el-table-column>

      <!-- 数据类型 -->
      <el-table-column
        prop="dtype"
        label="数据类型"
        width="140"
      >
        <template #default="{ row }">
          <el-select
            v-model="row.dtype"
            placeholder="选择类型"
          >
            <el-option
              v-for="type in FeatureDataTypeOptions"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </template>
      </el-table-column>

      <!-- 默认值 -->
      <el-table-column
        prop="defaultValue"
        label="默认值"
        width="140"
      >
        <template #default="{ row }">
          <el-input
            v-model="row.defaultValue"
            placeholder="默认值"
            clearable
          />
        </template>
      </el-table-column>

      <!-- 描述 -->
      <el-table-column
        prop="description"
        label="描述"
        min-width="180"
      >
        <template #default="{ row }">
          <el-input
            v-model="row.description"
            type="textarea"
            :rows="1"
            placeholder="特征描述"
            resize="none"
          />
        </template>
      </el-table-column>

      <!-- 操作 -->
      <el-table-column
        label="操作"
        width="100"
        align="center"
        fixed="right"
      >
        <template #default="{ $index }">
          <el-button
            type="danger"
            size="small"
            :icon="Delete"
            @click="handleDeleteFeature($index)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 统计信息 -->
    <div class="statistics">
      <div class="stat-item">
        <span class="label">特征总数：</span>
        <span class="value">{{ features.length }}</span>
      </div>
      <div class="stat-item">
        <span class="label">已配置：</span>
        <span class="value success">{{ configuredCount }}</span>
      </div>
      <div class="stat-item">
        <span class="label">未配置：</span>
        <span class="value warning">{{ unconfiguredCount }}</span>
      </div>
    </div>

    <!-- 快速添加模板 -->
    <div class="quick-add-section">
      <el-divider content-position="left">
        <span class="divider-title">快速添加常用特征</span>
      </el-divider>

      <div class="template-cards">
        <div
          v-for="template in featureTemplates"
          :key="template.name"
          class="template-card"
          @click="handleAddFromTemplate(template)"
        >
          <div class="card-header">
            <el-icon class="icon" :color="template.color">
              <component :is="template.icon" />
            </el-icon>
            <span class="title">{{ template.title }}</span>
          </div>
          <div class="card-content">
            {{ template.description }}
          </div>
          <div class="card-tags">
            <el-tag
              v-for="tag in template.features"
              :key="tag"
              size="small"
              type="info"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量导入对话框 -->
    <el-dialog
      v-model="showImportDialog"
      title="批量导入特征定义"
      width="700px"
    >
      <div class="import-section">
        <el-alert
          type="info"
          title="支持JSON格式导入"
          :closable="false"
          style="margin-bottom: 16px"
        >
          <template #default>
            <p style="margin: 0; font-size: 13px; color: #606266;">
              请粘贴或上传JSON格式的特征定义配置。格式示例：
            </p>
            <pre style="margin: 8px 0 0 0; font-size: 12px; color: #303133;">[
  {
    "name": "total_orders",
    "transformExpr": "sum(order_amount)",
    "timeWindow": "7d",
    "dtype": "FLOAT64",
    "defaultValue": 0
  }
]</pre>
          </template>
        </el-alert>

        <monaco-editor
          v-model="importJson"
          language="json"
          :height="300"
          :options="{
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 13
          }"
        />
      </div>

      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleImport"
        >
          导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 特征定义配置组件
 *
 * 功能：
 * - 特征列表管理
 * - 添加/编辑/删除特征
 * - Transform表达式选择器
 * - 数据类型和默认值配置
 * - 批量导入/导出
 * - 快速添加常用特征模板
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Delete,
  Upload,
  Download,
  Key,
  TrendCharts,
  Money,
  Timer,
  DataAnalysis,
  QuestionFilled,
  ArrowLeft,
  Check
} from '@element-plus/icons-vue'
import MonacoEditor from '@/components/MonacoEditor.vue'
import { useFeaturesStore } from '@/stores/features'
import { registerFeatureDefinition } from '@/api/modules/features'
import {
  TransformTypeOptions,
  TransformTypeGroups,
  TransformTypeGroupLabels,
  FeatureDataTypeOptions,
  TimeWindowOptions,
  ValidationPatterns,
  ValidationMessages
} from '@/constants/features'

// ==================== Props ====================
const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

// ==================== Emits ====================
const emit = defineEmits(['update:modelValue'])

// ==================== 路由和Store ====================
const router = useRouter()
const route = useRoute()
const featuresStore = useFeaturesStore()

// ==================== 响应式数据 ====================
const features = ref([])
const showImportDialog = ref(false)
const importJson = ref('')
const selectedViewName = ref('')
const saving = ref(false)

// ==================== 计算属性 ====================
/**
 * 可选的特征视图列表
 */
const viewOptions = computed(() => {
  return featuresStore.views.map(v => ({
    label: v.name,
    value: v.name,
    entity: v.entity
  }))
})

/**
 * 是否可以保存
 */
const canSave = computed(() => {
  return selectedViewName.value &&
    features.value.length > 0 &&
    features.value.some(f => f.name && f.transformExpr && f.dtype)
})

// ==================== 计算属性 ====================
/**
 * 分组后的Transform选项
 */
const groupedTransformOptions = computed(() => {
  const groups = {}

  TransformTypeOptions.forEach(option => {
    const groupLabel = TransformTypeGroupLabels[option.group]
    if (!groups[groupLabel]) {
      groups[groupLabel] = {
        label: groupLabel,
        options: []
      }
    }
    groups[groupLabel].options.push(option)
  })

  return Object.values(groups)
})

/**
 * 已配置的特征数量
 */
const configuredCount = computed(() => {
  return features.value.filter(f =>
    f.name &&
    f.transformExpr &&
    f.dtype
  ).length
})

/**
 * 未配置的特征数量
 */
const unconfiguredCount = computed(() => {
  return features.value.length - configuredCount.value
})

/**
 * 特征模板列表
 */
const featureTemplates = [
  {
    name: 'aggregate',
    title: '聚合特征',
    description: '常用的聚合统计特征',
    icon: TrendCharts,
    color: '#409eff',
    features: ['sum', 'avg', 'count', 'max', 'min']
  },
  {
    name: 'financial',
    title: '金融特征',
    description: '金额相关的统计特征',
    icon: Money,
    color: '#67c23a',
    features: ['total_amount', 'avg_amount', 'last_amount']
  },
  {
    name: 'temporal',
    title: '时间特征',
    description: '时间窗口相关特征',
    icon: Timer,
    color: '#e6a23c',
    features: ['first_time', 'last_time', 'time_diff']
  },
  {
    name: 'behavior',
    title: '行为特征',
    description: '用户行为统计特征',
    icon: DataAnalysis,
    color: '#f56c6c',
    features: ['click_count', 'view_count', 'action_rate']
  }
]

// ==================== 方法 ====================
/**
 * 添加新特征
 */
const handleAddFeature = () => {
  features.value.push({
    name: '',
    transformType: '',
    transformColumn: '',
    transformExpr: '',
    timeWindow: '',
    dtype: 'FLOAT64',
    defaultValue: '',
    description: ''
  })
}

/**
 * 删除特征
 */
const handleDeleteFeature = async (index) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个特征定义吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    features.value.splice(index, 1)
    emitUpdate()
    ElMessage.success('删除成功')
  } catch (error) {
    // 用户取消
  }
}

/**
 * 处理Transform类型变化
 */
const handleTransformTypeChange = (row) => {
  updateTransformExpr(row)
}

/**
 * 更新Transform表达式
 */
const updateTransformExpr = (row) => {
  if (row.transformType && row.transformColumn) {
    row.transformExpr = `${row.transformType}(${row.transformColumn})`
  } else {
    row.transformExpr = ''
  }
  emitUpdate()
}

/**
 * 验证特征名称
 */
const validateFeatureName = (row) => {
  if (row.name && !ValidationPatterns.FEATURE_NAME.test(row.name)) {
    ElMessage.warning(`特征名称 "${row.name}" 格式不正确`)
    row.name = ''
  }
  emitUpdate()
}

/**
 * 从模板添加特征
 */
const handleAddFromTemplate = (template) => {
  const templateFeatures = getTemplateFeatures(template.name)
  templateFeatures.forEach(feature => {
    features.value.push({
      name: feature.name,
      transformType: feature.transformType,
      transformColumn: feature.transformColumn,
      transformExpr: feature.transformExpr,
      timeWindow: feature.timeWindow || '',
      dtype: feature.dtype,
      defaultValue: feature.defaultValue,
      description: feature.description
    })
  })

  ElMessage.success(`已添加 ${templateFeatures.length} 个${template.title}`)
  emitUpdate()
}

/**
 * 获取模板特征
 */
const getTemplateFeatures = (templateName) => {
  const templates = {
    aggregate: [
      { name: 'total_count', transformType: 'count', transformColumn: 'id', timeWindow: '7d', dtype: 'INT64', defaultValue: 0, description: '记录总数' },
      { name: 'total_sum', transformType: 'sum', transformColumn: 'amount', timeWindow: '7d', dtype: 'FLOAT64', defaultValue: 0, description: '总和' },
      { name: 'average_value', transformType: 'avg', transformColumn: 'amount', timeWindow: '7d', dtype: 'FLOAT64', defaultValue: 0, description: '平均值' },
      { name: 'max_value', transformType: 'max', transformColumn: 'amount', timeWindow: '7d', dtype: 'FLOAT64', defaultValue: 0, description: '最大值' },
      { name: 'min_value', transformType: 'min', transformColumn: 'amount', timeWindow: '7d', dtype: 'FLOAT64', defaultValue: 0, description: '最小值' }
    ],
    financial: [
      { name: 'total_amount', transformType: 'sum', transformColumn: 'order_amount', timeWindow: '30d', dtype: 'FLOAT64', defaultValue: 0, description: '总金额' },
      { name: 'avg_order_value', transformType: 'avg', transformColumn: 'order_amount', timeWindow: '30d', dtype: 'FLOAT64', defaultValue: 0, description: '平均订单金额' },
      { name: 'last_order_amount', transformType: 'last', transformColumn: 'order_amount', timeWindow: '', dtype: 'FLOAT64', defaultValue: 0, description: '最后一次订单金额' }
    ],
    temporal: [
      { name: 'first_event_time', transformType: 'first', transformColumn: 'timestamp', timeWindow: '', dtype: 'INT64', defaultValue: 0, description: '首次事件时间' },
      { name: 'last_event_time', transformType: 'last', transformColumn: 'timestamp', timeWindow: '', dtype: 'INT64', defaultValue: 0, description: '最后事件时间' },
      { name: 'time_span', transformType: 'last', transformColumn: 'timestamp', timeWindow: '', dtype: 'INT64', defaultValue: 0, description: '时间跨度' }
    ],
    behavior: [
      { name: 'click_count', transformType: 'count', transformColumn: 'click_event', timeWindow: '7d', dtype: 'INT64', defaultValue: 0, description: '点击次数' },
      { name: 'view_count', transformType: 'count', transformColumn: 'view_event', timeWindow: '7d', dtype: 'INT64', defaultValue: 0, description: '浏览次数' },
      { name: 'conversion_rate', transformType: 'avg', transformColumn: 'is_converted', timeWindow: '30d', dtype: 'FLOAT64', defaultValue: 0, description: '转化率' }
    ]
  }

  return templates[templateName] || []
}

/**
 * 处理批量导入
 */
const handleImport = () => {
  try {
    const importedFeatures = JSON.parse(importJson.value)

    if (!Array.isArray(importedFeatures)) {
      throw new Error('导入数据必须是数组格式')
    }

    // 验证并添加特征
    let addedCount = 0
    importedFeatures.forEach(feature => {
      if (feature.name && feature.transformExpr) {
        features.value.push({
          name: feature.name,
          transformExpr: feature.transformExpr,
          timeWindow: feature.timeWindow || '',
          dtype: feature.dtype || 'FLOAT64',
          defaultValue: feature.defaultValue || '',
          description: feature.description || '',
          // 尝试解析表达式
          transformType: parseTransformType(feature.transformExpr),
          transformColumn: parseTransformColumn(feature.transformExpr)
        })
        addedCount++
      }
    })

    showImportDialog.value = false
    importJson.value = ''
    emitUpdate()

    ElMessage.success(`成功导入 ${addedCount} 个特征`)
  } catch (error) {
    ElMessage.error('导入失败: ' + error.message)
  }
}

/**
 * 解析Transform类型
 */
const parseTransformType = (expr) => {
  const match = expr.match(/^(\w+)\(/)
  return match ? match[1] : ''
}

/**
 * 解析Transform字段
 */
const parseTransformColumn = (expr) => {
  const match = expr.match(/\(([^)]+)\)/)
  return match ? match[1] : ''
}

/**
 * 处理导出配置
 */
const handleExport = () => {
  const exportData = features.value.map(f => ({
    name: f.name,
    transformExpr: f.transformExpr,
    timeWindow: f.timeWindow,
    dtype: f.dtype,
    defaultValue: f.defaultValue,
    description: f.description
  }))

  const jsonStr = JSON.stringify(exportData, null, 2)
  const blob = new Blob([jsonStr], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = 'feature-definitions.json'
  link.click()

  URL.revokeObjectURL(url)
  ElMessage.success('配置已导出')
}

/**
 * 触发更新事件
 */
const emitUpdate = () => {
  emit('update:modelValue', features.value)
}

/**
 * 获取行的类名
 */
const getRowClassName = ({ rowIndex }) => {
  const feature = features.value[rowIndex]
  if (!feature.name || !feature.transformExpr || !feature.dtype) {
    return 'warning-row'
  }
  return ''
}

// ==================== 初始化 ====================
// 如果有初始值，加载它
if (props.modelValue && props.modelValue.length > 0) {
  features.value = props.modelValue.map(f => ({
    ...f,
    transformType: parseTransformType(f.transformExpr),
    transformColumn: parseTransformColumn(f.transformExpr)
  }))
}

/**
 * 返回上一页
 */
const goBack = () => {
  router.back()
}

/**
 * 保存特征定义到特征视图
 */
const handleSave = async () => {
  if (!selectedViewName.value) {
    ElMessage.warning('请选择特征视图')
    return
  }

  const validFeatures = features.value.filter(f => f.name && f.transformExpr && f.dtype)
  if (validFeatures.length === 0) {
    ElMessage.warning('请至少配置一个完整的特征')
    return
  }

  // 查找选中的视图信息
  const selectedView = featuresStore.views.find(v => v.name === selectedViewName.value)
  if (!selectedView) {
    ElMessage.warning('特征视图不存在')
    return
  }

  saving.value = true
  try {
    const definition = {
      featureView: selectedViewName.value,
      entity: selectedView.entity || '',
      features: validFeatures.map(f => ({
        name: f.name,
        dtype: f.dtype,
        transformExpr: f.transformExpr,
        timeWindow: f.timeWindow || undefined,
        description: f.description,
        defaultValue: f.defaultValue || undefined
      })),
      source: {
        type: 'unknown',
        path: null
      }
    }

    await registerFeatureDefinition(definition)
    ElMessage.success(`成功保存 ${validFeatures.length} 个特征定义到「${selectedViewName.value}」`)

    // 清空当前列表
    features.value = []
    selectedViewName.value = ''
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(async () => {
  // 加载特征视图列表
  if (featuresStore.views.length === 0) {
    await featuresStore.fetchViews()
  }

  // 如果URL带 view 参数，预选中
  const viewParam = route.query.view
  if (viewParam) {
    selectedViewName.value = viewParam
  }
})
</script>

<style scoped lang="scss">
.feature-definition-config {
  padding: 20px;
  background: $bg-white;
  border-radius: $radius-md;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid $border-light;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
      margin: 0;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
  }
}

.view-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid $border-light;

  .left-actions {
    .title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 4px 0;
    }

    .subtitle {
      font-size: 13px;
      color: $text-muted;
    }
  }

  .right-actions {
    display: flex;
    gap: 8px;
  }
}

.transform-input {
  display: flex;
  align-items: center;
}

.expression-preview {
  margin-top: 8px;
}

.statistics {
  display: flex;
  gap: 24px;
  padding: 16px;
  margin-top: 16px;
  background: $bg-gray;
  border-radius: $radius-sm;

  .stat-item {
    display: flex;
    align-items: center;
    font-size: 14px;

    .label {
      color: #606266;
    }

    .value {
      margin-left: 8px;
      font-weight: 600;
      font-size: 16px;

      &.success {
        color: #67c23a;
      }

      &.warning {
        color: #e6a23c;
      }
    }
  }
}

.quick-add-section {
  margin-top: 32px;

  .divider-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }
}

.template-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.template-card {
  padding: 16px;
  background: $bg-white;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: #409eff;
    box-shadow: $shadow-hover;
    transform: translateY(-2px);
  }

  .card-header {
    display: flex;
    align-items: center;
    margin-bottom: 12px;

    .icon {
      font-size: 24px;
      margin-right: 12px;
    }

    .title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .card-content {
    font-size: 13px;
    color: #606266;
    margin-bottom: 12px;
    line-height: 1.5;
  }

  .card-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
}

.import-section {
  padding: 8px 0;
}

// 表格行样式
:deep(.warning-row) {
  background-color: #fef0f0;
}

// 响应式
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 12px;

    .header-right {
      width: 100%;
    }
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .statistics {
    flex-direction: column;
    gap: 12px;
  }

  .template-cards {
    grid-template-columns: 1fr;
  }
}
</style>
