import request from '@/utils/request'

export function getSubSystemPage(query) {
  return request({
    url: '/system/sub-system/page',
    method: 'get',
    params: query
  })
}

export function getSubSystem(id) {
  return request({
    url: '/system/sub-system/get?id=' + id,
    method: 'get'
  })
}

export function createSubSystem(data) {
  return request({
    url: '/system/sub-system/create',
    method: 'post',
    data: data
  })
}

export function updateSubSystem(data) {
  return request({
    url: '/system/sub-system/update',
    method: 'put',
    data: data
  })
}

export function deleteSubSystem(id) {
  return request({
    url: '/system/sub-system/delete?id=' + id,
    method: 'delete'
  })
}

export function deleteSubSystemList(ids) {
  return request({
    url: '/system/sub-system/delete-list?ids=' + ids.join(','),
    method: 'delete'
  })
}

export function getSubSystemOAuth2ClientSimpleList(excludeSubSystemId) {
  return request({
    url: '/system/sub-system/oauth2-client-simple-list',
    method: 'get',
    params: { excludeSubSystemId }
  })
}
