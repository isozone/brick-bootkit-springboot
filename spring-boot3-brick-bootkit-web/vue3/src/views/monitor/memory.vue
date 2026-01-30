<template>
  <div class="memory-monitor">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">内存监控</h1>
        <p class="page-subtitle">实时监控内存使用情况</p>
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
        <n-card title="JVM 堆内存" class="chart-card">
          <template #header-extra>
            <n-tag :type="heapPercent > 80 ? 'error' : heapPercent > 60 ? 'warning' : 'success'">
              {{ heapPercent }}%
            </n-tag>
          </template>
          <div ref="heapChartRef" class="chart-container"></div>
          <n-descriptions :column="1" size="small" label-placement="left" bordered>
            <n-descriptions-item :label="formatBytes(heapUsed) + ' / ' + formatBytes(heapMax)">
              已用 / 最大
            </n-descriptions-item>
            <n-descriptions-item label="堆内存初始值">{{ formatBytes(heapInit) }}</n-descriptions-item>
            <n-descriptions-item label="堆内存提交大小">{{ formatBytes(heapCommitted) }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="非堆内存" class="chart-card">
          <template #header-extra>
            <n-tag :type="nonHeapPercent > 80 ? 'error' : nonHeapPercent > 60 ? 'warning' : 'success'">
              {{ nonHeapPercent }}%
            </n-tag>
          </template>
          <div ref="nonHeapChartRef" class="chart-container"></div>
          <n-descriptions :column="1" size="small" label-placement="left" bordered>
            <n-descriptions-item :label="formatBytes(nonHeapUsed) + ' / ' + formatBytes(nonHeapMax)">
              已用 / 最大
            </n-descriptions-item>
            <n-descriptions-item label="Metaspace">{{ formatBytes(metaspace) }}</n-descriptions-item>
            <n-descriptions-item label="Code Cache">{{ formatBytes(codeCache) }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="系统内存" class="system-card">
      <n-grid :cols="4" :x-gap="16">
        <n-gi>
          <div class="memory-stat">
            <div class="stat-label">总内存</div>
            <div class="stat-value">{{ formatBytes(systemTotal) }}</div>
          </div>
        </n-gi>
        <n-gi>
          <div class="memory-stat">
            <div class="stat-label">已用内存</div>
            <div class="stat-value">{{ formatBytes(systemUsed) }}</div>
          </div>
        </n-gi>
        <n-gi>
          <div class="memory-stat">
            <div class="stat-label">空闲内存</div>
            <div class="stat-value">{{ formatBytes(systemFree) }}</div>
          </div>
        </n-gi>
        <n-gi>
          <div class="memory-stat">
            <div class="stat-label">使用率</div>
            <div class="stat-value">
              <n-tag :type="systemPercent > 80 ? 'error' : systemPercent > 60 ? 'warning' : 'success'">
                {{ systemPercent }}%
              </n-tag>
            </div>
          </div>
        </n-gi>
      </n-grid>
      <div class="memory-bar">
        <n-progress
          type="line"
          :percentage="systemPercent"
          :color="systemPercent > 80 ? '#ef4444' : systemPercent > 60 ? '#f59e0b' : '#10b981'"
        />
      </div>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { NCard, NGrid, NGi, NButton, NIcon, NTag, NDescriptions, NDescriptionsItem, NProgress } from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import * as echarts from 'echarts'
import { monitorApi } from '@/api/services'

const heapChartRef = ref(null)
const nonHeapChartRef = ref(null)
const memoryData = ref({})
const heapPercent = ref(0)
const nonHeapPercent = ref(0)
const systemTotal = ref(0)
const systemUsed = ref(0)
const systemFree = ref(0)
const systemPercent = ref(0)

let heapChart = null
let nonHeapChart = null
let refreshTimer = null

const loadData = async () => {
  try {
    const res = await monitorApi.getMemory()
    if (res.code === 200 && res.data) {
      memoryData.value = res.data
      
      // JVM 堆内存
      heapPercent.value = parseFloat(res.data.heapUsedPercent?.toFixed(1) || 0)
      const heapUsedVal = res.data.heapUsed || 0
      const heapMaxVal = res.data.heapMax || 1
      
      // 提取堆内存详情
      const heapMemory = res.data.heapMemoryUsage || {}
      const heapInit = heapMemory.init || 0
      const heapCommitted = heapMemory.committed || 0
      
      // 非堆内存
      const nonHeapMemory = res.data.nonHeapMemoryUsage || {}
      nonHeapPercent.value = parseFloat(((nonHeapMemory.used || 0) / (nonHeapMemory.max || 1) * 100).toFixed(1))
      
      // 系统内存
      systemTotal.value = res.data.systemTotal || 0
      systemUsed.value = res.data.systemUsed || 0
      systemFree.value = systemTotal.value - systemUsed.value
      systemPercent.value = parseFloat(res.data.systemUsedPercent?.toFixed(1) || 0)
      
      // 更新图表
      updateCharts(heapUsedVal, heapMaxVal, nonHeapMemory.used || 0, nonHeapMemory.max || 1)
    }
  } catch (e) {
    console.error('获取内存数据失败:', e)
  }
}

const initCharts = () => {
  if (heapChartRef.value) {
    heapChart = echarts.init(heapChartRef.value)
    heapChart.setOption({
      series: [{
        type: 'gauge',
        min: 0,
        max: 100,
        radius: '90%',
        progress: { show: true, width: 18, itemStyle: { color: '#10b981' } },
        axisLine: { lineStyle: { width: 18, color: [[1, '#e5e7eb']] } },
        axisTick: { show: false },
        splitLine: { length: 12, lineStyle: { width: 3, color: '#999' } },
        axisLabel: { distance: 25, color: '#6b7280', fontSize: 14 },
        anchor: { show: true, size: 26, itemStyle: { color: '#10b981', borderWidth: 4 } },
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
  
  if (nonHeapChartRef.value) {
    nonHeapChart = echarts.init(nonHeapChartRef.value)
    nonHeapChart.setOption({
      series: [{
        type: 'gauge',
        min: 0,
        max: 100,
        radius: '90%',
        progress: { show: true, width: 18, itemStyle: { color: '#f59e0b' } },
        axisLine: { lineStyle: { width: 18, color: [[1, '#e5e7eb']] } },
        axisTick: { show: false },
        splitLine: { length: 12, lineStyle: { width: 3, color: '#999' } },
        axisLabel: { distance: 25, color: '#6b7280', fontSize: 14 },
        anchor: { show: true, size: 26, itemStyle: { color: '#f59e0b', borderWidth: 4 } },
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

const updateCharts = (heapUsed, heapMax, nonHeapUsed, nonHeapMax) => {
  if (heapChart) {
    heapChart.setOption({
      series: [{ data: [{ value: heapPercent.value }] }]
    })
  }
  if (nonHeapChart) {
    nonHeapChart.setOption({
      series: [{ data: [{ value: nonHeapPercent.value }] }]
    })
  }
}

const formatBytes = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const handleResize = () => {
  if (heapChart) heapChart.resize()
  if (nonHeapChart) nonHeapChart.resize()
}

const heapUsed = ref(0)
const heapMax = ref(1)
const heapInit = ref(0)
const heapCommitted = ref(0)
const nonHeapUsed = ref(0)
const nonHeapMax = ref(0)
const metaspace = ref(0)
const codeCache = ref(0)

onMounted(() => {
  nextTick(() => {
    initCharts()
    loadData()
  })
  refreshTimer = setInterval(loadData, 5000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
  if (heapChart) heapChart.dispose()
  if (nonHeapChart) nonHeapChart.dispose()
})
</script>

<style lang="scss" scoped>
.memory-monitor { padding: 0; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  .page-title { font-size: 24px; font-weight: 600; color: #1f2937; margin: 0; }
  .page-subtitle { color: #6b7280; margin: 4px 0 0 0; font-size: 14px; }
}
.chart-card, .system-card { border-radius: 8px; margin-bottom: 16px; }
.chart-container { height: 280px; }
.memory-stat {
  text-align: center;
  .stat-label { font-size: 13px; color: #6b7280; margin-bottom: 4px; }
  .stat-value { font-size: 20px; font-weight: 600; color: #1f2937; }
}
.memory-bar { margin-top: 16px; }
</style>