<template>
  <div class="security-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">安全中心</h1>
        <p class="page-subtitle">插件安全扫描、策略与权限管理</p>
      </div>
    </div>

    <!-- 扫描工具 -->
    <n-card title="插件安全扫描" class="scan-card">
      <div class="scan-tools">
        <div class="scan-row">
          <n-input
            v-model:value="scanPluginId"
            placeholder="输入插件 ID 扫描（如 plugin-demo）"
            clearable
            style="width: 320px;"
          >
            <template #prefix>
              <n-icon><CubeOutline /></n-icon>
            </template>
          </n-input>
          <n-button type="primary" :loading="scanning" :disabled="!scanPluginId" @click="scanById">
            <template #icon>
              <n-icon><SearchOutline /></n-icon>
            </template>
            按插件 ID 扫描
          </n-button>
        </div>
        <div class="scan-row">
          <n-input
            v-model:value="scanPath"
            placeholder="输入插件文件路径（仅允许插件目录或上传临时目录内）"
            clearable
            style="width: 480px;"
          >
            <template #prefix>
              <n-icon><DocumentTextOutline /></n-icon>
            </template>
          </n-input>
          <n-button :loading="scanning" :disabled="!scanPath" @click="scanByPath">
            <template #icon>
              <n-icon><SearchOutline /></n-icon>
            </template>
            按路径扫描
          </n-button>
        </div>
      </div>
    </n-card>

    <!-- 扫描结果 -->
    <n-card v-if="scanResult" title="扫描报告" class="result-card">
      <template #header-extra>
        <n-tag :type="scanResult.valid ? 'success' : 'error'" size="small">
          {{ scanResult.valid ? '通过' : '存在违规' }}
        </n-tag>
        <n-tag :type="riskTagType" size="small" style="margin-left: 8px;">
          风险级别: {{ riskLevelText }}
        </n-tag>
      </template>

      <n-descriptions :column="3" label-placement="left" bordered size="small">
        <n-descriptions-item label="插件 ID">{{ scanResult.pluginId || '-' }}</n-descriptions-item>
        <n-descriptions-item label="验证时间">{{ scanResult.validationTime || '-' }}</n-descriptions-item>
        <n-descriptions-item label="问题总数">{{ scanResult.totalIssues ?? 0 }}</n-descriptions-item>
      </n-descriptions>

      <div class="issue-grid">
        <n-card size="small" title="违规项" class="issue-card">
          <template #header-extra>
            <n-tag type="error" size="small">{{ (scanResult.violations || []).length }}</n-tag>
          </template>
          <n-empty v-if="!(scanResult.violations || []).length" description="无违规" size="small" />
          <ul v-else class="issue-list">
            <li v-for="(v, i) in scanResult.violations" :key="i" class="violation">{{ v }}</li>
          </ul>
        </n-card>
        <n-card size="small" title="警告项" class="issue-card">
          <template #header-extra>
            <n-tag type="warning" size="small">{{ (scanResult.warnings || []).length }}</n-tag>
          </template>
          <n-empty v-if="!(scanResult.warnings || []).length" description="无警告" size="small" />
          <ul v-else class="issue-list">
            <li v-for="(w, i) in scanResult.warnings" :key="i" class="warning">{{ w }}</li>
          </ul>
        </n-card>
      </div>

      <!-- 代码扫描详情 -->
      <template v-if="scanResult.codeScanResult">
        <n-divider title-placement="left">代码扫描详情</n-divider>
        <n-descriptions :column="3" label-placement="left" bordered size="small">
          <n-descriptions-item label="风险评分">{{ scanResult.codeScanResult.riskScore ?? 0 }}</n-descriptions-item>
          <n-descriptions-item label="扫描目标">{{ scanResult.codeScanResult.pluginId || '-' }}</n-descriptions-item>
          <n-descriptions-item label="扫描耗时">{{ scanResult.codeScanResult.scanDurationMs ?? 0 }} ms</n-descriptions-item>
        </n-descriptions>
        <n-data-table
          v-if="scanResult.codeScanResult.violations?.length || scanResult.codeScanResult.warnings?.length"
          :columns="scanColumns"
          :data="scanRows"
          :bordered="false"
          size="small"
          style="margin-top: 12px;"
        />
      </template>
    </n-card>

    <!-- 安全策略 -->
    <n-card title="安全策略" class="policy-card">
      <div class="policy-row">
        <n-input
          v-model:value="policyPluginId"
          placeholder="输入插件 ID 管理策略"
          clearable
          style="width: 320px;"
        />
        <n-button type="primary" ghost :disabled="!policyPluginId" @click="loadPolicy">
          加载策略
        </n-button>
        <n-button type="primary" :loading="savingPolicy" :disabled="!policyPluginId" @click="savePolicy" style="margin-left: 8px;">
          保存策略
        </n-button>
      </div>

      <template v-if="policy">
        <n-divider title-placement="left">访问控制</n-divider>
        <n-space>
          <n-checkbox v-model:checked="policy.allowFileSystemAccess">允许文件系统访问</n-checkbox>
          <n-checkbox v-model:checked="policy.allowNetworkAccess">允许网络访问</n-checkbox>
          <n-checkbox v-model:checked="policy.allowSystemPropertyAccess">允许系统属性访问</n-checkbox>
          <n-checkbox v-model:checked="policy.allowReflectionAccess">允许反射访问</n-checkbox>
        </n-space>
        <n-divider title-placement="left">资源限制</n-divider>
        <n-space align="center">
          <n-input-number v-model:value="policy.maxMemoryUsage" :min="0" placeholder="最大内存(字节)" style="width: 220px;" />
          <n-input-number v-model:value="policy.maxThreadCount" :min="0" placeholder="最大线程数" style="width: 160px;" />
        </n-space>
      </template>
      <n-empty v-else-if="!policyLoading" description="输入插件 ID 后点击「加载策略」" />
    </n-card>

    <!-- 权限管理 -->
    <n-card title="权限管理" class="permission-card">
      <div class="permission-row">
        <n-input
          v-model:value="permPluginId"
          placeholder="输入插件 ID 查看权限"
          clearable
          style="width: 320px;"
        />
        <n-button type="primary" ghost :disabled="!permPluginId" @click="loadPermissions">
          查看权限
        </n-button>
      </div>

      <template v-if="permissions.length">
        <n-data-table
          :columns="permissionColumns"
          :data="permissions"
          :bordered="false"
          size="small"
          style="margin-top: 12px;"
        />
      </template>

      <n-divider title-placement="left">授予 / 撤销权限</n-divider>
      <div class="permission-row">
        <n-input v-model:value="grantForm.pluginId" placeholder="插件 ID" clearable style="width: 160px;" />
        <n-select
          v-model:value="grantForm.type"
          :options="permissionTypeOptions"
          placeholder="权限类型"
          style="width: 160px;"
        />
        <n-input v-model:value="grantForm.target" placeholder="目标（路径/主机/类名）" clearable style="width: 200px;" />
        <n-input v-model:value="grantForm.action" placeholder="操作（read/write）" clearable style="width: 140px;" />
        <n-button type="success" :disabled="!canGrant" @click="grantPermission">
          授予
        </n-button>
        <n-button type="warning" :disabled="!canGrant" @click="revokePermission">
          撤销
        </n-button>
      </div>
    </n-card>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NInput, NInputNumber, NSelect, NCheckbox, NTag, NSpace,
  NDataTable, NEmpty, NDivider, NDescriptions, NDescriptionsItem, useMessage
} from 'naive-ui'
import { CubeOutline, SearchOutline, DocumentTextOutline } from '@vicons/ionicons5'
import { securityApi } from '@/api/services'

