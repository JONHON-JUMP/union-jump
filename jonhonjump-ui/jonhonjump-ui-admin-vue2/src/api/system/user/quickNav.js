import request from '@/utils/request'

export function getUserQuickNavList() {
  return request({
    url: '/system/user/quick-nav/list',
    method: 'get'
  })
}

export function saveUserQuickNav(data) {
  return request({
    url: '/system/user/quick-nav/save',
    method: 'put',
    data
  })
}

export function syncUserQuickNavFromRole() {
  return request({
    url: '/system/user/quick-nav/sync-from-role',
    method: 'post'
  })
}
