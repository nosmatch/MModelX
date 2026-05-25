import { createRouter, createWebHistory } from 'vue-router'
import featuresRoutes from './features'
import samplesRoutes from './samples'
import trainingRoutes from './training'
import deploymentRoutes from './deployment'

const routes = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/overview',
    children: [
      {
        path: '/overview',
        name: 'Overview',
        component: () => import('@/views/Overview/index.vue'),
        meta: { title: '概览大盘' }
      },
      // 特征工程模块
      ...featuresRoutes,
      // 样本工程模块
      ...samplesRoutes,
      // 训练管理模块
      ...trainingRoutes,
      // K8s 部署管理模块
      ...deploymentRoutes,
      // 其他模块（原有路由）
      {
        path: '/serving',
        name: 'Serving',
        component: () => import('@/views/Serving/index.vue'),
        meta: { title: '模型部署' }
      },
      {
        path: '/datasources',
        name: 'DataSources',
        component: () => import('@/views/DataSources/DataSourceList.vue'),
        meta: { title: '数据源管理' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
