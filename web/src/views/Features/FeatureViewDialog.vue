<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      @submit.prevent="handleSubmit"
    >
      <!-- 基本信息部分 -->
      <div class="form-section">
        <h3 class="section-title">基本信息</h3>

        <!-- 视图名称 -->
        <el-form-item label="视图名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="3-50字符，仅允许字母、数字、下划线"
            :disabled="mode === 'edit'"
            clearable
          >
            <template #prefix>
              <el-icon><Document /></el-icon>
            </template>
          </el-input>
          <div class="form-tip">创建后不可修改</div>
        </el-form-item>

        <!-- 实体类型 -->
        <el-form-item label="实体类型" prop="entity">
          <el-select
            v-model="formData.entity"
            placeholder="选择或输入实体类型"
            filterable
            allow-create
            clearable
          >
            <el-option
              v-for="item in CommonEntityTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="form-tip">如：user_id、item_id、shop_id</div>
        </el-form-item>

        <!-- TTL -->
        <el-form-item label="TTL（天）" prop="ttl">
          <el-input-number
            v-model="formData.ttl"
            :min="DefaultValues.MIN_TTL"
            :max="DefaultValues.MAX_TTL"
            :step="1"
          />
          <div class="form-tip">特征在Redis中的缓存时间</div>
        </el-form-item>

        <!-- 描述 -->
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入特征视图描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 状态（仅编辑模式） -->
        <el-form-item v-if="mode === 'edit'" label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="选择状态" style="width: 100%">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="激活" value="ACTIVE" />
            <el-option label="已弃用" value="DEPRECATED" />
          </el-select>
          <div class="form-tip">草稿状态下可进行编辑，激活后可用于特征计算和物化</div>
        </el-form-item>
      </div>

      <!-- 数据源配置部分 -->
      <div class="form-section">
        <h3 class="section-title">数据源配置</h3>

        <!-- 数据源选择 -->
        <el-form-item label="数据源" prop="datasourceId">
          <el-select
            v-model="formData.datasourceId"
            placeholder="选择数据源"
            @change="handleDataSourceChange"
          >
            <el-option
              v-for="item in dataSources"
              :key="item.id"
              :label="`${item.name} (${DataSourceTypeLabels[item.type] || item.type})`"
              :value="item.id"
            />
          </el-select>
          <div class="form-tip">从数据源配置管理中选择已配置的数据源</div>
        </el-form-item>

        <!-- 动态数据源使用配置表单 -->
        <template v-if="selectedDataSourceType">
          <!-- PostgreSQL / MySQL 配置 -->
          <template v-if="selectedDataSourceType === 'postgresql' || selectedDataSourceType === 'mysql'">
            <el-form-item label="表名" prop="sourceConfig.table">
              <el-select
                v-model="formData.sourceConfig.table"
                placeholder="选择数据表"
                filterable
                clearable
                :loading="loadingTables"
                @change="handleTableChange"
                style="width: 100%"
              >
                <el-option
                  v-for="table in tables"
                  :key="table"
                  :label="table"
                  :value="table"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="实体字段" prop="sourceConfig.entityColumn">
              <el-select
                v-model="formData.sourceConfig.entityColumn"
                placeholder="选择实体字段"
                filterable
                clearable
                :loading="loadingColumns"
                :disabled="!formData.sourceConfig.table"
                style="width: 100%"
              >
                <el-option
                  v-for="col in columns"
                  :key="col.name"
                  :label="`${col.name} (${col.type})`"
                  :value="col.name"
                />
              </el-select>
              <div class="form-tip">用于关联实体ID的字段</div>
            </el-form-item>

            <el-form-item label="日期字段" prop="sourceConfig.dateColumn">
              <el-select
                v-model="formData.sourceConfig.dateColumn"
                placeholder="选择日期字段（可选）"
                filterable
                clearable
                :loading="loadingColumns"
                :disabled="!formData.sourceConfig.table"
                style="width: 100%"
              >
                <el-option
                  v-for="col in columns"
                  :key="col.name"
                  :label="`${col.name} (${col.type})`"
                  :value="col.name"
                />
              </el-select>
              <div class="form-tip">用于时间窗口计算的字段</div>
            </el-form-item>
          </template>

          <!-- API 配置 -->
          <template v-else-if="selectedDataSourceType === 'api'">
            <el-form-item label="接口路径" prop="sourceConfig.path">
              <el-input
                v-model="formData.sourceConfig.path"
                placeholder="/v1/data"
                clearable
              />
            </el-form-item>

            <el-form-item label="请求方法" prop="sourceConfig.method">
              <el-select v-model="formData.sourceConfig.method">
                <el-option label="GET" value="GET" />
                <el-option label="POST" value="POST" />
              </el-select>
            </el-form-item>

            <el-form-item label="实体字段" prop="sourceConfig.entityField">
              <el-input
                v-model="formData.sourceConfig.entityField"
                placeholder="响应JSON中的实体ID字段名"
                clearable
              />
            </el-form-item>

            <el-form-item label="请求头">
              <el-input
                v-model="apiHeadersText"
                type="textarea"
                :rows="3"
                placeholder='{"Authorization": "Bearer YOUR_TOKEN"}'
                @blur="updateApiHeaders"
              />
            </el-form-item>
          </template>

          <!-- Redis 配置 -->
          <template v-else-if="selectedDataSourceType === 'redis'">
            <el-form-item label="Key模式" prop="sourceConfig.keyPattern">
              <el-input
                v-model="formData.sourceConfig.keyPattern"
                placeholder="如：user:*、item:*"
                clearable
              />
            </el-form-item>

            <el-form-item label="实体字段" prop="sourceConfig.entityField">
              <el-input
                v-model="formData.sourceConfig.entityField"
                placeholder="Key中提取实体ID的字段"
                clearable
              />
            </el-form-item>

            <el-form-item label="数据结构" prop="sourceConfig.dataStructure">
              <el-select v-model="formData.sourceConfig.dataStructure">
                <el-option label="String" value="string" />
                <el-option label="Hash" value="hash" />
                <el-option label="List" value="list" />
                <el-option label="Set" value="set" />
                <el-option label="ZSet" value="zset" />
              </el-select>
            </el-form-item>
          </template>

          <!-- Kafka 配置 -->
          <template v-else-if="selectedDataSourceType === 'kafka'">
            <el-form-item label="Topic" prop="sourceConfig.topic">
              <el-input
                v-model="formData.sourceConfig.topic"
                placeholder="如：user-events"
                clearable
              />
            </el-form-item>

            <el-form-item label="实体字段" prop="sourceConfig.entityField">
              <el-input
                v-model="formData.sourceConfig.entityField"
                placeholder="消息JSON中的实体ID字段名"
                clearable
              />
            </el-form-item>

            <el-form-item label="最大记录数" prop="sourceConfig.maxPollRecords">
              <el-input-number
                v-model="formData.sourceConfig.maxPollRecords"
                :min="1"
                :max="10000"
                :step="100"
              />
            </el-form-item>

            <el-form-item label="消费者组" prop="sourceConfig.consumerGroup">
              <el-input
                v-model="formData.sourceConfig.consumerGroup"
                placeholder="feature-computation-group"
                clearable
              />
            </el-form-item>
          </template>
        </template>
      </div>

      <!-- YAML配置预览 -->
      <div class="form-section" v-if="formData.datasourceId">
        <h3 class="section-title">
          配置预览
          <el-button
            type="primary"
            size="small"
            :icon="Edit"
            @click="showYamlEditor = true"
            style="margin-left: 12px"
          >
            编辑YAML
          </el-button>
        </h3>

        <div class="yaml-preview">
          <pre>{{ yamlPreview }}</pre>
        </div>
      </div>
    </el-form>

    <!-- YAML编辑器对话框 -->
    <el-dialog
      v-model="showYamlEditor"
      title="编辑YAML配置"
      width="700px"
      append-to-body
    >
      <div class="yaml-editor">
        <monaco-editor
          v-model="yamlConfig"
          language="yaml"
          :height="400"
          :options="{
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 14,
            lineNumbers: 'on',
            folding: true
          }"
        />
      </div>
      <template #footer>
        <el-button @click="showYamlEditor = false">取消</el-button>
        <el-button type="primary" @click="applyYamlConfig">应用配置</el-button>
      </template>
    </el-dialog>

    <!-- 底部按钮 -->
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
      >
        {{ mode === 'create' ? '创建' : '保存' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * 创建/编辑特征视图对话框
 *
 * 功能：
 * - 基本信息表单（名称、实体类型、TTL、描述）
 * - 数据源配置表单（根据类型动态显示）
 * - YAML代码编辑器
 * - 表单验证
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  Edit
} from '@element-plus/icons-vue'
import MonacoEditor from '@/components/MonacoEditor.vue'
import { listDataSources, getDataSourceTables, getDataSourceColumns } from '@/api/modules/datasources'
import {
  DataSourceTypeLabels,
  CommonEntityTypes,
  DefaultValues,
  ValidationPatterns,
  ValidationMessages,
  PostgreSQLConfigTemplate,
  ApiConfigTemplate,
  RedisConfigTemplate,
  KafkaConfigTemplate
} from '@/constants/features'

// ==================== Props ====================
const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  mode: {
    type: String,
    default: 'create', // 'create' | 'edit'
    validator: (value) => ['create', 'edit'].includes(value)
  },
  viewData: {
    type: Object,
    default: null
  }
})

