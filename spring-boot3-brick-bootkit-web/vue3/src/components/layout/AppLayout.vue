<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <Sidebar v-if="!isEmbedded" :collapsed="collapsed" @toggle="toggleSidebar" />

    <!-- 主容器 -->
    <div class="main-container" :class="{ 'fullscreen': isEmbedded }">
      <!-- 顶部栏 -->
      <Header v-if="!isEmbedded" @toggle-sidebar="toggleSidebar" />

      <!-- 内容区域 -->
      <main class="content">
        <router-view />
      </main>

      <!-- 底部 -->
      <footer v-if="!isEmbedded" class="footer">
        Brick Bootkit Web © {{ new Date().getFullYear() }} Powered by Vue3 + NaiveUI
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const collapsed = ref(false)

const isEmbedded = computed(() => {
  const params = new URLSearchParams(window.location.search)
  return params.get('embedded') === 'true'
})

const toggleSidebar = () => {
  collapsed.value = !collapsed.value
}
</script>

<style lang="scss" scoped>
@import '@/styles/layout.scss';

.app-layout {
  .main-container {
    &.fullscreen {
      .content {
        height: 100vh;
        padding: 0;
      }
    }
  }
}
</style>
