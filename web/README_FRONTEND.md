# MModelX 前端项目

## 项目说明

这是 MModelX 机器学习平台的前端项目，基于 Vue 3 + Vite + Element Plus 构建。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite
- **UI组件库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP客户端**: Axios
- **图表库**: ECharts
- **代码编辑器**: Monaco Editor

## 项目结构

```
web/
├── src/
│   ├── api/              # API接口封装
│   │   └── modules/      # 各模块API
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   │   ├── charts/       # ECharts图表组件
│   │   ├── editors/      # 编辑器组件
│   │   ├── forms/        # 表单组件
│   │   └── tables/       # 表格组件
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置
│   ├── store/            # 状态管理
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   │   ├── Overview/     # 概览监控大盘
│   │   ├── Features/     # 特征工程
│   │   ├── Samples/      # 样本工程
│   │   ├── Training/     # 训练管理
│   │   └── Serving/      # 模型部署
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── public/               # 公共资源
├── index.html            # HTML模板
├── vite.config.js        # Vite配置
├── .eslintrc.cjs         # ESLint配置
├── .prettierrc           # Prettier配置
└── package.json          # 项目配置

```

## 开发指南

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## API代理配置

开发环境下，前端会自动代理 `/api` 请求到后端服务器：

```javascript
// vite.config.js
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

## 代码规范

项目使用 ESLint + Prettier 进行代码检查和格式化：

```bash
# 检查代码
npm run lint

# 自动修复
npm run lint:fix
```

## 功能模块

### 1. 概览监控大盘
- 系统状态卡片
- 关键指标趋势图
- 运行任务列表
- 资源使用监控

### 2. 特征工程
- 特征列表（树形表格）
- 特征定义编辑器（Monaco Editor）
- 特征分析（ECharts图表）
- 计算任务状态

### 3. 样本工程
- 数据集列表（版本管理）
- 样本构建配置（Point-in-time join）
- 样本质量分析
- 数据血缘关系

### 4. 训练管理
- 实验列表（对比视图）
- 训练配置表单
- 训练详情（实时监控）
- 实验对比分析

### 5. 模型部署
- 模型注册表
- A/B测试配置
- 推理监控
- 部署历史

## 环境变量

创建 `.env.local` 文件配置环境变量：

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=ws://localhost:8080
```

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 开发注意事项

1. **组件命名**: 使用多单词组件名避免与HTML标签冲突
2. **状态管理**: 优先使用Pinia进行状态管理，避免复杂的事件传递
3. **API调用**: 统一使用 `src/utils/request.js` 封装的axios实例
4. **路由配置**: 新增页面需要在 `src/router/index.js` 中配置路由
5. **样式开发**: 使用scoped样式避免样式污染

## 常见问题

### Q: Element Plus组件样式丢失？
A: 确保在 `main.js` 中正确引入了Element Plus样式文件。

### Q: 路由跳转不生效？
A: 检查路由配置是否正确，确保使用的是 `createWebHistory()`。

### Q: API请求失败？
A: 检查后端服务是否启动，确认代理配置是否正确。

## 联系方式

如有问题，请联系开发团队。