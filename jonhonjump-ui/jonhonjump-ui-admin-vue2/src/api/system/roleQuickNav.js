import request from '@/utils/request'

export function getRoleQuickNavList(roleId) {
  return request({
    url: '/system/role/quick-nav/list',
    method: 'get',
    params: { roleId }
  })
}

export function saveRoleQuickNav(data) {
  return request({
    url: '/system/role/quick-nav/save',
    method: 'put',
    data
  })
}
