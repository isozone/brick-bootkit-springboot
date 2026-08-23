import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/plugins-web/'
  },
  {
    path: '/plugins-web/',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/plugins-web/scripts',
    name: 'Scripts',
    component: () => import('@/views/scripts/index.vue'),
    meta: { title: '脚本列表' }
  },
  {
    path: '/plugins-web/scripts/editor/:id?',
    name: 'ScriptEditor',
    component: () => import('@/views/scripts/editor.vue'),
    meta: { title: '脚本编辑器' }
  },
  {
    path: '/plugins-web/scripts/templates',
    name: 'ScriptTemplates',
    component: () => import('@/views/scripts/templates.vue'),
    meta: { title: '脚本模板' }
  },
  {
    path: '/plugins-web/scripts/scheduler',
    name: 'ScriptScheduler',
    component: () => import('@/views/scripts/scheduler.vue'),
    meta: { title: '定时任务' }
  },
  {
    path: '/plugins-web/scripts/executions',
    name: 'ScriptExecutions',
    component: () => import('@/views/scripts/executions.vue'),
    meta: { title: '执行记录' }
  },
  {
    path: '/plugins-web/plugins',
    name: 'Plugins',
    component: () => import('@/views/plugins/index.vue'),
    meta: { title: '插件列表' }
  },
  {
    path: '/plugins-web/security',
    name: 'Security',
    component: () => import('@/views/security/index.vue'),
    meta: { title: '安全中心' }
  },
  {
    path: '/plugins-web/registry',
    name: 'Registry',
    component: () => import('@/views/registry/index.vue'),
    meta: { title: '服务注册中心' }
  },
  {
    path: '/plugins-web/config',
    name: 'Config',
    component: () => import('@/views/config/index.vue'),
    meta: { title: '插件配置' }
  },
  {
    path: '/plugins-web/performance',
    name: 'Performance',
    component: () => import('@/views/performance/index.vue'),
    meta: { title: '性能分析' }
  },
  {
    path: '/plugins-web/cluster',
    name: 'Cluster',
    component: () => import('@/views/cluster/index.vue'),
    meta: { title: '集群管理' }
  },
  {
    path: '/plugins-web/dependency',
    name: 'Dependency',
    component: () => import('@/views/dependency/index.vue'),
    meta: { title: '依赖分析' }
  },
  {
    path: '/plugins-web/rollout',
    name: 'Rollout',
    component: () => import('@/views/rollout/index.vue'),
    meta: { title: '灰度发布' }
  },
  {
    path: '/plugins-web/release',
    name: 'Release',
    component: () => import('@/views/release/index.vue'),
    meta: { title: '发布治理' }
  },
  {
    path: '/plugins-web/eventbus',
    name: 'EventBus',
    component: () => import('@/views/eventbus/index.vue'),
    meta: { title: '事件总线' }
  },
  {
    path: '/plugins-web/logs',
    name: 'Logs',
    component: () => import('@/views/logs/index.vue'),
    meta: { title: '插件日志' }
  },
  {
    path: '/plugins-web/marketplace',
    name: 'Marketplace',
    component: () => import('@/views/marketplace/index.vue'),
    meta: { title: '插件市场' }
  },
  {
    path: '/plugins-web/plugins/upload',
    name: 'PluginUpload',
    component: () => import('@/views/plugins/upload.vue'),
    meta: { title: '上传插件' }
  },
  {
    path: '/plugins-web/monitor',
    name: 'MonitorOverview',
    component: () => import('@/views/monitor/overview.vue'),
    meta: { title: '系统概览' }
  },
  {
    path: '/plugins-web/monitor/cpu',
    name: 'MonitorCPU',
    component: () => import('@/views/monitor/cpu.vue'),
    meta: { title: 'CPU监控' }
  },
  {
    path: '/plugins-web/monitor/memory',
    name: 'MonitorMemory',
    component: () => import('@/views/monitor/memory.vue'),
    meta: { title: '内存监控' }
  },
  {
    path: '/plugins-web/monitor/threads',
    name: 'MonitorThreads',
    component: () => import('@/views/monitor/threads.vue'),
    meta: { title: '线程监控' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || 'Brick Bootkit'} - Brick Bootkit Web`
  next()
})

export default router