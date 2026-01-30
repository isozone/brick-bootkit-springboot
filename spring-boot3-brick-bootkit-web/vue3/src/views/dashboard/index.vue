<template>
  <div class="dashboard">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">系统仪表盘</h1>
        <p class="page-subtitle">实时监控系统状态和插件运行情况</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="refreshData">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新数据
        </n-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" class="stat-grid">
      <n-gi>
        <n-card class="stat-card stat-primary">
          <div class="stat-content">
            <div class="stat-icon">
              <n-icon size="28"><CubeOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.total || 0 }}</div>
              <div class="stat-label">插件总数</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card stat-success">
          <div class="stat-content">
            <div class="stat-icon">
              <n-icon size="28"><PlayCircleOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.started || 0 }}</div>
              <div class="stat-label">运行中</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card stat-warning">
          <div class="stat-content">
            <div class="stat-icon">
              <n-icon size="28"><StopCircleOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.stopped || 0 }}</div>
              <div class="stat-label">已停止</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card stat-danger">
          <div class="stat-content">
            <div class="stat-icon">
              <n-icon size="28"><AlertCircleOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.failed || 0 }}</div>
              <div class="stat-label">异常状态</div>
            </div>
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 图表区域 -->
    <n-grid :cols="3" :x-gap="16" :y-gap="16" class="chart-section">
      <!-- CPU 使用率 -->
      <n-gi :span="2">
        <n-card title="CPU 使用率" class="chart-card">
          <template #header-extra>
            <n-tag type="primary" size="small">{{ cpuPercent }}%</n-tag>
          </template>
          <div ref="cpuChartRef" class="chart-container"></div>
        </n-card>
      </n-gi>
      <!-- 插件状态分布 -->
      <n-gi>
        <n-card title="插件状态分布" class="chart-card">
          <div ref="pluginChartRef" class="chart-container"></div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 内存信息 -->
    <n-grid :cols="2" :x-gap="16" :y-gap="16" class="chart-section">
      <!-- 系统内存 -->
      <n-gi>
        <n-card title="系统内存" class="chart-card">
          <template #header-extra>
            <span class="text-secondary">
              {{ formatBytes(systemMemoryUsed) }} / {{ formatBytes(systemMemoryTotal) }}
            </span>
          </template>
          <div ref="memoryChartRef" class="chart-container"></div>
          <div class="memory-usage">
            <div class="usage-label">
              <span>使用率</span>
              <span>{{ systemMemoryPercent }}%</span>
            </div>
            <n-progress
              type="line"
              :percentage="systemMemoryPercent"
              :color="getProgressColor(systemMemoryPercent)"
              :show-indicator="false"
            />
          </div>
        </n-card>
      </n-gi>
      <!-- JVM 堆内存 -->
      <n-gi>
        <n-card title="JVM 堆内存" class="chart-card">
          <template #header-extra>
            <span class="text-secondary">
              {{ formatBytes(heapUsed) }} / {{ formatBytes(heapMax) }}
            </span>
          </template>
          <div ref="jvmChartRef" class="chart-container"></div>
          <div class="memory-usage">
            <div class="usage-label">
              <span>使用率</span>
              <span>{{ heapPercent }}%</span>
            </div>
            <n-progress
              type="line"
              :percentage="heapPercent"
              :color="getProgressColor(heapPercent)"
              :show-indicator="false"
            />
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 系统信息 -->
    <n-card title="系统信息" class="info-card">
      <n-descriptions :column="4" label-placement="left" bordered>
        <n-descriptions-item label="操作系统">
          {{ systemInfo.osName || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="Java 版本">
          {{ systemInfo.javaVersion || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="运行时长">
          {{ formatUptime(systemInfo.uptime) }}
        </n-descriptions-item>
        <n-descriptions-item label="CPU 核心">
          {{ cpuInfo.availableProcessors || '-' }}
        </n-descriptions-item>
      </n-descriptions>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { NCard, NGrid, NGi, NIcon, NButton, NTag, NProgress, NDescriptions, NDescriptionsItem } from 'naive-ui'
import { RefreshOutline, CubeOutline, PlayCircleOutline, StopCircleOutline, AlertCircleOutline } from '@vicons/ionicons5'
import * as echarts from 'echarts'
import { monitorApi } from '@/api/services'

// 统计数据
const stats = ref({ total: 0, started: 0, stopped: 0, failed: 0 })

// 系统信息
const systemInfo = ref({ osName: '', javaVersion: '', uptime: 0 })
const cpuInfo = ref({ availableProcessors: 0, systemPercent: 0 })
const cpuPercent = ref(0)

// 内存信息
const heapUsed = ref(0)
const heapMax = ref(1)
const heapPercent = ref(0)
const systemMemoryTotal = ref(0)
const systemMemoryUsed = ref(0)
const systemMemoryPercent = ref(0)

// 图表引用
const cpuChartRef = ref(null)
const memoryChartRef = ref(null)
const jvmChartRef = ref(null)
const pluginChartRef = ref(null)

let cpuChart = null
let memoryChart = null
let jvmChart = null
let pluginChart = null
let refreshTimer = null

// 获取监控概览数据
const loadOverview = async () => {
  try {
    const res = await monitorApi.getOverview()
    // 后端返回结构: { code: 200, data: {...} }
    if (res.code === 200 && res.data) {
      const d = res.data
      
      // 更新统计数据
      if (d.pluginStatistics) {
        stats.value = d.pluginStatistics
      }
      
      // 更新系统信息
      if (d.system) {
        systemInfo.value = d.system
      }
      
      // 更新 CPU 信息
      if (d.cpu) {
        cpuInfo.value = d.cpu
        cpuPercent.value = parseFloat(d.cpu.systemPercent?.toFixed(1) || 0)
      }
      
      // 更新内存信息
      if (d.memory) {
        heapUsed.value = d.memory.heapUsed || 0
        heapMax.value = d.memory.heapMax || 1
        heapPercent.value = parseFloat(d.memory.heapUsedPercent?.toFixed(1) || 0)
        
        systemMemoryTotal.value = d.memory.systemTotal || 0
        systemMemoryUsed.value = d.memory.systemUsed || 0
        systemMemoryPercent.value = parseFloat(d.memory.systemUsedPercent?.toFixed(1) || 0)
      }
      
      updateCharts()
    }
  } catch (e) {
    console.error('获取监控数据失败:', e)
  }
}

// 更新图表数据
const updateCharts = () => {
  // CPU 仪表盘
  if (cpuChart) {
    cpuChart.setOption({
      series: [{ data: [{ value: cpuPercent.value }] }]
    })
  }
  
  // 系统内存仪表盘
  if (memoryChart) {
    memoryChart.setOption({
      series: [{ data: [{ value: systemMemoryPercent.value }] }]
    })
  }
  
  // JVM 内存仪表盘
  if (jvmChart) {
    jvmChart.setOption({
      series: [{ data: [{ value: heapPercent.value }] }]
    })
  }
  
  // 插件状态饼图
  if (pluginChart) {
    pluginChart.setOption({
      series: [{
        data: [
          { value: stats.value.started || 0, name: '运行中', itemStyle: { color: '#10b981' } },
          { value: stats.value.stopped || 0, name: '已停止', itemStyle: { color: '#6b7280' } },
          { value: stats.value.failed || 0, name: '异常', itemStyle: { color: '#ef4444' } }
        ]
      }]
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
          fontSize: 24,
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
  if (memoryChartRef.value) {
    memoryChart = echarts.init(memoryChartRef.value)
    memoryChart.setOption({
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
          fontSize: 24,
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
  if (jvmChartRef.value) {
    jvmChart = echarts.init(jvmChartRef.value)
    jvmChart.setOption({
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
          fontSize: 24,
          fontWeight: 'bold',
          formatter: '{value}%',
          offsetCenter: [0, '70%'],
          color: '#1f2937'
        },
        data: [{ value: 0 }]
      }]
    })
  }
  
  // 插件状态饼图
  if (pluginChartRef.value) {
    pluginChart = echarts.init(pluginChartRef.value)
    pluginChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', right: '5%', top: 'center' },
      series: [{
        name: '插件状态',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: []
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

// 获取进度条颜色
const getProgressColor = (percent) => {
  if (percent > 80) return '#ef4444'
  if (percent > 60) return '#f59e0b'
  return '#10b981'
}

// 刷新数据
const refreshData = () => {
  loadOverview()
}

// 窗口大小变化
const handleResize = () => {
  if (cpuChart) cpuChart.resize()
  if (memoryChart) memoryChart.resize()
  if (jvmChart) jvmChart.resize()
  if (pluginChart) pluginChart.resize()
}

onMounted(() => {
  nextTick(() => {
    initCharts()
    loadOverview()
  })
  
  // 每5秒刷新数据
  refreshTimer = setInterval(loadOverview, 5000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
  
  if (cpuChart) cpuChart.dispose()
  if (memoryChart) memoryChart.dispose()
  if (jvmChart) jvmChart.dispose()
  if (pluginChart) pluginChart.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard {
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
  
  &.stat-primary {
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    :deep(.n-card__content) {
      background: transparent;
      color: #fff;
    }
  }
  
  &.stat-success {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    :deep(.n-card__content) {
      background: transparent;
      color: #fff;
    }
  }
  
  &.stat-warning {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    :deep(.n-card__content) {
      background: transparent;
      color: #fff;
    }
  }
  
  &.stat-danger {
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    :deep(.n-card__content) {
      background: transparent;
      color: #fff;
    }
  }
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
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.chart-section {
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 8px;
}

.chart-container {
  height: 220px;
  width: 100%;
}

.memory-usage {
  margin-top: 16px;
  
  .usage-label {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 14px;
    color: #6b7280;
  }
}

.info-card {
  border-radius: 8px;
  :deep(.n-card-header) {
    margin-bottom: 0;
  }
}

.text-secondary {
  color: #6b7280;
}
</style>
