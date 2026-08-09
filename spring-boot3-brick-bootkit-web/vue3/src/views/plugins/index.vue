<template>
  <div class="plugins-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">{{ $t('plugins.title') }}</h1>
        <p class="page-subtitle">{{ $t('plugins.subtitle') }}</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="goUpload">
          <template #icon>
            <n-icon><CloudUploadOutline /></n-icon>
          </template>
          上传插件
        </n-button>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <n-button-group>
        <n-button
          :type="stateFilter === 'all' ? 'primary' : 'default'"
          @click="stateFilter = 'all'"
        >
          全部 {{ stats.total }}
        </n-button>
        <n-button
          :type="stateFilter === 'STARTED' ? 'success' : 'default'"
          @click="stateFilter = 'STARTED'"
        >
          运行中 {{ stats.started }}
        </n-button>
        <n-button
          :type="stateFilter === 'STOPPED' ? 'secondary' : 'default'"
          @click="stateFilter = 'STOPPED'"
        >
          已停止 {{ stats.stopped }}
        </n-button>
      </n-button-group>
      
      <n-input
        v-model:value="searchKeyword"
        placeholder="搜索插件..."
        clearable
        style="width: 200px;"
      >
        <template #prefix>
          <n-icon><SearchOutline /></n-icon>
        </template>
      </n-input>
    </div>

    <!-- 插件列表 -->
    <n-card class="list-card">
      <n-data-table
        :columns="columns"
        :data="filteredPlugins"
        :loading="loading"
        :bordered="false"
        :pagination="pagination"
        :row-key="row => row.pluginId"
      />
      
      <n-empty v-if="filteredPlugins.length === 0 && !loading" description="暂无插件">
        <template #extra>
          <n-button type="primary" size="small" @click="goUpload">
            上传插件
          </n-button>
        </template>
      </n-empty>
    </n-card>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard, NButton, NIcon, NInput, NDataTable, NTag, NSpace,
  NEmpty, NButtonGroup, NPopconfirm, useMessage
} from 'naive-ui'
import {
  CloudUploadOutline, SearchOutline, PlayOutline, StopOutline,
  RefreshOutline, TrashOutline, CubeOutline
} from '@vicons/ionicons5'
import { pluginsApi } from '@/api/services'

const router = useRouter()
const message = useMessage()

// 插件列表
const plugins = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const stateFilter = ref('all')
const stats = ref({ total: 0, started: 0, stopped: 0, failed: 0 })

// 分页
const pagination = ref({
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => {
    pagination.value.page = page
    loadPlugins()
  },
  onUpdatePageSize: (size) => {
    pagination.value.pageSize = size
    pagination.value.page = 1
    loadPlugins()
  }
})

// 过滤后的插件列表
const filteredPlugins = computed(() => {
  if (!searchKeyword.value) return plugins.value
  const keyword = searchKeyword.value.toLowerCase()
  return plugins.value.filter(p =>
    p.name?.toLowerCase().includes(keyword) ||
    p.pluginId?.toLowerCase().includes(keyword) ||
    p.description?.toLowerCase().includes(keyword)
  )
})

// 获取状态文本
const getStateText = (state) => {
  const stateMap = {
    'STARTED': '运行中',
    'STOPPED': '已停止',
    'LOADED': '已加载',
    'FAILED': '异常'
  }
  return stateMap[state] || state || '未知'
}

// 获取状态类型
const getStateType = (state) => {
  const typeMap = {
    'STARTED': 'success',
    'STOPPED': 'default',
    'LOADED': 'warning',
    'FAILED': 'error'
  }
  return typeMap[state] || 'default'
}

