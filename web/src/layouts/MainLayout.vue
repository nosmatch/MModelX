<template>
  <div class="main-layout">
    <!-- 左侧边栏 -->
    <aside class="sidebar">
      <div class="logo-section">
        <h1 class="logo">MModelX</h1>
        <p class="tagline">机器学习平台</p>
      </div>

      <nav class="sidebar-nav">
        <!-- 概览 -->
        <div
          class="nav-item"
          :class="{ active: $route.path === '/overview' }"
          @click="navigate('/overview')"
        >
          <span class="nav-icon">📊</span>
          <span class="nav-text">概览大盘</span>
        </div>

        <!-- 特征工程（可展开） -->
        <div class="nav-group">
          <div
            class="nav-item group-header"
            :class="{ active: isFeaturesActive, expanded: expandedMenus.features }"
            @click="toggleMenu('features')"
          >
            <span class="nav-icon">🔧</span>
            <span class="nav-text">特征工程</span>
            <span class="expand-icon">{{ expandedMenus.features ? '▼' : '▶' }}</span>
          </div>
          <div v-show="expandedMenus.features" class="nav-submenu">
            <div
              class="nav-item sub-item"
              :class="{ active: $route.path === '/features/views' }"
              @click="navigate('/features/views')"
            >
              <span class="nav-text">特征视图</span>
            </div>
            <div
              class="nav-item sub-item"
              :class="{ active: $route.path === '/features/compute' }"
              @click="navigate('/features/compute')"
            >
              <span class="nav-text">特征计算</span>
            </div>
            <div
              class="nav-item sub-item"
              :class="{ active: $route.path === '/features/materialize' }"
              @click="navigate('/features/materialize')"
            >
              <span class="nav-text">特征物化</span>
            </div>
            <div
              class="nav-item sub-item"
              :class="{ active: $route.path === '/features/online' }"
              @click="navigate('/features/online')"
            >
              <span class="nav-text">在线查询</span>
            </div>
            <div
              class="nav-item sub-item"
              :class="{ active: $route.path === '/features/visualization' }"
              @click="navigate('/features/visualization')"
            >
              <span class="nav-text">特征可视化</span>
            </div>
          </div>
        </div>

        <!-- 样本工程 -->
        <div
          class="nav-item"
          :class="{ active: $route.path === '/samples' }"
          @click="navigate('/samples')"
        >
          <span class="nav-icon">📦</span>
          <span class="nav-text">样本工程</span>
        </div>

        <!-- 训练管理 -->
        <div
          class="nav-item"
          :class="{ active: $route.path === '/training' }"
          @click="navigate('/training')"
        >
          <span class="nav-icon">🎯</span>
          <span class="nav-text">训练管理</span>
        </div>

        <!-- 模型部署 -->
        <div
          class="nav-item"
          :class="{ active: $route.path === '/serving' }"
          @click="navigate('/serving')"
        >
          <span class="nav-icon">🚀</span>
          <span class="nav-text">模型部署</span>
        </div>

        <!-- 数据源管理 -->
        <div
          class="nav-item"
          :class="{ active: $route.path === '/datasources' }"
          @click="navigate('/datasources')"
        >
          <span class="nav-icon">🗄️</span>
          <span class="nav-text">数据源管理</span>
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
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 展开的菜单
const expandedMenus = ref({
  features: true // 默认展开特征工程子菜单
})

// 检查特征工程菜单是否激活
const isFeaturesActive = computed(() => {
  return route.path.startsWith('/features')
})

// 页面标题
const pageTitle = computed(() => {
  const titles = {
    '/overview': '概览监控大盘',
    '/features/views': '特征视图',
    '/features/compute': '特征计算',
    '/features/materialize': '特征物化',
    '/features/online': '在线特征查询',
    '/features/visualization': '特征可视化',
    '/features': '特征工程',
    '/samples': '样本工程',
    '/training': '训练管理',
    '/serving': '模型部署',
    '/datasources': '数据源管理'
  }
  return titles[route.path] || 'MModelX'
})

// 导航到指定路径
const navigate = (path) => {
  router.push(path)
}

// 切换菜单展开状态
const toggleMenu = (menuName) => {
  expandedMenus.value[menuName] = !expandedMenus.value[menuName]
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

/* 导航组 */
.nav-group {
  margin-bottom: 4px;
}

.group-header {
  position: relative;
  padding-right: 36px;
}

.expand-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 10px;
  transition: transform 0.3s;
}

.group-header.expanded .expand-icon {
  transform: translateY(-50%) rotate(90deg);
}

/* 子菜单 */
.nav-submenu {
  padding-left: 20px;
}

.nav-submenu .sub-item {
  padding: 10px 20px 10px 40px;
  margin: 2px 12px;
  font-size: 14px;
  opacity: 0.8;
}

.nav-submenu .sub-item:hover {
  opacity: 1;
}

.nav-submenu .sub-item.active {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
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