<template>
  <el-dialog
    :title="dialogTitle"
    :model-value="props.visible"
    width="700px"
    :close-on-click-modal="false"
    @update:model-value="handleClose"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="training-form"
    >
      <!-- 实验名称 -->
      <el-form-item label="实验名称" prop="experimentName">
        <el-input
          v-model="form.experimentName"
          placeholder="请输入实验名称"
          clearable
        />
      </el-form-item>

      <!-- 数据集选择 -->
      <el-form-item label="数据集" prop="datasetName">
        <el-select
          v-model="selectedDataset"
          placeholder="选择数据集"
          clearable
          :loading="loadingDatasets"
          style="width: 100%; margin-bottom: 8px"
          @change="handleDatasetChange"
        >
          <el-option
            v-for="ds in datasetList"
            :key="ds.name"
            :label="ds.name + (ds.description ? ' (' + ds.description + ')' : '')"
            :value="ds.name"
          />
        </el-select>
      </el-form-item>

      <!-- 数据集版本 -->
      <el-form-item label="版本" prop="datasetVersion">
        <el-select
          v-model="form.datasetVersion"
          placeholder="选择版本或手动输入"
          clearable
          filterable
          allow-create
          default-first-option
          style="width: 100%"
        >
          <el-option
            v-for="v in versionList"
            :key="v"
            :label="v"
            :value="v"
          />
        </el-select>
      </el-form-item>

      <!-- 模型类型 -->
      <el-form-item label="模型类型" prop="modelType">
        <el-radio-group v-model="form.modelType">
          <el-radio-button label="lightgbm">LightGBM</el-radio-button>
          <el-radio-button label="xgboost">XGBoost</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 模型参数 -->
      <el-divider content-position="left">模型参数</el-divider>

      <template v-if="form.modelType === 'lightgbm'">
        <el-form-item label="num_leaves">
          <el-input-number v-model="form.params.num_leaves" :min="2" :max="256" />
        </el-form-item>
        <el-form-item label="learning_rate">
          <el-slider v-model="form.params.learning_rate" :min="0.001" :max="0.5" :step="0.001" show-input />
        </el-form-item>
        <el-form-item label="feature_fraction">
          <el-slider v-model="form.params.feature_fraction" :min="0.1" :max="1" :step="0.05" show-input />
        </el-form-item>
        <el-form-item label="bagging_fraction">
          <el-slider v-model="form.params.bagging_fraction" :min="0.1" :max="1" :step="0.05" show-input />
        </el-form-item>
        <el-form-item label="bagging_freq">
          <el-input-number v-model="form.params.bagging_freq" :min="0" :max="20" />
        </el-form-item>
      </template>

      <template v-else-if="form.modelType === 'xgboost'">
        <el-form-item label="max_depth">
          <el-input-number v-model="form.params.max_depth" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="learning_rate">
          <el-slider v-model="form.params.learning_rate" :min="0.001" :max="0.5" :step="0.001" show-input />
        </el-form-item>
        <el-form-item label="subsample">
          <el-slider v-model="form.params.subsample" :min="0.1" :max="1" :step="0.05" show-input />
        </el-form-item>
        <el-form-item label="colsample_bytree">
          <el-slider v-model="form.params.colsample_bytree" :min="0.1" :max="1" :step="0.05" show-input />
        </el-form-item>
      </template>

      <!-- 训练参数 -->
      <el-divider content-position="left">训练参数</el-divider>

      <el-form-item label="评估指标">
        <el-select v-model="form.metric" placeholder="选择评估指标">
          <el-option label="AUC" value="auc" />
          <el-option label="LogLoss" value="logloss" />
          <el-option label="RMSE" value="rmse" />
          <el-option label="MAE" value="mae" />
        </el-select>
      </el-form-item>

      <!-- 异步训练选项 -->
      <el-form-item label="执行方式">
        <el-switch
          v-model="form.async"
          active-text="异步训练"
          inactive-text="同步训练"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ form.async ? '提交任务' : '开始训练' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listDatasets, listDatasetVersions } from '@/api/modules/samples'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  mode: {
    type: String,
    default: 'create' // 'create' | 'tune'
  }
})

