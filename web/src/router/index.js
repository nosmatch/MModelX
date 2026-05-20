import { createRouter, createWebHistory } from 'vue-router'
import featuresRoutes from './features'

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
      // 特征工程模块（新路由）
      ...featuresRoutes,
      // 其他模块（原有路由）
      {
        path: '/samples',
        name: 'Samples',
        component: () => import('@/views/Samples/index.vue'),
        meta: { title: '样本工程' }
      },
      {
        path: '/training',
        name: 'Training',
        component: () => import('@/views/Training/index.vue'),
        meta: { title: '训练管理' }
      },
      {
        path: '/serving',
        name: 'Serving',
        component: () => import('@/views/Serving/index.vue'),
        meta: { title: '模型部署' }
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