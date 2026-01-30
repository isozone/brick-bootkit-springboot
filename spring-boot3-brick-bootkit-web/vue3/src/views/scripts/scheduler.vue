<template>
  <div class="script-scheduler-page">
    <div class="page-header">
      <h2 class="page-title">定时任务</h2>
      <n-button type="primary" @click="handleCreate">
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
        :bordered="false"
      />
    </div>

    <!-- 创建/编辑模态框 -->
    <n-modal v-model:show="showModal" preset="card" :title="isEdit ? '编辑任务' : '新建任务'" style="width: 650px" :bordered="false">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="任务名称" path="taskName">
          <n-input v-model:value="formData.taskName" placeholder="请输入任务名称" />
        </n-form-item>
        <n-form-item label="关联脚本" path="scriptName">
          <n-select
            v-model:value="formData.scriptName"
            :options="scriptOptions"
            placeholder="选择要执行的脚本"
            filterable
          />
        </n-form-item>
        <n-form-item label="Cron表达式" path="cronExpression">
          <n-input v-model:value="formData.cronExpression" placeholder="分 时 日 月 周 (如: 0 2 * * *)">
            <template #prefix>
              <n-tooltip trigger="hover">
                <template #trigger>
                  <n-icon color="#999"><TimeOutline /></n-icon>
                </template>
                <div style="max-width: 300px; font-size: 12px; line-height: 1.6;">
                  <div><b>Cron表达式格式:</b></div>
                  <div>分 时 日 月 周</div>
                  <div style="margin-top: 8px;"><b>示例:</b></div>
                  <div>• 0 2 * * * 每天凌晨2点</div>
                  <div>• */5 * * * * 每5分钟</div>
                  <div>• 0 0 1 * * 每月1号凌晨</div>
                  <div>• 0 0 * * 1 每周一凌晨</div>
                </div>
              </n-tooltip>
            </template>
          </n-input>
        </n-form-item>
        <n-form-item label="执行参数" path="parameters">
          <n-input v-model:value="formData.parameters" placeholder="脚本执行参数(可选, 空格分隔)" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="formData.description" type="textarea" :rows="2" placeholder="任务描述(可选)" />
        </n-form-item>
        <n-divider />
        <n-form-item label="高级选项">
          <n-space>
            <n-input-number v-model:value="formData.timeoutMinutes" :min="1" :max="1440" placeholder="超时(分钟)" style="width: 140px;">
              <template #prefix>超时</template>
              <template #suffix>分</template>
            </n-input-number>
            <n-input-number v-model:value="formData.retryCount" :min="0" :max="10" placeholder="重试次数" style="width: 120px;">
              <template #prefix>重试</template>
              <template #suffix>次</template>
            </n-input-number>
          </n-space>
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSave">确定</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h, onMounted, computed } from 'vue'
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
  NInputNumber,
  NTooltip,
  NDivider,
  useMessage,
  NPopconfirm
} from 'naive-ui'
import { AddOutline, PlayOutline, TrashOutline, TimeOutline, CreateOutline, PauseOutline, PlaySkipForwardOutline } from '@vicons/ionicons5'
import { PAGINATION, MESSAGE } from '@/constants'
import { schedulerApi } from '@/api/services'
import { scriptsApi } from '@/api/services'

const message = useMessage()

const loading = ref(false)
const submitting = ref(false)
const showModal = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const scripts = ref([])

const pagination = ref({
  page: 1,
  pageSize: 50,
  showSizePicker: true,
  pageSizes: [20, 50, 100],
  showTotal: (total) => `共 ${total} 条`
})

const tasks = ref([])

const scriptOptions = computed(() => 
  scripts.value.map(s => ({ label: s.displayName || s.scriptName, value: s.scriptName }))
)

const formData = ref({
  taskId: null,
  taskName: '',
  scriptName: null,
  cronExpression: '',
  parameters: '',
  description: '',
  timeoutMinutes: 30,
  retryCount: 0
})

