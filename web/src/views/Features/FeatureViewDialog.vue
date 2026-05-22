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
      </div>

      <!-- 数据源配置部分 -->
      <div class="form-section">
        <h3 class="section-title">数据源配置</h3>

        <!-- 数据源类型 -->
        <el-form-item label="数据源类型" prop="dataSourceType">
          <el-select
            v-model="formData.dataSourceType"
            placeholder="选择数据源类型"
            @change="handleDataSourceTypeChange"
          >
            <el-option
              v-for="item in DataSourceTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <!-- 动态数据源配置表单 -->
        <template v-if="formData.dataSourceType">
          <!-- PostgreSQL 配置 -->
          <template v-if="formData.dataSourceType === 'postgresql'">
            <el-form-item label="表名" prop="dataSourceConfig.table">
              <el-input
                v-model="formData.dataSourceConfig.table"
                placeholder="如：users、orders"
                clearable
              />
            </el-form-item>

            <el-form-item label="实体字段" prop="dataSourceConfig.entityColumn">
              <el-input
                v-model="formData.dataSourceConfig.entityColumn"
                placeholder="如：id、user_id"
                clearable
              />
            </el-form-item>

            <el-form-item label="日期字段" prop="dataSourceConfig.dateColumn">
              <el-input
                v-model="formData.dataSourceConfig.dateColumn"
                placeholder="如：created_at、updated_at"
                clearable
              />
            </el-form-item>
          </template>

          <!-- API 配置 -->
          <template v-else-if="formData.dataSourceType === 'api'">
            <el-form-item label="API地址" prop="dataSourceConfig.url">
              <el-input
                v-model="formData.dataSourceConfig.url"
                placeholder="https://api.example.com/data"
                clearable
              />
            </el-form-item>

            <el-form-item label="请求方法" prop="dataSourceConfig.method">
              <el-select v-model="formData.dataSourceConfig.method">
                <el-option label="GET" value="GET" />
                <el-option label="POST" value="POST" />
              </el-select>
            </el-form-item>

            <el-form-item label="实体字段" prop="dataSourceConfig.entityField">
              <el-input
                v-model="formData.dataSourceConfig.entityField"
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
          <template v-else-if="formData.dataSourceType === 'redis'">
            <el-form-item label="Key模式" prop="dataSourceConfig.keyPattern">
              <el-input
                v-model="formData.dataSourceConfig.keyPattern"
                placeholder="如：user:*、item:*"
                clearable
              />
            </el-form-item>

            <el-form-item label="实体字段" prop="dataSourceConfig.entityField">
              <el-input
                v-model="formData.dataSourceConfig.entityField"
                placeholder="Key中提取实体ID的字段"
                clearable
              />
            </el-form-item>

            <el-form-item label="数据结构" prop="dataSourceConfig.dataStructure">
              <el-select v-model="formData.dataSourceConfig.dataStructure">
                <el-option label="String" value="string" />
                <el-option label="Hash" value="hash" />
                <el-option label="List" value="list" />
                <el-option label="Set" value="set" />
                <el-option label="ZSet" value="zset" />
              </el-select>
            </el-form-item>
          </template>

          <!-- Kafka 配置 -->
          <template v-else-if="formData.dataSourceType === 'kafka'">
            <el-form-item label="Topic" prop="dataSourceConfig.topic">
              <el-input
                v-model="formData.dataSourceConfig.topic"
                placeholder="如：user-events"
                clearable
              />
            </el-form-item>

            <el-form-item label="实体字段" prop="dataSourceConfig.entityField">
              <el-input
                v-model="formData.dataSourceConfig.entityField"
                placeholder="消息JSON中的实体ID字段名"
                clearable
              />
            </el-form-item>

            <el-form-item label="最大记录数" prop="dataSourceConfig.maxPollRecords">
              <el-input-number
                v-model="formData.dataSourceConfig.maxPollRecords"
                :min="1"
                :max="10000"
                :step="100"
              />
            </el-form-item>

            <el-form-item label="消费者组" prop="dataSourceConfig.consumerGroup">
              <el-input
                v-model="formData.dataSourceConfig.consumerGroup"
                placeholder="feature-computation-group"
                clearable
              />
            </el-form-item>
          </template>

          <!-- 测试连接按钮 -->
          <el-form-item>
            <el-button
              type="info"
              :icon="Connection"
              :loading="testingConnection"
              @click="handleTestConnection"
            >
              测试连接
            </el-button>
            <span v-if="connectionResult" class="connection-result" :class="connectionResult.connected ? 'success' : 'error'">
              {{ connectionResult.connected ? '✓ 连接成功' : '✗ 连接失败' }}
            </span>
          </el-form-item>
        </template>
      </div>

      <!-- YAML配置预览 -->
      <div class="form-section" v-if="formData.dataSourceType">
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
 * - 连接测试
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  Connection,
  Edit
} from '@element-plus/icons-vue'
import MonacoEditor from '@/components/MonacoEditor.vue'
import { useFeaturesStore } from '@/stores/features'
import {
  DataSourceTypeOptions,
  CommonEntityTypes,
  DefaultValues,
  ValidationPatterns,
  ValidationMessages,
  PostgreSQLConfigTemplate,
  ApiConfigTemplate,
  RedisConfigTemplate,
  KafkaConfigTemplate
} from '@/constants/features'

// ==================== Store ====================
const featuresStore = useFeaturesStore()

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
const testingConnection = ref(false)
const connectionResult = ref(null)
const showYamlEditor = ref(false)
const yamlConfig = ref('')
const apiHeadersText = ref('')

