<template>
  <div class="plugin-upload-page">
    <div class="page-header">
      <n-button quaternary circle @click="$router.back()">
        <template #icon>
          <n-icon><ArrowBackOutline /></n-icon>
        </template>
      </n-button>
      <div class="page-title-wrap">
        <h2 class="page-title">上传插件</h2>
        <p class="page-subtitle">推荐按“上传临时文件 -> 校验 -> 安装启动”的顺序完成接入</p>
      </div>
    </div>

    <div class="upload-container">
      <div class="upload-section">
        <n-card title="推荐流程" class="guide-card">
          <div class="guide-steps">
            <div class="guide-step" :class="stepStatus.upload">
              <div class="guide-step__index">1</div>
              <div>
                <div class="guide-step__title">上传到临时目录</div>
                <div class="guide-step__desc">
                  先把文件交给后端托管，避免手工填写任意系统路径。
                </div>
              </div>
            </div>
            <div class="guide-step" :class="stepStatus.verify">
              <div class="guide-step__index">2</div>
              <div>
                <div class="guide-step__title">校验插件包</div>
                <div class="guide-step__desc">
                  在真正安装前先确认插件包结构和元信息是否有效。
                </div>
              </div>
            </div>
            <div class="guide-step" :class="stepStatus.install">
              <div class="guide-step__index">3</div>
              <div>
                <div class="guide-step__title">安装并按需启动</div>
                <div class="guide-step__desc">
                  校验通过后再安装，减少误操作带来的排查成本。
                </div>
              </div>
            </div>
          </div>
        </n-card>

        <n-card title="选择插件包" class="upload-card">
          <div class="upload-area" @click="triggerUpload" @drop.prevent="handleDrop" @dragover.prevent>
            <input
              ref="fileInputRef"
              type="file"
              accept=".jar,.zip"
              style="display: none"
              @change="handleFileSelect"
            />
            <div class="upload-icon">
              <n-icon size="64" color="#2563eb"><CloudUploadOutline /></n-icon>
            </div>
            <div class="upload-text">
              <p class="primary">点击或拖拽文件到此处上传</p>
              <p class="secondary">支持 .jar, .zip 格式，最大 50MB</p>
            </div>
          </div>

          <div v-if="uploadFile" class="file-info">
            <div class="file-icon">
              <n-icon size="32"><DocumentTextOutline /></n-icon>
            </div>
            <div class="file-details">
              <div class="file-name">{{ uploadFile.name }}</div>
              <div class="file-size">{{ formatFileSize(uploadFile.size) }}</div>
              <n-progress
                v-if="uploadProgress > 0"
                type="line"
                :percentage="uploadProgress"
                :show-indicator="false"
              />
            </div>
            <n-button quaternary circle @click="clearFile">
              <template #icon>
                <n-icon><CloseOutline /></n-icon>
              </template>
            </n-button>
          </div>

          <div class="wizard-options">
            <n-checkbox v-model:checked="autoStart">安装后自动启动</n-checkbox>
          </div>

          <div v-if="uploadFile" class="upload-actions">
            <n-button @click="clearFile">取消</n-button>
            <n-button type="primary" :loading="uploadingTemp" @click="handleUploadTemp">
              {{ uploadingTemp ? '上传中...' : '第一步：上传到临时目录' }}
            </n-button>
            <n-button tertiary :loading="uploadingDirect" @click="handleDirectUpload">
              {{ uploadingDirect ? '处理中...' : '跳过向导，一键上传安装' }}
            </n-button>
          </div>
        </n-card>

        <n-card title="当前结果" class="result-card">
          <ErrorHintPanel
            v-if="lastError.message"
            title="上传流程异常"
            :message="lastError.message"
            :error-key="lastError.errorKey"
            :hint-path="lastError.hintPath"
            :hint-anchor="lastError.hintAnchor"
            class="result-alert"
          />

          <div class="result-grid">
            <div class="result-item">
              <div class="result-item__label">临时文件</div>
              <div class="result-item__value">{{ tempFilePath || '-' }}</div>
            </div>
            <div class="result-item">
              <div class="result-item__label">校验结果</div>
              <div class="result-item__value">
                <n-tag v-if="verifyState === 'success'" type="success">通过</n-tag>
                <n-tag v-else-if="verifyState === 'error'" type="error">失败</n-tag>
                <span v-else>-</span>
              </div>
            </div>
            <div class="result-item">
              <div class="result-item__label">已安装插件</div>
              <div class="result-item__value">{{ installedPlugin?.pluginId || '-' }}</div>
            </div>
          </div>

          <n-alert v-if="verifyMessage" :type="verifyState === 'error' ? 'error' : 'success'" class="result-alert">
            {{ verifyMessage }}
          </n-alert>

          <div v-if="actionTips.length" class="result-tips">
            <div v-for="tip in actionTips" :key="tip" class="result-tip">
              {{ tip }}
            </div>
          </div>

          <div class="verify-actions">
            <n-button :disabled="!tempFilePath" :loading="verifying" @click="handleVerify">
              {{ verifying ? '校验中...' : '第二步：校验插件包' }}
            </n-button>
            <n-button
              type="primary"
              :disabled="!tempFilePath || verifyState === 'error'"
              :loading="installing"
              @click="handleInstallFromTemp"
            >
              {{ installing ? '安装中...' : autoStart ? '第三步：安装并启动' : '第三步：安装插件' }}
            </n-button>
          </div>
        </n-card>
      </div>

      <div class="history-section">
        <div class="section-header">上传历史</div>
        <div class="history-list">
          <div
            v-for="item in uploadHistory"
            :key="item.id"
            class="history-item"
            :class="item.status"
          >
            <div class="history-header">
              <div class="history-name">{{ item.name }}</div>
              <n-tag v-if="item.status === 'success'" type="success" size="small">成功</n-tag>
              <n-tag v-else type="error" size="small">失败</n-tag>
            </div>
            <div class="history-meta">
              <div class="meta-item">
                <span class="meta-label">插件ID:</span>
                <span class="meta-value">{{ item.pluginId || '-' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">版本:</span>
                <span class="meta-value">{{ item.version || '-' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">时间:</span>
                <span class="meta-value">{{ item.time }}</span>
              </div>
            </div>
            <div v-if="item.errorMessage" class="error-message">
              {{ item.errorMessage }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NAlert,
  NButton,
  NCard,
  NCheckbox,
  NIcon,
  NProgress,
  NTag,
  useMessage
} from 'naive-ui'
import {
  ArrowBackOutline,
  CloudUploadOutline,
  DocumentTextOutline,
  CloseOutline
} from '@vicons/ionicons5'
import ErrorHintPanel from '@/components/ErrorHintPanel.vue'
import { pluginsApi } from '@/api/services'
import { resolveApiErrorPayload } from '@/utils/error-helper'

const router = useRouter()
const message = useMessage()

const fileInputRef = ref(null)
const uploadFile = ref(null)
const uploadProgress = ref(0)
const uploadingTemp = ref(false)
const uploadingDirect = ref(false)
const verifying = ref(false)
const installing = ref(false)
const autoStart = ref(true)

const tempFilePath = ref('')
const verifyState = ref('idle')
const verifyMessage = ref('')
const installedPlugin = ref(null)
const actionTips = ref([])
const lastError = ref({ message: '', errorKey: '', hintPath: '', hintAnchor: '' })
const uploadHistory = ref([])

const stepStatus = computed(() => ({
  upload: tempFilePath.value ? 'done' : uploadFile.value ? 'active' : 'idle',
  verify: verifyState.value === 'success'
    ? 'done'
    : verifyState.value === 'error'
      ? 'error'
      : tempFilePath.value
        ? 'active'
        : 'idle',
  install: installedPlugin.value ? 'done' : (verifyState.value === 'success' ? 'active' : 'idle')
}))

const loadUploadHistory = async () => {
  try {
    const res = await pluginsApi.getAllUploadHistory()
    if (res.code === 200 && res.data) {
      uploadHistory.value = res.data.map(item => ({
        id: item.uploadId,
        name: item.pluginName,
        status: item.status.toLowerCase(),
        time: new Date(item.uploadTime).toLocaleString('zh-CN'),
        pluginId: item.pluginId,
        version: item.version,
        filePath: item.filePath,
        errorMessage: item.errorMessage
      }))
    }
  } catch (e) {
    console.error('加载上传历史失败:', e)
  }
}

onMounted(() => {
  loadUploadHistory()
})

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = (event) => {
  const file = event.target.files?.[0]
  if (file) validateAndSetFile(file)
}

const handleDrop = (event) => {
  const file = event.dataTransfer.files?.[0]
  if (file) validateAndSetFile(file)
}

const validateAndSetFile = (file) => {
  const validTypes = ['.jar', '.zip']
  const ext = '.' + file.name.split('.').pop().toLowerCase()

  if (!validTypes.includes(ext)) {
    message.error('仅支持 .jar 和 .zip 格式')
    return
  }

  if (file.size > 50 * 1024 * 1024) {
    message.error('文件大小不能超过 50MB')
    return
  }

  uploadFile.value = file
  tempFilePath.value = ''
  verifyState.value = 'idle'
  verifyMessage.value = ''
  installedPlugin.value = null
  actionTips.value = []
  lastError.value = { message: '', errorKey: '', hintPath: '', hintAnchor: '' }
}

const clearFile = () => {
  uploadFile.value = null
  uploadProgress.value = 0
  tempFilePath.value = ''
  verifyState.value = 'idle'
  verifyMessage.value = ''
  installedPlugin.value = null
  actionTips.value = []
  lastError.value = { message: '', errorKey: '', hintPath: '', hintAnchor: '' }
  if (fileInputRef.value) fileInputRef.value.value = ''
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const handleUploadTemp = async () => {
  if (!uploadFile.value) return
  uploadingTemp.value = true
  uploadProgress.value = 25
  try {
    const res = await pluginsApi.uploadTemp(uploadFile.value)
    if (res.code === 200) {
      tempFilePath.value = res.data
      uploadProgress.value = 100
      verifyState.value = 'idle'
      verifyMessage.value = '临时上传成功，可以继续校验插件包。'
      actionTips.value = ['下一步建议：先点击“校验插件包”，确认插件结构有效后再安装。']
      lastError.value = { message: '', errorKey: '', hintPath: '', hintAnchor: '' }
      message.success('临时上传成功')
    } else {
      const errorMessage = res.message || '临时上传失败'
      lastError.value = {
        message: errorMessage,
        errorKey: res.errorKey || '',
        hintPath: res.hintPath || '',
        hintAnchor: res.hintAnchor || ''
      }
      message.error(errorMessage)
    }
  } catch (error) {
    console.error('临时上传错误:', error)
    lastError.value = resolveApiErrorPayload(error, '临时上传失败')
    message.error(lastError.value.message)
  } finally {
    uploadingTemp.value = false
  }
}

const handleVerify = async () => {
  if (!tempFilePath.value) return
  verifying.value = true
  try {
    const res = await pluginsApi.verify(tempFilePath.value)
    if (res.code === 200 && res.data) {
      verifyState.value = res.data.valid ? 'success' : 'error'
      verifyMessage.value = res.data.message || (res.data.valid ? '插件验证通过' : '插件验证失败')
      actionTips.value = deriveActionTips(verifyMessage.value, verifyState.value)
      if (res.data.valid) {
        lastError.value = { message: '', errorKey: '', hintPath: '', hintAnchor: '' }
        message.success('插件验证通过')
      } else {
        lastError.value = {
          message: verifyMessage.value,
          errorKey: res.errorKey || '',
          hintPath: res.hintPath || '',
          hintAnchor: res.hintAnchor || ''
        }
        message.error(verifyMessage.value)
      }
    } else {
      verifyState.value = 'error'
      verifyMessage.value = res.message || '插件验证失败'
      message.error(verifyMessage.value)
      appendHintTip(res)
      lastError.value = {
        message: verifyMessage.value,
        errorKey: res.errorKey || '',
        hintPath: res.hintPath || '',
        hintAnchor: res.hintAnchor || ''
      }
    }
  } catch (error) {
    console.error('插件校验错误:', error)
    verifyState.value = 'error'
    verifyMessage.value = error.response?.data?.message || '插件验证失败'
    actionTips.value = deriveActionTips(verifyMessage.value, verifyState.value)
    appendHintTip(error.response?.data)
    lastError.value = resolveApiErrorPayload(error, '插件验证失败')
    message.error(verifyMessage.value)
  } finally {
    verifying.value = false
  }
}

const handleInstallFromTemp = async () => {
  if (!tempFilePath.value) return
  installing.value = true
  try {
    const res = await pluginsApi.installFromTemp(tempFilePath.value, autoStart.value)
    if (res.code === 200) {
      installedPlugin.value = res.data
      verifyState.value = 'success'
      verifyMessage.value = autoStart.value ? '插件已安装并启动。' : '插件已安装。'
      actionTips.value = autoStart.value
        ? ['下一步建议：去插件列表确认状态是否为 STARTED，再验证业务接口是否生效。']
        : ['下一步建议：安装已完成，可以去插件列表手动启动。']
      lastError.value = { message: '', errorKey: '', hintPath: '', hintAnchor: '' }
      message.success(verifyMessage.value)
      loadUploadHistory()
    } else {
      const errorMessage = res.message || '插件安装失败'
      lastError.value = {
        message: errorMessage,
        errorKey: res.errorKey || '',
        hintPath: res.hintPath || '',
        hintAnchor: res.hintAnchor || ''
      }
      message.error(errorMessage)
      appendHintTip(res)
    }
  } catch (error) {
    console.error('插件安装错误:', error)
    lastError.value = resolveApiErrorPayload(error, '插件安装失败')
    appendHintTip(error.response?.data)
    message.error(error.response?.data?.message || '插件安装失败')
  } finally {
    installing.value = false
  }
}

const handleDirectUpload = async () => {
  if (!uploadFile.value) return
  uploadingDirect.value = true
  uploadProgress.value = 0
  try {
    const res = await pluginsApi.upload(uploadFile.value, autoStart.value)
    if (res.code === 200) {
      installedPlugin.value = res.data
      verifyState.value = 'success'
      verifyMessage.value = autoStart.value ? '插件已一键上传并启动。' : '插件已一键上传安装。'
      actionTips.value = autoStart.value
        ? ['下一步建议：去插件列表或系统概览确认插件是否已启动。']
        : ['下一步建议：如果要进一步排查，建议改用分步向导上传并先执行校验。']
      lastError.value = { message: '', errorKey: '', hintPath: '', hintAnchor: '' }
      message.success(verifyMessage.value)
      clearFile()
      loadUploadHistory()
    } else {
      const errorMessage = res.message || '上传失败'
      lastError.value = {
        message: errorMessage,
        errorKey: res.errorKey || '',
        hintPath: res.hintPath || '',
        hintAnchor: res.hintAnchor || ''
      }
      message.error(errorMessage)
      appendHintTip(res)
    }
  } catch (error) {
    console.error('上传错误:', error)
    const errorMessage = error.response?.data?.message || '上传失败'
    actionTips.value = deriveActionTips(errorMessage, 'error')
    appendHintTip(error.response?.data)
    lastError.value = resolveApiErrorPayload(error, '上传失败')
    message.error(errorMessage)
  } finally {
    uploadingDirect.value = false
  }
}

const deriveActionTips = (messageText, status) => {
  if (!messageText) return []
  if (status === 'success') {
    return ['下一步建议：如果安装前还没做校验，建议至少再用 doctor 或插件列表确认一次运行状态。']
  }
  if (messageText.includes('主应用包名') || messageText.includes('plugin.mainPackage')) {
    return ['检查启动类是否使用标准 @SpringBootApplication 结构，必要时显式配置 plugin.mainPackage。']
  }
  if (messageText.includes('插件目录') || messageText.includes('plugin.pluginPath')) {
    return ['确认 plugin.pluginPath 指向有效目录，并且应用进程对该目录具有读权限。']
  }
  if (messageText.includes('路径不合法') || messageText.includes('临时文件')) {
    return ['不要手填系统路径，优先使用“上传到临时目录 -> 校验 -> 安装”的向导流程。']
  }
  if (messageText.includes('PluginWebAuthorizer') || messageText.includes('权限')) {
    return ['如果是生产或严格模式，请在宿主应用中提供 PluginWebAuthorizer Bean。']
  }
  if (messageText.includes('版本')) {
    return ['请检查待安装插件版本是否高于当前已安装版本。']
  }
  return ['建议先执行环境自检，再检查插件目录、主包推断和上传临时目录。']
}

const appendHintTip = (payload) => {
  const hintPath = payload?.hintPath
  if (!hintPath) return
  const hintAnchor = payload?.hintAnchor ? `#${payload.hintAnchor}` : ''
  const docTip = `参考文档：${hintPath}${hintAnchor}`
  if (!actionTips.value.includes(docTip)) {
    actionTips.value = [...actionTips.value, docTip]
  }
}
</script>

<style lang="scss" scoped>
.plugin-upload-page {
  .page-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;

    .page-title-wrap {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .page-title {
      font-size: 20px;
      font-weight: 600;
      margin: 0;
    }

    .page-subtitle {
      margin: 0;
      color: #64748b;
      font-size: 13px;
    }
  }

  .upload-container {
    display: grid;
    grid-template-columns: 1fr 500px;
    gap: 24px;
    max-width: 1600px;

    .upload-section {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .history-section {
      display: flex;
      flex-direction: column;
      gap: 0;
      max-height: 860px;
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 12px;
      overflow: hidden;

      .section-header {
        padding: 16px 20px;
        background: #f9fafb;
        border-bottom: 1px solid #e5e7eb;
        font-size: 16px;
        font-weight: 600;
      }

      .history-list {
        flex: 1;
        overflow-y: auto;
        padding: 8px;

        .history-item {
          padding: 16px 20px;
          margin-bottom: 10px;
          background: #ffffff;
          border: 1px solid #e5e7eb;
          border-radius: 8px;
          transition: all 0.2s;

          &:last-child {
            margin-bottom: 0;
          }

          &:hover {
            border-color: #2563eb;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.1);
          }

          &.success {
            border-left: 4px solid #22c55e;
          }

          &.failed {
            border-left: 4px solid #ef4444;
          }

          .history-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;

            .history-name {
              font-size: 15px;
              font-weight: 600;
              color: #1f2937;
            }
          }

          .history-meta {
            display: flex;
            flex-direction: column;
            gap: 6px;

            .meta-item {
              display: flex;
              gap: 8px;
              font-size: 13px;

              .meta-label {
                color: #64748b;
                min-width: 64px;
              }

              .meta-value {
                color: #334155;
                word-break: break-all;
              }
            }
          }

          .error-message {
            margin-top: 12px;
            padding: 10px 12px;
            border-radius: 8px;
            background: #fef2f2;
            color: #b91c1c;
            font-size: 13px;
          }
        }
      }
    }
  }
}

.guide-card,
.upload-card,
.result-card {
  border-radius: 12px;
}

.guide-steps {
  display: grid;
  gap: 12px;
}

.guide-step {
  display: grid;
  grid-template-columns: 40px 1fr;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #fff;

  &.done {
    border-color: #16a34a;
    background: #f0fdf4;
  }

  &.active {
    border-color: #2563eb;
    background: #eff6ff;
  }

  &.error {
    border-color: #dc2626;
    background: #fef2f2;
  }
}

.guide-step__index {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #1d4ed8;
  color: #fff;
  font-weight: 700;
}

.guide-step__title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.guide-step__desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  border: 2px dashed #cbd5e1;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fbff 0%, #f8fafc 100%);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #2563eb;
    background: linear-gradient(180deg, #eff6ff 0%, #f8fafc 100%);
  }
}

.upload-text {
  text-align: center;

  .primary {
    margin: 16px 0 6px;
    font-size: 16px;
    font-weight: 600;
    color: #0f172a;
  }

  .secondary {
    margin: 0;
    color: #64748b;
    font-size: 13px;
  }
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.file-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-name {
  font-weight: 600;
  color: #0f172a;
}

.file-size {
  color: #64748b;
  font-size: 13px;
}

.wizard-options {
  margin-top: 16px;
}

.upload-actions,
.verify-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.result-item {
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
}

.result-item__label {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}

.result-item__value {
  color: #0f172a;
  font-size: 13px;
  word-break: break-all;
}

.result-alert {
  margin-top: 16px;
}

.result-tips {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.result-tip {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .plugin-upload-page .upload-container {
    grid-template-columns: 1fr;
  }

  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
