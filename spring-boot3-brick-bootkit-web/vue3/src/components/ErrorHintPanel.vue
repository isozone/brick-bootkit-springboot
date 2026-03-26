<template>
  <n-alert v-if="message" type="error" :title="title" class="error-hint-panel">
    <div class="error-hint-panel__body">
      <p class="error-hint-panel__message">{{ message }}</p>
      <div v-if="errorKey" class="error-hint-panel__meta">
        <n-tag type="error" size="small">{{ errorKey }}</n-tag>
      </div>
      <div v-if="docsUrl" class="error-hint-panel__actions">
        <n-button text type="primary" @click="openDocs">
          查看排障文档
        </n-button>
        <code class="error-hint-panel__path">{{ hintPath }}<span v-if="hintAnchor">#{{ hintAnchor }}</span></code>
      </div>
    </div>
  </n-alert>
</template>

<script setup>
import { computed } from 'vue'
import { NAlert, NButton, NTag } from 'naive-ui'
import { buildDocsUrl } from '@/utils/error-helper'

const props = defineProps({
  title: {
    type: String,
    default: '操作失败'
  },
  message: {
    type: String,
    default: ''
  },
  errorKey: {
    type: String,
    default: ''
  },
  hintPath: {
    type: String,
    default: ''
  },
  hintAnchor: {
    type: String,
    default: ''
  }
})

const docsUrl = computed(() => buildDocsUrl(props.hintPath, props.hintAnchor))

const openDocs = () => {
  if (!docsUrl.value) return
  window.open(docsUrl.value, '_blank', 'noopener,noreferrer')
}
</script>

<style scoped lang="scss">
.error-hint-panel {
  border-radius: 12px;
}

.error-hint-panel__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.error-hint-panel__message {
  margin: 0;
  line-height: 1.6;
}

.error-hint-panel__meta {
  display: flex;
  gap: 8px;
}

.error-hint-panel__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.error-hint-panel__path {
  display: inline-block;
  padding: 6px 8px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.55);
  color: #7f1d1d;
  font-size: 12px;
  word-break: break-all;
}
</style>
