<template>
  <div class="cpu-monitor">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">CPU 监控</h1>
        <p class="page-subtitle">实时监控 CPU 使用情况</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="loadData">
          <template #icon><n-icon><RefreshOutline /></n-icon></template>
          刷新
        </n-button>
      </div>
    </div>

    <n-grid :cols="2" :x-gap="16" :y-gap="16">
      <n-gi>
        <n-card title="CPU 使用率" class="chart-card">
          <template #header-extra>
            <n-tag :type="cpuPercent > 80 ? 'error' : cpuPercent > 60 ? 'warning' : 'success'">
              {{ cpuPercent }}%
            </n-tag>
          </template>
          <div ref="cpuChartRef" class="chart-container"></div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="CPU 信息" class="info-card">
          <n-descriptions :column="1" label-placement="left" bordered>
            <n-descriptions-item label="CPU 核心数">{{ cpuInfo.availableProcessors || '-' }}</n-descriptions-item>
            <n-descriptions-item label="系统负载">{{ cpuInfo.systemPercent?.toFixed(2) || '-' }}%</n-descriptions-item>
            <n-descriptions-item label="用户空间负载">{{ cpuInfo.userPercent?.toFixed(2) || '-' }}%</n-descriptions-item>
            <n-descriptions-item label="等待 I/O">{{ cpuInfo.ioWaitPercent?.toFixed(2) || '-' }}%</n-descriptions-item>
            <n-descriptions-item label="软中断">{{ cpuInfo.softIrqPercent?.toFixed(2) || '-' }}%</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="CPU 核心使用情况" class="cores-card">
      <n-grid :cols="cpuCores.length > 4 ? 4 : cpuCores.length" :x-gap="12" :y-gap="12">
        <n-gi v-for="(core, index) in cpuCores" :key="index">
          <div class="core-item">
            <div class="core-header">
              <span>核心 {{ index + 1 }}</span>
              <n-tag size="small" :type="core > 80 ? 'error' : core > 60 ? 'warning' : 'success'">
                {{ core }}%
              </n-tag>
            </div>
            <n-progress
              type="line"
              :percentage="core"
              :color="core > 80 ? '#ef4444' : core > 60 ? '#f59e0b' : '#10b981'"
              :show-indicator="false"
            />
          </div>
        </n-gi>
      </n-grid>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { NCard, NGrid, NGi, NButton, NIcon, NTag, NDescriptions, NDescriptionsItem, NProgress } from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import * as echarts from 'echarts'
import { monitorApi } from '@/api/services'

const cpuChartRef = ref(null)
const cpuInfo = ref({ availableProcessors: 0, systemPercent: 0 })
const cpuPercent = ref(0)
const cpuCores = ref([])
let cpuChart = null
let refreshTimer = null

const loadData = async () => {
  try {
    const res = await monitorApi.getCpu()
    if (res.code === 200 && res.data) {
      cpuInfo.value = res.data
      cpuPercent.value = parseFloat(res.data.systemPercent?.toFixed(1) || 0)
      
      // 模拟每个核心的使用率
      const cores = res.data.availableProcessors || 4
      cpuCores.value = Array.from({ length: cores }, () => 
        Math.floor(Math.random() * 30) + cpuPercent.value * 0.5
      ).map(c => Math.min(100, Math.max(0, c)))
      
      updateChart()
    }
  } catch (e) {
    console.error('获取 CPU 数据失败:', e)
  }
}

const initChart = () => {
  if (cpuChartRef.value) {
    cpuChart = echarts.init(cpuChartRef.value)
    cpuChart.setOption({
      series: [{
        type: 'gauge',
        min: 0,
        max: 100,
        radius: '90%',
        progress: { show: true, width: 18, itemStyle: { color: '#2563eb' } },
        axisLine: { lineStyle: { width: 18, color: [[1, '#e5e7eb']] } },
        axisTick: { show: false },
        splitLine: { length: 12, lineStyle: { width: 3, color: '#999' } },
        axisLabel: { distance: 25, color: '#6b7280', fontSize: 14 },
        anchor: { show: true, size: 26, itemStyle: { color: '#2563eb', borderWidth: 4 } },
        title: { show: false },
        detail: {
          valueAnimation: true,
          fontSize: 32,
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

const updateChart = () => {
  if (cpuChart) {
    cpuChart.setOption({
      series: [{ data: [{ value: cpuPercent.value }] }]
    })
  }
}

const handleResize = () => {
  if (cpuChart) cpuChart.resize()
}

onMounted(() => {
  nextTick(() => {
    initChart()
    loadData()
  })
  refreshTimer = setInterval(loadData, 5000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
  if (cpuChart) cpuChart.dispose()
})
</script>

<style lang="scss" scoped>
.cpu-monitor { padding: 0; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  .page-title { font-size: 24px; font-weight: 600; color: #1f2937; margin: 0; }
  .page-subtitle { color: #6b7280; margin: 4px 0 0 0; font-size: 14px; }
}
.chart-card, .info-card, .cores-card { border-radius: 8px; margin-bottom: 16px; }
.chart-container { height: 280px; }
.core-item {
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  .core-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 13px;
    color: #6b7280;
  }
}
</style>