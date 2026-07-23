import request from '@/utils/request'

export function getSubSystemUsersByMainUserId(mainUserId) {
  return request({
    url: '/system/sub-system-users/list-by-main-user-id',
    method: 'get',
    params: { mainUserId }
  })
}

export function getMyExternalSystemList() {
  return request({
    url: '/system/sub-system-users/my-list',
    method: 'get'
  })
}

export function getMyPortalMenus(subSystemId) {
  return request({
    url: '/system/sub-system-users/my-menus',
    method: 'get',
    params: { subSystemId }
  })
}

export function getSubSystemClientSimpleList() {
  return request({
    url: '/system/sub-system-users/client-simple-list',
    method: 'get'
  })
}

export function getSubSystemUserPage(query) {
  return request({
    url: '/system/sub-system-users/page',
    method: 'get',
    params: query
  })
}

export function getSubSystemUser(id) {
  return request({
    url: '/system/sub-system-users/get?id=' + id,
    method: 'get'
  })
}

export function createSubSystemUser(data) {
  return request({
    url: '/system/sub-system-users/create',
    method: 'post',
    data: data
  })
}

export function updateSubSystemUser(data) {
  return request({
    url: '/system/sub-system-users/update',
    method: 'put',
    data: data
  })
}

export function deleteSubSystemUser(id) {
  return request({
    url: '/system/sub-system-users/delete?id=' + id,
    method: 'delete'
  })
}

export function deleteSubSystemUserList(ids) {
  return request({
    url: '/system/sub-system-users/delete-list?ids=' + ids.join(','),
    method: 'delete'
  })
}

export function updateSubSystemUserStatus(id, status) {
  return request({
    url: '/system/sub-system-users/update-status?id=' + id + '&status=' + status,
    method: 'put'
  })
}

export function getSubSystemRoleSimpleList(subSystemId) {
  return request({
    url: '/system/sub-system-users/role-simple-list',
    method: 'get',
    params: { subSystemId }
  })
}

export function getSubSystemPostSimpleList(subSystemId) {
  return request({
    url: '/system/sub-system-users/post-simple-list',
    method: 'get',
    params: { subSystemId }
  })
}

export function getSubSystemTeamSimpleList(subSystemId) {
  return request({
    url: '/system/sub-system-users/team-simple-list',
    method: 'get',
    params: { subSystemId }
  })
}

export function getSubSystemUserHomeMenuTree(subSystemId, roleIds) {
  return request({
    url: '/system/sub-system-users/home-menu-tree-list',
    method: 'get',
    params: {
      subSystemId,
      roleIds: roleIds && roleIds.length > 0 ? roleIds.join(',') : undefined
    }
  })
}

export function getSubSystemUserRoleIds(id) {
  return request({
    url: '/system/sub-system-users/list-role-ids?id=' + id,
    method: 'get'
  })
}

export function assignSubSystemUserRole(data) {
  return request({
    url: '/system/sub-system-users/assign-role',
    method: 'put',
    data: data
  })
}

/** 下载子系统用户导入模板 */
export function importSubSystemUserTemplate() {
  return request({
    url: '/system/sub-system-users/get-import-template',
    method: 'get',
    responseType: 'blob'
  })
}

/** 按用户名查询子系统用户 */
export function getSubSystemUserByUsername(subSystemId, username) {
  return request({
    url: '/system/sub-system-users/get-by-username',
    method: 'get',
    params: { subSystemId, username }
  })
}

/** 挂接主系统用户到子系统同名用户 */
export function bindSubSystemMainUser(subSystemId, mainUserId) {
  return request({
    url: '/system/sub-system-users/bind-main-user',
    method: 'post',
    params: { subSystemId, mainUserId }
  })
}

