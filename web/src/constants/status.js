/**
 * 全平台状态/枚举映射
 *
 * 对照后端 docs/enum-status-standard.md 维护，前端统一从这里读取标签和颜色，
 * 避免各页面分散硬编码导致翻译/色彩不一致。
 *
 * 命名约定：
 *   - XxxLabels  : { code -> 中文标签 }
 *   - XxxColors  : { code -> Element Plus tag 类型 (success/warning/danger/info/primary/'') }
 *
 * 用法：
 *   <el-tag :type="JobStatusColors[row.status]">{{ JobStatusLabels[row.status] || row.status }}</el-tag>
 *
 * @author MModelX Team
 */

// ============================================
// 训练任务状态（platform-training）
// ============================================
export const JobStatusLabels = {
  PENDING: '等待中',
  RUNNING: '运行中',
  SUCCESS: '成功',
  FAILED: '失败',
  CANCELLED: '已取消'
}

export const JobStatusColors = {
  PENDING: 'info',
  RUNNING: 'primary',
  SUCCESS: 'success',
  FAILED: 'danger',
  CANCELLED: 'warning'
}

// ============================================
// 实验状态（platform-training）
// ============================================
export const ExperimentStatusLabels = {
  RUNNING: '运行中',
  COMPLETED: '已完成',
  FAILED: '失败'
}

export const ExperimentStatusColors = {
  RUNNING: 'primary',
  COMPLETED: 'success',
  FAILED: 'danger'
}

// ============================================
// 模型阶段（platform-serving / platform-training）
// 统一使用短版本：暂存 / 生产 / 归档（去掉冗余的"环境"二字）
// ============================================
export const ModelStageLabels = {
  Staging: '暂存',
  Production: '生产',
  Archived: '归档'
}

export const ModelStageColors = {
  Staging: 'warning',
  Production: 'success',
  Archived: 'info'
}

// ============================================
// 样本配置 / 数据源 通用 ACTIVE 状态
// ============================================
export const ActiveStatusLabels = {
  ACTIVE: '激活',
  DISABLED: '禁用',
  ARCHIVED: '归档',
  ERROR: '错误'
}

export const ActiveStatusColors = {
  ACTIVE: 'success',
  DISABLED: 'warning',
  ARCHIVED: 'info',
  ERROR: 'danger'
}

// ============================================
// 标签类型（platform-sample）
// ============================================
export const LabelTypeLabels = {
  BINARY: '二分类',
  MULTICLASS: '多分类',
  REGRESSION: '回归'
}

export const LabelTypeColors = {
  BINARY: 'danger',
  MULTICLASS: 'warning',
  REGRESSION: 'success'
}

// ============================================
// 数据集划分策略（platform-sample）
// ============================================
export const SplitStrategyLabels = {
  RANDOM: '随机',
  TEMPORAL: '时序',
  STRATIFIED: '分层'
}

// ============================================
// K8s 部署状态（platform-deployment）
// ============================================
export const DeploymentStatusLabels = {
  RUNNING: '运行中',
  DEPLOYING: '部署中',
  STOPPED: '已停止',
  FAILED: '失败'
}

export const DeploymentStatusColors = {
  RUNNING: 'success',
  DEPLOYING: 'warning',
  STOPPED: 'info',
  FAILED: 'danger'
}

// K8s 集群侧的状态（与 deployment 表中保存的状态略不同，遵循 K8s 原文）
export const K8sStatusColors = {
  Running: 'success',
  Scaling: 'warning',
  Deploying: 'primary',
  Stopped: 'info',
  NotFound: 'danger'
}

// ============================================
// K8s Pod 状态
// ============================================
export const PodStatusColors = {
  Running: 'success',
  Pending: 'warning',
  Failed: 'danger',
  Succeeded: 'primary'
}
