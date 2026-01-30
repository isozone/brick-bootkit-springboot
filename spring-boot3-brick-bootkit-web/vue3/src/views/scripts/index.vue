<template>
  <div class="scripts-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">脚本管理</h1>
        <p class="page-subtitle">管理和执行各类脚本</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="showCreateModal = true">
          <template #icon>
            <n-icon><AddOutline /></n-icon>
          </template>
          新建脚本
        </n-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" class="stat-grid">
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon primary">
              <n-icon size="24"><CodeSlashOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ scripts.length }}</div>
              <div class="stat-label">脚本总数</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon success">
              <n-icon size="24"><PlayCircleOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ todayExecutions }}</div>
              <div class="stat-label">今日执行</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon warning">
              <n-icon size="24"><TimeOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ schedulerCount }}</div>
              <div class="stat-label">调度任务</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon info">
              <n-icon size="24"><DocumentTextOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ templateCount }}</div>
              <div class="stat-label">模板数量</div>
            </div>
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 脚本列表 -->
    <n-card title="脚本列表" class="list-card">
      <template #header-extra>
        <div class="table-actions">
          <n-select
            v-model:value="filterType"
            :options="scriptTypeOptions"
            placeholder="全部类型"
            clearable
            style="width: 140px;"
            size="small"
          />
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索脚本..."
            clearable
            style="width: 200px;"
            size="small"
          >
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
        </div>
      </template>
      
      <n-data-table
        :columns="columns"
        :data="filteredScripts"
        :loading="loading"
        :bordered="false"
        :pagination="false"
        size="small"
      />
      
      <n-empty v-if="filteredScripts.length === 0 && !loading" description="暂无脚本">
        <template #extra>
          <n-button type="primary" size="small" @click="showCreateModal = true">
            新建脚本
          </n-button>
        </template>
      </n-empty>
    </n-card>

    <!-- 新建脚本弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="dialog" title="新建脚本" style="width: 600px;">
      <n-form ref="formRef" :model="newScript" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="脚本名称" path="scriptName">
          <n-input v-model:value="newScript.scriptName" placeholder="请输入脚本名称" />
        </n-form-item>
        <n-form-item label="脚本类型" path="scriptType">
          <n-select
            v-model:value="newScript.scriptType"
            :options="SCRIPT_TYPES"
            placeholder="请选择脚本类型"
          />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="newScript.description" type="textarea" placeholder="请输入脚本描述" />
        </n-form-item>
        <n-form-item label="作者" path="author">
          <n-input v-model:value="newScript.author" placeholder="请输入作者" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showCreateModal = false">取消</n-button>
        <n-button type="primary" @click="createScript" :loading="creating">创建</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard, NGrid, NGi, NButton, NIcon, NInput, NSelect, NDataTable,
  NEmpty, NModal, NForm, NFormItem, NTag, NSpace, NPopconfirm, useMessage
} from 'naive-ui'
import {
  AddOutline, CodeSlashOutline, PlayCircleOutline, TimeOutline,
  DocumentTextOutline, SearchOutline, TrashOutline, PencilOutline, PlayOutline
} from '@vicons/ionicons5'
import { scriptsApi, schedulerApi, executionsApi } from '@/api/services'
import { SCRIPT_TYPES } from '@/constants'

const router = useRouter()
const message = useMessage()

// 脚本列表
const scripts = ref([])
const loading = ref(false)
const filterType = ref(null)
const searchKeyword = ref('')
const creating = ref(false)

// 统计数据
const todayExecutions = ref(0)
const schedulerCount = ref(0)
const templateCount = ref(0)

// 新建脚本弹窗
const showCreateModal = ref(false)
const newScript = ref({
  scriptName: '',
  scriptType: null,
  description: '',
  author: ''
})

const formRules = {
  scriptName: { required: true, message: '请输入脚本名称' },
  scriptType: { required: true, message: '请选择脚本类型' }
}

// 脚本类型选项
const scriptTypeOptions = SCRIPT_TYPES.map(t => ({ label: t.label, value: t.value }))

// 过滤后的脚本列表
const filteredScripts = computed(() => {
  let result = scripts.value
  if (filterType.value) {
    result = result.filter(s => s.scriptType === filterType.value)
  }
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(s =>
      s.scriptName?.toLowerCase().includes(keyword) ||
      s.description?.toLowerCase().includes(keyword)
    )
  }
  return result
})

