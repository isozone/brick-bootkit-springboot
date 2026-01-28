/**
 * Brick Web 通用 JavaScript
 * 企业级前端交互组件
 * 
 * @author brick-bootkit
 */

(function(global) {
    'use strict';

    const BrickWeb = {
        // API 基础路径
        apiBase: '/brick-web/api',
        
        // 页面路径
        pageBase: '/brick-web',
        
        // 状态
        loadingCount: 0,
        
        // 初始化
        init: function() {
            this.setupAjax();
            this.setupSweetAlert();
            this.setupKeyboardShortcuts();
            this.setupAnimations();
        },
        
        // 配置 Ajax
        setupAjax: function() {
            const self = this;
            
            // 请求拦截器
            axios.interceptors.request.use(
                function(config) {
                    self.showLoading();
                    // 添加请求标识
                    config.headers['X-Requested-With'] = 'XMLHttpRequest';
                    return config;
                },
                function(error) {
                    self.hideLoading();
                    return Promise.reject(error);
                }
            );
            
            // 响应拦截器
            axios.interceptors.response.use(
                function(response) {
                    self.hideLoading();
                    return response;
                },
                function(error) {
                    self.hideLoading();
                    self.handleError(error);
                    return Promise.reject(error);
                }
            );
        },
        
        // 配置 SweetAlert
        setupSweetAlert: function() {
            if (typeof Swal !== 'undefined') {
                Swal.mixin({
                    confirmButtonColor: '#2563eb',
                    cancelButtonColor: '#6b7280',
                    confirmButtonText: '确定',
                    cancelButtonText: '取消'
                });
            }
        },
        
        // 键盘快捷键
        setupKeyboardShortcuts: function() {
            document.addEventListener('keydown', function(e) {
                // Ctrl/Cmd + R 刷新
                if ((e.ctrlKey || e.metaKey) && e.key === 'r') {
                    e.preventDefault();
                    BrickWeb.refresh();
                }
                // Escape 关闭模态框
                if (e.key === 'Escape') {
                    const modal = document.querySelector('.modal.show');
                    if (modal) {
                        const bsModal = bootstrap.Modal.getInstance(modal);
                        if (bsModal) bsModal.hide();
                    }
                }
            });
        },
        
        // 动画设置
        setupAnimations: function() {
            // 为带有 fade-in 类的元素添加进入动画
            const observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('animated');
                        observer.unobserve(entry.target);
                    }
                });
            }, { threshold: 0.1 });
            
            document.querySelectorAll('.fade-in, .slide-in-left, .slide-in-right').forEach(el => {
                observer.observe(el);
            });
        },
        
        // 显示加载动画
        showLoading: function() {
            this.loadingCount++;
            if (!document.getElementById('loading-overlay')) {
                const overlay = document.createElement('div');
                overlay.id = 'loading-overlay';
                overlay.className = 'loading-overlay';
                overlay.innerHTML = `
                    <div class="brick-loading">
                        <div class="brick-loading-spinner">
                            <div class="spinner-layer">
                                <div class="circle-clipper left">
                                    <div class="circle"></div>
                                </div>
                                <div class="gap-patch">
                                    <div class="circle"></div>
                                </div>
                                <div class="circle-clipper right">
                                    <div class="circle"></div>
                                </div>
                            </div>
                        </div>
                        <div class="brick-loading-text">加载中...</div>
                    </div>
                `;
                document.body.appendChild(overlay);
                
                // 触发淡入动画
                setTimeout(() => overlay.classList.add('show'), 10);
            }
        },
        
        // 隐藏加载动画
        hideLoading: function() {
            this.loadingCount = Math.max(0, this.loadingCount - 1);
            if (this.loadingCount <= 0) {
                const overlay = document.getElementById('loading-overlay');
                if (overlay) {
                    overlay.classList.remove('show');
                    setTimeout(() => overlay.remove(), 300);
                    this.loadingCount = 0;
                }
            }
        },
        
        // 处理错误
        handleError: function(error) {
            let message = '系统错误';
            
            if (error.response) {
                const status = error.response.status;
                const data = error.response.data;
                
                switch (status) {
                    case 400:
                        message = data?.message || '请求参数错误';
                        break;
                    case 401:
                        message = '未登录或登录已过期';
                        break;
                    case 403:
                        message = '没有权限执行此操作';
                        break;
                    case 404:
                        message = '请求的资源不存在';
                        break;
                    case 500:
                        message = data?.message || '服务器内部错误';
                        break;
                    default:
                        message = data?.message || error.message || '系统错误';
                }
            } else if (error.message) {
                message = error.message;
            }
            
            this.toast('error', message);
        },
        
        // Toast 提示
        toast: function(type, message, duration = 4000) {
            if (typeof Swal !== 'undefined') {
                const icons = {
                    success: 'success',
                    error: 'error',
                    warning: 'warning',
                    info: 'info'
                };
                
                Swal.fire({
                    toast: true,
                    position: 'top-end',
                    icon: icons[type] || 'info',
                    title: message,
                    showConfirmButton: false,
                    timer: duration,
                    timerProgressBar: true,
                    didOpen: (toast) => {
                        toast.addEventListener('mouseenter', Swal.stopTimer);
                        toast.addEventListener('mouseleave', Swal.resumeTimer);
                    }
                });
            } else {
                // 降级为原生 alert
                const bgColor = {
                    success: '#10b981',
                    error: '#ef4444',
                    warning: '#f59e0b',
                    info: '#0ea5e9'
                }[type] || '#6b7280';
                
                this.showNotification(message, type, bgColor);
            }
        },
        
        // 显示通知（降级方案）
        showNotification: function(message, type = 'info', bgColor = '#2563eb') {
            let container = document.getElementById('notification-container');
            if (!container) {
                container = document.createElement('div');
                container.id = 'notification-container';
                container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 10000; display: flex; flex-direction: column; gap: 10px;';
                document.body.appendChild(container);
            }
            
            const notification = document.createElement('div');
            notification.style.cssText = `background: ${bgColor}; color: #fff; padding: 12px 20px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); display: flex; align-items: center; gap: 10px; animation: slideInRight 0.3s ease; min-width: 280px;`;
            notification.innerHTML = `<i class="bi bi-bell"></i> <span>${message}</span>`;
            
            container.appendChild(notification);
            
            setTimeout(() => {
                notification.style.animation = 'fadeOut 0.3s ease forwards';
                setTimeout(() => notification.remove(), 300);
            }, 4000);
        },
        
        // 确认对话框
        confirm: function(title, text, confirmCallback, options = {}) {
            const config = Object.assign({
                title: title,
                text: text,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#2563eb',
                cancelButtonColor: '#6b7280',
                confirmButtonText: '确定',
                cancelButtonText: '取消'
            }, options);
            
            if (typeof Swal !== 'undefined') {
                Swal.fire(config).then(function(result) {
                    if (result.isConfirmed && confirmCallback) {
                        confirmCallback();
                    }
                });
            } else if (confirm(text)) {
                confirmCallback();
            }
        },
        
        // 确认危险操作
        dangerConfirm: function(title, text, confirmCallback) {
            this.confirm(title, text, confirmCallback, {
                confirmButtonColor: '#ef4444',
                icon: 'error'
            });
        },
        
        // API 请求
        api: {
            // GET 请求
            get: function(url, params) {
                return axios.get(BrickWeb.apiBase + url, { params: params })
                    .then(function(response) {
                        return response.data;
                    });
            },
            
            // POST 请求
            post: function(url, data) {
                return axios.post(BrickWeb.apiBase + url, data)
                    .then(function(response) {
                        return response.data;
                    });
            },
            
            // POST JSON 请求
            postJson: function(url, data) {
                return axios.post(BrickWeb.apiBase + url, data, {
                    headers: { 'Content-Type': 'application/json' }
                }).then(function(response) {
                    return response.data;
                });
            },
            
            // PUT 请求
            put: function(url, data) {
                return axios.put(BrickWeb.apiBase + url, data)
                    .then(function(response) {
                        return response.data;
                    });
            },
            
            // DELETE 请求
            delete: function(url) {
                return axios.delete(BrickWeb.apiBase + url)
                    .then(function(response) {
                        return response.data;
                    });
            },
            
            // 上传文件
            upload: function(url, file, onProgress) {
                const formData = new FormData();
                formData.append('file', file);
                
                return axios.post(BrickWeb.apiBase + url, formData, {
                    headers: { 'Content-Type': 'multipart/form-data' },
                    onUploadProgress: onProgress
                }).then(function(response) {
                    return response.data;
                });
            }
        },
        
        // 获取 URL 参数
        getQueryParam: function(name) {
            const urlParams = new URLSearchParams(window.location.search);
            return urlParams.get(name);
        },
        
        // 获取所有 URL 参数
        getQueryParams: function() {
            return new URLSearchParams(window.location.search);
        },
        
        // 格式化字节大小
        formatBytes: function(bytes, decimals = 2) {
            if (bytes === 0) return '0 Bytes';
            
            const k = 1024;
            const dm = decimals < 0 ? 0 : decimals;
            const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
            
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            
            return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
        },
        
        // 格式化数字
        formatNumber: function(num) {
            if (num >= 1000000) {
                return (num / 1000000).toFixed(1) + 'M';
            } else if (num >= 1000) {
                return (num / 1000).toFixed(1) + 'K';
            }
            return num.toString();
        },
        
        // 格式化时间
        formatTime: function(timestamp) {
            if (!timestamp || timestamp <= 0) return '-';
            const date = new Date(timestamp);
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        },
        
        // 格式化日期
        formatDate: function(timestamp) {
            if (!timestamp || timestamp <= 0) return '-';
            const date = new Date(timestamp);
            return date.toLocaleDateString('zh-CN');
        },
        
        // 格式化持续时间
        formatDuration: function(milliseconds) {
            if (!milliseconds) return '0ms';
            
            if (milliseconds < 1000) {
                return milliseconds.toFixed(0) + 'ms';
            } else if (milliseconds < 60000) {
                return (milliseconds / 1000).toFixed(1) + 's';
            } else if (milliseconds < 3600000) {
                return (milliseconds / 60000).toFixed(1) + 'm';
            } else if (milliseconds < 86400000) {
                return (milliseconds / 3600000).toFixed(1) + 'h';
            } else {
                return (milliseconds / 86400000).toFixed(1) + 'd';
            }
        },
        
        // 相对时间
        relativeTime: function(timestamp) {
            if (!timestamp) return '-';
            
            const now = new Date().getTime();
            const diff = now - timestamp;
            
            if (diff < 60000) {
                return '刚刚';
            } else if (diff < 3600000) {
                return Math.floor(diff / 60000) + ' 分钟前';
            } else if (diff < 86400000) {
                return Math.floor(diff / 3600000) + ' 小时前';
            } else if (diff < 604800000) {
                return Math.floor(diff / 86400000) + ' 天前';
            } else {
                return this.formatDate(timestamp);
            }
        },
        
        // 刷新当前页面
        refresh: function() {
            window.location.reload();
        },
        
        // 跳转到页面
        navigateTo: function(path) {
            window.location.href = this.pageBase + path;
        },
        
        // 返回上一页
        goBack: function() {
            if (document.referrer) {
                window.history.back();
            } else {
                this.navigateTo('/index.html');
            }
        },
        
        // 复制到剪贴板
        copyToClipboard: function(text) {
            if (navigator.clipboard) {
                navigator.clipboard.writeText(text).then(() => {
                    this.toast('success', '已复制到剪贴板');
                }).catch(() => {
                    this.toast('error', '复制失败');
                });
            } else {
                // 降级方案
                const textarea = document.createElement('textarea');
                textarea.value = text;
                textarea.style.position = 'fixed';
                textarea.style.opacity = '0';
                document.body.appendChild(textarea);
                textarea.select();
                try {
                    document.execCommand('copy');
                    this.toast('success', '已复制到剪贴板');
                } catch (err) {
                    this.toast('error', '复制失败');
                }
                document.body.removeChild(textarea);
            }
        },
        
        // 下载文件
        downloadFile: function(url, filename) {
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            link.target = '_blank';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        },
        
        // 防抖
        debounce: function(func, wait) {
            let timeout;
            return function executedFunction(...args) {
                const later = () => {
                    clearTimeout(timeout);
                    func(...args);
                };
                clearTimeout(timeout);
                timeout = setTimeout(later, wait);
            };
        },
        
        // 节流
        throttle: function(func, limit) {
            let inThrottle;
            return function executedFunction(...args) {
                if (!inThrottle) {
                    func(...args);
                    inThrottle = true;
                    setTimeout(() => inThrottle = false, limit);
                }
            };
        },
        
        // 深拷贝
        deepClone: function(obj) {
            if (obj === null || typeof obj !== 'object') return obj;
            if (obj instanceof Date) return new Date(obj.getTime());
            if (obj instanceof Array) return obj.map(item => this.deepClone(item));
            if (typeof obj === 'object') {
                const clonedObj = {};
                for (const key in obj) {
                    if (obj.hasOwnProperty(key)) {
                        clonedObj[key] = this.deepClone(obj[key]);
                    }
                }
                return clonedObj;
            }
        },
        
        // 判断对象是否为空
        isEmpty: function(obj) {
            if (obj === null || obj === undefined) return true;
            if (Array.isArray(obj)) return obj.length === 0;
            if (typeof obj === 'object') return Object.keys(obj).length === 0;
            return false;
        },
        
        // UUID 生成
        generateUUID: function() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                const r = Math.random() * 16 | 0;
                const v = c === 'x' ? r : (r & 0x3 | 0x8);
                return v.toString(16);
            });
        },
        
        // 本地存储封装
        storage: {
            set: function(key, value, expires = null) {
                const data = {
                    value: value,
                    timestamp: Date.now(),
                    expires: expires
                };
                localStorage.setItem(key, JSON.stringify(data));
            },
            
            get: function(key, defaultValue = null) {
                try {
                    const item = localStorage.getItem(key);
                    if (!item) return defaultValue;
                    
                    const data = JSON.parse(item);
                    
                    // 检查是否过期
                    if (data.expires && Date.now() - data.timestamp > data.expires) {
                        localStorage.removeItem(key);
                        return defaultValue;
                    }
                    
                    return data.value;
                } catch (e) {
                    return defaultValue;
                }
            },
            
            remove: function(key) {
                localStorage.removeItem(key);
            },
            
            clear: function() {
                localStorage.clear();
            }
        },
        
        // 会话存储
        session: {
            set: function(key, value) {
                sessionStorage.setItem(key, JSON.stringify(value));
            },
            
            get: function(key, defaultValue = null) {
                try {
                    const item = sessionStorage.getItem(key);
                    return item ? JSON.parse(item) : defaultValue;
                } catch (e) {
                    return defaultValue;
                }
            },
            
            remove: function(key) {
                sessionStorage.removeItem(key);
            }
        },
        
        // 主题控制
        theme: {
            isDark: function() {
                return localStorage.getItem('theme') === 'dark' ||
                       (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: dark)').matches);
            },
            
            toggle: function() {
                const isDark = !this.isDark();
                document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
                localStorage.setItem('theme', isDark ? 'dark' : 'light');
                return isDark;
            },
            
            set: function(theme) {
                document.documentElement.setAttribute('data-theme', theme);
                localStorage.setItem('theme', theme);
            },
            
            init: function() {
                const theme = localStorage.getItem('theme') || 
                             (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
                this.set(theme);
            }
        }
    };
    
    // 初始化
    document.addEventListener('DOMContentLoaded', function() {
        BrickWeb.theme.init();
        BrickWeb.init();
        
        // 触发自定义事件
        window.dispatchEvent(new CustomEvent('brick-web-ready'));
    });
    
    // 暴露到全局
    global.BrickWeb = BrickWeb;
    
    // 添加 CSS 动画样式
    const style = document.createElement('style');
    style.textContent = `
        @keyframes slideInRight {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
        
        @keyframes fadeOut {
            from {
                opacity: 1;
            }
            to {
                opacity: 0;
                transform: translateX(100%);
            }
        }
        
        @keyframes slideInUp {
            from {
                transform: translateY(20px);
                opacity: 0;
            }
            to {
                transform: translateY(0);
                opacity: 1;
            }
        }
        
        .brick-loading {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 1rem;
        }
        
        .brick-loading-spinner {
            width: 50px;
            height: 50px;
        }
        
        .brick-loading-spinner .circle-clipper {
            display: inline-block;
            position: relative;
            width: 50%;
            height: 100%;
        }
        
        .brick-loading-spinner .circle-clipper.left {
            float: left;
        }
        
        .brick-loading-spinner .circle-clipper.right {
            float: right;
        }
        
        .brick-loading-spinner .circle {
            width: 200%;
            height: 100%;
            border-radius: 50%;
            border: 3px solid transparent;
            border-top-color: currentColor;
        }
        
        .brick-loading-spinner .gap-patch {
            position: absolute;
            top: 0;
            left: 50%;
            width: 50%;
            height: 100%;
            overflow: hidden;
            transform: translateX(-50%);
        }
        
        .brick-loading-spinner .gap-patch .circle {
            width: 200%;
            height: 100%;
            border-radius: 50%;
            border: 3px solid transparent;
            border-top-color: currentColor;
            position: absolute;
            left: -100%;
        }
        
        .brick-loading-spinner .spinner-layer {
            width: 100%;
            height: 100%;
            animation: rotate 1s linear infinite;
        }
        
        @keyframes rotate {
            to { transform: rotate(360deg); }
        }
        
        .brick-loading-text {
            color: #fff;
            font-size: 14px;
        }
    `;
    document.head.appendChild(style);
    
})(window);