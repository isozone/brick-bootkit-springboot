<template>
  <div class="performance-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">{{ $t('performance.title') }}</h1>
        <p class="page-subtitle">{{ $t('performance.subtitle') }}</p>
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

    <!-- 系统资源卡片 -->
    <n-grid :cols="3" :x-gap="16" :y-gap="16" class="system-grid">
      <n-gi>
        <n-card title="系统内存" class="system-card">
          <n-descriptions :column="1" size="small" label-placement="left">
            <n-descriptions-item label="总内存">{{ formatBytes(systemInfo.totalMemory) }}</n-descriptions-item>
            <n-descriptions-item label="已用内存">{{ formatBytes(systemInfo.usedMemory) }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="系统线程" class="system-card">
          <n-descriptions :column="1" size="small" label-placement="left">
            <n-descriptions-item label="总线程">{{ systemInfo.totalThreads || '-' }}</n-descriptions-item>
            <n-descriptions-item label="已用线程">{{ systemInfo.usedThreads || '-' }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="资源监控摘要" class="system-card">
          <n-descriptions :column="1" size="small" label-placement="left">
            <n-descriptions-item label="监控插件数">{{ summary.pluginCount || 0 }}</n-descriptions-item>
            <n-descriptions-item label="总内存占用">{{ formatBytes(summary.totalMemoryUsage) }}</n-descriptions-item>
            <n-descriptions-item label="总线程数">{{ summary.totalThreadCount || 0 }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 插件性能评分 -->
    <n-card title="插件性能评分" class="score-card">
      <n-empty v-if="scoreEntries.length === 0 && !loading" description="暂无插件性能数据" size="small" />
      <n-data-table
        :columns="scoreColumns"
        :data="scoreEntries"
        :loading="loading"
        :bordered="false"
        size="small"
      />
    </n-card>

    <!-- 资源占用详情 -->
    <n-card title="插件资源占用" class="usage-card">
      <n-empty v-if="usageEntries.length === 0 && !loading" description="暂无资源占用数据" size="small" />
      <n-data-table
        :columns="usageColumns"
        :data="usageEntries"
        :loading="loading"
        :bordered="false"
        size="small"
      />
    </n-card>

    <!-- 性能分析详情弹窗 -->
    <n-modal v-model:show="showAnalysis" preset="card" :title="`性能分析 - ${analysisPluginId}`" style="width: 720px;">
      <template v-if="analysis">
        <n-descriptions :column="2" label-placement="left" bordered size="small">
          <n-descriptions-item label="插件 ID">{{ analysis.pluginId || '-' }}</n-descriptions-item>
          <n-descriptions-item label="性能评分">
            <n-tag :type="scoreTagType(analysis.performanceScore)" size="small">
              {{ analysis.performanceScore?.toFixed?.(2) ?? '-' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="状态">
            <n-tag :type="analysis.status === 'HEALTHY' ? 'success' : analysis.status === 'WARNING' ? 'warning' : 'error'" size="small">
              {{ analysis.status || '-' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="分析时间">{{ formatTime(analysis.analysisTime) }}</n-descriptions-item>
        </n-descriptions>

        <n-divider title-placement="left">问题列表</n-divider>
        <n-data-table
          v-if="analysis.issues?.length"
          :columns="issueColumns"
          :data="analysis.issues"
          :bordered="false"
          size="small"
        />
        <n-empty v-else description="无性能问题" size="small" />

        <n-divider title-placement="left">优化建议</n-divider>
        <n-data-table
          v-if="analysis.recommendations?.length"
          :columns="recommendationColumns"
          :data="analysis.recommendations"
          :bordered="false"
          size="small"
        />
        <n-empty v-else description="暂无优化建议" size="small" />
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NTag, NDataTable, NEmpty, NModal, NDivider,
  NDescriptions, NDescriptionsItem, useMessage
} from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import { performanceApi } from '@/api/services'

const message = useMessage()

const loading = ref(false)
const systemInfo = ref({})
const summary = ref({})
const scores = ref({})
const usageMap = ref({})

const showAnalysis = ref(false)
const analysisPluginId = ref('')
const analysis = ref(null)

const scoreEntries = computed(() =>
  Object.entries(scores.value || {}).map(([pluginId, score]) => ({ pluginId, score }))
)

const usageEntries = computed(() =>
  Object.entries(usageMap.value || {}).map(([pluginId, usage]) => ({ pluginId, ...usage }))
)

const scoreColumns = [
  { title: '插件 ID', key: 'pluginId', minWidth: 160, render: (row) => h('span', { class: 'mono' }, row.pluginId || '-') },
  {
    title: '性能评分',
    key: 'score',
    width: 160,
    render: (row) => {
      const score = row.score ?? 0
      return h(NTag, { type: score >= 80 ? 'success' : score >= 60 ? 'warning' : 'error', size: 'small' }, { default: () => score.toFixed?.(2) ?? score })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (row) => h(NButton, { size: 'small', type: 'primary', ghost: true, onClick: () => openAnalysis(row.pluginId) }, { default: () => '分析' })
  }
]

const usageColumns = [
  { title: '插件 ID', key: 'pluginId', minWidth: 160, render: (row) => h('span', { class: 'mono' }, row.pluginId || '-') },
  { title: '内存(MB)', key: 'memoryMB', width: 100, render: (row) => formatMB(row.currentMemoryUsage) },
  { title: '峰值内存(MB)', key: 'peakMemoryMB', width: 110, render: (row) => formatMB(row.peakMemoryUsage) },
  { title: '线程数', key: 'currentThreadCount', width: 80, render: (row) => row.currentThreadCount ?? 0 },
  { title: '文件描述符', key: 'currentFileDescriptors', width: 90, render: (row) => row.currentFileDescriptors ?? 0 },
  { title: 'CPU 使用', key: 'currentCpuUsage', width: 90, render: (row) => `${row.currentCpuUsage ?? 0}%` },
  { title: '网络连接', key: 'currentNetworkConnections', width: 90, render: (row) => row.currentNetworkConnections ?? 0 },
  {
    title: '配额超限',
    key: 'quotaExceeded',
    width: 90,
    render: (row) => h(NTag, { type: row.quotaExceeded ? 'error' : 'success', size: 'small' }, { default: () => row.quotaExceeded ? '是' : '否' })
  }
]

const issueColumns = [
  {
    title: '级别',
    key: 'severity',
    width: 90,
    render: (row) => h(NTag, {
      type: row.severity === 'CRITICAL' ? 'error' : row.severity === 'WARNING' ? 'warning' : 'info',
      size: 'small'
    }, { default: () => row.severity || '-' })
  },
  { title: '编码', key: 'code', width: 160, render: (row) => h('span', { class: 'mono' }, row.code || '-') },
  { title: '描述', key: 'description' }
]

const recommendationColumns = [
  {
    title: '优先级',
    key: 'priority',
    width: 100,
    render: (row) => h(NTag, {
      type: row.priority === 'HIGH' ? 'error' : row.priority === 'MEDIUM' ? 'warning' : 'info',
      size: 'small'
    }, { default: () => row.priority || '-' })
  },
  { title: '建议', key: 'suggestion' },
  { title: '原因', key: 'reason', render: (row) => row.reason || '-' }
]

const scoreTagType = (score) => {
  if (score == null) return 'default'
  return score >= 80 ? 'success' : score >= 60 ? 'warning' : 'error'
}

const refreshData = async () => {
  loading.value = true
  try {
    const [systemRes, summaryRes, scoresRes, usageRes] = await Promise.all([
      performanceApi.getSystem(),
      performanceApi.getSummary(),
      performanceApi.getScores(),
      performanceApi.getAllUsage()
    ])
    if (systemRes.code === 200) systemInfo.value = systemRes.data || {}
    if (summaryRes.code === 200) summary.value = summaryRes.data || {}
    if (scoresRes.code === 200) scores.value = scoresRes.data || {}
    if (usageRes.code === 200) usageMap.value = usageRes.data || {}
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openAnalysis = async (pluginId) => {
  analysisPluginId.value = pluginId
  showAnalysis.value = true
  analysis.value = null
  try {
    const res = await performanceApi.analyze(pluginId)
    if (res.code === 200) {
      analysis.value = res.data
    } else {
      message.error(res.message || '分析失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '分析失败')
  }
}

const formatBytes = (bytes) => {
  if (bytes == null) return '-'
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`
}

const formatMB = (bytes) => {
  if (bytes == null) return '-'
  return (bytes / (1024 * 1024)).toFixed(2)
}

const formatTime = (t) => {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 19)
}

onMounted(refreshData)
</script>

<style lang="scss" scoped>
.performance-page {
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

.system-grid {
  margin-bottom: 24px;
}

.system-card,
.score-card,
.usage-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

:deep(.mono) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
}
</style>
