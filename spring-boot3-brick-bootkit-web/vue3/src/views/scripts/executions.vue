<template>
  <div class="script-executions-page">
    <div class="page-header">
      <h2 class="page-title">执行记录</h2>
      <div class="page-actions">
        <n-input v-model:value="searchKeyword" placeholder="搜索脚本名称..." style="width: 240px" />
        <n-select
          v-model:value="filterStatus"
          placeholder="执行状态"
          clearable
          style="width: 140px"
          :options="statusOptions"
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
      />
    </div>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import {
  NInput,
  NSelect,
  NButton,
  NIcon,
  NDataTable,
  NTag,
  NSpace,
  NTooltip,
  useMessage
} from 'naive-ui'
import { DownloadOutline, EyeOutline } from '@vicons/ionicons5'
import { PAGINATION, STATUS, MESSAGE } from '@/constants'

const message = useMessage()

const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref(null)
const pagination = ref({
  page: 1,
  pageSize: PAGINATION.defaultPageSizes,
  showSizePicker: true,
  pageSizes: PAGINATION.pageSizes,
  showTotal: (total) => `共 ${total} 条`
})

const statusOptions = [
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' },
  { label: '运行中', value: 'running' },
  { label: '已取消', value: 'cancelled' }
]

const executions = ref([
  { id: 1, scriptName: '数据库备份脚本', type: 'SQL', status: 'success', startTime: '2024-01-30 02:00:00', endTime: '2024-01-30 02:02:30', duration: '2分30秒', output: '备份完成' },
  { id: 2, scriptName: '日志清理脚本', type: 'Shell', status: 'success', startTime: '2024-01-30 03:00:00', endTime: '2024-01-30 03:00:45', duration: '45秒', output: '清理日志 123 个' },
  { id: 3, scriptName: '性能监控脚本', type: 'Python', status: 'failed', startTime: '2024-01-30 14:00:00', endTime: '2024-01-30 14:00:05', duration: '5秒', output: '连接超时' },
  { id: 4, scriptName: '数据同步脚本', type: 'Shell', status: 'running', startTime: '2024-01-30 14:25:00', endTime: '-', duration: '-', output: '同步中...' },
  { id: 5, scriptName: '文件清理脚本', type: 'Batch', status: 'success', startTime: '2024-01-29 04:00:00', endTime: '2024-01-29 04:01:15', duration: '1分15秒', output: '清理完成' }
])

const columns = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '脚本名称', key: 'scriptName', ellipsis: true },
  { title: '类型', key: 'type', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) => {
      const statusMap = {
        success: { type: 'success', label: '成功' },
        failed: { type: 'error', label: '失败' },
        running: { type: 'info', label: '运行中' },
        cancelled: { type: 'default', label: '已取消' }
      }
      const status = statusMap[row.status] || statusMap.cancelled
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

const handleExport = () => {
  message.success('导出成功')
}

const handleView = (row) => {
  message.info('查看详情')
}
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
}
</style>