// ==================== Emits ====================
const emit = defineEmits(['close', 'save'])

// ==================== 响应式数据 ====================
const formRef = ref(null)
const submitting = ref(false)
const showYamlEditor = ref(false)
const yamlConfig = ref('')
const apiHeadersText = ref('')
const dataSources = ref([])
const tables = ref([])
const columns = ref([])
const loadingTables = ref(false)
const loadingColumns = ref(false)

// 表单数据
const formData = ref({
  name: '',
  entity: '',
  ttl: DefaultValues.TTL,
  description: '',
  status: 'DRAFT',
  datasourceId: null,
  sourceConfig: {}
})

// ==================== 计算属性 ====================
/**
 * 对话框标题
 */
const dialogTitle = computed(() => {
  return props.mode === 'create' ? '创建特征视图' : '编辑特征视图'
})

/**
 * 当前选中的数据源类型
 */
const selectedDataSourceType = computed(() => {
  const ds = dataSources.value.find(d => d.id === formData.value.datasourceId)
  return ds ? ds.type : null
})

/**
 * YAML配置预览
 */
const yamlPreview = computed(() => {
  if (!formData.value.datasourceId || !formData.value.sourceConfig) {
    return '# 请先配置数据源'
  }

  const ds = dataSources.value.find(d => d.id === formData.value.datasourceId)
  const config = {
    name: formData.value.name,
    entity: formData.value.entity,
    ttl: formData.value.ttl,
    description: formData.value.description,
    datasource: ds ? { id: ds.id, name: ds.name, type: ds.type } : null,
    sourceConfig: formData.value.sourceConfig
  }

  // 简单的YAML转换
  return objectToYaml(config)
})

