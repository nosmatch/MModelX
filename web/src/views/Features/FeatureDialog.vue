<template>
  <el-dialog
    :title="dialogTitle"
    v-model="dialogVisible"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item label="特征名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入特征名称（字母、数字、下划线）"
          :disabled="isEdit"
        />
      </el-form-item>

      <el-form-item label="数据类型" prop="dtype">
        <el-select v-model="form.dtype" placeholder="选择数据类型" style="width: 100%">
          <el-option label="INT64（整数）" value="INT64" />
          <el-option label="FLOAT64（浮点数）" value="FLOAT64" />
          <el-option label="STRING（字符串）" value="STRING" />
          <el-option label="BOOLEAN（布尔值）" value="BOOLEAN" />
        </el-select>
      </el-form-item>

      <el-form-item label="Transform表达式" prop="transformExpr">
        <el-input
          v-model="form.transformExpr"
          type="textarea"
          :rows="3"
          placeholder="例如: SUM(order_amount)"
        />
        <div class="form-tip">支持SQL聚合函数和表达式</div>
      </el-form-item>

      <el-form-item label="时间窗口" prop="timeWindow">
        <el-select v-model="form.timeWindow" placeholder="选择时间窗口" clearable style="width: 100%">
          <el-option label="无（全部历史数据）" value="" />
          <el-option label="1小时" value="1h" />
          <el-option label="6小时" value="6h" />
          <el-option label="12小时" value="12h" />
          <el-option label="1天" value="1d" />
          <el-option label="3天" value="3d" />
          <el-option label="7天" value="7d" />
          <el-option label="14天" value="14d" />
          <el-option label="30天" value="30d" />
          <el-option label="60天" value="60d" />
          <el-option label="90天" value="90d" />
        </el-select>
        <div class="form-tip">聚合类特征建议设置，不选则使用全部历史数据</div>
      </el-form-item>

      <el-form-item label="默认值" prop="defaultValue">
        <el-input
          v-model="defaultValueStr"
          placeholder='{"value": 0} 或 {"value": "unknown"}'
        />
        <div class="form-tip">JSON格式，可选</div>
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          placeholder="特征的业务含义描述"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as featuresApi from '@/api/modules/features'
import { updateFeatureDefinition } from '@/api/modules/features'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  mode: {
    type: String,
    default: 'create' // 'create' | 'edit'
  },
  featureViewName: {
    type: String,
    default: ''
  },
  featureData: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'success', 'update:visible'])

const formRef = ref(null)
const submitting = ref(false)
const defaultValueStr = ref('')

// 对话框可见性（使用computed代理prop，避免直接v-model在prop上）
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const isEdit = computed(() => props.mode === 'edit')
const dialogTitle = computed(() => isEdit.value ? '编辑特征' : '添加特征')

const form = ref({
  name: '',
  dtype: 'FLOAT64',
  transformExpr: '',
  timeWindow: '',
  description: '',
  defaultValue: null
})

const rules = {
  name: [
    { required: true, message: '请输入特征名称', trigger: 'blur' },
    { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '名称只能包含字母、数字、下划线，且不能以数字开头', trigger: 'blur' }
  ],
  dtype: [
    { required: true, message: '请选择数据类型', trigger: 'change' }
  ],
  transformExpr: [
    { required: true, message: '请输入Transform表达式', trigger: 'blur' }
  ]
}

// 监听visible和featureData变化
watch(() => props.visible, (val) => {
  if (val) {
    if (isEdit.value && props.featureData) {
      form.value = {
        ...props.featureData,
        timeWindow: props.featureData.timeWindow || ''
      }
      defaultValueStr.value = props.featureData.defaultValue
        ? JSON.stringify(props.featureData.defaultValue)
        : ''
    } else {
      resetForm()
    }
  }
}, { immediate: true })

const resetForm = () => {
  form.value = {
    name: '',
    dtype: 'FLOAT64',
    transformExpr: '',
    timeWindow: '',
    description: '',
    defaultValue: null
  }
  defaultValueStr.value = ''
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const handleClose = () => {
  resetForm()
  emit('close')
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 解析默认值JSON
  let defaultValue = null
  if (defaultValueStr.value.trim()) {
    try {
      defaultValue = JSON.parse(defaultValueStr.value)
    } catch (e) {
      ElMessage.error('默认值JSON格式错误')
      return
    }
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      // 编辑模式：调用更新接口
      const spec = {
        name: form.value.name,
        dtype: form.value.dtype,
        transformExpr: form.value.transformExpr,
        timeWindow: form.value.timeWindow || null,
        description: form.value.description,
        defaultValue
      }
      await updateFeatureDefinition(props.featureViewName, form.value.name, spec)
    } else {
      // 新增模式：调用注册接口
      const definition = {
        featureView: props.featureViewName,
        entity: '',
        features: [
          {
            name: form.value.name,
            dtype: form.value.dtype,
            transformExpr: form.value.transformExpr,
            timeWindow: form.value.timeWindow || null,
            description: form.value.description,
            defaultValue
          }
        ],
        source: {
          type: 'unknown',
          path: null
        }
      }
      await featuresApi.registerFeatureDefinition(definition)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    emit('success')
    handleClose()
  } catch (error) {
    ElMessage.error((isEdit.value ? '更新' : '添加') + '失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: $text-muted;
  margin-top: 4px;
}
</style>
