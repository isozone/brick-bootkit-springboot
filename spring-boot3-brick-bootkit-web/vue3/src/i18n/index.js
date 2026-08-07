import { createI18n } from 'vue-i18n'
import zhCN from '../locales/zh-CN'
import enUS from '../locales/en-US'

/**
 * 全局国际化配置
 * 默认中文，可通过 localStorage 的 brick-locale 切换（zh-CN / en-US）
 */
const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: localStorage.getItem('brick-locale') || 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export default i18n
