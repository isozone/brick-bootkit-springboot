<template>
  <div class="dependency-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">依赖分析</h1>
        <p class="page-subtitle">插件依赖关系、兼容性与升级影响面分析</p>
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

    <!-- 版本兼容性矩阵 -->
    <n-card title="插件版本矩阵" class="matrix-card">
      <template #header-extra>
        <n-tag type="primary" size="small">插件 {{ matrix.length }}</n-tag>
      </template>
      <n-empty v-if="matrix.length === 0 && !loading" description="暂无插件数据" size="small" />
      <n-data-table
        :columns="matrixColumns"
        :data="matrix"
        :loading="loading"
        :bordered="false"
        size="small"
      />
    </n-card>

    <!-- 依赖图 -->
    <n-card title="依赖图" class="graph-card">
      <n-empty v-if="graphNodes.length === 0 && !loading" description="暂无依赖关系" size="small" />
      <template v-else>
        <div class="graph-stats">
          <n-tag size="small" type="primary">节点 {{ graphNodes.length }}</n-tag>
          <n-tag size="small" type="warning">边 {{ graphEdges.length }}</n-tag>
          <n-tag v-if="isolatedNodes.length" size="small" type="default">孤立插件 {{ isolatedNodes.length }}</n-tag>
        </div>
        <n-data-table
          :columns="edgeColumns"
          :data="graphEdges"
          :bordered="false"
          size="small"
          :pagination="{ pageSize: 20 }"
        />
      </template>
    </n-card>

    <!-- 依赖详情分析 -->
    <n-card title="插件依赖详情" class="detail-card">
      <div class="detail-toolbar">
        <n-select
          v-model:value="selectedPlugin"
          :options="pluginOptions"
          placeholder="选择插件"
          clearable
          style="width: 260px;"
          @update:value="loadDetail"
        />
      </div>

      <template v-if="detail">
        <n-descriptions :column="3" label-placement="left" bordered size="small">
          <n-descriptions-item label="插件 ID">
            <span class="mono">{{ detail.pluginId || '-' }}</span>
          </n-descriptions-item>
          <n-descriptions-item label="名称">{{ detail.name || '-' }}</n-descriptions-item>
          <n-descriptions-item label="版本">{{ detail.version || '-' }}</n-descriptions-item>
        </n-descriptions>

        <n-divider title-placement="left">依赖关系</n-divider>
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-card size="small" title="必需依赖">
              <template #header-extra>
                <n-tag type="error" size="small">{{ (detail.required || []).length }}</n-tag>
              </template>
              <n-empty v-if="!(detail.required || []).length" description="无必需依赖" size="small" />
              <ul v-else class="dep-list">
                <li v-for="dep in detail.required" :key="dep">
                  <span class="mono">{{ dep }}</span>
                  <n-tag v-if="detail.versions && detail.versions[dep]" size="small" style="margin-left: 8px;">{{ detail.versions[dep] }}</n-tag>
                </li>
              </ul>
            </n-card>
          </n-gi>
          <n-gi>
            <n-card size="small" title="可选依赖">
              <template #header-extra>
                <n-tag type="warning" size="small">{{ (detail.optional || []).length }}</n-tag>
              </template>
              <n-empty v-if="!(detail.optional || []).length" description="无可选依赖" size="small" />
              <ul v-else class="dep-list">
                <li v-for="dep in detail.optional" :key="dep">
                  <span class="mono">{{ dep }}</span>
                  <n-tag v-if="detail.versions && detail.versions[dep]" size="small" style="margin-left: 8px;">{{ detail.versions[dep] }}</n-tag>
                </li>
              </ul>
            </n-card>
          </n-gi>
        </n-grid>

        <n-divider title-placement="left">升级影响面（谁依赖该插件）</n-divider>
        <n-empty v-if="!(detail.reverseDependencies || []).length" description="无插件依赖此插件" size="small" />
        <n-space v-else>
          <n-tag v-for="dep in detail.reverseDependencies" :key="dep" type="info" size="small">
            <span class="mono">{{ dep }}</span>
          </n-tag>
        </n-space>

        <n-divider title-placement="left">解析与兼容性</n-divider>
        <n-space>
          <n-button size="small" type="primary" ghost @click="loadResolve">依赖解析</n-button>
          <n-button size="small" type="warning" ghost @click="loadCompatibility">兼容性检查</n-button>
          <n-button size="small" type="info" ghost @click="loadImpact">影响面分析</n-button>
        </n-space>

        <template v-if="resolveResult">
          <n-alert v-if="resolveResult.successful" type="success" :show-icon="true" style="margin-top: 12px;">
            依赖解析成功：{{ (resolveResult.dependencies || []).join(' → ') || '无依赖' }}
          </n-alert>
          <n-alert v-else type="error" :show-icon="true" style="margin-top: 12px;">
            依赖解析失败：{{ (resolveResult.errors || []).join('; ') }}
          </n-alert>
        </template>

        <template v-if="compatibilityResult">
          <n-alert
            :type="compatibilityResult.compatible ? 'success' : 'error'"
            :show-icon="true"
            style="margin-top: 12px;"
          >
            {{ compatibilityResult.compatible ? '兼容性检查通过' : '存在兼容性问题' }}
            <template v-if="(compatibilityResult.errors || []).length">
              ：{{ compatibilityResult.errors.join('; ') }}
            </template>
          </n-alert>
        </template>

        <template v-if="impactList.length">
          <n-divider title-placement="left">影响面列表</n-divider>
          <n-tag v-for="dep in impactList" :key="dep" type="warning" size="small" style="margin: 2px;">
            <span class="mono">{{ dep }}</span>
          </n-tag>
        </template>
      </template>
      <n-empty v-else-if="!loading" description="选择插件查看依赖详情" size="small" />
    </n-card>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NTag, NSelect, NDataTable, NEmpty, NDivider,
  NDescriptions, NDescriptionsItem, NAlert, NGrid, NGi, NSpace, useMessage
} from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import { dependencyApi } from '@/api/services'

