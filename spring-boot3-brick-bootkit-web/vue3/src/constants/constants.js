// 状态码常量
export const CODE = {
  SUCCESS: 200,
  ERROR: 500,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404
}

// 提示信息常量
export const MESSAGE = {
  // 成功提示
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
  // 错误提示
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
  },
  // 确认提示
  CONFIRM: {
    DELETE: '确定要删除该项吗？此操作不可恢复。',
    STOP: '确定要停止该服务吗？',
    UNINSTALL: '确定要卸载该插件吗？',
    EXECUTE: '确定要执行该脚本吗？'
  }
}

// 正则表达式常量
export const REGEX = {
  EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PHONE: /^1[3-9]\d{9}$/,
  URL: /^https?:\/\/[^\s]+$/,
  IP: /^(\d{1,3}\.){3}\d{1,3}$/,
  NUMBER: /^\d+$/,
  CHINESE: /[\u4e00-\u9fa5]/
}

// 文件类型常量
export const FILE_TYPE = {
  SCRIPT: ['.sh', '.py', '.js', '.sql', '.bat', '.ps1'],
  PLUGIN: ['.jar', '.zip'],
  IMAGE: ['.jpg', '.jpeg', '.png', '.gif', '.svg']
}

// 文件大小限制 (单位:字节)
export const FILE_SIZE = {
  SCRIPT: 1024 * 1024, // 1MB
  PLUGIN: 50 * 1024 * 1024, // 50MB
  IMAGE: 5 * 1024 * 1024 // 5MB
}
