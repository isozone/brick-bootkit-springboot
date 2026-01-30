<template>
  <div class="script-executions-page">
    <div class="page-header">
      <h2 class="page-title">执行记录</h2>
      <div class="page-actions">
        <n-input v-model:value="searchKeyword" placeholder="搜索脚本名称..." style="width: 240px" @input="handleSearch" />
        <n-select
          v-model:value="filterStatus"
          placeholder="执行状态"
          clearable
          style="width: 140px"
          :options="statusOptions"
          @update:value="handleFilterChange"
        />
        <n-button @click="handleExport">
          <template #icon>
            <n-icon><DownloadOutline /></n-icon>
          </template>
          导出
        </n-button>
      </div>
    </div>

    <div class="table-container">
      <n-data-table
        :columns="columns"
        :data="executions"
        :loading="loading"
        :pagination="pagination"
        :remote="true"
      />
    </div>

    <!-- 执行记录详情弹窗 -->
    <n-modal v-model:show="showDetailModal" preset="card" title="执行详情" style="width: 700px; max-width: 90vw;" :bordered="false">
      <n-descriptions label-placement="left" :column="1" bordered v-if="selectedRecord">
        <n-descriptions-item label="执行ID">
          <n-tag type="info" size="small">{{ selectedRecord.executionId || '-' }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="脚本名称">
          <span class="detail-value">{{ selectedRecord.scriptName || '-' }}</span>
        </n-descriptions-item>
        <n-descriptions-item label="脚本类型">
          <n-tag size="small">{{ selectedRecord.scriptType || 'SHELL' }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="执行状态">
          <n-tag :type="getStatusInfo(selectedRecord.status).type" size="small">
            {{ getStatusInfo(selectedRecord.status).label }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="提交人">
          <span class="detail-value">{{ selectedRecord.submittedBy || selectedRecord.executedBy || '-' }}</span>
        </n-descriptions-item>
        <n-descriptions-item label="开始时间">
          <span class="detail-value">{{ formatTime(selectedRecord.startTime) }}</span>
        </n-descriptions-item>
        <n-descriptions-item label="结束时间">
          <span class="detail-value">{{ formatTime(selectedRecord.endTime) || '-' }}</span>
        </n-descriptions-item>
        <n-descriptions-item label="执行耗时">
          <n-tag type="warning" size="small">{{ formatDuration(selectedRecord.executionTimeMs || selectedRecord.durationMs) }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="执行参数">
          <div class="param-content">{{ selectedRecord.parameters || '-' }}</div>
        </n-descriptions-item>
        <n-descriptions-item label="状态消息">
          <div class="param-content">{{ selectedRecord.statusMessage || '-' }}</div>
        </n-descriptions-item>
        <n-descriptions-item label="执行输出">
          <n-input
            type="textarea"
            :value="selectedRecord.output || '-'"
            :rows="8"
            readonly
            style="font-family: monospace; font-size: 12px;"
          />
        </n-descriptions-item>
        <n-descriptions-item label="错误信息" v-if="selectedRecord.errorMessage">
          <n-input
            type="textarea"
            :value="selectedRecord.errorMessage"
            :rows="6"
            readonly
            style="font-family: monospace; font-size: 12px; color: #d03050;"
          />
        </n-descriptions-item>
      </n-descriptions>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h, onMounted, watch } from 'vue'
import {
  NInput,
  NSelect,
  NButton,
  NIcon,
  NDataTable,
  NTag,
  NSpace,
  NTooltip,
  NModal,
  NDescriptions,
  NDescriptionsItem,
  NInputGroup,
  NScrollbar,
  useMessage
} from 'naive-ui'
import { DownloadOutline, EyeOutline } from '@vicons/ionicons5'
import { executionsApi } from '@/api/services'
import { PAGINATION, STATUS } from '@/constants'

const message = useMessage()

const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref(null)
const total = ref(0)
const pagination = ref({
  page: 1,
  pageSize: 50,
  showSizePicker: true,
  pageSizes: [20, 50, 100],
  showTotal: (total) => `共 ${total} 条`
})

const statusOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '运行中', value: 'RUNNING' },
  { label: '已取消', value: 'CANCELLED' }
]

const executions = ref([])
const selectedRecord = ref(null)
const showDetailModal = ref(false)

const getStatusInfo = (status) => {
  const statusMap = {
    SUCCESS: { type: 'success', label: '成功' },
    FAILED: { type: 'error', label: '失败' },
    RUNNING: { type: 'info', label: '运行中' },
    CANCELLED: { type: 'default', label: '已取消' }
  }
  return statusMap[status] || statusMap.CANCELLED
}

const formatDuration = (ms) => {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}秒`
  return `${Math.floor(ms / 60000)}分${Math.floor((ms % 60000) / 1000)}秒`
}

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time === 'string') return time.replace('T', ' ').substring(0, 19)
  return '-'
}

const loadExecutions = async (page = 1, size = 50) => {
  loading.value = true
  try {
    const res = await executionsApi.getList(page, size, searchKeyword.value || null, filterStatus.value)
    // 处理响应数据结构
    const data = res.data || res
    executions.value = (data.records || []).map((record, index) => ({
      key: `${record.executionId || record.id}_${index}`,
      id: record.executionId || record.id,
      scriptName: record.scriptName || '-',
      type: record.scriptType || '-',
      status: record.status || 'UNKNOWN',
      startTime: formatTime(record.startTime),
      endTime: formatTime(record.endTime),
      duration: formatDuration(record.executionTimeMs || record.durationMs),
      output: record.output || record.errorMessage || '-',
      rawRecord: record
    }))
    total.value = data.total || 0
    pagination.value = {
      ...pagination.value,
      page: data.page || page,
      pageSize: data.size || size,
      total: data.total || 0
    }
  } catch (e) {
    console.error('加载执行记录失败:', e)
    message.error('加载执行记录失败')
    executions.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  // 防抖处理
  if (window.searchTimeout) {
    clearTimeout(window.searchTimeout)
  }
  window.searchTimeout = setTimeout(() => {
    pagination.value.page = 1
    loadExecutions(1, pagination.value.pageSize)
  }, 300)
}

const handleFilterChange = () => {
  pagination.value.page = 1
  loadExecutions(1, pagination.value.pageSize)
}

const handlePageChange = (newPage) => {
  loadExecutions(newPage, pagination.value.pageSize)
}

const handlePageSizeChange = (newSize) => {
  pagination.value.page = 1
  pagination.value.pageSize = newSize
  loadExecutions(1, newSize)
}

const columns = [
  { title: '执行ID', key: 'id', width: 100, ellipsis: true },
  { title: '脚本名称', key: 'scriptName', ellipsis: true },
  { title: '类型', key: 'type', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) => {
      const status = getStatusInfo(row.status)
      return h(NTag, { type: status.type, size: 'small', round: true }, { default: () => status.label })
    }
  },
  { title: '开始时间', key: 'startTime', width: 160 },
  { title: '结束时间', key: 'endTime', width: 160 },
  { title: '耗时', key: 'duration', width: 100 },
  { title: '输出', key: 'output', ellipsis: true },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    render: (row) => h(NTooltip, { trigger: 'hover' }, {
      trigger: () => h(NButton, { quaternary: true, circle: true, size: 'small', onClick: () => handleView(row) }, {
        icon: () => h(NIcon, null, { default: () => h(EyeOutline) })
      }),
      default: () => '查看详情'
    })
  }
]

const handleExport = async () => {
  try {
    message.info('正在导出...')
    
    const response = await executionsApi.export(searchKeyword.value || null, filterStatus.value)
    
    // 获取 blob 数据
    const blob = response.data
    if (!blob || blob.size === 0) {
      message.warning('没有可导出的数据')
      return
    }
    
    // 创建下载链接
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    const filename = `execution_records_${new Date().toISOString().slice(0, 10)}.txt`
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(objectUrl)
    
    message.success('导出成功')
  } catch (e) {
    console.error('导出失败:', e)
    message.error('导出失败: ' + (e.message || '未知错误'))
  }
}

const handleView = (row) => {
  selectedRecord.value = row.rawRecord
  showDetailModal.value = true
}

// 监听分页变化
watch(() => pagination.value.page, (newPage) => {
  handlePageChange(newPage)
})

watch(() => pagination.value.pageSize, (newSize) => {
  handlePageSizeChange(newSize)
})

onMounted(() => {
  loadExecutions()
})
</script>

<style lang="scss" scoped>
.script-executions-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .page-actions {
      display: flex;
      gap: 12px;
    }
  }

  :deep(.detail-value) {
    font-weight: 500;
    color: #333;
  }

  :deep(.param-content) {
    background: #f5f7fa;
    padding: 12px;
    border-radius: 6px;
    font-family: monospace;
    font-size: 12px;
    line-height: 1.6;
    max-height: 150px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>