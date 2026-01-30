/**
 * 统一顶部导航组件
 * @description 提供统一的顶部导航栏，包含面包屑、操作按钮和时间显示
 */

const TopbarComponent = {
    // 面包屑配置
    breadcrumbs: [],

    // 操作按钮配置
    actions: [],

    // 时间显示元素
    timeElement: null,

    // 时间更新定时器
    timeInterval: null,

    // 初始化
    init(config = {}) {
        this.breadcrumbs = config.breadcrumbs || [];
        this.actions = config.actions || [];
        this.render();
        this.bindEvents();
        this.startTimer();
    },

    // 渲染顶部导航
    render() {
        const topbar = document.querySelector('.brick-topbar');
        if (!topbar) return;

        // 渲染面包屑
        this.renderBreadcrumbs(topbar);

        // 渲染操作按钮
        this.renderActions(topbar);

        // 渲染时间显示
        this.renderTime(topbar);
    },

    // 渲染面包屑
    renderBreadcrumbs(topbar) {
        let breadcrumbContainer = topbar.querySelector('.brick-breadcrumb');
        
        if (!breadcrumbContainer) {
            const leftSection = topbar.querySelector('div:first-child');
            if (leftSection) {
                breadcrumbContainer = document.createElement('nav');
                breadcrumbContainer.className = 'brick-breadcrumb';
                leftSection.appendChild(breadcrumbContainer);
            }
        }

        if (breadcrumbContainer) {
            let html = '';
            this.breadcrumbs.forEach((item, index) => {
                const isLast = index === this.breadcrumbs.length - 1;
                html += `<a href="${item.href}" class="${isLast ? 'brick-breadcrumb-item active' : ''}">${item.label}</a>`;
                if (!isLast) {
                    html += `<span class="brick-breadcrumb-separator">/</span>`;
                }
            });
            breadcrumbContainer.innerHTML = html;
        }
    },

    // 渲染操作按钮
    renderActions(topbar) {
        const actionsContainer = topbar.querySelector('.brick-actions');
        if (!actionsContainer) return;

        let html = `
            <button class="btn btn-secondary btn-sm" onclick="BrickWeb.refresh()">
                <i class="bi bi-arrow-clockwise"></i>
                <span class="d-none d-sm-inline">刷新</span>
            </button>
        `;

        this.actions.forEach(action => {
            html += `<button class="btn ${action.className || 'btn-secondary'} btn-sm" 
                            onclick="${action.onclick || ''}">
                <i class="bi ${action.icon || 'bi-gear'}"></i>
                ${action.label ? `<span class="d-none d-sm-inline">${action.label}</span>` : ''}
            </button>`;
        });

        // 时间显示
        html += `
            <div class="brick-time">
                <i class="bi bi-clock"></i>
                <span id="current-time"></span>
            </div>
        `;

        actionsContainer.innerHTML = html;
    },

    // 渲染时间显示
    renderTime(topbar) {
        this.timeElement = topbar.querySelector('#current-time');
        if (this.timeElement) {
            this.updateTime();
        }
    },

    // 更新时间
    updateTime() {
        if (!this.timeElement) return;
        
        const now = new Date();
        this.timeElement.textContent = now.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    },

    // 启动定时器
    startTimer() {
        this.updateTime();
        this.timeInterval = setInterval(() => {
            this.updateTime();
        }, 1000);
    },

    // 停止定时器
    stopTimer() {
        if (this.timeInterval) {
            clearInterval(this.timeInterval);
            this.timeInterval = null;
        }
    },

    // 绑定事件
    bindEvents() {
        const toggleBtn = document.querySelector('.brick-toggle-sidebar');
        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                if (window.Sidebar) {
                    window.Sidebar.toggle();
                }
            });
        }
    },

    // 更新面包屑
    updateBreadcrumbs(breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
        const topbar = document.querySelector('.brick-topbar');
        if (topbar) {
            this.renderBreadcrumbs(topbar);
        }
    },

    // 添加操作按钮
    addAction(action) {
        this.actions.push(action);
        const topbar = document.querySelector('.brick-topbar');
        if (topbar) {
            this.renderActions(topbar);
        }
    },

    // 移除操作按钮
    removeAction(index) {
        this.actions.splice(index, 1);
        const topbar = document.querySelector('.brick-topbar');
        if (topbar) {
            this.renderActions(topbar);
        }
    }
};

// 导出到全局
window.Topbar = TopbarComponent;