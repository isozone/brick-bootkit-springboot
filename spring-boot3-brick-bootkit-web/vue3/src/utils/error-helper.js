export const DOCS_BASE_URL = 'http://brickbootkit.aius.autos'

export function resolveApiErrorPayload(error, fallbackMessage = '操作失败') {
  const payload = error?.response?.data || {}
  return {
    message: payload.message || fallbackMessage,
    errorKey: payload.errorKey || null,
    hintPath: payload.hintPath || null,
    hintAnchor: payload.hintAnchor || null
  }
}

export function buildDocsUrl(hintPath, hintAnchor) {
  if (!hintPath) return ''
  const anchor = hintAnchor ? `#${hintAnchor}` : ''
  return `${DOCS_BASE_URL}${hintPath}${anchor}`
}
