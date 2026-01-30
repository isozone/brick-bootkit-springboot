/**
 * 统一侧边栏组件
 * @description 提供统一的侧边栏菜单，支持高亮当前页面和折叠功能
 */

const SidebarComponent = {
    // 菜单配置
    menuConfig: [
        {
            title: '概览',
            items: [
                { icon: 'bi-speedometer2', label: '仪表盘', href: '/brick-web/index.html', id: 'dashboard' }
            ]
        },
        {
            title: '插件管理',
            items: [
                { icon: 'bi-plug', label: '插件列表', href: '/brick-web/plugins/index.html', id: 'plugins-list' },
                { icon: 'bi-cloud-upload', label: '上传插件', href: '/brick-web/plugins/upload.html', id: 'plugins-upload' }
            ]
        },
        {
            title: '脚本管理',
            items: [
                { icon: 'bi-file-earmark-code', label: '脚本列表', href: '/brick-web/templates/scripts/index.html', id: 'scripts-list' },
                { icon: 'bi-pencil-square', label: '编辑脚本', href: '/brick-web/templates/scripts/editor.html', id: 'scripts-editor' },
                { icon: 'bi-grid-3x3', label: '模板管理', href: '/brick-web/templates/scripts/templates.html', id: 'scripts-templates' },
                { icon: 'bi-clock-history', label: '执行记录', href: '/brick-web/templates/scripts/executions.html', id: 'scripts-executions' },
                { icon: 'bi-clock', label: '调度任务', href: '/brick-web/templates/scripts/scheduler.html', id: 'scripts-scheduler' }
            ]
        },
        {
            title: '系统监控',
            items: [
                { icon: 'bi-bar-chart-line', label: '监控概览', href: '/brick-web/monitor/overview.html', id: 'monitor-overview' },
                { icon: 'bi-memory', label: '内存监控', href: '/brick-web/monitor/memory.html', id: 'monitor-memory' },
                { icon: 'bi-cpu', label: 'CPU 监控', href: '/brick-web/monitor/cpu.html', id: 'monitor-cpu' },
                { icon: 'bi-gear', label: '线程监控', href: '/brick-web/monitor/threads.html', id: 'monitor-threads' }
            ]
        },
        {
            title: '文档',
            items: [
                { icon: 'bi-book', label: 'API 文档', href: '/doc.html', id: 'api-doc', external: true }
            ]
        }
    ],

    // 当前页面ID
    currentPageId: '',

    // 初始化
    init(pageId = '') {
        this.currentPageId = pageId;
        this.render();
        this.updateActiveMenu();
        this.bindEvents();
    },

    // 渲染侧边栏
    render() {
        const sidebar = document.getElementById('sidebar');
        if (!sidebar) return;

        const nav = sidebar.querySelector('.brick-sidebar-nav');
        if (!nav) return;

        // 生成菜单HTML
        let menuHtml = '';
        this.menuConfig.forEach(section => {
            menuHtml += `
                <div class="brick-nav-section">
                    <div class="brick-nav-section-title">${section.title}</div>
                    <ul class="brick-nav">
            `;
            section.items.forEach(item => {
                menuHtml += `
                    <li class="brick-nav-item">
                        <a class="brick-nav-link" 
                           href="${item.href}" 
                           data-page-id="${item.id}"
                           ${item.external ? 'target="_blank"' : ''}>
                            <i class="bi ${item.icon}"></i>
                            <span>${item.label}</span>
                        </a>
                    </li>
                `;
            });
            menuHtml += `
                    </ul>
                </div>
            `;
        });

        nav.innerHTML = menuHtml;
    },

    // 更新当前激活的菜单项
    updateActiveMenu() {
        const currentPath = window.location.pathname;
        const menuLinks = document.querySelectorAll('.brick-nav-link');

        menuLinks.forEach(link => {
            link.classList.remove('active');
            const href = link.getAttribute('href');
            
            // 精确匹配或路径匹配
            if (href === currentPath || currentPath.startsWith(href) || this.currentPageId === link.dataset.pageId) {
                link.classList.add('active');
                
                // 展开父级菜单（如果有多级菜单）
                const parentSection = link.closest('.brick-nav-section');
                if (parentSection) {
                    parentSection.classList.add('expanded');
                }
            }
        });
    },

    // 绑定事件
    bindEvents() {
        // 点击菜单项时的处理
        const menuLinks = document.querySelectorAll('.brick-nav-link');
        menuLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                const href = link.getAttribute('href');
                const isExternal = link.getAttribute('target') === '_blank';
                
                // 如果是外部链接，不做处理
                if (isExternal) return;
                
                // 内部链接，移除所有激活状态
                menuLinks.forEach(l => l.classList.remove('active'));
                link.classList.add('active');
            });
        });
    },

    // 切换侧边栏折叠状态
    toggle() {
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('sidebar-overlay');
        
        if (sidebar) {
            sidebar.classList.toggle('open');
        }
        if (overlay) {
            overlay.classList.toggle('show');
        }
    },

    // 设置当前页面
    setCurrentPage(pageId) {
        this.currentPageId = pageId;
        this.updateActiveMenu();
    },

    // 获取当前激活的菜单项
    getActiveMenuItem() {
        const activeLink = document.querySelector('.brick-nav-link.active');
        return activeLink ? {
            id: activeLink.dataset.pageId,
            label: activeLink.querySelector('span').textContent,
            href: activeLink.getAttribute('href')
        } : null;
    }
};

// 导出到全局
window.Sidebar = SidebarComponent;