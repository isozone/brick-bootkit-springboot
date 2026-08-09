<template>
  <div class="logs-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">{{ $t('logs.title') }}</h1>
        <p class="page-subtitle">{{ $t('logs.subtitle') }}</p>
      </div>
      <div class="header-actions">
        <n-button type="primary" @click="loadLogs">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新
        </n-button>
      </div>
    </div>

    <!-- 日志工具栏 -->
    <n-card class="toolbar-card">
      <div class="toolbar">
        <n-input
          v-model:value="keyword"
          placeholder="输入插件 ID 或关键字过滤（如 demo-plugin）"
          clearable
          style="width: 320px;"
          @keyup.enter="loadLogs"
        >
          <template #prefix>
            <n-icon><SearchOutline /></n-icon>
          </template>
        </n-input>
        <n-select
          v-model:value="lines"
          :options="linesOptions"
          style="width: 120px;"
        />
        <n-button @click="loadLogs">查询</n-button>
        <n-tag v-if="logFile" type="info" size="small" style="margin-left: 8px;">
          {{ logFile }}
        </n-tag>
      </div>
    </n-card>

    <!-- 日志内容 -->
    <n-card class="log-card">
      <template #header-extra>
        <n-tag type="primary" size="small">共 {{ filteredLogs.length }} 行</n-tag>
      </template>
      <n-empty v-if="filteredLogs.length === 0 && !loading" description="暂无日志（请确认已配置 logging.file.name 或工作目录存在日志文件）">
        <template #extra>
          <n-button size="small" type="primary" @click="loadLogs">刷新</n-button>
        </template>
      </n-empty>
      <pre v-else class="log-content" :class="{ loading }">
        <template v-for="(line, i) in filteredLogs" :key="i">
          <span :class="lineClass(line)">{{ line }}</span>
        </template>
      </pre>
    </n-card>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NInput, NSelect, NTag, NEmpty, useMessage
} from 'naive-ui'
import { RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import { logsApi } from '@/api/services'

const message = useMessage()

const loading = ref(false)
const keyword = ref('')
const lines = ref(200)
const logFile = ref('')
const logs = ref([])

const linesOptions = [
  { label: '100 行', value: 100 },
  { label: '200 行', value: 200 },
  { label: '500 行', value: 500 },
  { label: '1000 行', value: 1000 }
]

const filteredLogs = computed(() => logs.value)

const loadLogs = async () => {
  loading.value = true
  try {
    const [fileRes, logsRes] = await Promise.all([
      logsApi.getLogFile(),
      logsApi.getLogs(keyword.value, lines.value)
    ])
    if (fileRes.code === 200) logFile.value = fileRes.data || ''
    if (logsRes.code === 200) logs.value = logsRes.data || []
  } catch (e) {
    message.error(e.response?.data?.message || '加载日志失败')
  } finally {
    loading.value = false
  }
}

const lineClass = (line) => {
  if (!line) return ''
  if (line.includes('ERROR') || line.includes('Exception')) return 'log-error'
  if (line.includes('WARN')) return 'log-warn'
  return ''
}

onMounted(loadLogs)
</script>

<style lang="scss" scoped>
.logs-page {
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

.toolbar-card,
.log-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.log-content {
  max-height: 70vh;
  overflow-y: auto;
  background: #0f172a;
  color: #e2e8f0;
  padding: 16px;
  border-radius: 8px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;

  &.loading {
    opacity: 0.5;
  }

  span {
    display: block;
  }

  .log-error {
    color: #f87171;
  }

  .log-warn {
    color: #fbbf24;
  }
}
</style>
