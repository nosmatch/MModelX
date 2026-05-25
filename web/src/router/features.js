/**
 * 特征工程模块路由配置
 *
 * @author MModelX Team
 * @since 2026-05-20
 */

export default [
  {
    path: '/features',
    name: 'Features',
    redirect: '/features/overview',
    meta: {
      title: '特征工程',
      icon: 'TrendCharts',
      order: 1
    }
  },
  {
    path: '/features/overview',
    name: 'FeatureOverview',
    component: () => import('@/views/Features/index.vue'),
    meta: {
      title: '特征概览',
      icon: 'HomeFilled',
      parent: 'Features',
      keepAlive: true
    }
  },
  {
    path: '/features/views',
    name: 'FeatureViewList',
    component: () => import('@/views/Features/FeatureViewList.vue'),
    meta: {
      title: '特征视图',
      icon: 'Document',
      parent: 'Features',
      keepAlive: true
    }
  },
  {
    path: '/features/compute',
    name: 'FeatureCompute',
    component: () => import('@/views/Features/FeatureCompute.vue'),
    meta: {
      title: '特征计算',
      icon: 'Operation',
      parent: 'Features'
    }
  },
  {
    path: '/features/materialize',
    name: 'FeatureMaterialize',
    component: () => import('@/views/Features/FeatureMaterialize.vue'),
    meta: {
      title: '特征物化',
      icon: 'Files',
      parent: 'Features'
    }
  },
  {
    path: '/features/online',
    name: 'OnlineFeatureQuery',
    component: () => import('@/views/Features/OnlineFeatureQuery.vue'),
    meta: {
      title: '在线查询',
      icon: 'Search',
      parent: 'Features'
    }
  },
  {
    path: '/features/visualization',
    name: 'FeatureVisualization',
    component: () => import('@/views/Features/FeatureVisualization.vue'),
    meta: {
      title: '特征可视化',
      icon: 'DataAnalysis',
      parent: 'Features'
    }
  },
  {
    path: '/features/views/:name',
    name: 'FeatureViewDetail',
    component: () => import('@/views/Features/FeatureViewDetail.vue'),
    meta: {
      title: '视图详情',
      icon: 'Document',
      parent: 'Features',
      hidden: true // 不在导航中显示
    }
  },
  {
    path: '/features/definitions',
    name: 'FeatureDefinitionConfig',
    component: () => import('@/views/Features/FeatureDefinitionConfig.vue'),
    meta: {
      title: '特征定义',
      icon: 'Document',
      parent: 'Features'
    }
  }
]
