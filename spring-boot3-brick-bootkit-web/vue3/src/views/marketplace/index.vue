<template>
  <div class="marketplace-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">{{ t('marketplace.title') }}</h1>
        <p class="page-subtitle">{{ t('marketplace.subtitle') }}</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="refreshData">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          {{ t('common.refresh') }}
        </n-button>
      </div>
    </div>

    <!-- 插件卡片列表 -->
    <n-empty v-if="plugins.length === 0 && !loading" description="插件市场暂无可用插件（可配置 plugin.web.marketplace-index-url）">
      <template #extra>
        <n-button size="small" type="primary" @click="refreshData">刷新</n-button>
      </template>
    </n-empty>

    <n-grid :cols="3" :x-gap="16" :y-gap="16" v-else>
      <n-gi v-for="plugin in plugins" :key="plugin.pluginId">
        <n-card class="plugin-card" :bordered="true">
          <template #header>
            <div class="plugin-header">
              <n-icon size="22" color="#2563eb"><CubeOutline /></n-icon>
              <span class="plugin-name">{{ plugin.name || plugin.pluginId }}</span>
            </div>
          </template>
          <template #header-extra>
            <n-tag v-if="plugin.installed" :type="stateTagType(plugin.state)" size="small">
              {{ stateText(plugin.state) }}
            </n-tag>
          </template>

          <div class="plugin-body">
            <div class="plugin-meta">
              <n-tag size="small" type="info">{{ plugin.pluginId }}</n-tag>
              <n-tag size="small" type="warning">v{{ plugin.version || '-' }}</n-tag>
            </div>
            <p class="plugin-desc">{{ plugin.description || '暂无描述' }}</p>
            <div v-if="plugin.sizeBytes" class="plugin-size">大小：{{ formatBytes(plugin.sizeBytes) }}</div>
          </div>

          <template #footer>
            <n-button
              type="primary"
              block
              :loading="installingId === plugin.pluginId"
              :disabled="plugin.installed"
              @click="installPlugin(plugin)"
            >
              {{ plugin.installed ? '已安装' : '下载安装' }}
            </n-button>
          </template>
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NCard, NButton, NIcon, NTag, NGrid, NGi, NEmpty, useMessage, useDialog } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { RefreshOutline, CubeOutline } from '@vicons/ionicons5'
import { marketplaceApi } from '@/api/services'

const message = useMessage()
const dialog = useDialog()
const { t } = useI18n()

const loading = ref(false)
const plugins = ref([])
const installingId = ref('')

const stateTagType = (state) => {
  const map = { STARTED: 'success', STOPPED: 'default', LOADED: 'warning', FAILED: 'error' }
  return map[state] || 'default'
}

const stateText = (state) => {
  const map = { STARTED: '运行中', STOPPED: '已停止', LOADED: '已加载', FAILED: '异常' }
  return map[state] || state || '已安装'
}

const refreshData = async () => {
  loading.value = true
  try {
    const res = await marketplaceApi.getList()
    if (res.code === 200) {
      plugins.value = res.data || []
    } else {
      message.error(res.message || '加载失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const installPlugin = (plugin) => {
  dialog.warning({
    title: '确认安装',
    content: `确定从插件市场下载并安装 ${plugin.pluginId} v${plugin.version || '-'} 吗？`,
    positiveText: '安装',
    negativeText: '取消',
    onPositiveClick: async () => {
      installingId.value = plugin.pluginId
      try {
        const res = await marketplaceApi.install(plugin.pluginId, true)
        if (res.code === 200) {
          message.success(res.data || '安装成功')
          refreshData()
        } else {
          message.error(res.message || '安装失败')
        }
      } catch (e) {
        message.error(e.response?.data?.message || '安装失败')
      } finally {
        installingId.value = ''
      }
    }
  })
}

const formatBytes = (bytes) => {
  if (!bytes) return '-'
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`
}

onMounted(refreshData)
</script>

<style lang="scss" scoped>
.marketplace-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;

  .page-title {
    font-size: 24px;
    font-weight: 600;
    color: #1f2937;
    margin: 0;
  }

  .page-subtitle {
    color: #6b7280;
    margin: 4px 0 0 0;
    font-size: 14px;
  }
}

.plugin-card {
  border-radius: 8px;
  margin-bottom: 16px;

  .plugin-header {
    display: flex;
    align-items: center;
    gap: 8px;

    .plugin-name {
      font-weight: 600;
      color: #1f2937;
    }
  }

  .plugin-body {
    min-height: 90px;

    .plugin-meta {
      display: flex;
      gap: 8px;
      margin-bottom: 8px;
    }

    .plugin-desc {
      color: #6b7280;
      font-size: 13px;
      margin: 0 0 8px 0;
      line-height: 1.5;
    }

    .plugin-size {
      color: #9ca3af;
      font-size: 12px;
    }
  }
}
</style>