// 表格列配置
const columns = [
  {
    title: '脚本名称',
    key: 'scriptName',
    render(row) {
      return h('div', { class: 'script-name' }, [
        h(NIcon, { size: 18, color: '#2563eb' }, () => h(CodeSlashOutline)),
        h('div', { class: 'script-info' }, [
          h('span', { class: 'script-name-text' }, row.scriptName),
          h('span', { class: 'script-desc' }, row.description || '暂无描述')
        ])
      ])
    }
  },
  {
    title: '类型',
    key: 'scriptType',
    width: 100,
    render(row) {
      const type = SCRIPT_TYPES.find(t => t.value === row.scriptType)
      return h(NTag, {
        size: 'small',
        color: { color: type?.color + '20', textColor: type?.color, borderColor: 'transparent' }
      }, () => type?.label || row.scriptType)
    }
  },
  {
    title: '版本',
    key: 'version',
    width: 80,
    render(row) {
      return h(NTag, { size: 'small', bordered: false }, () => row.version || 'v1.0')
    }
  },
  {
    title: '作者',
    key: 'author',
    width: 100
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-'
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render(row) {
      return h(NSpace, { size: 'small' }, () => [
        h(NButton, {
          size: 'small',
          quaternary: true,
          onClick: () => executeScript(row)
        }, () => h(NIcon, null, () => h(PlayOutline))),
        h(NButton, {
          size: 'small',
          quaternary: true,
          onClick: () => editScript(row)
        }, () => h(NIcon, null, () => h(PencilOutline))),
        h(NButton, {
          size: 'small',
          quaternary: true,
          onClick: () => viewHistory(row)
        }, () => h(NIcon, null, () => h(TimeOutline))),
        h(NPopconfirm, {
          onPositiveClick: () => deleteScript(row)
        }, {
          trigger: () => h(NButton, {
            size: 'small',
            quaternary: true,
            type: 'error'
          }, () => h(NIcon, null, () => h(TrashOutline))),
          default: () => '确定删除该脚本？'
        })
      ])
    }
  }
]

// 加载脚本列表
const loadScripts = async () => {
  loading.value = true
  try {
    const res = await scriptsApi.getAll()
    if (res.code === 200) {
      scripts.value = res.data || []
    }
  } catch (e) {
    message.error('加载脚本列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const [execRes, schedRes, tplRes] = await Promise.all([
      executionsApi.getAll().catch(() => ({ data: [] })),
      schedulerApi.getAll().catch(() => ({ data: [] })),
      // templatesApi.getAll().catch(() => ({ data: [] }))
    ])
    todayExecutions.value = execRes.data?.length || 0
    schedulerCount.value = schedRes.data?.length || 0
    // templateCount.value = tplRes.data?.length || 0
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
}

// 执行脚本
const executeScript = async (script) => {
  message.info(`执行脚本: ${script.scriptName}`)
  // 实际项目中这里应该打开参数输入对话框
}

// 编辑脚本
const editScript = (script) => {
  router.push(`/scripts/editor?name=${script.scriptName}`)
}

// 查看执行记录
const viewHistory = (script) => {
  router.push(`/scripts/executions?script=${script.scriptName}`)
}

// 删除脚本
const deleteScript = async (script) => {
  try {
    const res = await scriptsApi.delete(script.scriptName)
    if (res.code === 200) {
      message.success('删除成功')
      loadScripts()
    } else {
      message.error(res.message || '删除失败')
    }
  } catch (e) {
    message.error('删除失败')
  }
}

// 创建脚本
const createScript = async () => {
  if (!newScript.value.scriptName || !newScript.value.scriptType) {
    message.warning('请填写必填项')
    return
  }
  
  creating.value = true
  try {
    const res = await scriptsApi.create(newScript.value)
    if (res.code === 200) {
      message.success('创建成功')
      showCreateModal.value = false
      newScript.value = { scriptName: '', scriptType: null, description: '', author: '' }
      loadScripts()
    } else {
      message.error(res.message || '创建失败')
    }
  } catch (e) {
    message.error('创建失败')
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  loadScripts()
  loadStatistics()
})
</script>

<style lang="scss" scoped>
.scripts-page {
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
  display: flex;
  align-items: center;
  justify-content: center;
  
  &.primary {
    background: rgba(37, 99, 235, 0.1);
    color: #2563eb;
  }
  
  &.success {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
  }
  
  &.warning {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
  }
  
  &.info {
    background: rgba(59, 130, 246, 0.1);
    color: #3b82f6;
  }
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.list-card {
  border-radius: 8px;
}

.table-actions {
  display: flex;
  gap: 12px;
}

:deep(.script-name) {
  display: flex;
  align-items: center;
  gap: 10px;
}

:deep(.script-info) {
  display: flex;
  flex-direction: column;
}

:deep(.script-name-text) {
  font-weight: 500;
  color: #1f2937;
}

:deep(.script-desc) {
  font-size: 12px;
  color: #9ca3af;
}
</style>