// ==================== 表单验证规则 ====================
const formRules = {
  name: [
    { required: true, message: ValidationMessages.FEATURE_VIEW_NAME_REQUIRED, trigger: 'blur' },
    {
      pattern: ValidationPatterns.FEATURE_VIEW_NAME,
      message: ValidationMessages.FEATURE_VIEW_NAME_INVALID,
      trigger: 'blur'
    }
  ],
  entity: [
    { required: true, message: ValidationMessages.ENTITY_TYPE_REQUIRED, trigger: 'blur' },
    {
      pattern: ValidationPatterns.ENTITY_TYPE,
      message: ValidationMessages.ENTITY_TYPE_INVALID,
      trigger: 'blur'
    }
  ],
  ttl: [
    { required: true, message: ValidationMessages.TTL_REQUIRED, trigger: 'blur' },
    { type: 'number', min: DefaultValues.MIN_TTL, message: ValidationMessages.TTL_MIN, trigger: 'blur' },
    { type: 'number', max: DefaultValues.MAX_TTL, message: ValidationMessages.TTL_MAX, trigger: 'blur' }
  ],
  datasourceId: [
    { required: true, message: '请选择数据源', trigger: 'change' }
  ]
}

// ==================== 方法 ====================
/**
 * 加载数据源列表
 */
const loadDataSources = async () => {
  try {
    const response = await listDataSources()
    if (response.code === '200') {
      // 只显示已配置且状态为 ACTIVE（未禁用）的数据源
      const allSources = response.data || []
      dataSources.value = allSources.filter(ds => ds.status === 'ACTIVE')
    }
  } catch (error) {
    console.error('加载数据源列表失败:', error)
  }
}

/**
 * 初始化表单数据
 */