const message = useMessage()

const loading = ref(false)
const matrix = ref([])
const graphNodes = ref([])
const graphEdges = ref([])
const selectedPlugin = ref(null)
const detail = ref(null)
const resolveResult = ref(null)
const compatibilityResult = ref(null)
const impactList = ref([])

const pluginOptions = computed(() =>
  matrix.value.map(p => ({ label: `${p.name || p.pluginId} (${p.pluginId})`, value: p.pluginId }))
)

const isolatedNodes = computed(() => {
  const connected = new Set()
  graphEdges.value.forEach(e => { connected.add(e.from); connected.add(e.to) })
  return graphNodes.value.filter(n => !connected.has(n.id)).map(n => n.id)
})

const stateTypeMap = {
  STARTED: 'success',
  STOPPED: 'default',
  LOADED: 'warning',
  FAILED: 'error'
}

const matrixColumns = [
  { title: '插件 ID', key: 'pluginId', minWidth: 160, render: (row) => h('span', { class: 'mono' }, row.pluginId || '-') },
  { title: '名称', key: 'name', minWidth: 120, render: (row) => row.name || '-' },
  { title: '版本', key: 'version', width: 90, render: (row) => row.version || '-' },
  { title: '要求主版本', key: 'requires', width: 110, render: (row) => row.requires || '-' },
  {
    title: '状态',
    key: 'state',
    width: 90,
    render: (row) => h(NTag, { type: stateTypeMap[row.state] || 'default', size: 'small' }, { default: () => row.state || '-' })
  },
  { title: '依赖数', key: 'dependencyCount', width: 80, render: (row) => row.dependencyCount ?? 0 }
]

const edgeColumns = [
  {
    title: '依赖方',
    key: 'from',
    minWidth: 160,
    render: (row) => h('span', { class: 'mono' }, row.from || '-')
  },
  { title: '箭头', key: 'arrow', width: 50, render: () => '→' },
  {
    title: '被依赖方',
    key: 'to',
    minWidth: 160,
    render: (row) => h('span', { class: 'mono' }, row.to || '-')
  },
  {
    title: '类型',
    key: 'optional',
    width: 80,
    render: (row) => h(NTag, { type: row.optional ? 'warning' : 'error', size: 'small' }, { default: () => row.optional ? '可选' : '必需' })
  }
]

const refreshData = async () => {
  loading.value = true
  try {
    const [matrixRes, graphRes] = await Promise.all([
      dependencyApi.getMatrix(),
      dependencyApi.getGraph()
    ])
    if (matrixRes.code === 200) matrix.value = matrixRes.data || []
    if (graphRes.code === 200) {
      const graph = graphRes.data || {}
      graphNodes.value = graph.nodes || []
      graphEdges.value = graph.edges || []
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadDetail = async (pluginId) => {
  if (!pluginId) {
    detail.value = null
    return
  }
  detail.value = null
  resolveResult.value = null
  compatibilityResult.value = null
  impactList.value = []
  try {
    const res = await dependencyApi.getDetail(pluginId)
    if (res.code === 200) {
      detail.value = res.data
    } else {
      message.error(res.message || '加载详情失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载详情失败')
  }
}

const loadResolve = async () => {
  try {
    const res = await dependencyApi.resolve(selectedPlugin.value)
    if (res.code === 200) resolveResult.value = res.data
  } catch (e) {
    message.error(e.response?.data?.message || '解析失败')
  }
}

const loadCompatibility = async () => {
  try {
    const res = await dependencyApi.compatibility(selectedPlugin.value)
    if (res.code === 200) compatibilityResult.value = res.data
  } catch (e) {
    message.error(e.response?.data?.message || '兼容性检查失败')
  }
}

const loadImpact = async () => {
  try {
    const res = await dependencyApi.impact(selectedPlugin.value)
    if (res.code === 200) impactList.value = res.data || []
  } catch (e) {
    message.error(e.response?.data?.message || '影响面分析失败')
  }
}

onMounted(refreshData)
</script>

<style lang="scss" scoped>
.dependency-page {
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

.matrix-card,
.graph-card,
.detail-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.graph-stats {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.detail-toolbar {
  margin-bottom: 12px;
}

.dep-list {
  margin: 0;
  padding-left: 20px;
  max-height: 200px;
  overflow-y: auto;

  li {
    margin-bottom: 4px;
  }
}

:deep(.mono) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
}
</style>
