<template>
  <div class="script-templates-page">
    <div class="page-header">
      <h2 class="page-title">脚本模板</h2>
      <n-button type="primary" @click="showCreateModal = true">
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
        新建模板
      </n-button>
    </div>

    <div class="templates-grid">
      <div v-for="template in templates" :key="template.id" class="template-card">
        <div class="template-header">
          <div class="template-icon" :style="{ background: template.color }">
            <n-icon size="24"><CodeSlashOutline /></n-icon>
          </div>
          <n-tag size="small" type="info">{{ template.type }}</n-tag>
        </div>
        <div class="template-body">
          <h3 class="template-name">{{ template.name }}</h3>
          <p class="template-desc">{{ template.description }}</p>
        </div>
        <div class="template-footer">
          <n-button size="small" @click="handlePreview(template)">预览</n-button>
          <n-button size="small" type="primary" @click="handleUse(template)">使用</n-button>
        </div>
      </div>
    </div>

    <n-modal v-model:show="showPreview" preset="card" title="模板预览" style="width: 900px">
      <CodeEditor :model-value="currentTemplate?.code" :language="currentTemplate?.type" :readonly="true" :show-header="false" height="400px" />
    </n-modal>

    <n-modal v-model:show="showCreateModal" preset="card" title="新建模板" style="width: 900px">
      <n-form ref="formRef" :model="newTemplate" :rules="formRules" label-placement="top">
        <n-form-item label="模板名称" path="name">
          <n-input v-model:value="newTemplate.name" placeholder="请输入模板名称" />
        </n-form-item>
        <n-form-item label="模板类型" path="type">
          <n-select v-model:value="newTemplate.type" :options="scriptTypeOptions" placeholder="请选择模板类型" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="newTemplate.description" type="textarea" placeholder="请输入模板描述" :rows="2" />
        </n-form-item>
        <n-form-item label="模板代码" path="code">
          <CodeEditor v-model="newTemplate.code" :language="newTemplate.type || 'shell'" height="300px" title="模板代码" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleCreate">确定</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NTag, NModal, NInput, NSelect, NForm, NFormItem, useMessage } from 'naive-ui'
import { AddOutline, CodeSlashOutline } from '@vicons/ionicons5'
import { SCRIPT_TYPES } from '@/constants'
import CodeEditor from '@/components/CodeEditor.vue'

const router = useRouter()
const message = useMessage()

const showCreateModal = ref(false)
const showPreview = ref(false)
const currentTemplate = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const scriptTypeOptions = SCRIPT_TYPES.map(t => ({ label: t.label, value: t.value }))

const newTemplate = ref({
  name: '',
  type: '',
  description: '',
  code: ''
})

const formRules = {
  name: { required: true, message: '请输入模板名称' },
  type: { required: true, message: '请选择模板类型' }
}

const templates = ref([
  { id: 1, name: '数据库备份', type: 'SQL', description: '完整的数据库备份脚本', color: '#8b5cf6', code: '-- 数据库备份\nBACKUP DATABASE' },
  { id: 2, name: '日志轮转', type: 'Shell', description: '日志文件轮转和清理', color: '#2563eb', code: '#!/bin/bash\nfind /var/log -name "*.log"' },
  { id: 3, name: '系统监控', type: 'Python', description: '系统资源监控脚本', color: '#f59e0b', code: '#!/usr/bin/env python3\nimport psutil' },
  { id: 4, name: '文件同步', type: 'Shell', description: '文件同步到远程服务器', color: '#10b981', code: '#!/bin/bash\nrsync -avz' },
  { id: 5, name: '服务检查', type: 'PowerShell', description: 'Windows 服务状态检查', color: '#0284c7', code: 'Get-Service | Where-Object {$_.Status -eq "Running"}' },
  { id: 6, name: '性能测试', type: 'Python', description: 'Web 服务性能测试', color: '#ec4899', code: '#!/usr/bin/env python3\nimport locust' }
])

const handlePreview = (template) => {
  currentTemplate.value = template
  showPreview.value = true
}

const handleUse = (template) => {
  router.push({ path: '/scripts/editor', query: { templateId: template.id } })
  message.success('已选择模板')
}

const handleCreate = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const newId = Math.max(...templates.value.map(t => t.id), 0) + 1
    const typeInfo = SCRIPT_TYPES.find(t => t.value === newTemplate.value.type) || { color: '#6b7280' }

    const template = {
      id: newId,
      name: newTemplate.value.name,
      type: newTemplate.value.type,
      description: newTemplate.value.description,
      code: newTemplate.value.code,
      color: typeInfo.color
    }

    templates.value.unshift(template)

    newTemplate.value = { name: '', type: '', description: '', code: '' }
    showCreateModal.value = false

    message.success('模板创建成功')
  } catch (e) {
    console.error('创建模板失败:', e)
    message.error('创建模板失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.script-templates-page {
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

  .templates-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }

  .template-card {
    background: #fff;
    border-radius: 12px;
    border: 1px solid #e5e7eb;
    padding: 20px;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      transform: translateY(-2px);
    }

    .template-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .template-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
      }
    }

    .template-body {
      margin-bottom: 16px;

      .template-name {
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 8px;
      }

      .template-desc {
        font-size: 14px;
        color: #6b7280;
      }
    }

    .template-footer {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
    }
  }
}
</style>