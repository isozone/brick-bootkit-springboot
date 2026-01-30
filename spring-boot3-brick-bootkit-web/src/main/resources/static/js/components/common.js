/**
 * 公共组件工具库
 * @description 提供通用的UI组件和工具函数
 */

const BrickComponents = {
    // 状态标签
    statusBadge: {
        SUCCESS: { class: 'badge bg-success', text: '成功' },
        ERROR: { class: 'badge bg-danger', text: '失败' },
        WARNING: { class: 'badge bg-warning text-dark', text: '警告' },
        INFO: { class: 'badge bg-info text-dark', text: '信息' },
        SECONDARY: { class: 'badge bg-secondary', text: '其他' },
        
        // 插件状态
        STARTED: { class: 'badge bg-success', text: '运行中' },
        STOPPED: { class: 'badge bg-secondary', text: '已停止' },
        FAILED: { class: 'badge bg-danger', text: '异常' },
        LOADING: { class: 'badge bg-info text-dark', text: '加载中' },
        
        // 脚本类型
        SHELL: { class: 'badge bg-primary', text: 'Shell' },
        PYTHON: { class: 'badge bg-warning text-dark', text: 'Python' },
        JAVASCRIPT: { class: 'badge bg-success', text: 'JavaScript' },
        SQL: { class: 'badge bg-info text-dark', text: 'SQL' },
        BATCH: { class: 'badge bg-secondary', text: 'Batch' },
        POWERSHELL: { class: 'badge bg-primary', text: 'PowerShell' },
        
        // 获取状态标签HTML
        getHtml(type, customText = null) {
            const status = this[type];
            if (status) {
                return `<span class="${status.class}">${customText || status.text}</span>`;
            }
            return `<span class="badge bg-secondary">${type}</span>`;
        }
    },

    // 加载状态组件
    loading: {
        show(containerId, message) {
            message = message || '加载中...';
            const container = document.getElementById(containerId);
            if (!container) return;
            
            container.innerHTML = `
                <div class="brick-loading">
                    <div class="brick-spinner"></div>
                    <div class="mt-3">${message}</div>
                </div>
            `;
        },
        
        hide(containerId) {
            const container = document.getElementById(containerId);
            if (!container) return;
            
            const loading = container.querySelector('.brick-loading');
            if (loading) {
                loading.remove();
            }
        }
    },

    // 空状态组件
    empty: {
        show(containerId, options) {
            options = options || {};
            const container = document.getElementById(containerId);
            if (!container) return;
            
            const {
                icon = 'bi-inbox',
                title = '暂无数据',
                text = '当前没有任何数据',
                actionText = null,
                actionUrl = null,
                actionCallback = null
            } = options;
            
            let actionHtml = '';
            if (actionText) {
                if (actionCallback) {
                    actionHtml = `<button class="btn btn-primary btn-sm mt-3" onclick="${actionCallback}">${actionText}</button>`;
                } else if (actionUrl) {
                    actionHtml = `<a href="${actionUrl}" class="btn btn-primary btn-sm mt-3">${actionText}</a>`;
                }
            }
            
            container.innerHTML = `
                <div class="brick-empty">
                    <div class="brick-empty-icon">
                        <i class="bi ${icon}"></i>
                    </div>
                    <div class="brick-empty-title">${title}</div>
                    <div class="brick-empty-text">${text}</div>
                    ${actionHtml}
                </div>
            `;
        }
    },

    // 确认对话框
    confirm: {
        async show(options) {
            options = options || {};
            const {
                title = '确认操作',
                text = '确定要执行此操作吗？',
                confirmText = '确定',
                cancelText = '取消',
                confirmButtonColor = '#2563eb'
            } = options;
            
            const result = await Swal.fire({
                title: title,
                text: text,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: confirmText,
                cancelButtonText: cancelText,
                confirmButtonColor: confirmButtonColor
            });
            
            return result.isConfirmed;
        }
    },

    // 提示消息
    toast: {
        success(message, title) {
            title = title || '成功';
            Swal.fire({
                icon: 'success',
                title: title,
                text: message,
                timer: 2000,
                showConfirmButton: false
            });
        },
        
        error(message, title) {
            title = title || '错误';
            Swal.fire({
                icon: 'error',
                title: title,
                text: message
            });
        },
        
        warning(message, title) {
            title = title || '警告';
            Swal.fire({
                icon: 'warning',
                title: title,
                text: message
            });
        },
        
        info(message, title) {
            title = title || '提示';
            Swal.fire({
                icon: 'info',
                title: title,
                text: message
            });
        }
    },

    // 表单验证
    validator: {
        required(value, message) {
            message = message || '此字段为必填项';
            if (!value || value.trim() === '') {
                return { valid: false, message };
            }
            return { valid: true };
        },
        
        email(value, message) {
            message = message || '请输入有效的邮箱地址';
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(value)) {
                return { valid: false, message };
            }
            return { valid: true };
        },
        
        minLength(value, min, message) {
            if (value.length < min) {
                return { valid: false, message || `至少需要 ${min} 个字符` };
            }
            return { valid: true };
        },
        
        maxLength(value, max, message) {
            if (value.length > max) {
                return { valid: false, message || `最多允许 ${max} 个字符` };
            }
            return { valid: true };
        }
    },

    // 格式化工具
    formatter: {
        // 格式化日期时间
        datetime(date, format) {
            format = format || 'YYYY-MM-DD HH:mm:ss';
            if (!date) return '-';
            const d = new Date(date);
            
            const year = d.getFullYear();
            const month = String(d.getMonth() + 1).padStart(2, '0');
            const day = String(d.getDate()).padStart(2, '0');
            const hours = String(d.getHours()).padStart(2, '0');
            const minutes = String(d.getMinutes()).padStart(2, '0');
            const seconds = String(d.getSeconds()).padStart(2, '0');
            
            return format
                .replace('YYYY', year)
                .replace('MM', month)
                .replace('DD', day)
                .replace('HH', hours)
                .replace('mm', minutes)
                .replace('ss', seconds);
        },
        
        // 格式化文件大小
        fileSize(bytes) {
            if (bytes === 0) return '0 B';
            const k = 1024;
            const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
        },
        
        // 格式化数字
        number(num, decimals) {
            decimals = decimals !== undefined ? decimals : 2;
            if (isNaN(num)) return '-';
            return parseFloat(num).toFixed(decimals);
        },
        
        // 格式化百分比
        percent(value, decimals) {
            decimals = decimals !== undefined ? decimals : 1;
            if (isNaN(value)) return '-';
            return parseFloat(value).toFixed(decimals) + '%';
        },
        
        // 格式化时长
        duration(seconds) {
            if (!seconds || isNaN(seconds)) return '-';
            
            const days = Math.floor(seconds / (24 * 3600));
            const hours = Math.floor((seconds % (24 * 3600)) / 3600);
            const minutes = Math.floor((seconds % 3600) / 60);
            const secs = Math.floor(seconds % 60);
            
            const parts = [];
            if (days > 0) parts.push(`${days}天`);
            if (hours > 0) parts.push(`${hours}小时`);
            if (minutes > 0) parts.push(`${minutes}分钟`);
            if (secs > 0 || parts.length === 0) parts.push(`${secs}秒`);
            
            return parts.join('');
        }
    },

    // 复制到剪贴板
    clipboard: {
        copy(text) {
            return navigator.clipboard.writeText(text)
                .then(() => {
                    BrickComponents.toast.success('已复制到剪贴板');
                })
                .catch(() => {
                    BrickComponents.toast.error('复制失败');
                });
        }
    },

    // 下载文件
    download: {
        downloadFile(content, filename, mimeType) {
            mimeType = mimeType || 'text/plain';
            const blob = new Blob([content], { type: mimeType });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }
    },

    // 表格操作
    table: {
        // 导出为CSV
        exportToCSV(tableId, filename) {
            const table = document.getElementById(tableId);
            if (!table) return;
            
            let csv = [];
            const rows = table.querySelectorAll('tr');
            
            rows.forEach(row => {
                const cols = row.querySelectorAll('th, td');
                const rowData = [];
                cols.forEach(col => {
                    rowData.push(col.innerText);
                });
                csv.push(rowData.join(','));
            });
            
            const csvContent = csv.join('\n');
            this.downloadFile(csvContent, filename || 'export.csv', 'text/csv');
        },
        
        // 排序表格
        sort(tableId, columnIndex) {
            const table = document.getElementById(tableId);
            if (!table) return;
            
            const tbody = table.querySelector('tbody');
            const rows = Array.from(tbody.querySelectorAll('tr'));
            
            rows.sort((a, b) => {
                const aValue = a.cells[columnIndex].innerText;
                const bValue = b.cells[columnIndex].innerText;
                
                // 尝试数字排序
                const aNum = parseFloat(aValue);
                const bNum = parseFloat(bValue);
                
                if (!isNaN(aNum) && !isNaN(bNum)) {
                    return aNum - bNum;
                }
                
                return aValue.localeCompare(bValue);
            });
            
            rows.forEach(row => tbody.appendChild(row));
        }
    }
};

// 导出到全局
window.BrickComponents = BrickComponents;