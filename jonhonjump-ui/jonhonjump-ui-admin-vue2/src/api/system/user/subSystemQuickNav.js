import request from '@/utils/request'

export function getSubSystemUserQuickNavList(subSystemId) {
  return request({
    url: '/system/sub-system-user/quick-nav/list',
    method: 'get',
    params: { subSystemId }
  })
}

export function saveSubSystemUserQuickNav(data) {
  return request({
    url: '/system/sub-system-user/quick-nav/save',
    method: 'put',
    data
  })
}

export function syncSubSystemUserQuickNavFromRole(subSystemId) {
  return request({
    url: '/system/sub-system-user/quick-nav/sync-from-role',
    method: 'post',
    params: { subSystemId }
  })
}