const emit = defineEmits(['close', 'submit'])

const formRef = ref(null)
const submitting = ref(false)
const loadingDatasets = ref(false)

const datasetList = ref([])
const versionList = ref([])
const selectedDataset = ref('')

const dialogTitle = computed(() => {
  return props.mode === 'tune' ? '超参数调优配置' : '新建训练任务'
})

const defaultForm = () => ({
  experimentName: '',
  datasetVersion: '',
  modelType: 'lightgbm',
  params: {
    num_leaves: 31,
    learning_rate: 0.05,
    feature_fraction: 0.8,
    bagging_fraction: 0.8,
    bagging_freq: 5
  },
  metric: 'auc',
  async: true
})

const form = ref(defaultForm())

const rules = {
  experimentName: [
    { required: true, message: '请输入实验名称', trigger: 'blur' },
    { min: 3, max: 100, message: '长度在 3 到 100 个字符', trigger: 'blur' }
  ],
  datasetVersion: [
    { required: true, message: '请输入数据集版本', trigger: 'blur' }
  ],
  modelType: [
    { required: true, message: '请选择模型类型', trigger: 'change' }
  ]
}

// 模型类型切换时重置参数
watch(() => form.value.modelType, (newType) => {
  if (newType === 'lightgbm') {
    form.value.params = {
      num_leaves: 31,
      learning_rate: 0.05,
      feature_fraction: 0.8,
      bagging_fraction: 0.8,
      bagging_freq: 5
    }
  } else if (newType === 'xgboost') {
    form.value.params = {
      max_depth: 6,
      learning_rate: 0.05,
      subsample: 0.8,
      colsample_bytree: 0.8
    }
  }
})

// 组件挂载时加载数据集（v-if 重建时也会触发）
onMounted(() => {
  if (props.visible) {
    loadDatasets()
  }
})

// 监听 visible 变化（对话框重新打开时刷新）
watch(() => props.visible, (val) => {
  if (val) {
    form.value = defaultForm()
    selectedDataset.value = ''
    versionList.value = []
    loadDatasets()
  }
})

const loadDatasets = async () => {
  loadingDatasets.value = true
  try {
    const response = await listDatasets()
    const isSuccess = response.code === '200' || response.code === 200
    if (isSuccess) {
      datasetList.value = response.data || []
      console.log('[TrainingConfigDialog] 数据集加载成功:', datasetList.value.length, '条')
    } else {
      // 非成功状态码已由 request.js 拦截器统一提示
    }
  } catch (error) {
    console.error('加载数据集列表失败:', error)
    // 错误已由 request.js 拦截器统一提示
  } finally {
    loadingDatasets.value = false
  }
}

const handleDatasetChange = async (datasetName) => {
  form.value.datasetVersion = ''
  versionList.value = []
  if (!datasetName) return

  try {
    const response = await listDatasetVersions(datasetName)
    const isSuccess = response.code === '200' || response.code === 200
    if (isSuccess) {
      const versions = response.data || []
      if (versions.length > 0) {
        versionList.value = versions
      } else {
        // 没有版本时，默认提供 v1.0 作为选项
        versionList.value = ['v1.0']
      }
    }
  } catch (error) {
    console.error('加载版本列表失败:', error)
    versionList.value = ['v1.0']
  }
}

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true

  try {
    // 构建后端需要的配置格式
    const config = {
      experimentName: form.value.experimentName,
      datasetVersion: form.value.datasetVersion,
      model: {
        type: form.value.modelType,
        params: form.value.params
      },
      trainingParams: {
        metric: form.value.metric
      }
    }

    emit('submit', config, form.value.async)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.training-form {
  padding: 10px 0;
}

:deep(.el-slider) {
  width: 200px;
  margin-right: 16px;
}
</style>
