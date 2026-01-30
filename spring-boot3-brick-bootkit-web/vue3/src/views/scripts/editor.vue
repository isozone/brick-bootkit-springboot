<template>
  <div class="script-editor">
    <div class="editor-header">
      <div class="header-left">
        <n-button quaternary circle @click="handleGoBack">
          <template #icon>
            <n-icon><ArrowBackOutline /></n-icon>
          </template>
        </n-button>
        <span class="editor-title">{{ isEdit ? '编辑脚本' : '新建脚本' }}</span>
      </div>
      <div class="header-right">
        <n-button type="warning" @click="handleTestScript" :loading="testing">测试运行</n-button>
        <n-button @click="handleSaveDraft">保存草稿</n-button>
        <n-button type="primary" @click="handleSave">保存并发布</n-button>
      </div>
    </div>

    <div class="editor-content">
      <!-- 基本信息 -->
      <div class="info-panel">
        <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="80">
          <n-form-item label="名称" path="name">
            <n-input v-model:value="formData.name" placeholder="请输入脚本名称" />
          </n-form-item>
          <n-form-item label="类型" path="type">
            <n-select
              v-model:value="formData.type"
              :options="scriptTypeOptions"
              placeholder="选择脚本类型"
            />
          </n-form-item>
          <n-form-item label="描述" path="description">
            <n-input
              v-model:value="formData.description"
              type="textarea"
              placeholder="请输入脚本描述"
              :rows="3"
            />
          </n-form-item>
        </n-form>

        <div class="template-section">
          <div class="section-title">从模板创建</div>
          <div class="template-list">
            <div
              v-for="template in templates"
              :key="template.id"
              class="template-item"
              @click="applyTemplate(template)"
            >
              <div class="template-name">{{ template.name }}</div>
              <div class="template-desc">{{ template.description }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 代码编辑器 -->
      <div class="code-panel">
        <div class="editor-toolbar">
          <n-space>
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button quaternary circle size="small" @click="handleFormat">
                  <template #icon>
                    <n-icon><CodeOutline /></n-icon>
                  </template>
                </n-button>
              </template>
              格式化代码
            </n-tooltip>
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button quaternary circle size="small" @click="handleCopy">
                  <template #icon>
                    <n-icon><CopyOutline /></n-icon>
                  </template>
                </n-button>
              </template>
              复制代码
            </n-tooltip>
          </n-space>
          <div class="file-type">{{ getFileExtension(formData.type) }}</div>
        </div>

        <div class="editor-wrapper">
          <textarea
            ref="codeEditorRef"
            v-model="formData.code"
            class="code-textarea"
            placeholder="# 在此编写脚本代码..."
            @keydown.tab="handleTabKey"
          ></textarea>
        </div>
      </div>
    </div>

    <!-- 执行参数 -->
    <div class="params-panel">
      <div class="panel-title">执行参数</div>
      <n-dynamic-input
        v-model:value="formData.params"
        :min="0"
        :on-create="() => ({ key: '', value: '' })"
      >
        <template #create-button-default>添加参数</template>
        <template #default="{ value }">
          <div class="param-row">
            <n-input v-model:value="value.key" placeholder="参数名" style="width: 200px" />
            <n-input v-model:value="value.value" placeholder="参数值" style="flex: 1" />
          </div>
        </template>
      </n-dynamic-input>
    </div>
  </div>

  <!-- 测试结果弹窗 -->
  <n-modal v-model:show="showTestResult" preset="dialog" title="执行结果" style="width: 700px">
    <div v-if="testResult">
      <n-tag :type="testResult.success ? 'success' : 'error'" style="margin-bottom: 12px">
        {{ testResult.success ? '执行成功' : '执行失败' }}
      </n-tag>
      <div v-if="testResult.exitCode !== undefined" style="margin-bottom: 12px; color: #666">
        退出码: {{ testResult.exitCode }} | 耗时: {{ testResult.durationMs }}ms
      </div>
      <n-code :code="testResult.output || testResult.errorMessage" language="bash" style="max-height: 400px; overflow: auto" />
    </div>
  </n-modal>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NIcon,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NSpace,
  NTooltip,
  NDynamicInput,
  NModal,
  NCode,
  NTag,
  useMessage
} from 'naive-ui'
import {
  ArrowBackOutline,
  CodeOutline,
  CopyOutline,
  PlayOutline
} from '@vicons/ionicons5'
import { SCRIPT_TYPES, MESSAGE } from '@/constants'
import { templatesApi, scriptsApi } from '@/api/services'