// 表单数据
const formData = ref({
  name: '',
  entity: '',
  ttl: DefaultValues.TTL,
  description: '',
  dataSourceType: '',
  dataSourceConfig: {}
})

// ==================== 计算属性 ====================
/**
 * 对话框标题
 */
const dialogTitle = computed(() => {
  return props.mode === 'create' ? '创建特征视图' : '编辑特征视图'
})

/**
 * YAML配置预览
 */
const yamlPreview = computed(() => {
  if (!formData.value.dataSourceType || !formData.value.dataSourceConfig) {
    return '# 请先配置数据源'
  }

  const config = {
    name: formData.value.name,
    entity: formData.value.entity,
    ttl: formData.value.ttl,
    description: formData.value.description,
    dataSource: {
      type: formData.value.dataSourceType,
      config: formData.value.dataSourceConfig
    }
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
  dataSourceType: [
    { required: true, message: ValidationMessages.DATA_SOURCE_TYPE_REQUIRED, trigger: 'change' }
  ]
}

// ==================== 方法 ====================
/**
 * 初始化表单数据
 */
const initFormData = () => {
  if (props.mode === 'edit' && props.viewData) {
    // 编辑模式：填充现有数据
    formData.value = {
      name: props.viewData.name,
      entity: props.viewData.entity,
      ttl: props.viewData.ttl || DefaultValues.TTL,
      description: props.viewData.description || '',
      dataSourceType: props.viewData.dataSourceType || '',
      dataSourceConfig: props.viewData.dataSourceConfig || {}
    }

    // 如果是API类型，格式化headers
    if (formData.value.dataSourceType === 'api' && formData.value.dataSourceConfig.headers) {
      apiHeadersText.value = JSON.stringify(formData.value.dataSourceConfig.headers, null, 2)
    }
  } else {
    // 创建模式：使用默认值
    formData.value = {
      name: '',
      entity: '',
      ttl: DefaultValues.TTL,
      description: '',
      dataSourceType: '',
      dataSourceConfig: {}
    }
    apiHeadersText.value = ''
  }
}

/**
 * 处理数据源类型变化
 */
const handleDataSourceTypeChange = (type) => {
  // 根据类型设置默认配置模板
  switch (type) {
    case 'postgresql':
      formData.value.dataSourceConfig = { ...PostgreSQLConfigTemplate }
      break
    case 'api':
      formData.value.dataSourceConfig = { ...ApiConfigTemplate }
      apiHeadersText.value = JSON.stringify(ApiConfigTemplate.headers || {}, null, 2)
      break
    case 'redis':
      formData.value.dataSourceConfig = { ...RedisConfigTemplate }
      break
    case 'kafka':
      formData.value.dataSourceConfig = { ...KafkaConfigTemplate }
      break
    default:
      formData.value.dataSourceConfig = {}
  }

  connectionResult.value = null
}

/**
 * 更新API请求头
 */
const updateApiHeaders = () => {
  try {
    if (apiHeadersText.value.trim()) {
      const headers = JSON.parse(apiHeadersText.value)
      formData.value.dataSourceConfig.headers = headers
    } else {
      formData.value.dataSourceConfig.headers = {}
    }
  } catch (error) {
    ElMessage.warning('请求头JSON格式不正确')
  }
}

/**
 * 测试数据源连接
 */
const handleTestConnection = async () => {
  try {
    testingConnection.value = true
    connectionResult.value = null

    // 验证数据源配置
    if (!formData.value.dataSourceType) {
      ElMessage.warning('请先选择数据源类型')
      return
    }

    if (!formData.value.dataSourceConfig || Object.keys(formData.value.dataSourceConfig).length === 0) {
      ElMessage.warning('请先配置数据源信息')
      return
    }

    // 调用后端API测试连接
    const config = {
      type: formData.value.dataSourceType,
      config: JSON.stringify(formData.value.dataSourceConfig)
    }

    ElMessage.info('正在测试连接...')

    // 调用store中的测试连接方法
    const result = await featuresStore.testConnection(config)

    connectionResult.value = result

    if (result.connected) {
      ElMessage.success({
        message: '连接测试成功！',
        duration: 3000
      })
    } else {
      ElMessage.error({
        message: '连接测试失败，请检查配置',
        duration: 5000
      })
    }
  } catch (error) {
    connectionResult.value = { connected: false }
    ElMessage.error({
      message: '连接测试失败: ' + error.message,
      duration: 5000,
      showClose: true
    })
  } finally {
    testingConnection.value = false
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
    if (parsed.dataSource) {
      if (parsed.dataSource.type) {
        formData.value.dataSourceType = parsed.dataSource.type
      }
      if (parsed.dataSource.config) {
        if (typeof parsed.dataSource.config === 'string') {
          // 如果是字符串，尝试解析为JSON
          try {
            formData.value.dataSourceConfig = JSON.parse(parsed.dataSource.config)
          } catch {
            formData.value.dataSourceConfig = parsed.dataSource.config
          }
        } else {
          formData.value.dataSourceConfig = parsed.dataSource.config
        }
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
      dataSourceType: formData.value.dataSourceType,
      dataSourceConfig: JSON.stringify(formData.value.dataSourceConfig)
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
watch(() => props.visible, (visible) => {
  if (visible) {
    nextTick(() => {
      initFormData()
    })
  }
})

// 监听viewData变化（用于编辑模式下数据更新）
watch(() => props.viewData, () => {
  if (props.visible) {
    initFormData()
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
