<template>
  <el-card class="stat-card" shadow="hover">
    <div class="stat-content">
      <div class="stat-icon" :style="iconStyle">
        <el-icon :size="24"><component :is="icon" /></el-icon>
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ value }}</div>
        <div class="stat-label">{{ label }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
/**
 * 通用统计卡片
 *
 * 用法：
 *   <StatCard :icon="Document" tone="primary" :value="12" label="特征视图" />
 *
 * @author MModelX Team
 */
import { computed } from 'vue'

const props = defineProps({
  icon: {
    type: [Object, Function, String],
    required: true
  },
  value: {
    type: [Number, String],
    required: true
  },
  label: {
    type: String,
    required: true
  },
  tone: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'success', 'warning', 'danger', 'info'].includes(v)
  },
  iconBg: {
    type: String,
    default: ''
  },
  iconColor: {
    type: String,
    default: ''
  }
})

// 5 套预设配色：背景 + 图标色
const TONE_PALETTE = {
  primary: { bg: '#ecf5ff', color: '#409eff' },
  success: { bg: '#f0f9ff', color: '#67c23a' },
  warning: { bg: '#fdf6ec', color: '#e6a23c' },
  danger:  { bg: '#fef0f0', color: '#f56c6c' },
  info:    { bg: '#f4f4f5', color: '#909399' }
}

const iconStyle = computed(() => {
  if (props.iconBg || props.iconColor) {
    return {
      background: props.iconBg || undefined,
      color: props.iconColor || undefined
    }
  }
  const palette = TONE_PALETTE[props.tone] || TONE_PALETTE.primary
  return {
    background: palette.bg,
    color: palette.color
  }
})
</script>

<style scoped lang="scss">
.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    margin-right: 16px;
  }

  .stat-info {
    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: $text-primary;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: $text-muted;
      margin-top: 4px;
    }
  }
}
</style>
