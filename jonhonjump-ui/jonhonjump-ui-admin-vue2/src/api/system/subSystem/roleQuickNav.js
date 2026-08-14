import request from '@/utils/request'

export function getSubSystemRoleQuickNavList(roleId) {
  return request({
    url: '/system/sub-system-role/quick-nav/list',
    method: 'get',
    params: { roleId }
  })
}

export function saveSubSystemRoleQuickNav(data) {
  return request({
    url: '/system/sub-system-role/quick-nav/save',
    method: 'put',
    data
  })
}
