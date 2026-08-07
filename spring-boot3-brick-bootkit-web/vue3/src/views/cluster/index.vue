<template>
  <div class="cluster-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">集群管理</h1>
        <p class="page-subtitle">集群节点状态、插件状态同步与分布式锁管理</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="syncStates">
          <template #icon>
            <n-icon><SyncOutline /></n-icon>
          </template>
          同步本节点插件状态
        </n-button>
        <n-button @click="refreshData">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新
        </n-button>
      </div>
    </div>

    <!-- 当前节点信息 -->
    <n-card title="当前节点" class="current-card">
      <n-descriptions :column="4" label-placement="left" bordered size="small">
        <n-descriptions-item label="节点 ID">
          <span class="mono">{{ currentNode.nodeId || '-' }}</span>
        </n-descriptions-item>
        <n-descriptions-item label="主机名">{{ currentNode.host || '-' }}</n-descriptions-item>
        <n-descriptions-item label="启动时间">{{ formatTime(currentNode.startedAt) }}</n-descriptions-item>
        <n-descriptions-item label="本节点插件数">{{ currentNode.pluginCount ?? localPluginCount ?? 0 }}</n-descriptions-item>
      </n-descriptions>
    </n-card>

    <!-- 集群节点列表 -->
    <n-card title="集群节点" class="nodes-card">
      <template #header-extra>
        <n-tag type="success" size="small">在线 {{ nodes.length }}</n-tag>
      </template>
      <n-empty v-if="nodes.length === 0 && !loading" description="暂无在线节点（可能未启用集群模式）">
        <template #extra>
          <n-button size="small" type="primary" @click="refreshData">刷新</n-button>
        </template>
      </n-empty>
      <n-data-table
        :columns="nodeColumns"
        :data="nodes"
        :loading="loading"
        :bordered="false"
        size="small"
      />
    </n-card>

    <!-- 集群插件状态 -->
    <n-card title="集群插件状态" class="states-card">
      <template #header-extra>
        <n-tag type="primary" size="small">插件 {{ pluginStates.length }}</n-tag>
      </template>
      <n-empty v-if="pluginStates.length === 0 && !loading" description="暂无插件状态数据">
        <template #extra>
          <n-button size="small" type="primary" @click="syncStates">立即同步</n-button>
        </template>
      </n-empty>
      <n-data-table
        :columns="stateColumns"
        :data="pluginStates"
        :loading="loading"
        :bordered="false"
        size="small"
      />
    </n-card>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NTag, NDataTable, NEmpty, NDescriptions,
  NDescriptionsItem, useMessage
} from 'naive-ui'
import { RefreshOutline, SyncOutline } from '@vicons/ionicons5'
import { clusterApi } from '@/api/services'

const message = useMessage()

const loading = ref(false)
const currentNode = ref({})
const nodes = ref([])
const pluginStates = ref([])
const localPluginCount = ref(0)

const stateTypeMap = {
  STARTED: 'success',
  STOPPED: 'default',
  INSTALLED: 'warning',
  LOADED: 'warning',
  FAILED: 'error',
  UNKNOWN: 'default'
}

const nodeColumns = [
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) => h(NTag, { type: row.status === 'ONLINE' ? 'success' : 'default', size: 'small' }, { default: () => row.status || '-' })
  },
  { title: '节点 ID', key: 'nodeId', minWidth: 120, render: (row) => h('span', { class: 'mono' }, row.nodeId || '-') },
  { title: '主机名', key: 'host', minWidth: 140, render: (row) => row.host || '-' },
  { title: '插件数', key: 'pluginCount', width: 90, render: (row) => row.pluginCount ?? 0 },
  { title: '启动时间', key: 'startedAt', width: 180, render: (row) => formatTime(row.startedAt) },
  { title: '最后心跳', key: 'lastHeartbeat', width: 180, render: (row) => formatTime(row.lastHeartbeat) }
]

const stateColumns = [
  {
    title: '状态',
    key: 'state',
    width: 110,
    render: (row) => h(NTag, { type: stateTypeMap[row.state] || 'default', size: 'small' }, { default: () => row.state || '-' })
  },
  { title: '插件 ID', key: 'pluginId', minWidth: 160, render: (row) => h('span', { class: 'mono' }, row.pluginId || '-') },
  { title: '所在节点', key: 'nodeId', minWidth: 120, render: (row) => h('span', { class: 'mono' }, row.nodeId || '-') },
  { title: '更新时间', key: 'updatedAt', width: 180, render: (row) => formatTime(row.updatedAt) }
]

const refreshData = async () => {
  loading.value = true
  try {
    const [overviewRes, nodesRes, statesRes, currentRes] = await Promise.all([
      clusterApi.getOverview(),
      clusterApi.getNodes(),
      clusterApi.getPluginStates(),
      clusterApi.getCurrentNode()
    ])
    if (overviewRes.code === 200) {
      const overview = overviewRes.data || {}
      nodes.value = overview.nodes || []
      pluginStates.value = overview.pluginStates || []
      localPluginCount.value = overview.localPluginCount ?? 0
    }
    if (nodesRes.code === 200) nodes.value = nodesRes.data || []
    if (statesRes.code === 200) pluginStates.value = statesRes.data || []
    if (currentRes.code === 200) currentNode.value = currentRes.data || {}
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const syncStates = async () => {
  loading.value = true
  try {
    const res = await clusterApi.syncPluginStates()
    if (res.code === 200) {
      message.success(`已同步 ${res.data ?? 0} 个插件状态到集群`)
      refreshData()
    } else {
      message.error(res.message || '同步失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '同步失败')
  } finally {
    loading.value = false
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
.cluster-page {
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

.current-card,
.nodes-card,
.states-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

:deep(.mono) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
}
</style>
