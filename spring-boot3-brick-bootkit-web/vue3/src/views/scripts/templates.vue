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

    <!-- 预览模态框 -->
    <n-modal v-model:show="showPreview" preset="card" title="模板预览" style="width: 800px">
      <n-code :code="currentTemplate?.code" language="shell" />
    </n-modal>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NTag, NModal, NCode, useMessage } from 'naive-ui'
import { AddOutline, CodeSlashOutline } from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()

const showCreateModal = ref(false)
const showPreview = ref(false)
const currentTemplate = ref(null)

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
</script>

<style lang="scss" scoped>
.script-templates-page {
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
