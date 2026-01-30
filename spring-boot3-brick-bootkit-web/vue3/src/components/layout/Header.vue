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
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { NIcon, NTooltip } from 'naive-ui'
import {
  MenuOutline,
  RefreshOutline,
  ExpandOutline,
  ContractOutline
} from '@vicons/ionicons5'

defineEmits(['toggle-sidebar'])

const route = useRoute()
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
</script>

<style lang="scss" scoped>
@import '@/styles/layout.scss';
</style>
