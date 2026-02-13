// 主题配置常量
export const themeOverrides = {
  common: {
    primaryColor: '#2563eb',
    primaryColorHover: '#1d4ed8',
    primaryColorPressed: '#1e40af',
    primaryColorSuppl: '#3b82f6',
    borderRadius: '6px',
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif"
  },
  Button: {
    borderRadiusMedium: '6px'
  },
  Card: {
    borderRadius: '8px'
  },
  DataTable: {
    borderRadius: '8px'
  },
  Menu: {
    borderRadius: '6px'
  }
}

// 菜单配置常量
export const MENU_CONFIG = {
  // 侧边栏菜单
  sidebar: [
    {
      key: 'dashboard',
      label: '首页',
      icon: 'HomeOutline',
      path: '/plugins-web/'
    },
    {
      key: 'scripts',
      label: '脚本管理',
      icon: 'CodeSlashOutline',
      children: [
        { key: 'scripts-list', label: '脚本列表', path: '/plugins-web/scripts' },
        { key: 'scripts-editor', label: '脚本编辑器', path: '/plugins-web/scripts/editor' },
        { key: 'scripts-templates', label: '脚本模板', path: '/plugins-web/scripts/templates' },
        { key: 'scripts-scheduler', label: '定时任务', path: '/plugins-web/scripts/scheduler' },
        { key: 'scripts-executions', label: '执行记录', path: '/plugins-web/scripts/executions' }
      ]
    },
    {
      key: 'plugins',
      label: '插件管理',
      icon: 'CubeOutline',
      children: [
        { key: 'plugins-list', label: '插件列表', path: '/plugins-web/plugins' },
        { key: 'plugins-upload', label: '上传插件', path: '/plugins-web/plugins/upload' }
      ]
    },
    {
      key: 'monitor',
      label: '系统监控',
      icon: 'StatsChartOutline',
      children: [
        { key: 'monitor-overview', label: '概览', path: '/plugins-web/monitor' },
        { key: 'monitor-cpu', label: 'CPU监控', path: '/plugins-web/monitor/cpu' },
        { key: 'monitor-memory', label: '内存监控', path: '/plugins-web/monitor/memory' },
        { key: 'monitor-threads', label: '线程监控', path: '/plugins-web/monitor/threads' }
      ]
    }
  ],

  // 用户菜单
  userMenu: [
    { key: 'profile', label: '个人中心' },
    { key: 'settings', label: '系统设置' },
    { type: 'divider' },
    { key: 'logout', label: '退出登录' }
  ]
}

// API 路径常量 - 基于后端 Controller 真实接口
export const API_PATHS = {
  // === 监控相关接口 ===
  MONITOR_OVERVIEW: '/plugins-web/api/monitor/overview',
  MONITOR_MEMORY: '/plugins-web/api/monitor/memory',
  MONITOR_CPU: '/plugins-web/api/monitor/cpu',
  MONITOR_THREADS: '/plugins-web/api/monitor/threads',
  MONITOR_THREADS_DETAIL: '/plugins-web/api/monitor/threads/detail',
  MONITOR_THREAD_POOLS: '/plugins-web/api/monitor/thread-pools',
  MONITOR_SYSTEM: '/plugins-web/api/monitor/system',
  MONITOR_HISTORY: '/plugins-web/api/monitor/history',

  // === 脚本相关接口 ===
  SCRIPTS_LIST: '/plugins-web/api/v1/scripts',
  SCRIPTS_CREATE: '/plugins-web/api/v1/scripts',
  SCRIPTS_UPDATE: '/plugins-web/api/v1/scripts',
  SCRIPTS_DELETE: '/plugins-web/api/v1/scripts',
  SCRIPTS_CONTENT: '/plugins-web/api/v1/scripts/{scriptName}/content',
  SCRIPTS_VERSIONS: '/plugins-web/api/v1/scripts/{scriptName}/versions',
  SCRIPTS_RESTORE_VERSION: '/plugins-web/api/v1/scripts/{scriptName}/versions/{version}/restore',
  SCRIPTS_TEMPLATES: '/plugins-web/api/v1/scripts/templates',
  SCRIPTS_SCHEDULER: '/plugins-web/api/v1/scripts/scheduler',
  SCRIPTS_EXECUTIONS: '/plugins-web/api/v1/scripts/executions',
  SCRIPTS_EXECUTIONS_LIST: '/plugins-web/api/v1/scripts/executions/list',

  // === 插件相关接口 ===
  PLUGINS_LIST: '/plugins-web/api/plugins',
  PLUGINS_ALL: '/plugins-web/api/plugins/all',
  PLUGINS_DETAIL: '/plugins-web/api/plugins/{pluginId}',
  PLUGINS_UPLOAD: '/plugins-web/api/plugins/upload',
  PLUGINS_INSTALL: '/plugins-web/api/plugins/install',
  PLUGINS_START: '/plugins-web/api/plugins/{pluginId}/start',
  PLUGINS_STOP: '/plugins-web/api/plugins/{pluginId}/stop',
  PLUGINS_RESTART: '/plugins-web/api/plugins/{pluginId}/restart',
  PLUGINS_UNINSTALL: '/plugins-web/api/plugins/{pluginId}',
  PLUGINS_UPLOAD_HISTORY: '/plugins-web/api/plugins/upload-history'
}

