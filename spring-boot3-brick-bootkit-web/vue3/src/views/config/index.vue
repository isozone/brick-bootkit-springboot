<template>
  <div class="config-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">插件配置</h1>
        <p class="page-subtitle">查看与热更新插件配置，支持版本管理与回滚</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="refreshData">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新
        </n-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" class="stat-grid">
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon primary">
              <n-icon size="24"><SettingsOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalConfigurations || 0 }}</div>
              <div class="stat-label">配置总数</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon success">
              <n-icon size="24"><GitCommitOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalVersions || 0 }}</div>
              <div class="stat-label">版本总数</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon info">
              <n-icon size="24"><EyeOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.activeWatchers || 0 }}</div>
              <div class="stat-label">热更新监控</div>
            </div>
          </div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon warning">
              <n-icon size="24"><NotificationsOutline /></n-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.listenerCount || 0 }}</div>
              <div class="stat-label">变更监听器</div>
            </div>
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 配置列表 -->
    <n-card title="插件配置列表" class="list-card">
      <template #header-extra>
        <n-input v-model:value="keyword" placeholder="搜索插件 ID" clearable style="width: 200px;" size="small">
          <template #prefix>
            <n-icon><SearchOutline /></n-icon>
          </template>
        </n-input>
      </template>

      <n-empty v-if="configList.length === 0 && !loading" description="暂无插件配置">
        <template #extra>
          <n-button size="small" type="primary" @click="refreshData">刷新</n-button>
        </template>
      </n-empty>

      <n-data-table
        :columns="columns"
        :data="filteredConfigs"
        :loading="loading"
        :bordered="false"
        :pagination="pagination"
        size="small"
      />
    </n-card>

    <!-- 配置详情弹窗 -->
    <n-modal
      v-model:show="showDetail"
      preset="card"
      :title="`配置详情 - ${detailPluginId}`"
      style="width: 720px;"
    >
      <template v-if="detailConfig">
        <n-descriptions :column="2" label-placement="left" bordered size="small">
          <n-descriptions-item label="插件 ID">{{ detailConfig.pluginId || '-' }}</n-descriptions-item>
          <n-descriptions-item label="版本">{{ detailConfig.version || '-' }}</n-descriptions-item>
          <n-descriptions-item label="环境">{{ detailConfig.environment || '-' }}</n-descriptions-item>
          <n-descriptions-item label="启用">
            <n-tag :type="detailConfig.enabled ? 'success' : 'default'" size="small">
              {{ detailConfig.enabled ? '是' : '否' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="创建时间">{{ formatTime(detailConfig.createdAt) }}</n-descriptions-item>
          <n-descriptions-item label="更新时间">{{ formatTime(detailConfig.updatedAt) }}</n-descriptions-item>
        </n-descriptions>

        <n-divider title-placement="left">配置属性</n-divider>
        <n-data-table
          v-if="propertyRows.length"
          :columns="propertyColumns"
          :data="propertyRows"
          :bordered="false"
          size="small"
        />
        <n-empty v-else description="暂无配置属性" size="small" />

        <n-divider title-placement="left">编辑属性</n-divider>
        <div class="edit-row">
          <n-input v-model:value="newProperty.key" placeholder="属性名" clearable style="width: 200px;" />
          <n-input v-model:value="newProperty.value" placeholder="属性值" clearable style="width: 200px;" />
          <n-button type="primary" size="small" :disabled="!newProperty.key" @click="addProperty">添加</n-button>
        </div>
        <div class="modal-actions">
          <n-input v-model:value="versionDescription" placeholder="变更说明（可选）" clearable style="width: 320px;" />
          <n-button type="primary" :loading="saving" @click="saveConfig">保存并热更新</n-button>
        </div>

        <n-divider title-placement="left">版本历史</n-divider>
        <n-data-table
          v-if="versionList.length"
          :columns="versionColumns"
          :data="versionList"
          :bordered="false"
          size="small"
        />
        <n-empty v-else description="暂无版本历史" size="small" />
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NInput, NTag, NDataTable, NEmpty, NModal, NDivider,
  NDescriptions, NDescriptionsItem, NSpace, useMessage, useDialog
} from 'naive-ui'
import {
  RefreshOutline, SettingsOutline, GitCommitOutline, EyeOutline,
  NotificationsOutline, SearchOutline
} from '@vicons/ionicons5'
import { configApi } from '@/api/services'

const message = useMessage()
const dialog = useDialog()

const stats = ref({})
const configs = ref([])
const loading = ref(false)
const keyword = ref('')

const showDetail = ref(false)
const detailPluginId = ref('')
const detailConfig = ref(null)
const versionList = ref([])
const propertyRows = ref([])
const newProperty = ref({ key: '', value: '' })
const versionDescription = ref('')
const saving = ref(false)

const pagination = ref({ page: 1, pageSize: 10, showSizePicker: true, pageSizes: [10, 20, 50] })

const filteredConfigs = computed(() => {
  if (!keyword.value) return configs.value
  const kw = keyword.value.toLowerCase()
  return configs.value.filter(c => (c.pluginId || '').toLowerCase().includes(kw))
})

const columns = [
  { title: '插件 ID', key: 'pluginId', minWidth: 160, render: (row) => h('span', { class: 'mono' }, row.pluginId || '-') },
  { title: '版本', key: 'version', width: 90, render: (row) => row.version || '-' },
  { title: '环境', key: 'environment', width: 100, render: (row) => row.environment || '-' },
  {
    title: '启用',
    key: 'enabled',
    width: 70,
    render: (row) => h(NTag, { type: row.enabled ? 'success' : 'default', size: 'small' }, { default: () => row.enabled ? '是' : '否' })
  },
  { title: '属性数', key: 'propertyCount', width: 80, render: (row) => (row.properties ? Object.keys(row.properties).length : 0) },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    render: (row) => h(NSpace, { size: 4 }, {
      default: () => [
        h(NButton, { size: 'small', type: 'primary', ghost: true, onClick: () => openDetail(row.pluginId) }, { default: () => '查看/编辑' }),
        h(NButton, { size: 'small', type: 'error', ghost: true, onClick: () => confirmRemove(row.pluginId) }, { default: () => '删除' })
      ]
    })
  }
]

