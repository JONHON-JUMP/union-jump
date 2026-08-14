import request from '@/utils/request'

export function getUserPortalDefault() {
  return request({
    url: '/system/user/portal-default/get',
    method: 'get'
  })
}

export function saveUserPortalDefault(data) {
  return request({
    url: '/system/user/portal-default/save',
    method: 'put',
    data
  })
}

export function clearUserPortalDefault() {
  return request({
    url: '/system/user/portal-default/clear',
    method: 'delete'
  })
}