const formRules = {
  taskName: { required: true, message: '请输入任务名称' },
  scriptName: { required: true, message: '请选择关联脚本' },
  cronExpression: { required: true, message: '请输入Cron表达式' }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time === 'string') return time.replace('T', ' ').substring(0, 16)
  return '-'
}

// 获取状态信息
const getStatusInfo = (status) => {
  const statusMap = {
    CREATED: { type: 'default', label: '已创建' },
    ENABLED: { type: 'success', label: '启用' },
    DISABLED: { type: 'warning', label: '禁用' },
    ERROR: { type: 'error', label: '错误' }
  }
  return statusMap[status] || statusMap.CREATED
}

// 加载脚本列表
const loadScripts = async () => {
  try {
    const res = await scriptsApi.getAll()
    const data = res.data || res
    scripts.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('加载脚本列表失败:', e)
  }
}

// 加载任务列表
const loadTasks = async () => {
  loading.value = true
  try {
    const res = await schedulerApi.getAll()
    if (res.code === 200 || res.code === 0) {
      const data = res.data || res
      tasks.value = (Array.isArray(data) ? data : []).map(task => ({
        ...task,
        key: task.taskId
      }))
      pagination.value.total = tasks.value.length
    }
  } catch (e) {
    console.error('加载定时任务失败:', e)
    message.error('加载定时任务失败')
  } finally {
    loading.value = false
  }
}

// 新建任务
const handleCreate = () => {
  isEdit.value = false
  formData.value = {
    taskId: null,
    taskName: '',
    scriptName: null,
    cronExpression: '',
    parameters: '',
    description: '',
    timeoutMinutes: 30,
    retryCount: 0
  }
  showModal.value = true
}

// 编辑任务
const handleEdit = (row) => {
  isEdit.value = true
  formData.value = {
    taskId: row.taskId,
    taskName: row.taskName,
    scriptName: row.scriptName,
    cronExpression: row.cronExpression,
    parameters: row.parameters || '',
    description: row.description || '',
    timeoutMinutes: row.timeoutMinutes || 30,
    retryCount: row.retryCount || 0
  }
  showModal.value = true
}

// 保存任务
const handleSave = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const taskData = {
      taskName: formData.value.taskName,
      scriptName: formData.value.scriptName,
      cronExpression: formData.value.cronExpression,
      parameters: formData.value.parameters,
      description: formData.value.description,
      timeoutMinutes: formData.value.timeoutMinutes,
      retryCount: formData.value.retryCount,
      taskStatus: 'CREATED'
    }

    let res
    if (isEdit.value) {
      res = await schedulerApi.update(formData.value.taskId, taskData)
    } else {
      res = await schedulerApi.create(taskData)
    }

    if (res.code === 200 || res.code === 0) {
      message.success(isEdit.value ? MESSAGE.SUCCESS.UPDATE : MESSAGE.SUCCESS.CREATE)
      showModal.value = false
      loadTasks()
    } else {
      message.error(res.message || (isEdit.value ? MESSAGE.ERROR.UPDATE : MESSAGE.ERROR.CREATE))
    }
  } catch (e) {
    console.error('保存定时任务失败:', e)
    message.error(e.response?.data?.message || (isEdit.value ? MESSAGE.ERROR.UPDATE : MESSAGE.ERROR.CREATE))
  } finally {
    submitting.value = false
  }
}

// 启用任务
const handleEnable = async (row) => {
  try {
    const res = await schedulerApi.enable(row.taskId)
    if (res.code === 200 || res.code === 0) {
      message.success('启用成功')
      loadTasks()
    } else {
      message.error(res.message || '启用失败')
    }
  } catch (e) {
    console.error('启用任务失败:', e)
    message.error('启用失败')
  }
}

