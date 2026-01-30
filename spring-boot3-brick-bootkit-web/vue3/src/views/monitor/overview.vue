<template>
  <div class="monitor-overview">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">系统监控</h1>
        <p class="page-subtitle">实时监控服务器运行状态</p>
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

    <!-- 监控卡片 -->
    <n-grid :cols="3" :x-gap="16" :y-gap="16" class="monitor-grid">
      <!-- CPU 监控 -->
      <n-gi>
        <n-card title="CPU 使用率" class="monitor-card">
          <template #header-extra>
            <n-tag :type="cpuPercent > 80 ? 'error' : cpuPercent > 60 ? 'warning' : 'success'" size="small">
              {{ cpuPercent }}%
            </n-tag>
          </template>
          <div ref="cpuChartRef" class="chart-container"></div>
          <n-descriptions :column="1" size="small" label-placement="left">
            <n-descriptions-item label="核心数">{{ cpuInfo.availableProcessors || '-' }}</n-descriptions-item>
            <n-descriptions-item label="系统负载">{{ cpuInfo.systemPercent?.toFixed(2) || '-' }}%</n-descriptions-item>
            <n-descriptions-item label="进程 CPU">{{ cpuInfo.processPercent?.toFixed(2) || '-' }}%</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>

          <!-- 内存监控 -->
          <n-gi>
              <n-card title="内存使用" class="monitor-card">
                <template #header-extra>
                  <n-tag :type="heapPercent > 80 ? 'error' : heapPercent > 60 ? 'warning' : 'success'" size="small">
                    {{ heapPercent }}%
                  </n-tag>
                </template>
                <div ref="memoryChartRef" class="chart-container"></div>
                <n-descriptions :column="1" size="small" label-placement="left">
                  <n-descriptions-item :label="formatBytes(heapUsed) + ' / ' + formatBytes(heapMax)">
                    JVM 堆内存
                  </n-descriptions-item>
                  <n-descriptions-item :label="formatBytes(nonHeapUsed)">
                    非堆内存
                  </n-descriptions-item>
                </n-descriptions>
              </n-card>
            </n-gi>
      
            <!-- 系统内存 -->
            <n-gi>
              <n-card title="系统内存" class="monitor-card">
                <template #header-extra>
                  <n-tag :type="systemMemoryPercent > 80 ? 'error' : systemMemoryPercent > 60 ? 'warning' : 'success'" size="small">
                    {{ systemMemoryPercent }}%
                  </n-tag>
                </template>
                <div ref="systemMemoryChartRef" class="chart-container"></div>
                <n-descriptions :column="1" size="small" label-placement="left">
                  <n-descriptions-item :label="formatBytes(systemMemoryUsed) + ' / ' + formatBytes(systemMemoryTotal)">
                    已用 / 总计
                  </n-descriptions-item>
                  <n-descriptions-item :label="formatBytes(systemMemoryFree)">
                    空闲内存
                  </n-descriptions-item>
                </n-descriptions>
              </n-card>
            </n-gi>
          </n-grid>
    <!-- 系统信息 -->
    <n-card title="系统信息" class="info-card">
      <n-descriptions :column="4" label-placement="left" bordered>
        <n-descriptions-item label="操作系统">{{ systemInfo.osName || '-' }}</n-descriptions-item>
        <n-descriptions-item label="系统架构">{{ systemInfo.osArch || '-' }}</n-descriptions-item>
        <n-descriptions-item label="系统版本">{{ systemInfo.osVersion || '-' }}</n-descriptions-item>
        <n-descriptions-item label="Java 版本">{{ systemInfo.javaVersion || '-' }}</n-descriptions-item>
        <n-descriptions-item label="运行时长">{{ formatUptime(systemInfo.uptime) }}</n-descriptions-item>
      </n-descriptions>
    </n-card>

    <!-- 线程信息 -->
    <n-card title="线程信息" class="thread-card">
      <template #header-extra>
        <n-button text type="primary" @click="$router.push('/monitor/threads')">
          查看详情 >
        </n-button>
      </template>
      <n-descriptions :column="4" label-placement="left" bordered>
        <n-descriptions-item label="活动线程">{{ threadInfo.total || '-' }}</n-descriptions-item>
        <n-descriptions-item label="守护线程">{{ threadInfo.daemon || '-' }}</n-descriptions-item>
        <n-descriptions-item label="峰值线程">{{ threadInfo.peak || '-' }}</n-descriptions-item>
        <n-descriptions-item label="已启动线程">{{ threadInfo.started || '-' }}</n-descriptions-item>
        <n-descriptions-item label="可运行">{{ threadInfo.runnableCount || '-' }}</n-descriptions-item>
        <n-descriptions-item label="等待中">{{ threadInfo.waitingCount || '-' }}</n-descriptions-item>
        <n-descriptions-item label="定时等待">{{ threadInfo.timedWaitingCount || '-' }}</n-descriptions-item>
        <n-descriptions-item label="阻塞">{{ threadInfo.blockedCount || '-' }}</n-descriptions-item>
      </n-descriptions>
    </n-card>

    <!-- 插件统计 -->
    <n-card title="插件统计" class="plugin-stat-card">
      <template #header-extra>
        <n-button text type="primary" @click="$router.push('/plugins')">
          管理插件 >
        </n-button>
      </template>
      <n-descriptions :column="4" label-placement="left" bordered>
        <n-descriptions-item label="总插件数">
          <n-tag type="info">{{ pluginStatistics.total || 0 }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="运行中">
          <n-tag type="success">{{ pluginStatistics.started || 0 }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="已停止">
          <n-tag type="warning">{{ pluginStatistics.stopped || 0 }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="启动失败">
          <n-tag type="error">{{ pluginStatistics.failed || 0 }}</n-tag>
        </n-descriptions-item>
      </n-descriptions>
    </n-card>

    <!-- GC 收集器 -->
    <n-card title="GC 统计" class="gc-card">
      <n-data-table
        :columns="gcColumns"
        :data="gcInfo"
        :bordered="false"
        size="small"
        :pagination="false"
      />
    </n-card>

    <!-- 内存池详情 -->
    <n-card title="内存池详情" class="memory-pools-card">
      <n-data-table
        :columns="memoryPoolColumns"
        :data="memoryPools"
        :bordered="false"
        size="small"
        :pagination="false"
      />
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import {
  NCard, NGrid, NGi, NButton, NIcon, NTag, NDescriptions,
  NDescriptionsItem, NDataTable, useMessage
} from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import * as echarts from 'echarts'
import { monitorApi } from '@/api/services'

const message = useMessage()

// CPU 信息
const cpuInfo = ref({ availableProcessors: 0, systemPercent: 0 })
const cpuPercent = ref(0)

// 内存信息
const heapUsed = ref(0)
const heapMax = ref(1)
const heapPercent = ref(0)
const nonHeapUsed = ref(0)
const systemMemoryTotal = ref(0)
const systemMemoryUsed = ref(0)
const systemMemoryFree = ref(0)
const systemMemoryPercent = ref(0)
const memoryPools = ref([])

// GC 表格列
const gcColumns = [
  { title: '收集器名称', key: 'name' },
  { title: '执行次数', key: 'count' },
  { title: '总耗时(ms)', key: 'time' },
  { title: '最大单次耗时(ms)', key: 'maxTime' }
]

// 内存池表格列
const memoryPoolColumns = [
  { title: '内存池名称', key: 'name' },
  { 
    title: '已使用', 
    key: 'used',
    render: (row) => formatBytes(row.used)
  },
  { 
    title: '最大', 
    key: 'max',
    render: (row) => row.max > 0 ? formatBytes(row.max) : '无限制'
  },
  {
    title: '使用率',
    key: 'usagePercent',
    render: (row) => {
      if (row.max > 0) {
        const percent = ((row.used / row.max) * 100).toFixed(1)
        return `${percent}%`
      }
      return '-'
    }
  }
]

// 系统信息
const systemInfo = ref({})
const threadInfo = ref({})
const pluginStatistics = ref({})
const gcInfo = ref([])

// 图表引用
const cpuChartRef = ref(null)
const memoryChartRef = ref(null)
const systemMemoryChartRef = ref(null)

let cpuChart = null
let memoryChart = null
let systemMemoryChart = null
let refreshTimer = null

// 获取监控数据
const loadData = async () => {
  try {
    // 获取概览数据
    const overviewRes = await monitorApi.getOverview()
    if (overviewRes.code === 200 && overviewRes.data) {
      const d = overviewRes.data
      
      // CPU 信息 - 后端返回 d.cpu
      if (d.cpu) {
        cpuInfo.value = d.cpu
        cpuPercent.value = parseFloat(d.cpu.systemPercent?.toFixed(1) || 0)
      }
      
      // 内存信息 - 后端返回 d.memory
      if (d.memory) {
        heapUsed.value = d.memory.heapUsed || 0
        heapMax.value = d.memory.heapMax || 1
        heapPercent.value = parseFloat(d.memory.heapUsedPercent?.toFixed(1) || 0)
        nonHeapUsed.value = d.memory.nonHeapUsed || 0
        
        systemMemoryTotal.value = d.memory.systemTotal || 0
        systemMemoryUsed.value = d.memory.systemUsed || 0
        systemMemoryFree.value = d.memory.systemFree || 0
        systemMemoryPercent.value = parseFloat(d.memory.systemUsedPercent?.toFixed(1) || 0)
        
        // 内存池详情
        if (d.memory.memoryPools) {
          memoryPools.value = Object.values(d.memory.memoryPools).map(pool => ({
            name: pool.name,
            used: pool.used,
            max: pool.max
          }))
        }
      }
      
      // 系统信息 - 后端返回 d.system
      if (d.system) {
        systemInfo.value = d.system
      }
      
      // 线程信息 - 后端返回 d.threads
      if (d.threads) {
        threadInfo.value = d.threads
      }
      
      // 插件统计 - 后端返回 d.pluginStatistics
      if (d.pluginStatistics) {
        pluginStatistics.value = d.pluginStatistics
      }
      
      // GC 收集器 - 后端返回 d.gcCollectors
      if (d.gcCollectors) {
        gcInfo.value = d.gcCollectors
      }
      
      updateCharts()
    }
  } catch (e) {
    console.error('获取监控数据失败:', e)
  }
}

// 更新图表
const updateCharts = () => {
  if (cpuChart) {
    cpuChart.setOption({
      series: [{ data: [{ value: cpuPercent.value }] }]
    })
  }
  if (memoryChart) {
    memoryChart.setOption({
      series: [{ data: [{ value: heapPercent.value }] }]
    })
  }
  if (systemMemoryChart) {
    systemMemoryChart.setOption({
      series: [{ data: [{ value: systemMemoryPercent.value }] }]
    })
  }
}

// 初始化图表
const initCharts = () => {
  // CPU 仪表盘
  if (cpuChartRef.value) {
    cpuChart = echarts.init(cpuChartRef.value)
    cpuChart.setOption({
      series: [{
        type: 'gauge',
        min: 0,
        max: 100,
        radius: '90%',
        progress: { show: true, width: 12, itemStyle: { color: '#2563eb' } },
        axisLine: { lineStyle: { width: 12, color: [[1, '#e5e7eb']] } },
        axisTick: { show: false },
        splitLine: { length: 8, lineStyle: { width: 2, color: '#999' } },
        axisLabel: { distance: 20, color: '#6b7280', fontSize: 12 },
        anchor: { show: true, size: 20, itemStyle: { color: '#2563eb', borderWidth: 3 } },
        title: { show: false },
        detail: {
          valueAnimation: true,
          fontSize: 20,
          fontWeight: 'bold',
          formatter: '{value}%',
          offsetCenter: [0, '70%'],
          color: '#1f2937'
        },
        data: [{ value: 0 }]
      }]
    })
  }
  
  // JVM 内存仪表盘
  if (memoryChartRef.value) {
    memoryChart = echarts.init(memoryChartRef.value)
    memoryChart.setOption({
      series: [{
        type: 'gauge',
        min: 0,
        max: 100,
        radius: '90%',
        progress: { show: true, width: 12, itemStyle: { color: '#10b981' } },
        axisLine: { lineStyle: { width: 12, color: [[1, '#e5e7eb']] } },
        axisTick: { show: false },
        splitLine: { length: 8, lineStyle: { width: 2, color: '#999' } },
        axisLabel: { distance: 20, color: '#6b7280', fontSize: 12 },
        anchor: { show: true, size: 20, itemStyle: { color: '#10b981', borderWidth: 3 } },
        title: { show: false },
        detail: {
          valueAnimation: true,
          fontSize: 20,
          fontWeight: 'bold',
          formatter: '{value}%',
          offsetCenter: [0, '70%'],
          color: '#1f2937'
        },
        data: [{ value: 0 }]
      }]
    })
  }
  
  // 系统内存仪表盘
  if (systemMemoryChartRef.value) {
    systemMemoryChart = echarts.init(systemMemoryChartRef.value)
    systemMemoryChart.setOption({
      series: [{
        type: 'gauge',
        min: 0,
        max: 100,
        radius: '90%',
        progress: { show: true, width: 12, itemStyle: { color: '#06b6d4' } },
        axisLine: { lineStyle: { width: 12, color: [[1, '#e5e7eb']] } },
        axisTick: { show: false },
        splitLine: { length: 8, lineStyle: { width: 2, color: '#999' } },
        axisLabel: { distance: 20, color: '#6b7280', fontSize: 12 },
        anchor: { show: true, size: 20, itemStyle: { color: '#06b6d4', borderWidth: 3 } },
        title: { show: false },
        detail: {
          valueAnimation: true,
          fontSize: 20,
          fontWeight: 'bold',
          formatter: '{value}%',
          offsetCenter: [0, '70%'],
          color: '#1f2937'
        },
        data: [{ value: 0 }]
      }]
    })
  }
}

// 格式化字节
const formatBytes = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 格式化运行时长
const formatUptime = (seconds) => {
  if (!seconds) return '-'
  const days = Math.floor(seconds / (24 * 3600))
  const hours = Math.floor((seconds % (24 * 3600)) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  
  if (days > 0) return `${days}天 ${hours}小时 ${minutes}分钟`
  if (hours > 0) return `${hours}小时 ${minutes}分钟`
  return `${minutes}分钟`
}

// 刷新数据
const refreshData = () => {
  loadData()
  message.success('数据已刷新')
}

// 窗口大小变化
const handleResize = () => {
  if (cpuChart) cpuChart.resize()
  if (memoryChart) memoryChart.resize()
  if (systemMemoryChart) systemMemoryChart.resize()
}

onMounted(() => {
  nextTick(() => {
    initCharts()
    loadData()
  })
  
  // 每5秒刷新数据
  refreshTimer = setInterval(loadData, 5000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
  
  if (cpuChart) cpuChart.dispose()
  if (memoryChart) memoryChart.dispose()
  if (systemMemoryChart) systemMemoryChart.dispose()
})
</script>

<style lang="scss" scoped>
.monitor-overview {
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

.monitor-grid {
  margin-bottom: 24px;
}

.monitor-card {
  border-radius: 8px;
}

.chart-container {
  height: 180px;
  margin-bottom: 16px;
}

.info-card, .thread-card, .plugin-stat-card, .gc-card, .memory-pools-card {
  border-radius: 8px;
  margin-bottom: 24px;
}
</style>
