<template>
  <div class="threads-monitor">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">线程监控</h1>
        <p class="page-subtitle">查看系统线程状态和死锁检测</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="loadData">
          <template #icon><n-icon><RefreshOutline /></n-icon></template>
          刷新
        </n-button>
      </div>
    </div>

    <!-- 线程统计 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" class="stat-grid">
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <n-icon size="28" color="#2563eb"><LayersOutline /></n-icon>
            <div class="stat-info">
              <div class="stat-value">{{ threadInfo.threadCount || 0 }}</div>
              <div class="stat-label">活动线程</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <n-icon size="28" color="#10b981"><TrendingUpOutline /></n-icon>
            <div class="stat-info">
              <div class="stat-value">{{ threadInfo.peakThreadCount || 0 }}</div>
              <div class="stat-label">峰值线程</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <n-icon size="28" color="#f59e0b"><TimeOutline /></n-icon>
            <div class="stat-info">
              <div class="stat-value">{{ threadInfo.daemonThreadCount || 0 }}</div>
              <div class="stat-label">守护线程</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <n-icon size="28" color="#ef4444"><WarningOutline /></n-icon>
            <div class="stat-info">
              <div class="stat-value">{{ deadlockedCount }}</div>
              <div class="stat-label">死锁线程</div>
            </div>
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 死锁警告 -->
    <n-alert v-if="deadlockedCount > 0" type="error" title="检测到死锁线程" class="deadlock-alert">
      系统检测到 {{ deadlockedCount }} 个线程处于死锁状态，请及时处理！
    </n-alert>

    <!-- 线程列表 -->
    <n-card title="线程列表" class="list-card">
      <template #header-extra>
        <n-input
          v-model:value="searchKeyword"
          placeholder="搜索线程..."
          clearable
          style="width: 200px;"
          size="small"
        >
          <template #prefix><n-icon><SearchOutline /></n-icon></template>
        </n-input>
      </template>
      
      <n-data-table
        :columns="columns"
        :data="filteredThreads"
        :loading="loading"
        :bordered="false"
        :max-height="400"
        :virtual-scroll="true"
        size="small"
      />
    </n-card>

    <!-- 线程池信息 -->
    <n-card title="线程池信息" class="pool-card">
      <n-data-table
        :columns="poolColumns"
        :data="threadPools"
        :bordered="false"
        size="small"
      />
    </n-card>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import {
  NCard, NGrid, NGi, NButton, NIcon, NInput, NDataTable, NTag,
  NAlert, NSpace, useMessage
} from 'naive-ui'
import {
  RefreshOutline, LayersOutline, TrendingUpOutline, TimeOutline,
  WarningOutline, SearchOutline
} from '@vicons/ionicons5'
import { monitorApi } from '@/api/services'

const message = useMessage()
const loading = ref(false)
const searchKeyword = ref('')
const threadInfo = ref({})
const deadlockedCount = ref(0)
const threadList = ref([])
const threadPools = ref([])

// 过滤后的线程列表
const filteredThreads = computed(() => {
  if (!searchKeyword.value) return threadList.value
  const keyword = searchKeyword.value.toLowerCase()
  return threadList.value.filter(t =>
    t.name?.toLowerCase().includes(keyword) ||
    t.state?.toLowerCase().includes(keyword) ||
    t.stackTrace?.some(s => s?.toLowerCase().includes(keyword))
  )
})

// 获取状态类型
const getStateType = (state) => {
  const map = {
    'RUNNABLE': 'success',
    'WAITING': 'warning',
    'TIMED_WAITING': 'info',
    'BLOCKED': 'error'
  }
  return map[state] || 'default'
}

// 获取状态文本
const getStateText = (state) => {
  const map = {
    'RUNNABLE': '运行中',
    'WAITING': '等待中',
    'TIMED_WAITING': '限时等待',
    'BLOCKED': '阻塞中'
  }
  return map[state] || state
}

// 表格列配置
const columns = [
  { title: '线程 ID', key: 'threadId', width: 100 },
  { title: '线程名称', key: 'threadName', ellipsis: true },
  {
    title: '状态',
    key: 'threadState',
    width: 100,
    render(row) {
      return h(NTag, { size: 'small', type: getStateType(row.threadState) }, () => getStateText(row.threadState))
    }
  },
  { title: '优先级', key: 'priority', width: 80 },
  {
    title: 'CPU 时间',
    key: 'cpuTime',
    width: 120,
    render(row) {
      return row.cpuTime ? (row.cpuTime / 1000000).toFixed(2) + ' ms' : '-'
    }
  },
  {
    title: '用户',
    key: 'isDaemon',
    width: 80,
    render(row) {
      return h(NTag, { size: 'small', type: row.isDaemon ? 'info' : 'success' }, () => row.isDaemon ? '守护' : '用户')
    }
  }
]

// 线程池列配置
const poolColumns = [
  { title: '线程池名称', key: 'poolName' },
  { title: '核心线程数', key: 'corePoolSize' },
  { title: '最大线程数', key: 'maximumPoolSize' },
  { title: '活跃线程数', key: 'activeCount' },
  { title: '任务队列大小', key: 'queueSize' },
  { title: '已完成任务数', key: 'completedTaskCount' }
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await monitorApi.getThreadDetail()
    if (res.code === 200 && res.data) {
      threadInfo.value = {
        threadCount: res.data.threadCount,
        peakThreadCount: res.data.peakThreadCount,
        daemonThreadCount: res.data.daemonThreadCount
      }
      deadlockedCount.value = res.data.deadlockedThreads?.length || 0
      threadList.value = res.data.threadInfos || []
      // 线程池信息需要从其他接口获取，这里使用模拟数据
      threadPools.value = [
        { poolName: 'common-pool', corePoolSize: 4, maximumPoolSize: 8, activeCount: 2, queueSize: 100, completedTaskCount: 1520 }
      ]
    }
  } catch (e) {
    message.error('获取线程数据失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.threads-monitor { padding: 0; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  .page-title { font-size: 24px; font-weight: 600; color: #1f2937; margin: 0; }
  .page-subtitle { color: #6b7280; margin: 4px 0 0 0; font-size: 14px; }
}
.stat-grid { margin-bottom: 24px; }
.stat-card { border-radius: 8px; }
.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-value { font-size: 28px; font-weight: 700; color: #1f2937; }
.stat-label { font-size: 14px; color: #6b7280; }
.deadlock-alert { margin-bottom: 16px; }
.list-card, .pool-card { border-radius: 8px; margin-bottom: 16px; }
</style>