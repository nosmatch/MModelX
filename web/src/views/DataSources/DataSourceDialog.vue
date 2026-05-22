<template>
  <el-dialog
    :model-value="visible"
    :title="mode === 'create' ? '创建数据源' : '编辑数据源'"
    width="600px"
    :close-on-click-modal="false"
    @close="$emit('close')"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <!-- 基本信息 -->
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" :disabled="mode === 'edit'" placeholder="全局唯一标识名称" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="form.type" :disabled="mode === 'edit'" style="width:100%" @change="onTypeChange">
          <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>

      <!-- 连接配置（JDBC / Redis） -->
      <template v-if="needsHostPort">
        <el-form-item label="主机" prop="host">
          <el-input v-model="form.host" placeholder="hostname 或 IP" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width:100%" />
        </el-form-item>
      </template>

      <!-- 数据库名（仅 JDBC） -->
      <el-form-item v-if="isJdbc" label="数据库" prop="databaseName">
        <el-input v-model="form.databaseName" placeholder="数据库名称" />
      </el-form-item>

      <!-- API 端点 -->
      <el-form-item v-if="form.type === 'api'" label="API 地址" prop="host">
        <el-input v-model="form.host" placeholder="https://api.example.com" />
      </el-form-item>

      <!-- 认证 -->
      <template v-if="needsAuth">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password
            :placeholder="mode === 'edit' ? '不修改请留空' : ''" />
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="$emit('close')">取消</el-button>
      <el-button :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  visible: Boolean,
  mode: { type: String, default: 'create' }, // 'create' | 'edit'
  data: { type: Object, default: null }
})
const emit = defineEmits(['close', 'save'])

const typeOptions = [
  { value: 'postgresql', label: 'PostgreSQL' },
  { value: 'mysql', label: 'MySQL' },
  { value: 'redis', label: 'Redis' },
  { value: 'kafka', label: 'Kafka' },
  { value: 'minio', label: 'MinIO' },
  { value: 'api', label: 'HTTP API' },
  { value: 'local_file', label: '本地文件' }
]

const defaultPorts = { postgresql: 5432, mysql: 3306, redis: 6379, kafka: 9092, minio: 9000 }

const formRef = ref()
const saving = ref(false)

const form = ref({
  name: '', type: 'postgresql', description: '',
  host: '', port: 5432, databaseName: '', username: '', password: ''
})

const isJdbc = computed(() => ['postgresql', 'mysql'].includes(form.value.type))
const needsHostPort = computed(() => form.value.type !== 'api' && form.value.type !== 'local_file')
const needsAuth = computed(() => ['postgresql', 'mysql', 'redis', 'kafka'].includes(form.value.type))

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机/地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }]
}

function onTypeChange(type) {
  if (defaultPorts[type]) form.value.port = defaultPorts[type]
}

// 编辑时回填数据
watch(() => props.data, (val) => {
  if (val) {
    form.value = { ...form.value, ...val, password: '' }
  } else {
    form.value = { name: '', type: 'postgresql', description: '', host: '', port: 5432, databaseName: '', username: '', password: '' }
  }
}, { immediate: true })

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form.value }
    // 密码字段：编辑时为空则不传
    if (props.mode === 'edit' && !payload.password) delete payload.password
    else if (payload.password) payload.passwordEncrypted = payload.password
    delete payload.password
    emit('save', payload)
  } finally {
    saving.value = false
  }
}
</script>