// 获取状态图标类
const getStateClass = (state) => {
  const classMap = {
    'STARTED': 'started',
    'STOPPED': 'stopped',
    'FAILED': 'failed'
  }
  return classMap[state] || ''
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 表格列配置
const columns = [
  {
    title: '插件信息',
    key: 'pluginId',
    width: 220,
    render(row) {
      const iconClass = 'plugin-icon ' + getStateClass(row.state)
      const name = row.name || row.pluginId
      return h('div', { class: 'plugin-info' }, [
        h('div', { class: iconClass }, [
          h(NIcon, { size: 20 }, () => h(CubeOutline))
        ]),
        h('div', { class: 'plugin-details' }, [
          h('span', { class: 'plugin-name' }, name),
          h('span', { class: 'plugin-id' }, row.pluginId)
        ])
      ])
    }
  },
  {
    title: '版本',
    key: 'version',
    width: 100,
    render(row) {
      return h(NTag, { size: 'small', bordered: false }, () => row.version || '-')
    }
  },
  {
    title: '状态',
    key: 'state',
    width: 100,
    render(row) {
      return h(NTag, {
        size: 'small',
        type: getStateType(row.state)
      }, () => getStateText(row.state))
    }
  },
  {
    title: '启动时间',
    key: 'startTime',
    width: 160,
    render(row) {
      return h('span', { class: 'text-secondary' }, formatTime(row.startTime))
    }
  },
  {
    title: '描述',
    key: 'description',
    ellipsis: { tooltip: true }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render(row) {
      const actions = []
      actions.push(h(NButton, {
        size: 'small',
        quaternary: true,
        type: 'success',
        disabled: row.state === 'STARTED',
        onClick: () => startPlugin(row.pluginId)
      }, () => h(NIcon, null, () => h(PlayOutline))))
      actions.push(h(NButton, {
        size: 'small',
        quaternary: true,
        type: 'warning',
        disabled: row.state !== 'STARTED',
        onClick: () => stopPlugin(row.pluginId)
      }, () => h(NIcon, null, () => h(StopOutline))))
      actions.push(h(NButton, {
        size: 'small',
        quaternary: true,
        onClick: () => restartPlugin(row.pluginId)
      }, () => h(NIcon, null, () => h(RefreshOutline))))
      actions.push(h(NPopconfirm, {
        onPositiveClick: () => uninstallPlugin(row.pluginId)
      }, {
        trigger: () => h(NButton, {
          size: 'small',
          quaternary: true,
          type: 'error'
        }, () => h(NIcon, null, () => h(TrashOutline))),
        default: () => '确定卸载该插件？此操作不可恢复！'
      }))
      return h(NSpace, { size: 'small' }, { default: () => actions })
    }
  }
]

// 加载插件列表
const loadPlugins = async () => {
  loading.value = true
  try {
    const res = await pluginsApi.getList(
      pagination.value.page,
      pagination.value.pageSize,
      stateFilter.value,
      searchKeyword.value
    )
    
    if (res.code === 200 && res.data) {
      const pageData = res.data
      plugins.value = pageData.records || []
      pagination.value.pageCount = pageData.pages || 1
      pagination.value.itemCount = pageData.total || 0
      
      // 计算统计数据
      stats.value.total = pageData.total || (pageData.records ? pageData.records.length : 0)
      stats.value.started = pageData.records ? pageData.records.filter(p => p.state === 'STARTED').length : 0
      stats.value.stopped = pageData.records ? pageData.records.filter(p => p.state === 'STOPPED').length : 0
      stats.value.failed = pageData.records ? pageData.records.filter(p => p.state === 'FAILED').length : 0
    }
  } catch (e) {
    message.error('加载插件列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 启动插件
const startPlugin = async (pluginId) => {
  try {
    const res = await pluginsApi.start(pluginId)
    if (res.code === 200) {
      message.success('插件启动成功')
      loadPlugins()
    } else {
      message.error(res.message || '启动失败')
    }
  } catch (e) {
    message.error('启动失败')
  }
}

// 停止插件
const stopPlugin = async (pluginId) => {
  try {
    const res = await pluginsApi.stop(pluginId)
    if (res.code === 200) {
      message.success('插件停止成功')
      loadPlugins()
    } else {
      message.error(res.message || '停止失败')
    }
  } catch (e) {
    message.error('停止失败')
  }
}

// 重启插件
const restartPlugin = async (pluginId) => {
  try {
    const res = await pluginsApi.restart(pluginId)
    if (res.code === 200) {
      message.success('插件重启成功')
      loadPlugins()
    } else {
      message.error(res.message || '重启失败')
    }
  } catch (e) {
    message.error('重启失败')
  }
}

// 卸载插件
const uninstallPlugin = async (pluginId) => {
  try {
    const res = await pluginsApi.uninstall(pluginId)
    if (res.code === 200) {
      message.success('插件卸载成功')
      loadPlugins()
    } else {
      message.error(res.message || '卸载失败')
    }
  } catch (e) {
    message.error('卸载失败')
  }
}

// 跳转到上传页面
const goUpload = () => {
  router.push('/plugins/upload')
}

// 监听筛选条件变化
watch(stateFilter, () => {
  pagination.value.page = 1
  loadPlugins()
})

onMounted(() => {
  loadPlugins()
})
</script>

<style lang="scss" scoped>
.plugins-page {
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

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-card {
  border-radius: 8px;
}

.plugin-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.plugin-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #6b7280;
  
  &.started {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
  }
  
  &.stopped {
    background: #f3f4f6;
    color: #6b7280;
  }
  
  &.failed {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
  }
}

.plugin-details {
  display: flex;
  flex-direction: column;
}

.plugin-name {
  font-weight: 500;
  color: #1f2937;
}

.plugin-id {
  font-size: 12px;
  color: #9ca3af;
}

.text-secondary {
  color: #6b7280;
  font-size: 13px;
}
</style>