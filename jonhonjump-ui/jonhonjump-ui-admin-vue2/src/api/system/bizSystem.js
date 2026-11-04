import request from '@/utils/request'

export function listBizSystem(query) {
  return request({
    url: '/system/biz-system/page',
    method: 'get',
    params: query
  })
}

export function getBizSystem(id) {
  return request({
    url: '/system/biz-system/get?id=' + id,
    method: 'get'
  })
}

export function addBizSystem(data) {
  return request({
    url: '/system/biz-system/create',
    method: 'post',
    data: data
  })
}

export function updateBizSystem(data) {
  return request({
    url: '/system/biz-system/update',
    method: 'put',
    data: data
  })
}

export function delBizSystem(id) {
  return request({
    url: '/system/biz-system/delete?id=' + id,
    method: 'delete'
  })
}

export function delBizSystemList(ids) {
  return request({
    url: '/system/biz-system/delete-list?ids=' + ids,
    method: 'delete'
  })
}
