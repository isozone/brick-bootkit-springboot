<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-notification-provider>
        <n-dialog-provider>
          <n-loading-bar-provider>
            <AppLayout />
          </n-loading-bar-provider>
        </n-dialog-provider>
      </n-notification-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import AppLayout from '@/components/layout/AppLayout.vue'
import { themeOverrides } from '@/constants'
</script>

<style lang="scss">
#app {
  height: 100vh;
  width: 100vw;
}
</style>