const props = defineProps({
  scriptId: {
    type: [String, Number],
    default: null
  }
})

const emit = defineEmits(['save', 'close'])

const route = useRoute()
const router = useRouter()
const message = useMessage()

const formRef = ref(null)
const codeEditorRef = ref(null)
const isEdit = computed(() => !!props.scriptId)

const formData = ref({
  name: '',
  type: 'SHELL',
  description: '',
  code: '',
  params: []
})

const testing = ref(false)
const testResult = ref(null)
const showTestResult = ref(false)

const formRules = {
  name: { required: true, message: '请输入脚本名称' },
  type: { required: true, message: '请选择脚本类型' },
  code: { required: true, message: '请输入脚本代码' }
}

const scriptTypeOptions = SCRIPT_TYPES.map(type => ({
  label: type.label,
  value: type.value
}))

const templates = ref([
  { id: 1, name: 'Shell 基础模板', description: '适用于简单的 Shell 脚本', code: '#!/bin/bash\necho "Hello World"' },
  { id: 2, name: 'Python 基础模板', description: '适用于简单的 Python 脚本', code: '#!/usr/bin/env python3\nprint("Hello World")' },
  { id: 3, name: '数据库备份模板', description: '适用于数据库备份操作', code: '-- 数据库备份脚本\nSELECT * INTO OUTFILE' }
])

const loadTemplate = async (templateId) => {
  try {
    const res = await templatesApi.getById(templateId)
    // 处理响应数据结构
    const template = res.data || res
    if (res.code === 200 || res.code === 0 || template.templateId) {
      formData.value.name = template.displayName || template.templateName || ''
      formData.value.type = template.scriptType || 'SHELL'
      formData.value.description = template.description || ''
      formData.value.code = template.templateContent || template.code || ''
      message.success('已加载模板')
    } else {
      message.error('加载模板失败')
    }
  } catch (e) {
    console.error('加载模板失败:', e)
    message.error('加载模板失败')
  }
}

const loadScript = async (scriptName) => {
  try {
    const res = await scriptsApi.getByName(scriptName)
    // 处理响应数据结构
    const script = res.data || res
    if (res.code === 200 || res.code === 0 || script.scriptName) {
      formData.value.name = script.scriptName
      formData.value.type = script.scriptType || 'SHELL'
      formData.value.description = script.description || ''
      
      // 获取脚本内容
      try {
        const contentRes = await scriptsApi.getContent(scriptName)
        let content = ''
        if (contentRes.data) {
          content = contentRes.data.content || ''
        } else if (contentRes.content) {
          content = contentRes.content
        } else if (typeof contentRes === 'string') {
          content = contentRes
        } else if (typeof contentRes.data === 'string') {
          content = contentRes.data
        }
        formData.value.code = content
      } catch (e) {
        console.error('获取脚本内容失败:', e)
        formData.value.code = ''
      }
    } else {
      message.error('加载脚本失败')
    }
  } catch (e) {
    console.error('加载脚本失败:', e)
    message.error('加载脚本失败')
  }
}

const getFileExtension = (type) => {
  const extMap = {
    SHELL: '.sh',
    PYTHON: '.py',
    JAVASCRIPT: '.js',
    SQL: '.sql',
    BATCH: '.bat',
    POWERSHELL: '.ps1'
  }
  return extMap[type] || '.txt'
}

const applyTemplate = (template) => {
  formData.value.code = template.code
  message.success('已应用模板')
}

const handleFormat = () => {
  message.info('代码格式化功能')
}

const handleCopy = () => {
  navigator.clipboard.writeText(formData.value.code)
  message.success('已复制到剪贴板')
}

const handleGoBack = () => {
  router.back()
}

const handleTabKey = (e) => {
  e.preventDefault()
  const textarea = codeEditorRef.value
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  formData.value.code = formData.value.code.substring(0, start) + '  ' + formData.value.code.substring(end)
  nextTick(() => {
    textarea.selectionStart = textarea.selectionEnd = start + 2
  })
}

