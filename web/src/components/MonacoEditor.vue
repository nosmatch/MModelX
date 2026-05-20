<template>
  <div ref="editorContainer" class="monaco-editor-container"></div>
</template>

<script setup>
/**
 * Monaco Editor 组件
 *
 * 基于 Microsoft Monaco Editor 的代码编辑器
 * 支持多种语言高亮、代码补全、智能提示等功能
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'

// ==================== Props ====================
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'javascript'
  },
  theme: {
    type: String,
    default: 'vs' // 'vs' | 'vs-dark' | 'hc-black'
  },
  height: {
    type: [String, Number],
    default: 400
  },
  options: {
    type: Object,
    default: () => ({})
  },
  readOnly: {
    type: Boolean,
    default: false
  }
})

// ==================== Emits ====================
const emit = defineEmits(['update:modelValue', 'change'])

// ==================== 响应式数据 ====================
const editorContainer = ref(null)
let editorInstance = null

// ==================== 初始化编辑器 ====================
onMounted(() => {
  if (!editorContainer.value) return

  // 创建编辑器实例
  editorInstance = monaco.editor.create(editorContainer.value, {
    value: props.modelValue,
    language: props.language,
    theme: props.theme,
    readOnly: props.readOnly,
    automaticLayout: true,
    minimap: {
      enabled: false
    },
    scrollBeyondLastLine: false,
    fontSize: 14,
    lineNumbers: 'on',
    folding: true,
    bracketPairColorization: {
      enabled: true
    },
    ...props.options
  })

  // 监听内容变化
  editorInstance.onDidChangeModelContent(() => {
    const value = editorInstance.getValue()
    emit('update:modelValue', value)
    emit('change', value)
  })

  // 设置高度
  setEditorHeight()
})

// ==================== 监听props变化 ====================
// 监听modelValue变化
watch(() => props.modelValue, (newValue) => {
  if (editorInstance && newValue !== editorInstance.getValue()) {
    editorInstance.setValue(newValue)
  }
})

// 监听语言变化
watch(() => props.language, (newLanguage) => {
  if (editorInstance) {
    const model = editorInstance.getModel()
    monaco.editor.setModelLanguage(model, newLanguage)
  }
})

// 监听主题变化
watch(() => props.theme, (newTheme) => {
  if (editorInstance) {
    monaco.editor.setTheme(newTheme)
  }
})

// 监听只读状态
watch(() => props.readOnly, (newReadOnly) => {
  if (editorInstance) {
    editorInstance.updateOptions({ readOnly: newReadOnly })
  }
})

// 监听高度变化
watch(() => props.height, () => {
  setEditorHeight()
})

// ==================== 方法 ====================
/**
 * 设置编辑器高度
 */
const setEditorHeight = () => {
  if (!editorContainer.value) return

  const height = typeof props.height === 'number'
    ? `${props.height}px`
    : props.height

  editorContainer.value.style.height = height
}

/**
 * 获取编辑器实例（用于父组件调用）
 */
defineExpose({
  getEditor: () => editorInstance,
  getValue: () => editorInstance?.getValue(),
  setValue: (value) => editorInstance?.setValue(value),
  format: () => editorInstance?.getAction('editor.action.formatDocument')?.run()
})

// ==================== 清理 ====================
onBeforeUnmount(() => {
  if (editorInstance) {
    editorInstance.dispose()
    editorInstance = null
  }
})
</script>

<style scoped lang="scss">
.monaco-editor-container {
  width: 100%;
  min-height: 200px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
</style>
