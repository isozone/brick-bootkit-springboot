<template>
  <div class="registry-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">服务注册中心</h1>
        <p class="page-subtitle">查看插件间服务注册、发现与依赖关系</p>
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

    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" class="stat-grid">
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon primary">
              <n-icon size="24"><GitNetworkOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalServices || 0 }}</div>
              <div class="stat-label">已注册服务</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon success">
              <n-icon size="24"><CubeOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalPlugins || 0 }}</div>
              <div class="stat-label">服务插件</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon info">
              <n-icon size="24"><ExtensionPuzzleOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalInterfaces || 0 }}</div>
              <div class="stat-label">服务接口</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon warning">
              <n-icon size="24"><LinkOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalDependencies || 0 }}</div>
              <div class="stat-label">服务依赖</div>
            </div>
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 服务列表 -->
    <n-card title="已注册服务" class="list-card">
      <n-empty v-if="groups.length === 0 && !loading" description="暂无注册服务">
        <template #extra>
          <n-button size="small" type="primary" @click="refreshData">刷新</n-button>
        </template>
      </n-empty>

      <template v-for="group in groups" :key="group.pluginId">
        <n-divider v-if="group.pluginId" title-placement="left">
          <n-tag type="primary" size="small">{{ group.pluginId }}</n-tag>
        </n-divider>
        <n-data-table
          :columns="columns"
          :data="group.services || []"
          :loading="loading"
          :bordered="false"
          size="small"
        />
      </template>
    </n-card>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NCard, NButton, NIcon, NTag, NDataTable, NEmpty, NDivider, useMessage } from 'naive-ui'
import { RefreshOutline, GitNetworkOutline, CubeOutline, ExtensionPuzzleOutline, LinkOutline } from '@vicons/ionicons5'
import { registryApi } from '@/api/services'

const message = useMessage()

const stats = ref({ totalServices: 0, totalPlugins: 0, totalInterfaces: 0, totalDependencies: 0 })
const groups = ref([])
const loading = ref(false)

const stateTypeMap = {
  REGISTERED: 'default',
  INITIALIZING: 'warning',
  READY: 'success',
  ACTIVE: 'success',
  PAUSED: 'warning',
  STOPPED: 'default',
  UNREGISTERED: 'default',
  UNHEALTHY: 'error',
  CIRCUIT_OPEN: 'error',
  OVERLOADED: 'warning'
}

const columns = [
  {
    title: '服务 ID',
    key: 'serviceId',
    minWidth: 200,
    render: (row) => h('span', { class: 'mono' }, row.serviceId || '-')
  },
  {
    title: '接口',
    key: 'serviceInterface',
    minWidth: 200,
    render: (row) => h('span', { class: 'mono' }, row.serviceInterface?.getName?.() || '-')
  },
  {
    title: '实现类',
    key: 'serviceImplementation',
    minWidth: 200,
    render: (row) => h('span', { class: 'mono' }, row.serviceImplementation?.getName?.() || '-')
  },
  { title: '版本', key: 'version', width: 90, render: (row) => row.version || '-' },
  {
    title: '状态',
    key: 'state',
    width: 110,
    render: (row) => h(NTag, { type: stateTypeMap[row.state] || 'default', size: 'small' }, { default: () => row.state || '-' })
  },
  {
    title: '健康',
    key: 'healthy',
    width: 80,
    render: (row) => h(NTag, { type: row.healthy === false ? 'error' : 'success', size: 'small' }, { default: () => row.healthy === false ? '异常' : '正常' })
  },
  { title: '调用次数', key: 'callCount', width: 90, render: (row) => row.callCount ?? 0 },
  { title: '注册时间', key: 'registeredAt', width: 180, render: (row) => row.registeredAt ? formatTime(row.registeredAt) : '-' }
]

const refreshData = async () => {
  loading.value = true
  try {
    const [statsRes, servicesRes] = await Promise.all([
      registryApi.getStatistics(),
      registryApi.getServices()
    ])
    if (statsRes.code === 200) stats.value = statsRes.data || {}
    if (servicesRes.code === 200) groups.value = servicesRes.data || []
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
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
.registry-page {
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

.stat-grid {
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;

  &.primary { background: rgba(37, 99, 235, 0.1); color: #2563eb; }
  &.success { background: rgba(16, 185, 129, 0.1); color: #10b981; }
  &.warning { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
  &.info { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.list-card {
  border-radius: 8px;
}

:deep(.mono) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
}
</style>
