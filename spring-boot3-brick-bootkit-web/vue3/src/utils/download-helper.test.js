import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { downloadBlobResponse } from './download-helper'

describe('download-helper', () => {
  const createObjectURL = vi.fn(() => 'blob:fake-url')
  const revokeObjectURL = vi.fn()
  let clickSpy

  beforeEach(() => {
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL })
    clickSpy = vi.fn()
    vi.spyOn(document.body, 'appendChild').mockImplementation(() => {})
    vi.spyOn(document.body, 'removeChild').mockImplementation(() => {})
    vi.spyOn(document, 'createElement').mockImplementation(() => ({ click: clickSpy, set href(v) {}, get href() { return '' } }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('无响应数据时不执行下载', () => {
    downloadBlobResponse({}, 'fallback.jar')
    expect(createObjectURL).not.toHaveBeenCalled()
    expect(clickSpy).not.toHaveBeenCalled()
  })

  it('从 content-disposition 提取文件名', () => {
    const response = {
      data: new Blob(['hello']),
      headers: { 'content-disposition': 'attachment; filename="demo-plugin.jar"' }
    }
    downloadBlobResponse(response, 'fallback.jar')
    expect(createObjectURL).toHaveBeenCalled()
    expect(clickSpy).toHaveBeenCalled()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:fake-url')
  })
})
