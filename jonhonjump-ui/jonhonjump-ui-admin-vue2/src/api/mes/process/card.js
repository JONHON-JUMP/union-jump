import request from '@/utils/publicRequest'

export function getDocumentPdfUrl(accno) {
  const base = (process.env.VUE_APP_BASE_API || '').replace(/\/$/, '')
  return `${base}/admin-api/mes/process/query/document-pdf?accno=${encodeURIComponent(accno)}`
}

export function queryProcessCard(data) {
  return request({
    url: '/mes/process/query/card',
    method: 'post',
    data
  })
}

export function queryProcessFileUrl(data) {
  return request({
    url: '/mes/process/query/file-url',
    method: 'post',
    data
  })
}
