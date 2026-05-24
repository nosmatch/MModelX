/**
 * 训练管理模块路由配置
 *
 * @author MModelX Team
 * @since 2026-05-23
 */

export default [
  {
    path: '/training',
    name: 'Training',
    redirect: '/training/overview',
    meta: {
      title: '训练管理',
      icon: 'Cpu',
      order: 3
    }
  },
  {
    path: '/training/overview',
    name: 'TrainingOverview',
    component: () => import('@/views/Training/index.vue'),
    meta: {
      title: '训练概览',
      icon: 'List',
      parent: 'Training',
      keepAlive: true
    }
  },
  {
    path: '/training/experiments',
    name: 'ExperimentList',
    component: () => import('@/views/Training/ExperimentList.vue'),
    meta: {
      title: '实验列表',
      icon: 'Collection',
      parent: 'Training'
    }
  },
  {
    path: '/training/models',
    name: 'ModelList',
    component: () => import('@/views/Training/ModelList.vue'),
    meta: {
      title: '模型列表',
      icon: 'Box',
      parent: 'Training'
    }
  },
  {
    path: '/training/tuning',
    name: 'TuningPage',
    component: () => import('@/views/Training/TuningPage.vue'),
    meta: {
      title: '超参数调优',
      icon: 'Setting',
      parent: 'Training'
    }
  }
]