const initFormData = async () => {
  if (props.mode === 'edit' && props.viewData) {
    // 编辑模式：填充现有数据
    formData.value = {
      name: props.viewData.name,
      entity: props.viewData.entity,
      ttl: props.viewData.ttl || DefaultValues.TTL,
      description: props.viewData.description || '',
      status: props.viewData.status || 'DRAFT',
      datasourceId: props.viewData.datasourceId || null,
      sourceConfig: {}
    }

    // 解析 sourceConfig
    if (props.viewData.sourceConfig) {
      try {
        formData.value.sourceConfig = JSON.parse(props.viewData.sourceConfig)
      } catch (e) {
        formData.value.sourceConfig = {}
      }
    }

    // 如果是数据库类型，重新加载表名和字段列表
    const ds = dataSources.value.find(d => d.id === formData.value.datasourceId)
    if (ds && (ds.type === 'postgresql' || ds.type === 'mysql')) {
      await loadTables(formData.value.datasourceId)
      if (formData.value.sourceConfig.table) {
        await handleTableChange(formData.value.sourceConfig.table)
      }
    }

    // 如果是API类型，格式化headers
    if (ds && ds.type === 'api' && formData.value.sourceConfig.headers) {
      apiHeadersText.value = JSON.stringify(formData.value.sourceConfig.headers, null, 2)
    }
  } else {
    // 创建模式：使用默认值
    formData.value = {
      name: '',
      entity: '',
      ttl: DefaultValues.TTL,
      description: '',
      status: 'DRAFT',
      datasourceId: null,
      sourceConfig: {}
    }
    apiHeadersText.value = ''
    tables.value = []
    columns.value = []
  }
}

/**
 * 处理数据源选择变化
 */
const handleDataSourceChange = async (datasourceId) => {
  // 清空表名和字段列表
  tables.value = []
  columns.value = []

  const ds = dataSources.value.find(d => d.id === datasourceId)

  // 根据数据源类型设置默认配置模板
  const type = ds ? ds.type : null
  switch (type) {
    case 'postgresql':
    case 'mysql':
      formData.value.sourceConfig = { ...PostgreSQLConfigTemplate }
      // 自动加载表名列表
      await loadTables(datasourceId)
      break
    case 'api':
      formData.value.sourceConfig = { ...ApiConfigTemplate }
      apiHeadersText.value = JSON.stringify(ApiConfigTemplate.headers || {}, null, 2)
      break
    case 'redis':
      formData.value.sourceConfig = { ...RedisConfigTemplate }
      break
    case 'kafka':
      formData.value.sourceConfig = { ...KafkaConfigTemplate }
      break
    default:
      formData.value.sourceConfig = {}
  }
}

/**
 * 加载数据源表名列表
 */
const loadTables = async (datasourceId) => {
  if (!datasourceId) return
  try {
    loadingTables.value = true
    const response = await getDataSourceTables(datasourceId)
    if (response.code === '200') {
      tables.value = response.data || []
    }
  } catch (error) {
    console.error('加载表名列表失败:', error)
    ElMessage.warning('加载表名列表失败: ' + error.message)
  } finally {
    loadingTables.value = false
  }
}

/**
 * 处理表名选择变化
 */
const handleTableChange = async (tableName) => {
  if (!tableName) {
    columns.value = []
    return
  }
  try {
    loadingColumns.value = true
    const response = await getDataSourceColumns(formData.value.datasourceId, tableName)
    if (response.code === '200') {
      columns.value = response.data || []
    }
  } catch (error) {
    console.error('加载字段列表失败:', error)
    ElMessage.warning('加载字段列表失败: ' + error.message)
  } finally {
    loadingColumns.value = false
  }
}

/**
 * 更新API请求头
 */
const updateApiHeaders = () => {
  try {
    if (apiHeadersText.value.trim()) {
      const headers = JSON.parse(apiHeadersText.value)
      formData.value.sourceConfig.headers = headers
    } else {
      formData.value.sourceConfig.headers = {}
    }
  } catch (error) {
    ElMessage.warning('请求头JSON格式不正确')
  }
}

/**
 * 应用YAML配置
 */
const applyYamlConfig = () => {
  try {
    // 解析YAML配置（使用简单的JSON解析，因为用户编辑的可能是JSON格式）
    const parsed = parseYamlOrJson(yamlConfig.value)

    // 更新表单数据
    if (parsed.name) formData.value.name = parsed.name
    if (parsed.entity) formData.value.entity = parsed.entity
    if (parsed.ttl) formData.value.ttl = parsed.ttl
    if (parsed.description) formData.value.description = parsed.description

    // 更新数据源配置
    if (parsed.datasource && parsed.datasource.id) {
      formData.value.datasourceId = parsed.datasource.id
    }
    if (parsed.sourceConfig) {
      if (typeof parsed.sourceConfig === 'string') {
        try {
          formData.value.sourceConfig = JSON.parse(parsed.sourceConfig)
        } catch {
          formData.value.sourceConfig = parsed.sourceConfig
        }
      } else {
        formData.value.sourceConfig = parsed.sourceConfig
      }
    }

    ElMessage.success('YAML配置已应用到表单')
    showYamlEditor.value = false
  } catch (error) {
    ElMessage.error('YAML格式不正确: ' + error.message)
  }
}

