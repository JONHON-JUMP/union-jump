import request from '@/utils/request'

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
