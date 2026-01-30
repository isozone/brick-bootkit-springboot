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

    <div class="upload-content">
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

      <div class="upload-history">
        <div class="section-title">上传历史</div>
        <div class="history-list">
          <div v-for="item in uploadHistory" :key="item.id" class="history-item">
            <div class="history-icon">
              <n-icon size="20"><CheckmarkCircleOutline v-if="item.status === 'success'" /></n-icon>
            </div>
            <div class="history-info">
              <div class="history-name">{{ item.name }}</div>
              <div class="history-time">{{ item.time }}</div>
            </div>
            <n-tag v-if="item.status === 'success'" type="success" size="small">成功</n-tag>
            <n-tag v-else type="error" size="small">失败</n-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
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

const router = useRouter()
const message = useMessage()

const fileInputRef = ref(null)
const uploadFile = ref(null)
const uploadProgress = ref(0)
const uploading = ref(false)

const uploadHistory = ref([
  { id: 1, name: 'demo-test-upload-plus-0.0.3.jar', status: 'success', time: '2024-01-30 10:30:00' },
  { id: 2, name: 'file-upload-plugin-1.2.0.jar', status: 'success', time: '2024-01-28 15:20:00' },
  { id: 3, name: 'data-export-plugin-1.0.5.jar', status: 'failed', time: '2024-01-25 09:15:00' }
])

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

  // 模拟上传进度
  const interval = setInterval(() => {
    if (uploadProgress.value < 90) {
      uploadProgress.value += 10
    }
  }, 200)

  setTimeout(() => {
    clearInterval(interval)
    uploadProgress.value = 100
    uploading.value = false
    message.success('上传成功')

    uploadHistory.value.unshift({
      id: Date.now(),
      name: uploadFile.value.name,
      status: 'success',
      time: new Date().toLocaleString()
    })

    clearFile()
  }, 2500)
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

  .upload-content {
    max-width: 640px;
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

  .upload-history {
    margin-top: 32px;

    .section-title {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 16px;
    }

    .history-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .history-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background: #f9fafb;
      border-radius: 8px;

      .history-icon {
        color: #10b981;
      }

      .history-info {
        flex: 1;

        .history-name {
          font-weight: 500;
        }

        .history-time {
          font-size: 12px;
          color: #6b7280;
        }
      }
    }
  }
}
</style>
