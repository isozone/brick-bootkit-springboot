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
  retry: (id) => service.post(`${API_PATHS.SCRIPTS_EXECUTIONS}/${id}/retry`)
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

export default service