// 禁用任务
const handleDisable = async (row) => {
  try {
    const res = await schedulerApi.pause(row.taskId)
    if (res.code === 200 || res.code === 0) {
      message.success('禁用成功')
      loadTasks()
    } else {
      message.error(res.message || '禁用失败')
    }
  } catch (e) {
    console.error('禁用任务失败:', e)
    message.error('禁用失败')
  }
}

// 立即执行
const handleExecute = async (row) => {
  try {
    const res = await schedulerApi.execute(row.taskId)
    if (res.code === 200 || res.code === 0) {
      message.success('任务已提交执行')
    } else {
      message.error(res.message || '执行失败')
    }
  } catch (e) {
    console.error('执行任务失败:', e)
    message.error('执行失败')
  }
}

// 删除任务
const handleDelete = async (row) => {
  try {
    const res = await schedulerApi.delete(row.taskId)
    if (res.code === 200 || res.code === 0) {
      message.success(MESSAGE.SUCCESS.DELETE)
      loadTasks()
    } else {
      message.error(res.message || MESSAGE.ERROR.DELETE)
    }
  } catch (e) {
    console.error('删除任务失败:', e)
    message.error(MESSAGE.ERROR.DELETE)
  }
}

// 表格列配置
const columns = [
  { title: '任务ID', key: 'taskId', width: 180, ellipsis: { tooltip: true } },
  { title: '任务名称', key: 'taskName', ellipsis: true },
  { title: '脚本', key: 'scriptName', ellipsis: true },
  { title: 'Cron表达式', key: 'cronExpression', width: 120 },
  {
    title: '状态',
    key: 'taskStatus',
    width: 90,
    render: (row) => {
      const status = getStatusInfo(row.taskStatus)
      return h(NTag, { type: status.type, size: 'small', round: true }, { default: () => status.label })
    }
  },
  { title: '下次执行', key: 'nextExecution', width: 140, render: (row) => formatTime(row.nextExecution) },
  { title: '最后更新', key: 'updatedAt', width: 140, render: (row) => formatTime(row.updatedAt) },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: (row) => h(NSpace, { size: 'small' }, {
      default: () => [
        row.taskStatus === 'ENABLED' || row.taskStatus === 'enabled'
          ? h(NTooltip, { trigger: 'hover' }, {
              trigger: () => h(NButton, { 
                quaternary: true, circle: true, size: 'small', type: 'warning', 
                onClick: () => handleDisable(row) 
              }, {
                icon: () => h(NIcon, null, { default: () => h(PauseOutline) })
              }),
              default: () => '禁用'
            })
          : h(NTooltip, { trigger: 'hover' }, {
              trigger: () => h(NButton, { 
                quaternary: true, circle: true, size: 'small', type: 'success', 
                onClick: () => handleEnable(row) 
              }, {
                icon: () => h(NIcon, null, { default: () => h(PlaySkipForwardOutline) })
              }),
              default: () => '启用'
            }),
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, { 
            quaternary: true, circle: true, size: 'small', type: 'info', 
            onClick: () => handleExecute(row) 
          }, {
            icon: () => h(NIcon, null, { default: () => h(PlayOutline) })
          }),
          default: () => '立即执行'
        }),
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, { 
            quaternary: true, circle: true, size: 'small', 
            onClick: () => handleEdit(row) 
          }, {
            icon: () => h(NIcon, null, { default: () => h(CreateOutline) })
          }),
          default: () => '编辑'
        }),
        h(NPopconfirm, { onPositiveClick: () => handleDelete(row) }, {
          trigger: () => h(NButton, { 
            quaternary: true, circle: true, size: 'small', type: 'error' 
          }, {
            icon: () => h(NIcon, null, { default: () => h(TrashOutline) })
          }),
          default: () => '确定删除该任务?'
        })
      ]
    })
  }
]

onMounted(() => {
  loadScripts()
  loadTasks()
})
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

  :deep(.n-data-table) {
    .n-data-table__pagination {
      padding: 16px 0;
    }
  }
}
</style>