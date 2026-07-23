import request from '@/utils/request'

export function getMyProcessInstancePage(query) {
  return request({
    url: '/bpm/process-instance/my-page',
    method: 'get',
    params: query
  })
}

export function getProcessInstanceManagerPage(query) {
  return request({
    url: '/bpm/process-instance/manager-page',
    method: 'get',
    params: query
  })
}

export function getProcessInstanceCopyPage(query) {
  return request({
    url: '/bpm/process-instance/copy/page',
    method: 'get',
    params: query
  })
}

export function createProcessInstance(data) {
  return request({
    url: '/bpm/process-instance/create',
    method: 'POST',
    data: data
  })
}

export function cancelProcessInstance(id, reason) {
  return request({
    url: '/bpm/process-instance/cancel-by-start-user',
    method: 'DELETE',
    data: {
      id,
      reason
    }
  })
}

export function cancelProcessInstanceByAdmin(id, reason) {
  return request({
    url: '/bpm/process-instance/cancel-by-admin',
    method: 'DELETE',
    data: {
      id,
      reason
    }
  })
}

export function getProcessInstance(id) {
  return request({
    url: '/bpm/process-instance/get?id=' + id,
    method: 'get',
  })
}
