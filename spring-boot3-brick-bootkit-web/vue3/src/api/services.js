import axios from 'axios'
import { API_PATHS } from '@/constants'

// 创建 axios 实例
const service = axios.create({
  baseURL: '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 添加时间戳防止缓存
    if (config.method === 'get') {
      config.params = { ...config.params, _t: Date.now() }
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    // 如果是 blob 响应，直接返回（用于文件导出）
    if (response.config.responseType === 'blob') {
      return response
    }
    
    const res = response.data
    // 如果是直接返回数据的接口（如 ScriptRepositoryController）
    if (Array.isArray(res) || res.scriptName !== undefined || res.templateId !== undefined || res.success !== undefined) {
      return { code: 200, data: res }
    }
    // 处理删除等操作成功返回 true 或 success 的情况
    if (res === true || res === 'true' || res.success === true || res.data === true) {
      return { code: 200, data: res }
    }
    return res
  },
  (error) => {
    console.error('响应错误:', error)
    return Promise.reject(error)
  }
)

// ==================== 监控相关 API ====================

export const monitorApi = {
  // 获取监控概览
  getOverview: () => service.get(API_PATHS.MONITOR_OVERVIEW),
  
  // 获取内存信息
  getMemory: () => service.get(API_PATHS.MONITOR_MEMORY),
  
  // 获取 CPU 信息
  getCpu: () => service.get(API_PATHS.MONITOR_CPU),
  
  // 获取线程信息
  getThreads: () => service.get(API_PATHS.MONITOR_THREADS),
  
  // 获取线程详细信息
  getThreadDetail: () => service.get(API_PATHS.MONITOR_THREADS_DETAIL),
  
  // 获取线程池信息
  getThreadPools: () => service.get(API_PATHS.MONITOR_THREAD_POOLS),
  
  // 获取系统信息
  getSystem: () => service.get(API_PATHS.MONITOR_SYSTEM),
  
  // 获取历史监控数据
  getHistory: (type = 'memory', since = 3600) => 
    service.get(API_PATHS.MONITOR_HISTORY, { params: { type, since } })
}

export const doctorApi = {
  getReport: () => service.get(API_PATHS.DOCTOR_REPORT),
  exportText: () => service.get(API_PATHS.DOCTOR_EXPORT_TEXT, { responseType: 'blob' }),
  exportJson: () => service.get(API_PATHS.DOCTOR_EXPORT_JSON, { responseType: 'blob' })
}

// ==================== 脚本相关 API ====================

export const scriptsApi = {
  // 获取所有脚本列表
  getAll: () => service.get(API_PATHS.SCRIPTS_LIST),
  
  // 根据名称获取脚本
  getByName: (scriptName) => service.get(`${API_PATHS.SCRIPTS_LIST}/${scriptName}`),
  
  // 创建脚本
  create: (data) => service.post(API_PATHS.SCRIPTS_LIST, data),
  
  // 更新脚本
  update: (scriptName, data) => service.put(`${API_PATHS.SCRIPTS_LIST}/${scriptName}`, data),
  
  // 删除脚本
  delete: (scriptName) => service.post(`${API_PATHS.SCRIPTS_LIST}/${scriptName}/delete`),
  
  // 获取脚本内容
  getContent: (scriptName) => service.get(`${API_PATHS.SCRIPTS_LIST}/${scriptName}/content`),
  
    // 更新脚本内容
    updateContent: (scriptName, content) => 
      service.put(`${API_PATHS.SCRIPTS_LIST}/${scriptName}/content`, { content }),
  
    // 执行脚本（测试用）
    execute: (data) => service.post(`${API_PATHS.SCRIPTS_LIST}/execute`, data),
  
    // 获取脚本版本列表  getVersions: (scriptName) => service.get(`${API_PATHS.SCRIPTS_LIST}/${scriptName}/versions`),
  
  // 保存脚本版本
  saveVersion: (scriptName, version) => 
    service.post(`${API_PATHS.SCRIPTS_LIST}/${scriptName}/versions`, version),
  
  // 恢复到指定版本
  restoreVersion: (scriptName, version) => 
    service.post(`${API_PATHS.SCRIPTS_LIST}/${scriptName}/versions/${version}/restore`)
}

// ==================== 模板相关 API ====================

export const templatesApi = {
  getAll: () => service.get(API_PATHS.SCRIPTS_TEMPLATES),
  getById: (id) => service.get(`${API_PATHS.SCRIPTS_TEMPLATES}/${id}`),
  create: (data) => service.post(API_PATHS.SCRIPTS_TEMPLATES, data),
  update: (id, data) => service.put(`${API_PATHS.SCRIPTS_TEMPLATES}/${id}`, data),
  delete: (id) => service.delete(`${API_PATHS.SCRIPTS_TEMPLATES}/${id}`)
}

// ==================== 调度任务相关 API ====================

export const schedulerApi = {
  getAll: () => service.get(API_PATHS.SCRIPTS_SCHEDULER),
  getById: (id) => service.get(`${API_PATHS.SCRIPTS_SCHEDULER}/${id}`),
  create: (data) => service.post(API_PATHS.SCRIPTS_SCHEDULER, data),
  update: (id, data) => service.put(`${API_PATHS.SCRIPTS_SCHEDULER}/${id}`, data),
  delete: (id) => service.delete(`${API_PATHS.SCRIPTS_SCHEDULER}/${id}`),
  enable: (id) => service.post(`${API_PATHS.SCRIPTS_SCHEDULER}/${id}/enable`),
  disable: (id) => service.post(`${API_PATHS.SCRIPTS_SCHEDULER}/${id}/disable`),
  pause: (id) => service.post(`${API_PATHS.SCRIPTS_SCHEDULER}/${id}/pause`),
  resume: (id) => service.post(`${API_PATHS.SCRIPTS_SCHEDULER}/${id}/resume`),
  execute: (id) => service.post(`${API_PATHS.SCRIPTS_SCHEDULER}/${id}/execute`)
}

// ==================== 执行记录相关 API ====================

export const executionsApi = {
  // 分页获取执行记录列表
  getList: (page = 1, size = 50, scriptName = null, status = null) => {
    const params = { page, size }
    if (scriptName) params.scriptName = scriptName
    if (status) params.status = status
    return service.get(API_PATHS.SCRIPTS_EXECUTIONS_LIST, { params })
  },
  getAll: () => service.get(API_PATHS.SCRIPTS_EXECUTIONS),
  getById: (id) => service.get(`${API_PATHS.SCRIPTS_EXECUTIONS}/${id}`),
  getByScript: (scriptName) => service.get(`${API_PATHS.SCRIPTS_EXECUTIONS}`, { 
    params: { scriptName } 
  }),
  cancel: (id) => service.post(`${API_PATHS.SCRIPTS_EXECUTIONS}/${id}/cancel`),
  retry: (id) => service.post(`${API_PATHS.SCRIPTS_EXECUTIONS}/${id}/retry`),
  // 导出执行记录
  export: (scriptName = null, status = null) => {
    const params = {}
    if (scriptName) params.scriptName = scriptName
    if (status) params.status = status
    return service.get(`${API_PATHS.SCRIPTS_EXECUTIONS}/export`, { 
      params,
      responseType: 'blob'
    })
  }
}

// ==================== 插件相关 API ====================

export const pluginsApi = {
  // 获取插件列表（分页）
  getList: (page = 1, size = 10, state = 'all', keyword = '') => 
    service.get(API_PATHS.PLUGINS_LIST, { 
      params: { page, size, state, keyword } 
    }),
  
  // 获取所有插件（不分页）
  getAll: () => service.get(API_PATHS.PLUGINS_ALL),
  
  // 获取插件详情
  getDetail: (pluginId) => service.get(API_PATHS.PLUGINS_DETAIL.replace('{pluginId}', pluginId)),

  // 上传到临时目录
  uploadTemp: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return service.post(API_PATHS.PLUGINS_UPLOAD_TEMP, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  // 上传插件
  upload: (file, autoStart = false) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('autoStart', autoStart)
    return service.post(API_PATHS.PLUGINS_UPLOAD, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  // 安装插件
  install: (pluginPath, autoStart = false) => 
    service.post(API_PATHS.PLUGINS_INSTALL, { pluginPath, autoStart }),

  // 从临时目录安装插件
  installFromTemp: (tempFilePath, autoStart = true) =>
    service.post(API_PATHS.PLUGINS_INSTALL_TEMP, { tempFilePath, autoStart }),

  // 验证插件
  verify: (pluginPath) =>
    service.post(API_PATHS.PLUGINS_VERIFY, { pluginPath }),
  
  // 启动插件
  start: (pluginId) => service.post(API_PATHS.PLUGINS_START.replace('{pluginId}', pluginId)),
  
  // 停止插件
  stop: (pluginId) => service.post(API_PATHS.PLUGINS_STOP.replace('{pluginId}', pluginId)),
  
  // 重启插件
  restart: (pluginId) => service.post(API_PATHS.PLUGINS_RESTART.replace('{pluginId}', pluginId)),
  
  // 卸载插件
  uninstall: (pluginId) => service.delete(API_PATHS.PLUGINS_UNINSTALL.replace('{pluginId}', pluginId)),
  
  // 获取上传历史
  getUploadHistory: (page = 1, size = 10, pluginId, status) => {
    const params = { page, size }
    if (pluginId) params.pluginId = pluginId
    if (status) params.status = status
    return service.get(API_PATHS.PLUGINS_UPLOAD_HISTORY, { params })
  },
  
  // 获取所有上传历史
  getAllUploadHistory: () => service.get(API_PATHS.PLUGINS_UPLOAD_HISTORY + '/all'),
  
  // 获取上传历史详情
  getUploadHistoryById: (uploadId) => service.get(API_PATHS.PLUGINS_UPLOAD + '/history/' + uploadId),
  
  // 删除上传历史
  deleteUploadHistory: (uploadId) => service.delete(API_PATHS.PLUGINS_UPLOAD + '/history/' + uploadId),
  
  // 批量删除上传历史
  deleteUploadHistoryBefore: (beforeDate) => service.delete(API_PATHS.PLUGINS_UPLOAD + '/history', { params: { beforeDate } }),
  
  // 清空所有上传历史
  clearAllUploadHistory: () => service.delete(API_PATHS.PLUGINS_UPLOAD + '/history/all')
}

// ==================== 安全中心相关 API ====================

export const securityApi = {
  // 按插件 ID 扫描
  scanByPluginId: (pluginId) => service.get(`${API_PATHS.SECURITY_SCAN_BY_ID}/${pluginId}`),

  // 按文件路径扫描
  scanByPath: (path) => service.get(API_PATHS.SECURITY_SCAN_BY_PATH, { params: { path } }),

  // 获取插件安全策略
  getPolicy: (pluginId) => service.get(`${API_PATHS.SECURITY_POLICY}/${pluginId}`),

  // 设置插件安全策略
  setPolicy: (data) => service.post(API_PATHS.SECURITY_POLICY, data),

  // 授予插件权限
  grantPermission: (data) => service.post(`${API_PATHS.SECURITY_PERMISSIONS}/grant`, data),

  // 撤销插件权限
  revokePermission: (data) => service.post(`${API_PATHS.SECURITY_PERMISSIONS}/revoke`, data),

  // 获取插件已授予的权限
  getPermissions: (pluginId) => service.get(`${API_PATHS.SECURITY_PERMISSIONS}/${pluginId}`)
}

// ==================== 服务注册中心相关 API ====================

export const registryApi = {
  // 获取注册中心统计信息
  getStatistics: () => service.get(API_PATHS.REGISTRY_STATISTICS),

  // 获取所有注册服务（按插件分组）
  getServices: () => service.get(API_PATHS.REGISTRY_SERVICES),

  // 获取指定插件的服务列表
  getServicesByPlugin: (pluginId) => service.get(`${API_PATHS.REGISTRY_SERVICES_BY_PLUGIN}/${pluginId}`),

  // 获取所有注册的插件 ID
  getPlugins: () => service.get(API_PATHS.REGISTRY_PLUGINS)
}

// ==================== 插件配置相关 API ====================

export const configApi = {
  // 获取配置统计
  getStatistics: () => service.get(API_PATHS.CONFIG_STATISTICS),

  // 获取所有插件配置
  getAll: () => service.get(API_PATHS.CONFIG_LIST),

  // 获取指定插件配置
  getByPluginId: (pluginId) => service.get(`${API_PATHS.CONFIG_DETAIL}/${pluginId}`),

  // 热更新插件配置
  update: (pluginId, configuration, versionDescription) =>
    service.put(`${API_PATHS.CONFIG_DETAIL}/${pluginId}`, { configuration, versionDescription }),

  // 获取配置版本历史
  getVersions: (pluginId) => service.get(`${API_PATHS.CONFIG_VERSIONS}/${pluginId}/versions`),

  // 回滚配置
  rollback: (pluginId, versionId) =>
    service.post(`${API_PATHS.CONFIG_ROLLBACK}/${pluginId}/rollback`, { versionId }),

  // 删除插件配置
  remove: (pluginId) => service.delete(`${API_PATHS.CONFIG_DETAIL}/${pluginId}`)
}

// ==================== 性能分析相关 API ====================

export const performanceApi = {
  // 分析插件性能
  analyze: (pluginId) => service.get(`${API_PATHS.PERFORMANCE_ANALYZE}/${pluginId}`),

  // 获取插件资源使用情况
  getUsage: (pluginId) => service.get(`${API_PATHS.PERFORMANCE_USAGE}/${pluginId}`),

  // 获取所有插件资源使用情况
  getAllUsage: () => service.get(API_PATHS.PERFORMANCE_USAGE),

  // 获取资源监控摘要
  getSummary: () => service.get(API_PATHS.PERFORMANCE_SUMMARY),

  // 获取系统资源信息
  getSystem: () => service.get(API_PATHS.PERFORMANCE_SYSTEM),

  // 获取插件性能历史
  getHistory: (pluginId, limit = 20) =>
    service.get(`${API_PATHS.PERFORMANCE_HISTORY}/${pluginId}`, { params: { limit } }),

  // 获取所有插件性能评分
  getScores: () => service.get(API_PATHS.PERFORMANCE_SCORES),

  // 获取插件配额
  getQuota: (pluginId) => service.get(`${API_PATHS.PERFORMANCE_QUOTA}/${pluginId}`),

  // 设置插件配额
  setQuota: (pluginId, quota) => service.post(`${API_PATHS.PERFORMANCE_QUOTA}/${pluginId}`, quota),

  // 获取默认配额
  getDefaultQuota: () => service.get(`${API_PATHS.PERFORMANCE_QUOTA}/default`),

  // 对比插件性能与基线
  compareBaseline: (pluginId) => service.get(`${API_PATHS.PERFORMANCE_BASELINE_COMPARE}/${pluginId}`),

  // 获取插件性能基线
  getBaseline: (pluginId) => service.get(`${API_PATHS.PERFORMANCE_BASELINE}/${pluginId}`)
}

// ==================== 集群管理相关 API ====================

export const clusterApi = {
  // 获取集群总览
  getOverview: () => service.get(API_PATHS.CLUSTER_OVERVIEW),

  // 获取所有在线节点
  getNodes: () => service.get(API_PATHS.CLUSTER_NODES),

  // 获取当前节点信息
  getCurrentNode: () => service.get(API_PATHS.CLUSTER_NODES_CURRENT),

  // 获取集群插件状态列表
  getPluginStates: () => service.get(API_PATHS.CLUSTER_PLUGIN_STATES),

  // 手动同步本节点插件状态到集群
  syncPluginStates: () => service.post(API_PATHS.CLUSTER_PLUGIN_SYNC)
}

// ==================== 依赖分析相关 API ====================

export const dependencyApi = {
  // 获取插件依赖图
  getGraph: () => service.get(API_PATHS.DEPENDENCY_GRAPH),

  // 获取指定插件依赖详情
  getDetail: (pluginId) => service.get(`${API_PATHS.DEPENDENCY_DETAIL}/${pluginId}`),

  // 获取插件依赖解析结果
  resolve: (pluginId) => service.get(`${API_PATHS.DEPENDENCY_RESOLVE}/${pluginId}/resolve`),

  // 检查插件兼容性
  compatibility: (pluginId) => service.get(`${API_PATHS.DEPENDENCY_COMPATIBILITY}/${pluginId}/compatibility`),

  // 升级影响面分析
  impact: (pluginId) => service.get(`${API_PATHS.DEPENDENCY_IMPACT}/${pluginId}/impact`),

  // 获取版本兼容性矩阵
  getMatrix: () => service.get(API_PATHS.DEPENDENCY_MATRIX)
}

// ==================== 灰度发布相关 API ====================

export const rolloutApi = {
  // 获取灰度发布配置
  getConfig: () => service.get(API_PATHS.ROLLOUT_CONFIG),

  // 获取灰度探针列表
  getProbes: () => service.get(API_PATHS.ROLLOUT_PROBES),

  // 模拟灰度决策
  check: (pluginId) => service.post(`${API_PATHS.ROLLOUT_CHECK}/${pluginId}`)
}

// ==================== 发布治理相关 API ====================

export const releaseApi = {
  // 获取发布记录列表
  list: (limit = 50) => service.get(API_PATHS.RELEASES_LIST, { params: { limit } }),

  // 获取发布记录详情
  get: (releaseId) => service.get(`${API_PATHS.RELEASES_LIST}/${releaseId}`),

  // 删除发布记录
  remove: (releaseId) => service.delete(`${API_PATHS.RELEASES_LIST}/${releaseId}`),

  // 集群发布聚合视图
  cluster: () => service.get(API_PATHS.RELEASES_CLUSTER)
}

// ==================== 金丝雀路由相关 API ====================

export const canaryApi = {
  // 获取多版本服务路由权重分组
  routing: () => service.get(API_PATHS.CANARY_ROUTING),

  // 更新服务实现的分流权重
  updateWeight: (pluginId, interfaceName, weight) =>
    service.post(API_PATHS.CANARY_WEIGHT, { pluginId, interfaceName, weight })
}

// ==================== 事件总线相关 API ====================

export const eventbusApi = {
  // 获取事件统计
  getStats: () => service.get(API_PATHS.EVENTBUS_STATS),

  // 获取事件类型列表
  getTypes: () => service.get(API_PATHS.EVENTBUS_TYPES),

  // 获取最近事件流
  getRecent: (limit = 50) => service.get(API_PATHS.EVENTBUS_RECENT, { params: { limit } })
}

// ==================== 日志查看相关 API ====================

export const logsApi = {
  // 获取当前日志文件路径
  getLogFile: () => service.get(API_PATHS.LOGS_FILE),

  // 读取最近日志（可按关键字过滤）
  getLogs: (keyword = '', lines = 200) =>
    service.get(API_PATHS.LOGS_LIST, { params: { keyword, lines } })
}

// ==================== 插件市场相关 API ====================

export const marketplaceApi = {
  // 获取插件市场清单
  getList: () => service.get(API_PATHS.MARKETPLACE_LIST),

  // 下载并安装市场插件
  install: (pluginId, autoStart = true) =>
    service.post(`${API_PATHS.MARKETPLACE_INSTALL}/${pluginId}`, { autoStart })
}

export default service