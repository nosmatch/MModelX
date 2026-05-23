<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="780px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="130px"
      @submit.prevent="handleSubmit"
    >
      <!-- 基本信息 -->
      <div class="form-section">
        <h3 class="section-title">基本信息</h3>

        <el-form-item label="配置名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入配置名称"
            :disabled="mode === 'edit'"
            clearable
          />
          <div class="form-tip">创建后不可修改，建议用有意义的名称</div>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="请输入配置描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="标签类型" prop="labelType">
          <el-select v-model="formData.labelType" placeholder="选择标签类型" style="width: 100%">
            <el-option label="二分类" value="BINARY" />
            <el-option label="多分类" value="MULTICLASS" />
            <el-option label="回归" value="REGRESSION" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="选择状态" style="width: 100%">
            <el-option label="激活" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
            <el-option label="归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
      </div>

      <!-- 数据源配置 -->
      <div class="form-section">
        <h3 class="section-title">数据源配置</h3>

        <el-form-item label="实体字段" prop="entityColumn">
          <el-input v-model="formData.entityColumn" placeholder="如：user_id、entity_id" clearable />
          <div class="form-tip">用于关联特征的实体标识字段</div>
        </el-form-item>

        <el-form-item label="时间字段" prop="timeColumn">
          <el-input v-model="formData.timeColumn" placeholder="如：event_time、timestamp" clearable />
          <div class="form-tip">用于 Point-in-time join 的时间戳字段</div>
        </el-form-item>

        <el-form-item label="标签来源" prop="labelSource">
          <el-input
            v-model="formData.labelSource"
            placeholder="标签表名或SQL"
            clearable
          />
          <div class="form-tip">指定标签数据所在的表或查询</div>
        </el-form-item>

        <el-form-item label="标签列名" prop="labelColumn">
          <el-input
            v-model="formData.labelColumn"
            placeholder="如：is_churned、is_click"
            clearable
          />
          <div class="form-tip">标签在表中的列名</div>
        </el-form-item>

        <el-form-item label="特征视图" prop="featureViews">
          <el-select
            v-model="formData.featureViews"
            multiple
            filterable
            placeholder="选择关联的特征视图"
            style="width: 100%"
          >
            <el-option
              v-for="view in featureViewOptions"
              :key="view.name"
              :label="view.name"
              :value="view.name"
            />
          </el-select>
          <div class="form-tip">选择用于构造样本的特征视图</div>
        </el-form-item>
      </div>

      <!-- 样本划分 -->
      <div class="form-section">
        <h3 class="section-title">样本划分</h3>

        <el-form-item label="划分策略" prop="splitStrategy">
          <el-radio-group v-model="formData.splitStrategy">
            <el-radio-button label="RANDOM">随机划分</el-radio-button>
            <el-radio-button label="TEMPORAL">时序划分</el-radio-button>
            <el-radio-button label="STRATIFIED">分层划分</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="训练集比例" prop="trainRatio">
          <el-slider v-model="formData.trainRatio" :min="0" :max="1" :step="0.05" show-input />
        </el-form-item>

        <el-form-item label="验证集比例" prop="valRatio">
          <el-slider v-model="formData.valRatio" :min="0" :max="1" :step="0.05" show-input />
        </el-form-item>

        <el-form-item label="测试集比例" prop="testRatio">
          <el-slider v-model="formData.testRatio" :min="0" :max="1" :step="0.05" show-input />
        </el-form-item>

        <el-form-item v-if="ratioSum !== 1" class="ratio-warning">
          <el-alert
            :title="`比例之和为 ${(ratioSum * 100).toFixed(0)}%，应为 100%`"
            type="warning"
            :closable="false"
            show-icon
          />
        </el-form-item>
      </div>

      <!-- 高级选项 -->
      <div class="form-section">
        <h3 class="section-title">高级选项</h3>

        <el-form-item label="负采样比例" prop="negativeSamplingRatio">
          <el-input-number
            v-model="formData.negativeSamplingRatio"
            :min="0"
            :max="10"
            :step="0.1"
            :precision="2"
            style="width: 180px"
          />
          <div class="form-tip">0 表示不进行负采样，1 表示 1:1 正负样本</div>
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ mode === 'create' ? '创建' : '保存' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useFeaturesStore } from '@/stores/features'

