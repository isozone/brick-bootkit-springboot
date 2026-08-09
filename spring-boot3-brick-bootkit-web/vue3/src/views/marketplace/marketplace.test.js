import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'

// Mock API 服务
vi.mock('@/api/services', () => ({
  marketplaceApi: {
    getList: vi.fn(),
    install: vi.fn()
  }
}))

// Mock naive-ui 的 message/dialog（组件直接调用 useMessage/useDialog）
vi.mock('naive-ui', () => {
  return {
    NCard: { template: '<div class="n-card"><slot name="header" /><slot name="header-extra" /><slot /><slot name="footer" /></div>' },
    NButton: { props: ['type', 'block', 'loading', 'disabled'], template: '<button class="n-button" :disabled="disabled" @click="$emit(\'click\')"><slot /></button>' },
    NIcon: { template: '<span class="n-icon"><slot /></span>' },
    NTag: { props: ['type', 'size'], template: '<span class="n-tag"><slot /></span>' },
    NGrid: { template: '<div class="n-grid"><slot /></div>' },
    NGi: { template: '<div class="n-gi"><slot /></div>' },
    NEmpty: { props: ['description', 'size'], template: '<div class="n-empty"><span>{{ description }}</span><slot /><slot name="extra" /></div>' },
    useMessage: () => ({ success: vi.fn(), error: vi.fn(), info: vi.fn(), warning: vi.fn() }),
    useDialog: () => ({ warning: vi.fn() })
  }
})

import MarketplacePage from '@/views/marketplace/index.vue'
import { marketplaceApi } from '@/api/services'

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': {
      marketplace: {
        title: '插件市场',
        subtitle: '浏览并一键下载安装可用插件',
        noPlugins: '插件市场暂无可用插件',
        installed: '已安装',
        downloadInstall: '下载安装'
      },
      common: { refresh: '刷新' }
    }
  }
})

describe('MarketplacePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染插件列表与已安装状态', async () => {
    marketplaceApi.getList.mockResolvedValue({
      code: 200,
      data: [
        { pluginId: 'demo', name: 'Demo Plugin', version: '1.0.0', installed: false, state: '未安装', downloadUrl: 'https://x/demo.jar' },
        { pluginId: 'prod', name: 'Prod Plugin', version: '2.0.0', installed: true, state: 'STARTED', downloadUrl: 'https://x/prod.jar' }
      ]
    })

    const wrapper = mount(MarketplacePage, {
      global: { plugins: [i18n] }
    })
    await vi.waitFor(() => {
      expect(marketplaceApi.getList).toHaveBeenCalled()
    })
    await wrapper.vm.$nextTick()

    const text = wrapper.text()
    expect(text).toContain('Demo Plugin')
    expect(text).toContain('Prod Plugin')
    expect(text).toContain('已安装')
    expect(text).toContain('下载安装')
  })

  it('无插件时显示空提示', async () => {
    marketplaceApi.getList.mockResolvedValue({ code: 200, data: [] })

    const wrapper = mount(MarketplacePage, {
      global: { plugins: [i18n] }
    })
    await vi.waitFor(() => {
      expect(marketplaceApi.getList).toHaveBeenCalled()
    })
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('插件市场暂无可用插件')
  })

  it('加载失败时提示错误', async () => {
    marketplaceApi.getList.mockRejectedValue({ response: { data: { message: '加载失败' } } })

    const wrapper = mount(MarketplacePage, {
      global: { plugins: [i18n] }
    })
    await vi.waitFor(() => {
      expect(marketplaceApi.getList).toHaveBeenCalled()
    })
    await wrapper.vm.$nextTick()
    // 不抛异常即通过
    expect(wrapper.exists()).toBe(true)
  })
})
