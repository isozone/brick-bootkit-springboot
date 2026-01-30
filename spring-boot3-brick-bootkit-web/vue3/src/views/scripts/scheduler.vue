<template>
  <div class="script-scheduler-page">
    <div class="page-header">
      <h2 class="page-title">定时任务</h2>
      <n-button type="primary" @click="showCreateModal = true">
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
        新建任务
      </n-button>
    </div>

    <div class="table-container">
      <n-data-table
        :columns="columns"
        :data="tasks"
        :loading="loading"
        :pagination="pagination"
      />
    </div>

    <!-- 创建/编辑模态框 -->
    <n-modal v-model:show="showCreateModal" preset="card" :title="isEdit ? '编辑任务' : '新建任务'" style="width: 600px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="任务名称" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入任务名称" />
        </n-form-item>
        <n-form-item label="关联脚本" path="scriptId">
          <n-select
            v-model:value="formData.scriptId"
            :options="scriptOptions"
            placeholder="选择关联脚本"
          />
        </n-form-item>
        <n-form-item label="Cron 表达式" path="cron">
          <n-input v-model:value="formData.cron" placeholder="* * * * *" />
        </n-form-item>
        <n-form-item label="执行策略" path="strategy">
          <n-radio-group v-model:value="formData.strategy">
            <n-radio value="serial">顺序执行</n-radio>
            <n-radio value="parallel">并行执行</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="formData.description" type="textarea" :rows="3" placeholder="任务描述" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" @click="handleSave">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import {
  NButton,
  NIcon,
  NDataTable,
  NTag,
  NSpace,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NRadioGroup,
  NRadio,
  NTooltip,
  useMessage
} from 'naive-ui'
import { AddOutline, PlayOutline, TrashOutline, TimeOutline } from '@vicons/ionicons5'
import { PAGINATION, STATUS, MESSAGE } from '@/constants'

const message = useMessage()

const loading = ref(false)
const showCreateModal = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const pagination = ref({
  page: 1,
  pageSize: PAGINATION.defaultPageSize,
  showSizePicker: true,
  pageSizes: PAGINATION.pageSizes,
  showTotal: (total) => `共 ${total} 条`
})

const tasks = ref([
  { id: 1, name: '每日备份', scriptName: '数据库备份脚本', cron: '0 2 * * *', strategy: 'serial', status: 'active', lastRun: '2024-01-30 02:00:00', nextRun: '2024-01-31 02:00:00' },
  { id: 2, name: '日志清理', scriptName: '日志清理脚本', cron: '0 3 * * *', strategy: 'serial', status: 'active', lastRun: '2024-01-30 03:00:00', nextRun: '2024-01-31 03:00:00' },
  { id: 3, name: '性能监控', scriptName: '性能监控脚本', cron: '*/5 * * * *', strategy: 'parallel', status: 'running', lastRun: '2024-01-30 14:25:00', nextRun: '2024-01-30 14:30:00' },
  { id: 4, name: '数据同步', scriptName: '数据同步脚本', cron: '0 4 * * 0', strategy: 'serial', status: 'inactive', lastRun: '2024-01-28 04:00:00', nextRun: '2024-02-04 04:00:00' }
])

const scriptOptions = [
  { label: '数据库备份脚本', value: 1 },
  { label: '日志清理脚本', value: 2 },
  { label: '性能监控脚本', value: 3 },
  { label: '数据同步脚本', value: 4 }
]

const formData = ref({
  name: '',
  scriptId: null,
  cron: '',
  strategy: 'serial',
  description: ''
})

const formRules = {
  name: { required: true, message: '请输入任务名称' },
  scriptId: { required: true, message: '请选择关联脚本' },
  cron: { required: true, message: '请输入 Cron 表达式' }
}

const columns = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '任务名称', key: 'name' },
  { title: '关联脚本', key: 'scriptName' },
  { title: 'Cron 表达式', key: 'cron', width: 120 },
  { title: '执行策略', key: 'strategy', width: 100, render: (row) => row.strategy === 'serial' ? '顺序执行' : '并行执行' },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => {
      const statusMap = {
        active: { type: 'success', label: '启用' },
        inactive: { type: 'default', label: '禁用' },
        running: { type: 'info', label: '运行中' }
      }
      const status = statusMap[row.status] || statusMap.inactive
      return h(NTag, { type: status.type, size: 'small', round: true }, { default: () => status.label })
    }
  },
  { title: '下次执行', key: 'nextRun', width: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (row) => h(NSpace, { size: 'small' }, {
      default: () => [
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, { quaternary: true, circle: true, size: 'small', type: 'success', onClick: () => handleRun(row) }, {
            icon: () => h(NIcon, null, { default: () => h(PlayOutline) })
          }),
          default: () => '立即执行'
        }),
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, { quaternary: true, circle: true, size: 'small', type: 'error', onClick: () => handleDelete(row) }, {
            icon: () => h(NIcon, null, { default: () => h(TrashOutline) })
          }),
          default: () => '删除'
        })
      ]
    })
  }
]

const handleRun = (row) => {
  message.success(`立即执行任务: ${row.name}`)
}

const handleDelete = (row) => {
  message.warning('确定要删除该任务吗？')
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
    message.success(MESSAGE.SUCCESS.SAVE)
    showCreateModal.value = false
  } catch (e) {
    // 验证失败
  }
}
</script>

<style lang="scss" scoped>
.script-scheduler-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
    }
  }
}
</style>
