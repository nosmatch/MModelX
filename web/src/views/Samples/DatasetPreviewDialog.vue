<template>
  <el-dialog
    v-model="visible"
    title="样本数据预览"
    width="90%"
    top="5vh"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <div v-loading="loading" class="preview-container">
      <!-- 头部信息 -->
      <div class="preview-header">
        <el-descriptions :column="4" size="small" border>
          <el-descriptions-item label="数据集">{{ datasetName }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ version }}</el-descriptions-item>
          <el-descriptions-item label="总样本数">{{ formatNumber(totalCount) }}</el-descriptions-item>
          <el-descriptions-item label="预览条数">{{ formatNumber(previewCount) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 划分切换 -->
      <div class="preview-toolbar">
        <el-radio-group v-model="currentSplit" @change="handleSplitChange">
          <el-radio-button label="train">训练集</el-radio-button>
          <el-radio-button label="val">验证集</el-radio-button>
          <el-radio-button label="test">测试集</el-radio-button>
        </el-radio-group>
        <el-input-number
          v-model="limit"
          :min="10"
          :max="200"
          :step="10"
          size="small"
          style="width: 120px; margin-left: 16px"
          @change="handleLimitChange"
        >
          <template #suffix>条</template>
        </el-input-number>
        <el-button size="small" type="primary" :icon="Refresh" style="margin-left: 8px" @click="loadPreview">
          刷新
        </el-button>
      </div>

      <!-- 数据表格 -->
      <div class="preview-table-wrapper">
        <el-table
          v-if="columns.length > 0"
          :data="tableData"
          stripe
          size="small"
          max-height="500"
          border
        >
          <el-table-column type="index" width="50" align="center" />
          <el-table-column
            v-for="col in columns"
            :key="col"
            :prop="col"
            :label="col"
            min-width="120"
            show-overflow-tooltip
          />
        </el-table>
        <el-empty v-else description="暂无数据" />
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { previewDataset } from '@/api/modules/samples'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  datasetName: { type: String, default: '' },
  version: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const currentSplit = ref('train')
const limit = ref(50)
const totalCount = ref(0)
const previewCount = ref(0)
const columns = ref([])
const tableData = ref([])

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    loadPreview()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadPreview = async () => {
  if (!props.datasetName || !props.version) return

  loading.value = true
  try {
    const res = await previewDataset(
      props.datasetName,
      props.version,
      currentSplit.value,
      limit.value
    )
    if (res.code === '200' || res.code === 200) {
      const data = res.data
      totalCount.value = data.totalCount || 0
      previewCount.value = data.previewCount || 0
      columns.value = data.columns || []
      tableData.value = data.data || []
    } else {
      ElMessage.error(res.message || '加载预览数据失败')
    }
  } catch (error) {
    ElMessage.error('加载预览数据失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleSplitChange = () => {
  loadPreview()
}

const handleLimitChange = () => {
  loadPreview()
}

const handleClose = () => {
  visible.value = false
  columns.value = []
  tableData.value = []
}

const formatNumber = (num) => {
  if (num === undefined || num === null) return '-'
  return num.toLocaleString()
}
</script>

<style scoped lang="scss">
.preview-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-header {
  :deep(.el-descriptions__cell) {
    padding: 8px 12px;
  }
}

.preview-toolbar {
  display: flex;
  align-items: center;
}

.preview-table-wrapper {
  :deep(.el-table) {
    font-size: 13px;
  }
  :deep(.el-table__cell) {
    padding: 4px 0;
  }
}
</style>
