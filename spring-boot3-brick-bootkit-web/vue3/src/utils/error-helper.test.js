import { describe, it, expect } from 'vitest'
import { resolveApiErrorPayload, buildDocsUrl, DOCS_BASE_URL } from './error-helper'

describe('error-helper', () => {
  describe('resolveApiErrorPayload', () => {
    it('从 response.data 提取错误信息', () => {
      const error = {
        response: {
          data: {
            message: '插件不存在',
            errorKey: 'PLUGIN_NOT_FOUND',
            hintPath: '/troubleshooting',
            hintAnchor: 'common-errors'
          }
        }
      }
      const payload = resolveApiErrorPayload(error)
      expect(payload.message).toBe('插件不存在')
      expect(payload.errorKey).toBe('PLUGIN_NOT_FOUND')
      expect(payload.hintPath).toBe('/troubleshooting')
      expect(payload.hintAnchor).toBe('common-errors')
    })

    it('缺少 response 时使用兜底消息', () => {
      const payload = resolveApiErrorPayload({}, '兜底消息')
      expect(payload.message).toBe('兜底消息')
      expect(payload.errorKey).toBeNull()
    })

    it('无自定义兜底消息时使用默认消息', () => {
      const payload = resolveApiErrorPayload({})
      expect(payload.message).toBe('操作失败')
    })

    it('response.data 为空对象时使用兜底消息', () => {
      const payload = resolveApiErrorPayload({ response: { data: {} } }, '网络错误')
      expect(payload.message).toBe('网络错误')
    })
  })

  describe('buildDocsUrl', () => {
    it('拼接文档 URL 与锚点', () => {
      expect(buildDocsUrl('/troubleshooting', 'common-errors'))
        .toBe(`${DOCS_BASE_URL}/troubleshooting#common-errors`)
    })

    it('无锚点时只拼路径', () => {
      expect(buildDocsUrl('/quickstart', null)).toBe(`${DOCS_BASE_URL}/quickstart`)
    })

    it('路径为空时返回空字符串', () => {
      expect(buildDocsUrl('', 'anchor')).toBe('')
      expect(buildDocsUrl(null, 'anchor')).toBe('')
    })
  })
})
