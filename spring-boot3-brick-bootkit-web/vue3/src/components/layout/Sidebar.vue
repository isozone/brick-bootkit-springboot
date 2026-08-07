<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sidebar-header">
      <router-link to="/plugins-web/" class="logo">
        <div class="logo-icon">
          <n-icon size="20" color="#fff">
            <LayersOutline />
          </n-icon>
        </div>
        <span v-if="!collapsed" class="logo-text">Brick Bootkit</span>
      </router-link>
    </div>

    <div class="sidebar-menu">
      <n-menu
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="20"
        :options="menuOptions"
        :value="activeKey"
        @update:value="handleMenuClick"
      />
    </div>

    <div class="sidebar-footer">
      <n-tooltip placement="right">
        <template #trigger>
          <n-button quaternary circle @click="$emit('toggle')">
            <template #icon>
              <n-icon>
                <MenuOutline v-if="!collapsed" />
                <ChevronForwardOutline v-else />
              </n-icon>
            </template>
          </n-button>
        </template>
        {{ collapsed ? '展开菜单' : '收起菜单' }}
      </n-tooltip>
    </div>
  </aside>
</template>

<script setup>
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NMenu, NButton, NIcon, NTooltip } from 'naive-ui'
import { LayersOutline, MenuOutline, ChevronBackOutline, ChevronForwardOutline, HomeOutline, CodeSlashOutline, CubeOutline, StatsChartOutline, ShieldCheckmarkOutline, GitNetworkOutline, SettingsOutline, SpeedometerOutline, ServerOutline } from '@vicons/ionicons5'
import { MENU_CONFIG } from '@/constants'

defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

defineEmits(['toggle'])

const route = useRoute()
const router = useRouter()

const activeKey = computed(() => {
  const path = route.path
  // 先尝试精确匹配完整路径
  for (const item of MENU_CONFIG.sidebar) {
    if (item.children) {
      const child = item.children.find(c => path === c.path)
      if (child) return child.key
    }
    if (path === item.path) return item.key
  }
  
  // 如果没有精确匹配，再尝试前缀匹配
  for (const item of MENU_CONFIG.sidebar) {
    if (item.children) {
      const child = item.children.find(c => path.startsWith(c.path))
      if (child) return child.key
    }
  }
  return null
})

const menuOptions = MENU_CONFIG.sidebar.map(item => {
  const option = {
    label: item.label,
    key: item.key,
    icon: () => h(NIcon, null, { default: () => h(getIcon(item.icon)) })
  }

  if (item.children) {
    option.children = item.children.map(child => ({
      label: child.label,
      key: child.key,
      path: child.path
    }))
  } else {
    option.path = item.path
  }

  return option
})

const handleMenuClick = (key) => {
  const findPath = (items) => {
    for (const item of items) {
      if (item.key === key) return item.path
      if (item.children) {
        const found = findPath(item.children)
        if (found) return found
      }
    }
    return null
  }

  const path = findPath(MENU_CONFIG.sidebar)
  if (path && route.path !== path) {
    router.push(path)
  }
}

const getIcon = (iconName) => {
  const icons = {
    HomeOutline,
    CodeSlashOutline,
    CubeOutline,
    StatsChartOutline,
    ShieldCheckmarkOutline,
    GitNetworkOutline,
    SettingsOutline,
    SpeedometerOutline,
    ServerOutline
  }
  return icons[iconName] || HomeOutline
}
</script>

<style lang="scss">
// 全局覆盖 naive-ui 菜单样式（不要 scoped）
@import '@/styles/layout.scss';

.sidebar {
  display: flex;
  flex-direction: column;
  background: #1e293b;

  .n-menu {
    // 使用 CSS 变量覆盖 naive-ui 默认样式
    --n-item-text-color: #ffffff !important;
    --n-item-text-color-hover: #ffffff !important;
    --n-item-text-color-active: #facc15 !important;
    --n-item-color-hover: rgba(255, 255, 255, 0.15) !important;
    --n-item-color-active: rgba(234, 179, 8, 0.25) !important;
    --n-item-icon-color: rgba(255, 255, 255, 0.85) !important;
    --n-item-icon-color-active: #facc15 !important;
    
    background: transparent;

    .n-menu-item {
      margin: 4px 8px;
      border-radius: 6px;

      // 一级菜单选中状态 - 金色
      &.n-menu-item--selected > .n-menu-item-content {
        font-weight: 600;
      }
    }

    .n-menu-item-content__icon {
      color: inherit;
    }
  }

  .sidebar-header {
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }

  .sidebar-footer {
    border-top-color: rgba(255, 255, 255, 0.1);
  }
}

// 强制覆盖一级菜单的选中颜色（使用更高优先级）
.sidebar .n-menu .n-menu-item.n-menu-item--selected > .n-menu-item-content,
.sidebar .n-menu .n-menu-item.n-menu-item--actived > .n-menu-item-content {
  color: #facc15 !important;
  background: linear-gradient(90deg, rgba(234, 179, 8, 0.25), transparent) !important;
}

.sidebar .n-menu .n-menu-item.n-menu-item--selected > .n-menu-item-content .n-menu-item-content__text,
.sidebar .n-menu .n-menu-item.n-menu-item--actived > .n-menu-item-content .n-menu-item-content__text {
  color: #facc15 !important;
}

.sidebar .n-menu .n-menu-item.n-menu-item--selected > .n-menu-item-content .n-menu-item-content__icon,
.sidebar .n-menu .n-menu-item.n-menu-item--actived > .n-menu-item-content .n-menu-item-content__icon {
  color: #facc15 !important;
}
</style>
