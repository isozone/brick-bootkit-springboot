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
    <n-grid :cols="5" :x-gap="16" :y-gap="16" class="stat-grid">
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <n-icon size="28" color="#2563eb"><LayersOutline /></n-icon>
            <div class="stat-info">
              <div class="stat-value">{{ threadInfo.total || 0 }}</div>
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
              <div class="stat-value">{{ threadInfo.peak || 0 }}</div>
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
              <div class="stat-value">{{ threadInfo.daemon || 0 }}</div>
              <div class="stat-label">守护线程</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <n-icon size="28" color="#8b5cf6"><PeopleOutline /></n-icon>
            <div class="stat-info">
              <div class="stat-value">{{ threadInfo.started || 0 }}</div>
              <div class="stat-label">已启动线程</div>
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

    <!-- 线程状态统计 -->
    <n-card title="线程状态统计" class="state-stat-card">
      <n-descriptions :column="6" label-placement="left" bordered size="small">
        <n-descriptions-item label="可运行">
          <n-tag type="success" size="small">{{ threadInfo.runnableCount || 0 }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="等待中">
          <n-tag type="warning" size="small">{{ threadInfo.waitingCount || 0 }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="限时等待">
          <n-tag type="info" size="small">{{ threadInfo.timedWaitingCount || 0 }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="阻塞">
          <n-tag type="error" size="small">{{ threadInfo.blockedCount || 0 }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="新建">
          <n-tag size="small">{{ threadInfo.newCount || 0 }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="已终止">
          <n-tag size="small">{{ threadInfo.terminatedCount || 0 }}</n-tag>
        </n-descriptions-item>
      </n-descriptions>
    </n-card>

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
  NAlert, NDescriptions, NDescriptionsItem, NSpace, useMessage
} from 'naive-ui'
import {
  RefreshOutline, LayersOutline, TrendingUpOutline, TimeOutline,
  WarningOutline, SearchOutline, PeopleOutline
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
    t.threadName?.toLowerCase().includes(keyword) ||
    t.threadState?.toLowerCase().includes(keyword)
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
  { title: '线程 ID', key: 'threadId', width: 90 },
  { title: '线程名称', key: 'threadName', ellipsis: true, minWidth: 180 },
  {
    title: '状态',
    key: 'threadState',
    width: 90,
    render(row) {
      return h(NTag, { size: 'small', type: getStateType(row.threadState) }, () => getStateText(row.threadState))
    }
  },
  { title: '优先级', key: 'priority', width: 70 },
  {
    title: 'CPU 时间',
    key: 'cpuTime',
    width: 100,
    render(row) {
      if (!row.cpuTime || row.cpuTime === 0) return '-'
      // 纳秒转换为毫秒
      return (row.cpuTime / 1000000).toFixed(2) + ' ms'
    }
  },
  {
    title: '类型',
    key: 'daemon',
    width: 70,
    render(row) {
      return h(NTag, { size: 'small', type: row.daemon ? 'info' : 'success' }, () => row.daemon ? '守护' : '用户')
    }
  },
  {
    title: '阻塞次数',
    key: 'blockedCount',
    width: 90,
    render(row) {
      return row.blockedCount > 0 ? h('span', { style: { color: '#ef4444' } }, row.blockedCount) : row.blockedCount
    }
  },
  {
    title: '等待次数',
    key: 'waitedCount',
    width: 90,
    render(row) {
      return row.waitedCount > 0 ? h('span', { style: { color: '#f59e0b' } }, row.waitedCount) : row.waitedCount
    }
  },
  {
    title: '锁名称',
    key: 'lockName',
    width: 150,
    ellipsis: true,
    render(row) {
      return row.lockName ? row.lockName : '-'
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
      // 线程统计信息 - 后端返回 data.threadInfo
      if (res.data.threadInfo) {
        threadInfo.value = {
          total: res.data.threadInfo.total,
          peak: res.data.threadInfo.peak,
          daemon: res.data.threadInfo.daemon,
          started: res.data.threadInfo.started,
          newCount: res.data.threadInfo.newCount,
          runnableCount: res.data.threadInfo.runnableCount,
          blockedCount: res.data.threadInfo.blockedCount,
          waitingCount: res.data.threadInfo.waitingCount,
          timedWaitingCount: res.data.threadInfo.timedWaitingCount,
          terminatedCount: res.data.threadInfo.terminatedCount
        }
      }
      deadlockedCount.value = res.data.deadlockedThreads?.length || 0
      // 线程列表 - 后端返回 data.threads
      threadList.value = res.data.threads || []
      
      // 获取线程池信息
      const poolsRes = await monitorApi.getThreadPools()
      if (poolsRes.code === 200 && poolsRes.data) {
        threadPools.value = poolsRes.data
      }
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
.stat-grid { margin-bottom: 16px; }
.stat-card { border-radius: 8px; }
.state-stat-card { border-radius: 8px; margin-bottom: 16px; }
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