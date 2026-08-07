<template>
  <div class="rollout-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">灰度发布</h1>
        <p class="page-subtitle">插件升级灰度策略配置、探针管理与决策模拟</p>
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

    <!-- 灰度配置 -->
    <n-card title="灰度发布配置" class="config-card">
      <n-descriptions :column="3" label-placement="left" bordered size="small">
        <n-descriptions-item label="发布模式">
          <n-tag :type="config.mode === 'GRAY' ? 'warning' : 'success'" size="small">
            {{ config.modeDescription || config.mode || '-' }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="安装后自动启动">
          <n-tag :type="config.autoStart ? 'success' : 'default'" size="small">
            {{ config.autoStart ? '是' : '否' }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="失败自动回滚">
          <n-tag :type="config.rollbackOnFailure ? 'success' : 'default'" size="small">
            {{ config.rollbackOnFailure ? '是' : '否' }}
          </n-tag>
        </n-descriptions-item>
      </n-descriptions>
      <n-alert type="info" :show-icon="true" style="margin-top: 12px;">
        灰度模式（plugin.rolloutMode=gray）下，插件升级时会依次执行宿主注入的灰度探针；任一探针未通过则升级失败并触发回滚。
      </n-alert>
    </n-card>

    <!-- 已注册探针 -->
    <n-card title="已注册灰度探针" class="probe-card">
      <template #header-extra>
        <n-tag type="primary" size="small">探针 {{ probes.length }}</n-tag>
      </template>
      <n-empty v-if="probes.length === 0 && !loading" description="暂无灰度探针（宿主未注入 PluginRolloutProbe）" size="small" />
      <n-space v-else>
        <n-tag v-for="probe in probes" :key="probe" type="info" size="small">
          <span class="mono">{{ probe }}</span>
        </n-tag>
      </n-space>
    </n-card>

    <!-- 灰度决策模拟 -->
    <n-card title="灰度决策模拟" class="check-card">
      <div class="check-toolbar">
        <n-select
          v-model:value="selectedPlugin"
          :options="pluginOptions"
          placeholder="选择插件"
          clearable
          style="width: 260px;"
        />
        <n-button type="primary" :loading="checking" :disabled="!selectedPlugin" @click="runCheck">
          <template #icon>
            <n-icon><RocketOutline /></n-icon>
          </template>
          模拟灰度决策
        </n-button>
      </div>

      <template v-if="decision">
        <n-alert
          :type="decision.passed ? 'success' : 'error'"
          :show-icon="true"
          style="margin-top: 12px;"
        >
          {{ decision.passed ? '灰度决策通过：所有探针校验成功，可升级' : '灰度决策未通过：存在未通过的探针，建议回滚' }}
        </n-alert>
        <n-data-table
          :columns="probeResultColumns"
          :data="decision.probes || []"
          :bordered="false"
          size="small"
          style="margin-top: 12px;"
        />
      </template>
    </n-card>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NTag, NSelect, NDataTable, NEmpty,
  NDescriptions, NDescriptionsItem, NAlert, NSpace, useMessage
} from 'naive-ui'
import { RefreshOutline, RocketOutline } from '@vicons/ionicons5'
import { rolloutApi, pluginsApi } from '@/api/services'

const message = useMessage()

const loading = ref(false)
const checking = ref(false)
const config = ref({})
const probes = ref([])
const pluginOptions = ref([])
const selectedPlugin = ref(null)
const decision = ref(null)

const probeResultColumns = [
  { title: '探针名称', key: 'name', minWidth: 160, render: (row) => h('span', { class: 'mono' }, row.name || '-') },
  {
    title: '结果',
    key: 'passed',
    width: 90,
    render: (row) => h(NTag, { type: row.passed ? 'success' : 'error', size: 'small' }, { default: () => row.passed ? '通过' : '未通过' })
  },
  { title: '说明', key: 'message', minWidth: 200, render: (row) => row.message || '-' }
]

const refreshData = async () => {
  loading.value = true
  try {
    const [configRes, probesRes, pluginsRes] = await Promise.all([
      rolloutApi.getConfig(),
      rolloutApi.getProbes(),
      pluginsApi.getAll()
    ])
    if (configRes.code === 200) config.value = configRes.data || {}
    if (probesRes.code === 200) probes.value = probesRes.data || []
    if (pluginsRes.code === 200) {
      pluginOptions.value = (pluginsRes.data || []).map(p => ({
        label: `${p.name || p.pluginId} (${p.pluginId})`,
        value: p.pluginId
      }))
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const runCheck = async () => {
  if (!selectedPlugin.value) return
  checking.value = true
  decision.value = null
  try {
    const res = await rolloutApi.check(selectedPlugin.value)
    if (res.code === 200) {
      decision.value = res.data
    } else {
      message.error(res.message || '模拟失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '模拟失败')
  } finally {
    checking.value = false
  }
}

onMounted(refreshData)
</script>

<style lang="scss" scoped>
.rollout-page {
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

.config-card,
.probe-card,
.check-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.check-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.mono) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
}
</style>