const props = defineProps({
  visible: { type: Boolean, required: true },
  mode: { type: String, default: 'create' },
  configData: { type: Object, default: null }
})

const emit = defineEmits(['close', 'save'])

const formRef = ref(null)
const submitting = ref(false)
const featuresStore = useFeaturesStore()

const featureViewOptions = computed(() => featuresStore.views)

const formData = ref({
  name: '',
  description: '',
  labelType: 'BINARY',
  status: 'ACTIVE',
  entityColumn: '',
  timeColumn: '',
  labelSource: '',
  labelColumn: '',
  featureViews: [],
  splitStrategy: 'RANDOM',
  trainRatio: 0.7,
  valRatio: 0.15,
  testRatio: 0.15,
  negativeSamplingRatio: 0
})

const dialogTitle = computed(() => props.mode === 'create' ? '创建样本配置' : '编辑样本配置')

const ratioSum = computed(() => {
  return (formData.value.trainRatio || 0) + (formData.value.valRatio || 0) + (formData.value.testRatio || 0)
})

const formRules = {
  name: [
    { required: true, message: '请输入配置名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  labelType: [{ required: true, message: '请选择标签类型', trigger: 'change' }],
  entityColumn: [{ required: true, message: '请输入实体字段', trigger: 'blur' }],
  splitStrategy: [{ required: true, message: '请选择划分策略', trigger: 'change' }],
  trainRatio: [{ required: true, message: '请设置训练集比例', trigger: 'change' }],
  valRatio: [{ required: true, message: '请设置验证集比例', trigger: 'change' }],
  testRatio: [{ required: true, message: '请设置测试集比例', trigger: 'change' }]
}

const loadFeatureViews = async () => {
  if (featuresStore.views.length === 0) {
    try {
      await featuresStore.fetchViews()
    } catch (error) {
      console.error('加载特征视图失败:', error)
    }
  }
}

const initFormData = () => {
  if (props.mode === 'edit' && props.configData) {
    formData.value = {
      name: props.configData.name,
      description: props.configData.description || '',
      labelType: props.configData.labelType || 'BINARY',
      status: props.configData.status || 'ACTIVE',
      entityColumn: props.configData.entityColumn || '',
      timeColumn: props.configData.timeColumn || '',
      labelSource: props.configData.labelSource || '',
      labelColumn: props.configData.labelColumn || '',
      featureViews: props.configData.featureViews || [],
      splitStrategy: props.configData.splitStrategy || 'RANDOM',
      trainRatio: props.configData.trainRatio ?? 0.7,
      valRatio: props.configData.valRatio ?? 0.15,
      testRatio: props.configData.testRatio ?? 0.15,
      negativeSamplingRatio: props.configData.negativeSamplingRatio ?? 0
    }
  } else {
    formData.value = {
      name: '',
      description: '',
      labelType: 'BINARY',
      status: 'ACTIVE',
      entityColumn: '',
      timeColumn: '',
      labelSource: '',
      labelColumn: '',
      featureViews: [],
      splitStrategy: 'RANDOM',
      trainRatio: 0.7,
      valRatio: 0.15,
      testRatio: 0.15,
      negativeSamplingRatio: 0
    }
  }
}

const handleSubmit = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    if (Math.abs(ratioSum.value - 1) > 0.001) {
      ElMessage.warning('训练/验证/测试集比例之和必须等于 100%')
      return
    }

    submitting.value = true
    const submitData = { ...formData.value }
    emit('save', submitData)
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  formRef.value?.resetFields()
  emit('close')
}

watch(() => props.visible, (visible) => {
  if (visible) {
    loadFeatureViews()
    initFormData()
  }
}, { immediate: true })
</script>

<style scoped lang="scss">
.form-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid $border-light;

  &:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 20px 0;
  }
}

.form-tip {
  font-size: 12px;
  color: $text-muted;
  margin-top: 4px;
  line-height: 1.5;
}

.ratio-warning {
  margin-bottom: 0;
}
</style>
