<template>
  <header class="header">
    <div class="header-left">
      <div class="toggle-btn" @click="$emit('toggle-sidebar')">
        <n-icon size="20">
          <MenuOutline />
        </n-icon>
      </div>

      <div class="breadcrumb">
        <span>{{ getBreadcrumb(route.path) }}</span>
      </div>
    </div>

    <div class="header-right">
      <!-- 刷新按钮 -->
      <div class="header-action" @click="refreshPage">
        <n-tooltip>
          <template #trigger>
            <n-icon size="20">
              <RefreshOutline />
            </n-icon>
          </template>
          刷新页面
        </n-tooltip>
      </div>

      <!-- 全屏按钮 -->
      <div class="header-action" @click="toggleFullscreen">
        <n-tooltip>
          <template #trigger>
            <n-icon size="20">
              <ExpandOutline v-if="!isFullscreen" />
              <ContractOutline v-else />
            </n-icon>
          </template>
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </n-tooltip>
      </div>

      <!-- 用户下拉菜单 -->
      <n-dropdown :options="userMenuOptions" @select="handleUserAction">
        <div class="user-dropdown">
          <div class="user-avatar">管</div>
          <div class="user-info">
            <div class="user-name">管理员</div>
            <div class="user-role">超级管理员</div>
          </div>
          <n-icon size="16">
            <ChevronDownOutline />
          </n-icon>
        </div>
      </n-dropdown>
    </div>
  </header>
</template>

<script setup>
import { ref, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon, NTooltip, NDropdown } from 'naive-ui'
import {
  MenuOutline,
  RefreshOutline,
  ExpandOutline,
  ContractOutline,
  ChevronDownOutline,
  PersonOutline,
  SettingsOutline,
  LogOutOutline
} from '@vicons/ionicons5'
import { MENU_CONFIG } from '@/constants'

defineEmits(['toggle-sidebar'])

const route = useRoute()
const router = useRouter()
const isFullscreen = ref(false)

const getBreadcrumb = (path) => {
  const paths = path.split('/').filter(Boolean)
  if (paths.length === 0) return '首页'

  const labels = {
    scripts: '脚本管理',
    plugins: '插件管理',
    monitor: '系统监控',
    editor: '编辑器',
    templates: '脚本模板',
    scheduler: '定时任务',
    executions: '执行记录',
    upload: '上传插件',
    cpu: 'CPU监控',
    memory: '内存监控',
    threads: '线程监控'
  }

  return labels[paths[paths.length - 1]] || paths[paths.length - 1]
}

const refreshPage = () => {
  window.location.reload()
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

const userMenuOptions = [
  {
    label: '个人中心',
    key: 'profile',
    icon: () => h(NIcon, null, { default: () => h(PersonOutline) })
  },
  {
    label: '系统设置',
    key: 'settings',
    icon: () => h(NIcon, null, { default: () => h(SettingsOutline) })
  },
  { type: 'divider' },
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h(NIcon, null, { default: () => h(LogOutOutline) })
  }
]

const handleUserAction = (key) => {
  switch (key) {
    case 'profile':
      // 跳转到个人中心
      break
    case 'settings':
      // 跳转到系统设置
      break
    case 'logout':
      // 退出登录
      break
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/layout.scss';
</style>