/**
 * 解析YAML或JSON格式
 */
const parseYamlOrJson = (text) => {
  text = text.trim()

  // 尝试JSON解析
  if (text.startsWith('{')) {
    return JSON.parse(text)
  }

  // 简单的YAML解析（不支持复杂嵌套）
  const lines = text.split('\n')
  const result = {}
  let currentObj = result
  const stack = [{ obj: result, indent: 0 }]

  for (let line of lines) {
    if (!line.trim() || line.trim().startsWith('#')) continue

    const indent = line.search(/\S/)
    const trimmed = line.trim()

    // 查找当前层级的对象
    while (stack.length > 0 && stack[stack.length - 1].indent >= indent) {
      stack.pop()
    }

    currentObj = stack[stack.length - 1].obj

    if (trimmed.includes(':')) {
      const [key, ...valueParts] = trimmed.split(':')
      const value = valueParts.join(':').trim()

      if (value) {
        // 有值
        currentObj[key.trim()] = parseValue(value)
      } else {
        // 没有值，创建嵌套对象
        const newObj = {}
        currentObj[key.trim()] = newObj
        stack.push({ obj: newObj, indent })
      }
    }
  }

  return result
}

/**
 * 解析值
 */
const parseValue = (value) => {
  value = value.trim()

  // 去除引号
  if ((value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))) {
    return value.slice(1, -1)
  }

  // 数字
  if (!isNaN(value)) {
    return Number(value)
  }

  // 布尔值
  if (value === 'true') return true
  if (value === 'false') return false
  if (value === 'null') return null

  return value
}

/**
 * 将对象转换为简单的YAML格式
 */
const objectToYaml = (obj, indent = 0) => {
  const spaces = '  '.repeat(indent)
  let yaml = ''

  for (const [key, value] of Object.entries(obj)) {
    if (value === null || value === undefined) {
      yaml += `${spaces}${key}: null\n`
    } else if (typeof value === 'object') {
      yaml += `${spaces}${key}:\n`
      yaml += objectToYaml(value, indent + 1)
    } else if (typeof value === 'string') {
      yaml += `${spaces}${key}: "${value}"\n`
    } else {
      yaml += `${spaces}${key}: ${value}\n`
    }
  }

  return yaml
}

/**
 * 提交表单
 */
const handleSubmit = async () => {
  try {
    // 验证表单
    const valid = await formRef.value.validate()
    if (!valid) return

    submitting.value = true

    // 构建提交数据
    const submitData = {
      name: formData.value.name,
      entity: formData.value.entity,
      ttl: formData.value.ttl,
      description: formData.value.description,
      status: formData.value.status,
      datasourceId: formData.value.datasourceId,
      sourceConfig: JSON.stringify(formData.value.sourceConfig)
    }

    emit('save', submitData)
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}

/**
 * 关闭对话框
 */
const handleClose = () => {
  formRef.value?.resetFields()
  emit('close')
}

// ==================== 监听器 ====================
watch(() => props.visible, async (visible) => {
  if (visible) {
    await loadDataSources()
    await initFormData()
  }
}, { immediate: true })

// 监听viewData变化（用于编辑模式下数据更新）
watch(() => props.viewData, async () => {
  if (props.visible) {
    await initFormData()
  }
}, { deep: true })
</script>

<style scoped lang="scss">
.form-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #ebeef5;

  &:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 20px 0;
    display: flex;
    align-items: center;
  }
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.5;
}

.yaml-preview {
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  max-height: 300px;
  overflow-y: auto;

  pre {
    margin: 0;
    font-family: 'Courier New', monospace;
    font-size: 13px;
    color: #303133;
    line-height: 1.6;
    white-space: pre-wrap;
    word-wrap: break-word;
  }
}

.yaml-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.connection-result {
  margin-left: 12px;
  font-size: 14px;
  font-weight: 500;

  &.success {
    color: #67c23a;
  }

  &.error {
    color: #f56c6c;
  }
}

// 响应式调整
@media (max-width: 768px) {
  :deep(.el-dialog) {
    width: 95% !important;
  }

  :deep(.el-form-item__label) {
    width: 100px !important;
  }
}
</style>