const message = useMessage()

const scanning = ref(false)
const scanPluginId = ref('')
const scanPath = ref('')
const scanResult = ref(null)

const policyPluginId = ref('')
const policy = ref(null)
const policyLoading = ref(false)
const savingPolicy = ref(false)

const permPluginId = ref('')
const permissions = ref([])
const grantForm = ref({ pluginId: '', type: null, target: '', action: '' })

const permissionTypeOptions = [
  { label: '文件系统', value: 'file_system' },
  { label: '网络', value: 'network' },
  { label: '系统属性', value: 'system_property' },
  { label: '反射', value: 'reflection' },
  { label: '运行时', value: 'runtime' },
  { label: '数据库', value: 'database' },
  { label: 'JMX', value: 'jmx' },
  { label: '全部', value: 'all' }
]

const riskTagType = computed(() => {
  const level = scanResult.value?.riskLevel
  if (level === 'CRITICAL' || level === 'HIGH') return 'error'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
})

const riskLevelText = computed(() => {
  const level = scanResult.value?.riskLevel
  const map = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险', CRITICAL: '严重风险' }
  return map[level] || level || '-'
})

const canGrant = computed(() =>
  grantForm.value.pluginId && grantForm.value.type && grantForm.value.target && grantForm.value.action
)

// 扫描
const scanById = async () => {
  scanning.value = true
  try {
    const res = await securityApi.scanByPluginId(scanPluginId.value)
    if (res.code === 200) {
      scanResult.value = res.data
      message.success('扫描完成')
    } else {
      message.error(res.message || '扫描失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '扫描失败')
  } finally {
    scanning.value = false
  }
}

const scanByPath = async () => {
  scanning.value = true
  try {
    const res = await securityApi.scanByPath(scanPath.value)
    if (res.code === 200) {
      scanResult.value = res.data
      message.success('扫描完成')
    } else {
      message.error(res.message || '扫描失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '扫描失败')
  } finally {
    scanning.value = false
  }
}

// 策略
const loadPolicy = async () => {
  policyLoading.value = true
  try {
    const res = await securityApi.getPolicy(policyPluginId.value)
    if (res.code === 200) {
      policy.value = res.data || {
        allowFileSystemAccess: false,
        allowNetworkAccess: false,
        allowSystemPropertyAccess: false,
        allowReflectionAccess: false,
        maxMemoryUsage: 0,
        maxThreadCount: 0
      }
    } else {
      message.error(res.message || '加载策略失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载策略失败')
  } finally {
    policyLoading.value = false
  }
}

const savePolicy = async () => {
  savingPolicy.value = true
  try {
    const res = await securityApi.setPolicy({ pluginId: policyPluginId.value, ...policy.value })
    if (res.code === 200) {
      message.success('策略已保存')
    } else {
      message.error(res.message || '保存失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '保存失败')
  } finally {
    savingPolicy.value = false
  }
}

// 权限
const loadPermissions = async () => {
  try {
    const res = await securityApi.getPermissions(permPluginId.value)
    if (res.code === 200) {
      permissions.value = res.data || []
      if (!permissions.value.length) message.info('该插件暂无权限')
    } else {
      message.error(res.message || '加载权限失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '加载权限失败')
  }
}

const grantPermission = async () => {
  try {
    const res = await securityApi.grantPermission(grantForm.value)
    if (res.code === 200) {
      message.success('已授予权限')
      if (permPluginId.value === grantForm.value.pluginId) loadPermissions()
    } else {
      message.error(res.message || '授予失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '授予失败')
  }
}

const revokePermission = async () => {
  try {
    const res = await securityApi.revokePermission(grantForm.value)
    if (res.code === 200) {
      message.success('已撤销权限')
      if (permPluginId.value === grantForm.value.pluginId) loadPermissions()
    } else {
      message.error(res.message || '撤销失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '撤销失败')
  }
}

// 扫描详情表格
const scanColumns = [
  { title: '级别', key: 'level', width: 80, render: (row) => h(NTag, { type: row.level === 'violation' ? 'error' : 'warning', size: 'small' }, { default: () => row.level === 'violation' ? '违规' : '警告' }) },
  { title: '类型', key: 'type' },
  { title: '描述', key: 'description' },
  { title: '位置', key: 'location' }
]

const scanRows = computed(() => {
  const csr = scanResult.value?.codeScanResult
  if (!csr) return []
  const rows = []
  ;(csr.violations || []).forEach(v => rows.push({ level: 'violation', type: v.type, description: v.description, location: v.location }))
  ;(csr.warnings || []).forEach(w => rows.push({ level: 'warning', type: w.type, description: w.description, location: w.location }))
  return rows
})

const permissionColumns = [
  { title: '类型', key: 'type', render: (row) => row.type?.getCode ? row.type.getCode() : row.type },
  { title: '目标', key: 'target' },
  { title: '操作', key: 'action' },
  { title: '描述', key: 'description' }
]
</script>

<style lang="scss" scoped>
.security-page {
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

.scan-card,
.result-card,
.policy-card,
.permission-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.scan-tools {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scan-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.policy-row,
.permission-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.issue-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 12px;
}

.issue-list {
  margin: 0;
  padding-left: 20px;

  .violation {
    color: #dc2626;
    margin-bottom: 4px;
  }

  .warning {
    color: #d97706;
    margin-bottom: 4px;
  }
}
</style>
