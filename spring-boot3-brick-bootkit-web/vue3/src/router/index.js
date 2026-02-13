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