const propertyColumns = [
  { title: '属性名', key: 'key', render: (row) => h('span', { class: 'mono' }, row.key) },
  { title: '属性值', key: 'value', render: (row) => h('span', { class: 'mono' }, JSON.stringify(row.value)) }
]

const versionColumns = [
  {
    title: '版本 ID',
    key: 'versionId',
    minWidth: 160,
    render: (row) => h('span', { class: 'mono' }, row.versionId || '-')
  },
  {
    title: '变更说明',
    key: 'changeReason',
    minWidth: 160,
    render: (row) => row.changeReason || '-'
  },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    render: (row) => h(NButton, {
      size: 'small',
      type: 'warning',
      ghost: true,
      disabled: row.versionId === detailConfig.value?.version,
      onClick: () => confirmRollback(row)
    }, { default: () => '回滚' })
  }
]

const refreshData = async () => {
  loading.value = true
  try {
    const [statsRes, listRes] = await Promise.all([
      configApi.getStatistics(),
      configApi.getAll()
    ])
    if (statsRes.code === 200) stats.value = statsRes.data || {}
    if (listRes.code === 200) {
      const data = listRes.data || {}
      configs.value = Object.entries(data).map(([pluginId, cfg]) => ({ pluginId, ...cfg }))
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (pluginId) => {
  detailPluginId.value = pluginId
  showDetail.value = true
  detailConfig.value = null
  versionList.value = []
  propertyRows.value = []
  try {
    const [detailRes, versionRes] = await Promise.all([
      configApi.getByPluginId(pluginId),
      configApi.getVersions(pluginId)
    ])
    if (detailRes.code === 200) {
      detailConfig.value = detailRes.data
      buildPropertyRows()
    }
    if (versionRes.code === 200) versionList.value = versionRes.data || []
  } catch (e) {
    message.error(e.response?.data?.message || '加载配置失败')
  }
}

const buildPropertyRows = () => {
  const props = detailConfig.value?.properties || {}
  propertyRows.value = Object.entries(props).map(([key, value]) => ({ key, value }))
}

const addProperty = () => {
  if (!newProperty.value.key) return
  const key = newProperty.value.key
  let value = newProperty.value.value
  // 简单类型推断
  if (value === 'true' || value === 'false') value = value === 'true'
  else if (/^-?\d+$/.test(value)) value = Number(value)
  propertyRows.value.push({ key, value })
  newProperty.value = { key: '', value: '' }
}

const saveConfig = async () => {
  if (!detailConfig.value) return
  saving.value = true
  try {
    const configuration = {
      ...detailConfig.value,
      properties: Object.fromEntries(propertyRows.value.map(r => [r.key, r.value]))
    }
    const res = await configApi.update(detailPluginId.value, configuration, versionDescription.value || 'Web 控制台热更新')
    if (res.code === 200) {
      message.success('配置已热更新')
      detailConfig.value = res.data
      versionDescription.value = ''
      refreshData()
      openDetail(detailPluginId.value)
    } else {
      message.error(res.message || '更新失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

const confirmRollback = (row) => {
  dialog.warning({
    title: '确认回滚',
    content: `确定要将配置回滚到版本 ${row.versionId} 吗？`,
    positiveText: '回滚',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await configApi.rollback(detailPluginId.value, row.versionId)
        if (res.code === 200) {
          message.success('已回滚')
          refreshData()
          openDetail(detailPluginId.value)
        } else {
          message.error(res.message || '回滚失败')
        }
      } catch (e) {
        message.error(e.response?.data?.message || '回滚失败')
      }
    }
  })
}

const confirmRemove = (pluginId) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除插件 ${pluginId} 的配置吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await configApi.remove(pluginId)
        if (res.code === 200) {
          message.success('配置已删除')
          refreshData()
        } else {
          message.error(res.message || '删除失败')
        }
      } catch (e) {
        message.error(e.response?.data?.message || '删除失败')
      }
    }
  })
}

const formatTime = (t) => {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 19)
}

onMounted(refreshData)
</script>

<style lang="scss" scoped>
.config-page {
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

  &.primary { background: rgba(37, 99, 235, 0.1); color: #2563eb; }
  &.success { background: rgba(16, 185, 129, 0.1); color: #10b981; }
  &.warning { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
  &.info { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
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

.edit-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.modal-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

:deep(.mono) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
}
</style>
