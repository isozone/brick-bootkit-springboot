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
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton, NIcon, NTag, NModal, NInput, NSelect, NForm, NFormItem, useMessage } from 'naive-ui'
import { AddOutline, CodeSlashOutline } from '@vicons/ionicons5'
import { SCRIPT_TYPES } from '@/constants'
import { templatesApi } from '@/api/services'
import CodeEditor from '@/components/CodeEditor.vue'

const router = useRouter()
const route = useRoute()
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
  type: { required: true, message: '请选择模板类型' },
  code: { required: true, message: '请输入模板代码' }
}

const templates = ref([])

const typeColorMap = {
  SQL: '#8b5cf6',
  Shell: '#2563eb',
  Python: '#f59e0b',
  PowerShell: '#0284c7',
  JavaScript: '#ec4899'
}

const getTypeColor = (type) => typeColorMap[type?.toUpperCase()] || '#6b7280'

const loadTemplates = async () => {
  try {
    const res = await templatesApi.getAll()
    if (res.code === 200 || res.code === 0) {
      const data = res.data || res
      templates.value = data.map(t => ({
        id: t.templateId || t.id,
        name: t.displayName || t.templateName || t.name,
        type: t.scriptType || t.type,
        description: t.description,
        code: t.templateContent || t.code,
        color: getTypeColor(t.scriptType || t.type)
      }))
    }
  } catch (e) {
    console.error('加载模板列表失败:', e)
    message.error('加载模板列表失败')
  }
}

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
    const templateData = {
      templateName: newTemplate.value.name,
      displayName: newTemplate.value.name,
      description: newTemplate.value.description,
      scriptType: newTemplate.value.type?.toUpperCase(),
      templateContent: newTemplate.value.code,
      category: 'custom'
    }

    const res = await templatesApi.create(templateData)
    
    if (res.code === 200 || res.code === 0) {
      const created = res.data
      templates.value.unshift({
        id: created.templateId || created.id,
        name: created.displayName || created.templateName || newTemplate.value.name,
        type: created.scriptType || newTemplate.value.type,
        description: created.description,
        code: created.templateContent || newTemplate.value.code,
        color: getTypeColor(created.scriptType || newTemplate.value.type)
      })

      newTemplate.value = { name: '', type: '', description: '', code: '' }
      showCreateModal.value = false
      message.success('模板创建成功')
    } else {
      message.error(res.message || '创建模板失败')
    }
  } catch (e) {
    console.error('创建模板失败:', e)
    message.error(e.response?.data?.message || '创建模板失败')
  } finally {
    submitting.value = false
  }
}

// 监听路由变化，重新加载数据
watch(
  () => route.path,
  (newPath) => {
    if (newPath === '/scripts/templates') {
      loadTemplates()
    }
  }
)

onMounted(() => {
  loadTemplates()
})
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