export default [
  {
    path: '/deployment',
    name: 'Deployment',
    component: () => import('@/views/Deployment/index.vue'),
    meta: { title: 'K8s 部署管理', parent: 'Serving' }
  }
]