// 状态常量
export const STATUS = {
  // 脚本状态
  SCRIPT: {
    DRAFT: 'draft',
    ACTIVE: 'active',
    INACTIVE: 'inactive'
  },
  // 插件状态
  PLUGIN: {
    INSTALLED: 'installed',
    RUNNING: 'running',
    STOPPED: 'stopped',
    ERROR: 'error'
  },
  // 执行状态
  EXECUTION: {
    PENDING: 'pending',
    RUNNING: 'running',
    SUCCESS: 'success',
    FAILED: 'failed',
    CANCELLED: 'cancelled'
  },
  // 任务状态
  TASK: {
    SCHEDULED: 'scheduled',
    RUNNING: 'running',
    COMPLETED: 'completed',
    FAILED: 'failed'
  }
}

// 脚本类型常量
export const SCRIPT_TYPES = [
  { value: 'SHELL', label: 'Shell', color: '#2563eb' },
  { value: 'PYTHON', label: 'Python', color: '#f59e0b' },
  { value: 'JAVASCRIPT', label: 'JavaScript', color: '#10b981' },
  { value: 'SQL', label: 'SQL', color: '#8b5cf6' },
  { value: 'BATCH', label: 'Batch', color: '#6b7280' },
  { value: 'POWERSHELL', label: 'PowerShell', color: '#0284c7' }
]

// 分页默认值
export const PAGINATION = {
  defaultPageSize: 10,
  pageSizes: [10, 20, 50, 100]
}

// 日期时间格式
export const DATE_FORMAT = {
  DATETIME: 'YYYY-MM-DD HH:mm:ss',
  DATE: 'YYYY-MM-DD',
  TIME: 'HH:mm:ss'
}

// 提示信息常量
export const MESSAGE = {
  SUCCESS: {
    CREATE: '创建成功',
    UPDATE: '更新成功',
    DELETE: '删除成功',
    SAVE: '保存成功',
    UPLOAD: '上传成功',
    EXECUTE: '执行成功',
    START: '启动成功',
    STOP: '停止成功',
    INSTALL: '安装成功',
    UNINSTALL: '卸载成功'
  },
  ERROR: {
    CREATE: '创建失败',
    UPDATE: '更新失败',
    DELETE: '删除失败',
    SAVE: '保存失败',
    UPLOAD: '上传失败',
    EXECUTE: '执行失败',
    START: '启动失败',
    STOP: '停止失败',
    INSTALL: '安装失败',
    UNINSTALL: '卸载失败',
    NETWORK: '网络错误，请稍后重试',
    UNKNOWN: '未知错误'
  }
}