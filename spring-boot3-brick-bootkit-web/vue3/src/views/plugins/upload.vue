<template>
  <div class="plugin-upload-page">
    <div class="page-header">
      <n-button quaternary circle @click="$router.back()">
        <template #icon>
          <n-icon><ArrowBackOutline /></n-icon>
        </template>
      </n-button>
      <h2 class="page-title">上传插件</h2>
    </div>

    <div class="upload-container">
      <div class="upload-section">
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

        <div v-if="uploadFile" class="upload-actions">
          <n-button @click="clearFile">取消</n-button>
          <n-button type="primary" :loading="uploading" @click="handleUpload">
            {{ uploading ? '上传中...' : '开始上传' }}
          </n-button>
        </div>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import {
  NButton,
  NIcon,
  NProgress,
  NTag,
  useMessage
} from 'naive-ui'
import {
  ArrowBackOutline,
  CloudUploadOutline,
  DocumentTextOutline,
  CloseOutline,
  CheckmarkCircleOutline
} from '@vicons/ionicons5'
import { pluginsApi } from '@/api/services'

const router = useRouter()
const message = useMessage()

const fileInputRef = ref(null)
const uploadFile = ref(null)
const uploadProgress = ref(0)
const uploading = ref(false)
const uploadHistory = ref([])

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
}

const clearFile = () => {
  uploadFile.value = null
  uploadProgress.value = 0
  if (fileInputRef.value) fileInputRef.value.value = ''
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const handleUpload = async () => {
  if (!uploadFile.value) return

  uploading.value = true
  uploadProgress.value = 0

  const formData = new FormData()
  formData.append('file', uploadFile.value)
  formData.append('autoStart', true)
  formData.append('overwrite', true)

  try {
    // 使用 axios 直接上传文件
    const response = await axios.post('/plugins-web/api/plugins/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        uploadProgress.value = percentCompleted
      }
    })

    if (response.data.code === 200) {
      message.success('上传成功')
      uploadHistory.value.unshift({
        id: Date.now(),
        name: uploadFile.value.name,
        status: 'success',
        time: new Date().toLocaleString('zh-CN'),
        pluginId: response.data.data.pluginId,
        version: response.data.data.version
      })
      clearFile()
      loadUploadHistory()
    } else {
      message.error(response.data.message || '上传失败')
      uploadHistory.value.unshift({
        id: Date.now(),
        name: uploadFile.value.name,
        status: 'failed',
        time: new Date().toLocaleString('zh-CN'),
        errorMessage: response.data.message || '未知错误'
      })
    }
  } catch (error) {
    console.error('上传错误:', error)
    message.error(error.response?.data?.message || '上传失败')
    uploadHistory.value.unshift({
      id: Date.now(),
      name: uploadFile.value.name,
      status: 'failed',
      time: new Date().toLocaleString('zh-CN'),
      errorMessage: error.response?.data?.message || error.message || '网络错误'
    })
  } finally {
    uploading.value = false
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

    .page-title {
      font-size: 20px;
      font-weight: 600;
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
      max-height: 700px;
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
            gap: 20px;
            font-size: 13px;
            color: #6b7280;
            flex-wrap: nowrap;

            .meta-item {
              display: flex;
              align-items: center;
              gap: 6px;
              white-space: nowrap;

              .meta-label {
                color: #9ca3af;
              }

              .meta-value {
                color: #374151;
                font-weight: 500;
              }
            }
          }

          .error-message {
            margin-top: 10px;
            padding: 8px 12px;
            background: #fef2f2;
            border-radius: 6px;
            font-size: 13px;
            color: #dc2626;
            border-left: 3px solid #ef4444;
          }
        }
      }
    }
  }

  .upload-area {
    border: 2px dashed #d1d5db;
    border-radius: 12px;
    padding: 48px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #2563eb;
      background: rgba(37, 99, 235, 0.02);
    }

    .upload-text {
      margin-top: 16px;

      .primary {
        font-size: 16px;
        color: #1f2937;
        margin-bottom: 8px;
      }

      .secondary {
        font-size: 14px;
        color: #6b7280;
      }
    }
  }

  .file-info {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: #f9fafb;
    border-radius: 8px;
    margin-top: 16px;

    .file-icon {
      width: 48px;
      height: 48px;
      background: #e5e7eb;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .file-details {
      flex: 1;

      .file-name {
        font-weight: 500;
        margin-bottom: 4px;
      }

      .file-size {
        font-size: 12px;
        color: #6b7280;
      }
    }
  }

  .upload-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 16px;
  }
}
</style>
