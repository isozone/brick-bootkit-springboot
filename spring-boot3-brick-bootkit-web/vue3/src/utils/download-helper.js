export function downloadBlobResponse(response, fallbackFilename) {
  const blob = response?.data
  if (!blob) return

  const disposition = response.headers?.['content-disposition'] || ''
  const matched = disposition.match(/filename=\"?([^\";]+)\"?/)
  const filename = matched?.[1] || fallbackFilename

  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
