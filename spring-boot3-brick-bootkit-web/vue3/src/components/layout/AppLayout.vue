<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <Sidebar :collapsed="collapsed" @toggle="toggleSidebar" />

    <!-- 主容器 -->
    <div class="main-container">
      <!-- 顶部栏 -->
      <Header @toggle-sidebar="toggleSidebar" />

      <!-- 内容区域 -->
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <!-- 底部 -->
      <footer class="footer">
        Brick Bootkit Web © {{ new Date().getFullYear() }} Powered by Vue3 + NaiveUI
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const collapsed = ref(false)

const toggleSidebar = () => {
  collapsed.value = !collapsed.value
}
</script>

<style lang="scss" scoped>
@import '@/styles/layout.scss';
</style>
