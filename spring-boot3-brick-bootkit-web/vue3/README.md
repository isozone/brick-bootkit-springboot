# Brick Bootkit Web - Vue3重构版本

基于 Vue3 + Vite + NaiveUI 的现代化前端项目。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite 5
- **UI 组件库**: NaiveUI
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **图表**: ECharts
- **图标**: @vicons/ionicons5
- **日期处理**: Day.js
- **CSS 预处理器**: SCSS

## 项目结构

```
vue3/
├── src/
│   ├── api/                 # API 接口封装
│   │   ├── index.js         # Axios 配置
│   │   └── services.js      # 业务 API
│   ├── components/          # 公共组件
│   │   └── layout/          # 布局组件
│   │       ├── AppLayout.vue
│   │       ├── Sidebar.vue
│   │       └── Header.vue
│   ├── constants/           # 常量定义
│   │   ├── index.js         # 主题、菜单、API 路径
│   │   └── constants.js     # 状态码、提示信息、校验规则
│   ├── router/              # 路由配置
│   │   └── index.js
│   ├── styles/              # 样式文件
│   │   ├── main.scss        # 全局样式
│   │   ├── layout.scss      # 布局样式
│   │   └── variables.scss   # SCSS 变量
│   ├── views/               # 页面组件
│   │   ├── dashboard/       # 首页
│   │   ├── scripts/         # 脚本管理
│   │   ├── plugins/         # 插件管理
│   │   └── monitor/         # 系统监控
│   ├── App.vue              # 根组件
│   └── main.js              # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 功能模块

### 1. 首页 (Dashboard)
- 系统资源概览
- 统计卡片展示
- 资源使用趋势图表
- 最近执行记录

### 2. 脚本管理 (Scripts)
- 脚本列表管理
- 脚本编辑器
- 脚本模板库
- 定时任务配置
- 执行记录查看

### 3. 插件管理 (Plugins)
- 插件列表展示
- 插件上传
- 插件启停控制
- 插件卸载

### 4. 系统监控 (Monitor)
- 系统概览
- CPU 监控
- 内存监控
- 线程监控

## 安装与运行

```bash
# 安装依赖
npm install

# 开发模式运行
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

## 常量管理

所有常量统一在 `src/constants/` 目录下管理：

- `index.js`: 主题配置、菜单配置、API 路径、状态常量
- `constants.js`: 状态码、提示信息、正则表达式、文件类型

## API 配置

API 接口在 `src/api/services.js` 中封装，支持：
- 脚本 CRUD 操作
- 插件管理
- 监控数据获取

## 部署

```bash
# 构建
npm run build

# 构建产物在 dist/ 目录
```

可部署到任何静态文件服务器或 Nginx 后。
