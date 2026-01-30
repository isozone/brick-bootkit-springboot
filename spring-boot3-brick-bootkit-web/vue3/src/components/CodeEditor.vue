<template>
  <div class="code-editor-wrapper" :style="{ height: height }">
    <div v-if="showHeader" class="editor-header">
      <span class="editor-title">{{ title || language }}</span>
      <div class="editor-actions">
        <n-button size="small" quaternary circle @click="handleCopy">
          <template #icon>
            <n-icon><CopyOutline /></n-icon>
          </template>
        </n-button>
      </div>
    </div>
    <div ref="editorRef" class="editor-content" :class="{ 'with-header': showHeader }"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useMessage } from 'naive-ui'
import { CopyOutline } from '@vicons/ionicons5'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { keymap } from '@codemirror/view'
import { defaultKeymap, indentWithTab } from '@codemirror/commands'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'shell'
  },
  readonly: {
    type: Boolean,
    default: false
  },
  showHeader: {
    type: Boolean,
    default: true
  },
  height: {
    type: String,
    default: '400px'
  },
  title: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const message = useMessage()
const editorRef = ref(null)
let editorView = null

const languageExtensions = {
  javascript: () => import('@codemirror/lang-javascript'),
  js: () => import('@codemirror/lang-javascript'),
  typescript: () => import('@codemirror/lang-javascript'),
  ts: () => import('@codemirror/lang-javascript'),
  python: () => import('@codemirror/lang-python'),
  py: () => import('@codemirror/lang-python'),
  sql: () => import('@codemirror/lang-sql'),
  shell: () => Promise.resolve([]),
  bash: () => Promise.resolve([]),
  html: () => import('@codemirror/lang-html'),
  xml: () => import('@codemirror/lang-xml'),
  css: () => import('@codemirror/lang-css'),
  json: () => import('@codemirror/lang-json')
}

const getLanguageExtension = async (lang) => {
  const lowerLang = lang?.toLowerCase() || 'shell'
  const langFn = languageExtensions[lowerLang]
  if (langFn) {
    try {
      const langModule = await langFn()
      return langModule.default ? langModule.default() : langModule
    } catch {
      return []
    }
  }
  return []
}

const initEditor = async () => {
  if (!editorRef.value) return

  const extensions = [
    basicSetup,
    keymap.of([indentWithTab, ...defaultKeymap]),
    EditorView.updateListener.of((update) => {
      if (update.docChanged) {
        const value = update.state.doc.toString()
        emit('update:modelValue', value)
        emit('change', value)
      }
    }),
    EditorState.readOnly.of(props.readonly),
    EditorView.theme({
      '&': { height: '100%', fontSize: '14px' },
      '.cm-scroller': { overflow: 'auto' },
      '.cm-content': { fontFamily: "'Fira Code', 'Consolas', monospace" }
    })
  ]

  const langExt = await getLanguageExtension(props.language)
  if (langExt.length > 0) {
    extensions.push(langExt)
  }

  const state = EditorState.create({
    doc: props.modelValue,
    extensions
  })

  editorView = new EditorView({
    state,
    parent: editorRef.value
  })
}

const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(props.modelValue)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

watch(() => props.modelValue, (newValue) => {
  if (editorView && newValue !== editorView.state.doc.toString()) {
    editorView.dispatch({
      changes: { from: 0, to: editorView.state.doc.length, insert: newValue }
    })
  }
})

watch(() => props.language, async () => {
  if (editorView) {
    const currentContent = editorView.state.doc.toString()
    editorView.destroy()
    await initEditor()
    if (currentContent) {
      editorView.dispatch({
        changes: { from: 0, to: editorView.state.doc.length, insert: currentContent }
      })
    }
  }
})

watch(() => props.readonly, (readonly) => {
  if (editorView) {
    editorView.dispatch({
      effects: EditorState.readOnly.of(readonly)
    })
  }
})

onMounted(async () => {
  await initEditor()
})

onBeforeUnmount(() => {
  if (editorView) {
    editorView.destroy()
  }
})
</script>

<style lang="scss" scoped>
.code-editor-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #1e1e1e;

  .editor-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #2d2d2d;
    border-bottom: 1px solid #404040;

    .editor-title {
      font-size: 13px;
      font-weight: 500;
      color: #a0a0a0;
      text-transform: uppercase;
    }
  }

  .editor-content {
    flex: 1;
    overflow: hidden;

    &.with-header {
      height: calc(100% - 40px);
    }
  }
}

:deep(.cm-editor) {
  height: 100%;
  width: 100%;

  .cm-scroller {
    font-family: 'Fira Code', 'Consolas', monospace;
    overflow: auto;
  }

  .cm-gutters {
    background: #2d2d2d;
    border-right: 1px solid #404040;
  }

  .cm-activeLineGutter {
    background: #3a3a3a;
  }
}
</style>
