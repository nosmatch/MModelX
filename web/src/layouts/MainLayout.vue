<template>
  <div class="main-layout">
    <!-- 左侧边栏 -->
    <aside class="sidebar">
      <div class="logo-section">
        <h1 class="logo">MModelX</h1>
        <p class="tagline">机器学习平台</p>
      </div>

      <nav class="sidebar-nav">
        <div
          class="nav-item"
          :class="{ active: $route.path === '/overview' }"
          @click="navigate('/overview')"
        >
          <span class="nav-icon">📊</span>
          <span class="nav-text">概览大盘</span>
        </div>

        <div
          class="nav-item"
          :class="{ active: $route.path === '/features' }"
          @click="navigate('/features')"
        >
          <span class="nav-icon">🔧</span>
          <span class="nav-text">特征工程</span>
        </div>

        <div
          class="nav-item"
          :class="{ active: $route.path === '/samples' }"
          @click="navigate('/samples')"
        >
          <span class="nav-icon">📦</span>
          <span class="nav-text">样本工程</span>
        </div>

        <div
          class="nav-item"
          :class="{ active: $route.path === '/training' }"
          @click="navigate('/training')"
        >
          <span class="nav-icon">🎯</span>
          <span class="nav-text">训练管理</span>
        </div>

        <div
          class="nav-item"
          :class="{ active: $route.path === '/serving' }"
          @click="navigate('/serving')"
        >
          <span class="nav-icon">🚀</span>
          <span class="nav-text">模型部署</span>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-avatar">
            <span>👤</span>
          </div>
          <div class="user-details">
            <p class="user-name">管理员</p>
            <p class="user-role">系统管理员</p>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧主内容区 -->
    <div class="main-content-wrapper">
      <!-- 顶部Header -->
      <header class="top-header">
        <div class="header-left">
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <div class="header-actions">
            <span class="action-item">🔔</span>
            <span class="action-item">⚙️</span>
          </div>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const pageTitle = computed(() => {
  const titles = {
    '/overview': '概览监控大盘',
    '/features': '特征工程',
    '/samples': '样本工程',
    '/training': '训练管理',
    '/serving': '模型部署'
  }
  return titles[route.path] || 'MModelX'
})

const navigate = (path) => {
  router.push(path)
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

/* 左侧边栏样式 */
.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e6e6e6;
  flex-shrink: 0;
}

.logo-section {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  margin: 0;
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  letter-spacing: -1px;
}

.tagline {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.sidebar-nav {
  flex: 1;
  padding: 20px 0;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  margin: 4px 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s ease;
  color: rgba(255, 255, 255, 0.7);
}

.nav-item:hover {
  background: rgba(64, 158, 255, 0.1);
  color: #fff;
}

.nav-item.active {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.2) 0%, rgba(64, 158, 255, 0.1) 100%);
  color: #409eff;
  font-weight: 500;
}

.nav-icon {
  font-size: 20px;
  margin-right: 12px;
  width: 24px;
  text-align: center;
}

.nav-text {
  font-size: 15px;
  white-space: nowrap;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  background: rgba(64, 158, 255, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.user-details {
  flex: 1;
}

.user-name {
  margin: 0 0 2px 0;
  font-size: 14px;
  color: #fff;
  font-weight: 500;
}

.user-role {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

/* 右侧内容区样式 */
.main-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 16px;
}

.action-item {
  font-size: 20px;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background 0.3s;
}

.action-item:hover {
  background: #f5f5f5;
}

.main-content {
  flex: 1;
  background: #f5f5f5;
  padding: 24px;
  overflow-y: auto;
}

/* 滚动条样式 */
.sidebar-nav::-webkit-scrollbar,
.main-content::-webkit-scrollbar {
  width: 6px;
}

.sidebar-nav::-webkit-scrollbar-track,
.main-content::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.main-content::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.main-content::-webkit-scrollbar-thumb:hover {
  background: #999;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }

  .nav-text {
    font-size: 14px;
  }

  .main-content {
    padding: 16px;
  }
}
</style>