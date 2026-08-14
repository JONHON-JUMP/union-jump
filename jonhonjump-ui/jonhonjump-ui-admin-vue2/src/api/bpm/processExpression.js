import request from '@/utils/request'

export function getProcessExpressionPage(query) {
  return request({
    url: '/bpm/process-expression/page',
    method: 'get',
    params: query
  })
}

export function getProcessExpression(id) {
  return request({
    url: '/bpm/process-expression/get?id=' + id,
    method: 'get'
  })
}

export function createProcessExpression(data) {
  return request({
    url: '/bpm/process-expression/create',
    method: 'post',
    data: data
  })
}

export function updateProcessExpression(data) {
  return request({
    url: '/bpm/process-expression/update',
    method: 'put',
    data: data
  })
}

export function deleteProcessExpression(id) {
  return request({
    url: '/bpm/process-expression/delete?id=' + id,
    method: 'delete'
  })
}