const handleTestScript = async () => {
  try {
    // 验证必填项
    if (!formData.value.code) {
      message.warning('请输入脚本代码')
      return
    }
    
    testing.value = true
    
    const request = {
      scriptName: formData.value.name,
      scriptType: formData.value.type,
      scriptContent: formData.value.code,
      params: formData.value.params,
      timeoutSeconds: 60
    }
    
    const res = await scriptsApi.execute(request)
    
    if (res.code === 200) {
      testResult.value = {
        success: res.data.success,
        output: res.data.output || '',
        errorMessage: res.data.errorMessage || '',
        exitCode: res.data.exitCode,
        durationMs: res.data.durationMs
      }
      showTestResult.value = true
    } else {
      message.error(res.message || '执行失败')
    }
  } catch (e) {
    console.error('执行失败:', e)
    message.error('执行失败: ' + (e.response?.data?.message || e.message))
  } finally {
    testing.value = false
  }
}

const handleSaveDraft = async () => {
  try {
    await formRef.value?.validate()
    
    // 保存脚本信息 - 草稿状态
    const scriptInfo = {
      scriptName: formData.value.name,
      scriptType: formData.value.type,
      description: formData.value.description,
      content: formData.value.code,
      enabled: false  // 草稿：未启用
    }
    
    await scriptsApi.create(scriptInfo)
    
    // 保存脚本内容
    await scriptsApi.updateContent(formData.value.name, formData.value.code)
    
    message.success('草稿保存成功')
    router.push('/scripts')
  } catch (e) {
    console.error('保存失败:', e)
    message.error(e.response?.data?.message || MESSAGE.ERROR.SAVE)
  }
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
    
    // 保存脚本信息 - 已发布状态
    const scriptInfo = {
      scriptName: formData.value.name,
      scriptType: formData.value.type,
      description: formData.value.description,
      content: formData.value.code,
      enabled: true  // 发布：已启用
    }
    
    await scriptsApi.create(scriptInfo)
    
    // 保存脚本内容
    await scriptsApi.updateContent(formData.value.name, formData.value.code)
    
    message.success('保存并发布成功')
    router.push('/scripts')
  } catch (e) {
    console.error('保存失败:', e)
    message.error(e.response?.data?.message || MESSAGE.ERROR.SAVE)
  }
}

onMounted(() => {
  const scriptName = route.query.name
  const templateId = route.query.templateId
  
  if (scriptName) {
    // 编辑模式
    loadScript(scriptName)
  } else if (templateId) {
    // 从模板创建
    loadTemplate(templateId)
  }
})

onBeforeUnmount(() => {
  // 清理状态
  if (codeEditorRef.value) {
    codeEditorRef.value = null
  }
})
</script>

<style lang="scss" scoped>
.script-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #e5e7eb;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .editor-title {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .header-right {
    display: flex;
    gap: 12px;
  }
}

.editor-content {
  flex: 1;
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
  padding: 20px;
  overflow: hidden;
}

.info-panel {
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  overflow-y: auto;

  .template-section {
    margin-top: 24px;

    .section-title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 12px;
      color: #6b7280;
    }

    .template-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .template-item {
      padding: 12px;
      background: #fff;
      border-radius: 6px;
      cursor: pointer;
      border: 1px solid #e5e7eb;
      transition: all 0.2s;

      &:hover {
        border-color: #2563eb;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.1);
      }

      .template-name {
        font-weight: 500;
        margin-bottom: 4px;
      }

      .template-desc {
        font-size: 12px;
        color: #6b7280;
      }
    }
  }
}

.code-panel {
  display: flex;
  flex-direction: column;
  background: #1e1e1e;
  border-radius: 8px;
  overflow: hidden;

  .editor-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #2d2d2d;
    border-bottom: 1px solid #404040;

    .file-type {
      color: #858585;
      font-size: 12px;
      font-family: monospace;
    }
  }

  .editor-wrapper {
    flex: 1;
    overflow: hidden;

    .code-textarea {
      width: 100%;
      height: 100%;
      background: #1e1e1e;
      color: #d4d4d4;
      border: none;
      outline: none;
      resize: none;
      padding: 16px;
      font-family: 'Fira Code', 'Consolas', monospace;
      font-size: 14px;
      line-height: 1.6;
    }
  }
}

.params-panel {
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;

  .panel-title {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .param-row {
    display: flex;
    gap: 12px;
  }
}
</style>