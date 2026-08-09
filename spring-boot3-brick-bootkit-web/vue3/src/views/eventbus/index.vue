<template>
  <div class="eventbus-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">{{ $t('eventbus.title') }}</h1>
        <p class="page-subtitle">{{ $t('eventbus.subtitle') }}</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="refreshData">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新
        </n-button>
      </div>
    </div>

    <!-- 事件统计 -->
    <n-card title="事件统计" class="stats-card">
      <template #header-extra>
        <n-tag type="primary" size="small">类型 {{ statsEntries.length }}</n-tag>
      </template>
      <n-empty v-if="statsEntries.length === 0 && !loading" description="暂无事件统计" size="small" />
      <n-data-table
        :columns="statsColumns"
        :data="statsEntries"
        :loading="loading"
        :bordered="false"
        size="small"
      />
    </n-card>

    <!-- 事件类型 -->
    <n-card title="事件类型" class="types-card">
      <n-empty v-if="types.length === 0 && !loading" description="暂无事件类型" size="small" />
      <n-space v-else>
        <n-tag v-for="type in types" :key="type" type="info" size="small">
          <span class="mono">{{ type }}</span>
        </n-tag>
      </n-space>
    </n-card>

    <!-- 最近事件流 -->
    <n-card title="最近事件流" class="recent-card">
      <template #header-extra>
        <n-space>
          <n-tag type="warning" size="small">事件 {{ recentEvents.length }}</n-tag>
          <n-select
            v-model:value="limit"
            :options="limitOptions"
            size="small"
            style="width: 100px;"
            @update:value="loadRecent"
          />
        </n-space>
      </template>
      <n-empty v-if="recentEvents.length === 0 && !loading" description="暂无事件（可通过插件上传/启动/停止触发）" size="small">
        <template #extra>
          <n-button size="small" type="primary" @click="refreshData">刷新</n-button>
        </template>
      </n-empty>
      <n-data-table
        :columns="eventColumns"
        :data="recentEvents"
        :loading="loading"
        :bordered="false"
        size="small"
      />
    </n-card>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NTag, NSelect, NDataTable, NEmpty, NSpace, useMessage
} from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import { eventbusApi } from '@/api/services'

const message = useMessage()

const loading = ref(false)
const stats = ref({})
const types = ref([])
const recentEvents = ref([])
const limit = ref(50)

const limitOptions = [
  { label: '50', value: 50 },
  { label: '100', value: 100 },
  { label: '200', value: 200 }
]

const statsEntries = computed(() =>
  Object.entries(stats.value || {}).map(([key, count]) => {
    const [type, pluginId] = key.split('.')
    return { type, pluginId, count }
  })
)

const statsColumns = [
  { title: '事件类型', key: 'type', minWidth: 160, render: (row) => h('span', { class: 'mono' }, row.type || '-') },
  { title: '来源插件', key: 'pluginId', minWidth: 140, render: (row) => h('span', { class: 'mono' }, row.pluginId || '-') },
  { title: '次数', key: 'count', width: 80, render: (row) => row.count ?? 0 }
]

const eventTypeTag = {
  PLUGIN_INSTALLED: 'success',
  PLUGIN_UNINSTALLED: 'default',
  PLUGIN_STARTING: 'info',
  PLUGIN_STARTED: 'success',
  PLUGIN_STOPPING: 'warning',
  PLUGIN_STOPPED: 'default',
  SERVICE_REGISTERED: 'success',
  SERVICE_UNREGISTERED: 'default',
  SERVICE_CHANGED: 'warning',
  CONFIG_CHANGED: 'warning',
  CONFIG_RELOADED: 'info',
  PLUGIN_ERROR: 'error',
  PLUGIN_RECOVERED: 'success',
  CUSTOM: 'primary'
}

const eventColumns = [
  {
    title: '事件类型',
    key: 'type',
    minWidth: 160,
    render: (row) => h(NTag, { type: eventTypeTag[row.type] || 'default', size: 'small' }, { default: () => row.type || '-' })
  },
  { title: '来源插件', key: 'sourcePluginId', minWidth: 140, render: (row) => h('span', { class: 'mono' }, row.sourcePluginId || '-') },
  { title: '目标插件', key: 'targetPluginId', minWidth: 140, render: (row) => row.targetPluginId ? h('span', { class: 'mono' }, row.targetPluginId) : '广播' },
  {
    title: '类型',
    key: 'broadcast',
    width: 80,
    render: (row) => h(NTag, { type: row.broadcast ? 'info' : 'warning', size: 'small' }, { default: () => row.broadcast ? '广播' : '定向' })
  },
  { title: '时间', key: 'timeText', minWidth: 180, render: (row) => formatTime(row.timestamp) }
]

const refreshData = async () => {
  loading.value = true
  try {
    const [statsRes, typesRes, recentRes] = await Promise.all([
      eventbusApi.getStats(),
      eventbusApi.getTypes(),
      eventbusApi.getRecent(limit.value)
    ])
    if (statsRes.code === 200) stats.value = statsRes.data || {}
    if (typesRes.code === 200) types.value = typesRes.data || []
    if (recentRes.code === 200) {
      recentEvents.value = (recentRes.data || []).map(e => ({ ...e, timeText: formatTime(e.timestamp) }))
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadRecent = async () => {
  try {
    const res = await eventbusApi.getRecent(limit.value)
    if (res.code === 200) {
      recentEvents.value = (res.data || []).map(e => ({ ...e, timeText: formatTime(e.timestamp) }))
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
  }
}

const formatTime = (ts) => {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(refreshData)
</script>

<style lang="scss" scoped>
.eventbus-page {
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

.stats-card,
.types-card,
.recent-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

:deep(.mono) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
}
</style>
