import request from '@/utils/request'

export function getProcessListenerPage(query) {
  return request({
    url: '/bpm/process-listener/page',
    method: 'get',
    params: query
  })
}

export function getProcessListener(id) {
  return request({
    url: '/bpm/process-listener/get?id=' + id,
    method: 'get'
  })
}

export function createProcessListener(data) {
  return request({
    url: '/bpm/process-listener/create',
    method: 'post',
    data: data
  })
}

export function updateProcessListener(data) {
  return request({
    url: '/bpm/process-listener/update',
    method: 'put',
    data: data
  })
}

export function deleteProcessListener(id) {
  return request({
    url: '/bpm/process-listener/delete?id=' + id,
    method: 'delete'
  })
}
