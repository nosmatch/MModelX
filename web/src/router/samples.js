export default [
  {
    path: '/samples',
    name: 'Samples',
    redirect: '/samples/index',
    meta: {
      title: '样本工程',
      icon: 'Collection',
      order: 2
    }
  },
  {
    path: '/samples/index',
    name: 'SampleIndex',
    component: () => import('@/views/Samples/index.vue'),
    meta: {
      title: '样本概览',
      icon: 'HomeFilled',
      parent: 'Samples',
      keepAlive: true
    }
  },
  {
    path: '/samples/build',
    name: 'SampleBuild',
    component: () => import('@/views/Samples/SampleBuild.vue'),
    meta: {
      title: '样本构建',
      icon: 'VideoPlay',
      parent: 'Samples'
    }
  },
  {
    path: '/samples/datasets',
    name: 'DatasetList',
    component: () => import('@/views/Samples/DatasetList.vue'),
    meta: {
      title: '数据集管理',
      icon: 'FolderOpened',
      parent: 'Samples',
      keepAlive: true
    }
  }
]
