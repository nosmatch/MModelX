<template>
  <div class="overview-page">
    <div class="status-cards">
      <div class="card">
        <h3>系统状态</h3>
        <p class="status">运行中 ✅</p>
      </div>
      <div class="card">
        <h3>活跃实验</h3>
        <p class="number">12个</p>
      </div>
      <div class="card">
        <h3>在线模型</h3>
        <p class="number">5个</p>
      </div>
      <div class="card">
        <h3>今日告警</h3>
        <p class="warning">3条</p>
      </div>
    </div>

    <div class="content-sections">
      <div class="section">
        <h3>📈 关键指标趋势</h3>
        <div class="placeholder-chart">
          <p>图表区域 - 开发中...</p>
        </div>
      </div>

      <div class="section">
        <h3>🔥 运行中任务</h3>
        <div class="task-list">
          <div class="task-item">
            <span class="task-name">特征计算 user_features_v2</span>
            <span class="task-progress">78%</span>
          </div>
          <div class="task-item">
            <span class="task-name">训练 ctr_model_exp_2025</span>
            <span class="task-progress">45%</span>
          </div>
        </div>
      </div>
    </div>

    <div class="dev-note">
      <p>⚠️ 页面开发中，更多功能即将上线...</p>
    </div>
  </div>
</template>

<script setup>
// TODO: 实现概览监控大盘逻辑
</script>

<style scoped>
.overview-page {
  max-width: 1400px;
  margin: 0 auto;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.card {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card h3 {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
  font-weight: 500;
}

.card .status {
  font-size: 28px;
  color: #67c23a;
  margin: 0;
  font-weight: 600;
}

.card .number {
  font-size: 36px;
  color: #409eff;
  margin: 0;
  font-weight: bold;
}

.card .warning {
  font-size: 28px;
  color: #e6a23c;
  margin: 0;
  font-weight: 600;
}

.content-sections {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.section {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.section h3 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.placeholder-chart {
  background: #f8f9fa;
  border: 2px dashed #dee2e6;
  border-radius: 4px;
  padding: 40px;
  text-align: center;
  color: #6c757d;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
}

.task-name {
  font-size: 14px;
  color: #333;
}

.task-progress {
  font-size: 14px;
  color: #409eff;
  font-weight: 600;
}

.dev-note {
  background: #fff;
  border: 1px solid #fef0f0;
  border-left: 4px solid #f56c6c;
  border-radius: 4px;
  padding: 16px;
  margin-top: 24px;
}

.dev-note p {
  margin: 0;
  color: #f56c6c;
  font-size: 14px;
}

@media (max-width: 1024px) {
  .content-sections {
    grid-template-columns: 1fr;
  }
}
</style>