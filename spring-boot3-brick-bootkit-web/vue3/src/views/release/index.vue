<template>
  <div class="release-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">发布治理</h1>
        <p class="page-subtitle">插件发布流程编排、灰度探针与发布审计可视化</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" :loading="loading" @click="refreshData">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新
        </n-button>
      </div>
    </div>

    <!-- 当前发布模式 -->
    <n-card title="当前发布模式" class="mode-card">
      <n-descriptions :column="2" label-placement="left" bordered size="small">
        <n-descriptions-item label="发布模式">
          <n-tag :type="config.mode === 'GRAY' ? 'warning' : 'success'" size="small">
            {{ config.modeDescription || config.mode || '直接发布' }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="失败自动回滚">
          <n-tag :type="config.rollbackOnFailure ? 'success' : 'default'" size="small">
            {{ config.rollbackOnFailure ? '开启' : '关闭' }}
          </n-tag>
        </n-descriptions-item>
      </n-descriptions>
      <n-alert type="info" :show-icon="true" style="margin-top: 12px;">
        灰度模式（plugin.rolloutMode=gray）下，插件升级会执行宿主注入的灰度探针；任一探针未通过则升级失败并触发备份回滚，同时写入一条 FAILED 发布记录。
        多版本服务可通过 ServiceMetadata 声明 WEIGHTED 权重实现调用级金丝雀流量按比例分流。
      </n-alert>
    </n-card>

    <!-- 发布记录 -->
    <n-card title="发布记录" class="record-card">
      <template #header-extra>
        <n-tag type="primary" size="small">共 {{ releases.length }} 条</n-tag>
      </template>
      <n-data-table
        :columns="columns"
        :data="releases"
        :bordered="false"
        :loading="loading"
        size="small"
        :row-key="(row) => row.releaseId"
      />
    </n-card>

    <!-- 发布详情 -->
    <n-modal
      v-model:show="detailVisible"
      title="发布记录详情"
      preset="card"
      style="width: 640px;"
    >
      <n-descriptions v-if="detail" :column="1" label-placement="left" bordered size="small">
        <n-descriptions-item label="发布 ID">{{ detail.releaseId }}</n-descriptions-item>
        <n-descriptions-item label="插件 ID">{{ detail.pluginId }}</n-descriptions-item>
        <n-descriptions-item label="插件名称">{{ detail.pluginName }}</n-descriptions-item>
        <n-descriptions-item label="版本演进">{{ detail.fromVersion || '-' }} → {{ detail.toVersion || '-' }}</n-descriptions-item>
        <n-descriptions-item label="发布模式">{{ detail.mode }}</n-descriptions-item>
        <n-descriptions-item label="状态">
          <n-tag :type="statusType(detail.status)" size="small">{{ detail.status }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="操作人">{{ detail.operator }}</n-descriptions-item>
        <n-descriptions-item label="开始时间">{{ formatTime(detail.startTime) }}</n-descriptions-item>
        <n-descriptions-item label="结束时间">{{ formatTime(detail.endTime) }}</n-descriptions-item>
        <n-descriptions-item label="备份路径">{{ detail.backupPath || '-' }}</n-descriptions-item>
        <n-descriptions-item label="错误信息">
          <span class="mono error-text">{{ detail.errorMessage || '-' }}</span>
        </n-descriptions-item>
      </n-descriptions>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NTag, NDataTable, NEmpty,
  NDescriptions, NDescriptionsItem, NAlert, NModal, useMessage, useDialog
} from 'naive-ui'
import { RefreshOutline, EyeOutline, TrashOutline } from '@vicons/ionicons5'
import { releaseApi, rolloutApi } from '@/api/services'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const config = ref({})
const releases = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const statusType = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'error'
  if (status === 'UPGRADING') return 'warning'
  return 'default'
}

const formatTime = (ts) => {
  if (!ts) return '-'
  try {
    const d = new Date(ts)
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
      `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch (e) {
    return String(ts)
  }
}

const columns = [
  {
    title: '发布 ID',
    key: 'releaseId',
    minWidth: 200,
    render: (row) => h('span', { class: 'mono' }, row.releaseId || '-')
  },
  {
    title: '插件',
    key: 'pluginName',
    minWidth: 160,
    render: (row) => `${row.pluginName || row.pluginId || '-'}`
  },
  {
    title: '版本演进',
    key: 'version',
    minWidth: 160,
    render: (row) => `${row.fromVersion || '-'} → ${row.toVersion || '-'}`
  },
  {
    title: '模式',
    key: 'mode',
    width: 110,
    render: (row) => h(NTag, { size: 'small', type: row.mode === 'GRAY' ? 'warning' : 'success' },
      { default: () => row.mode || 'DIRECT' })
  },
  {
    title: '状态',
    key: 'status',
    width: 110,
    render: (row) => h(NTag, { size: 'small', type: statusType(row.status) },
      { default: () => row.status || '-' })
  },
  {
    title: '操作人',
    key: 'operator',
    width: 110,
    render: (row) => row.operator || '-'
  },
  {
    title: '开始时间',
    key: 'startTime',
    minWidth: 170,
    render: (row) => formatTime(row.startTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render: (row) => h('div', { style: 'display:flex; gap:8px;' }, [
      h(NButton, {
        size: 'small', quaternary: true, type: 'primary',
        onClick: () => openDetail(row)
      }, { default: () => '查看', icon: () => h(NIcon, null, { default: () => h(EyeOutline) }) }),
      h(NButton, {
        size: 'small', quaternary: true, type: 'error',
        onClick: () => removeRelease(row)
      }, { default: () => '删除', icon: () => h(NIcon, null, { default: () => h(TrashOutline) }) })
    ])
  }
]

const openDetail = async (row) => {
  try {
    const res = await releaseApi.get(row.releaseId)
    if (res.code === 200) {
      detail.value = res.data
      detailVisible.value = true
    } else {
      message.error(res.message || '获取详情失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '获取详情失败')
  }
}

const removeRelease = (row) => {
  dialog.warning({
    title: '删除发布记录',
    content: `确认删除发布记录 ${row.releaseId}？该操作不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await releaseApi.remove(row.releaseId)
        if (res.code === 200) {
          message.success('删除成功')
          await refreshData()
        } else {
          message.error(res.message || '删除失败')
        }
      } catch (e) {
        message.error(e.response?.data?.message || '删除失败')
      }
    }
  })
}

const refreshData = async () => {
  loading.value = true
  try {
    const [configRes, listRes] = await Promise.all([
      rolloutApi.getConfig().catch(() => ({ code: 200, data: {} })),
      releaseApi.list(50)
    ])
    if (configRes.code === 200) config.value = configRes.data || {}
    if (listRes.code === 200) {
      releases.value = listRes.data || []
    } else {
      message.error(listRes.message || '加载发布记录失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(refreshData)
</script>

<style lang="scss" scoped>
.release-page {
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

.mode-card,
.record-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

:deep(.mono) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
}

.error-text {
  color: #ef4444;
}
</style>
