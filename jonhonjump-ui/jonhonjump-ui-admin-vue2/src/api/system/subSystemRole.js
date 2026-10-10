import request from '@/utils/request'
import { getSubSystemClientSimpleList } from '@/api/system/subSystemUsers'

export { getSubSystemClientSimpleList }

export function getSubSystemRolePage(query) {
  return request({
    url: '/system/sub-system-role/page',
    method: 'get',
    params: query
  })
}

export function getSubSystemRole(id) {
  return request({
    url: '/system/sub-system-role/get?id=' + id,
    method: 'get'
  })
}

export function createSubSystemRole(data) {
  return request({
    url: '/system/sub-system-role/create',
    method: 'post',
    data: data
  })
}

export function updateSubSystemRole(data) {
  return request({
    url: '/system/sub-system-role/update',
    method: 'put',
    data: data
  })
}

export function deleteSubSystemRole(id) {
  return request({
    url: '/system/sub-system-role/delete?id=' + id,
    method: 'delete'
  })
}

export function deleteSubSystemRoleList(ids) {
  return request({
    url: '/system/sub-system-role/delete-list?ids=' + ids.join(','),
    method: 'delete'
  })
}

export function updateSubSystemRoleStatus(id, status) {
  return request({
    url: '/system/sub-system-role/update-status?id=' + id + '&status=' + status,
    method: 'put'
  })
}

/** 修改角色接口注册状态（0未注册 1已注册） */
export function updateSubSystemRoleRegisterStatus(id, roleRegistered) {
  return request({
    url: '/system/sub-system-role/update-register-status?id=' + id + '&roleRegistered=' + roleRegistered,
    method: 'put'
  })
}

/** 未注册角色调对方「角色新增」接口补注册 */
export function registerSubSystemRole(id, data) {
  return request({
    url: '/system/sub-system-role/register?id=' + id,
    method: 'post',
    data: data || {}
  })
}

/** 按车间列出外部系统已有角色（角色名 + roleId），供「关联外部角色」选择 */
export function getSubSystemExternalRoleList(apiSubSystemId, workshopCode) {
  return request({
    url: '/system/sub-system-role/external-role-list',
    method: 'post',
    data: { apiSubSystemId, workshopCode }
  })
}

/** 关联/解除外部系统角色（externalRoleId 为空 = 解除关联） */
export function bindSubSystemExternalRole(id, externalRoleId) {
  return request({
    url: '/system/sub-system-role/bind-external-role?id=' + id
      + '&externalRoleId=' + encodeURIComponent(externalRoleId == null ? '' : externalRoleId),
    method: 'put'
  })
}

export function getSubSystemMenuSimpleList(subSystemId) {
  return request({
    url: '/system/sub-system-role/menu-simple-list',
    method: 'get',
    params: { subSystemId }
  })
}

export function getSubSystemRoleMenuIds(roleId) {
  return request({
    url: '/system/sub-system-role/list-role-menu-ids',
    method: 'get',
    params: { roleId }
  })
}

export function assignSubSystemRoleMenu(data) {
  return request({
    url: '/system/sub-system-role/assign-role-menu',
    method: 'put',
    data: data
  })
}

export function assignSubSystemRoleDataScope(data) {
  return request({
    url: '/system/sub-system-role/assign-role-data-scope',
    method: 'put',
    data: data
  })
}

/** 下载外部系统角色导入模板 */
export function importSubSystemRoleTemplate() {
  return request({
    url: '/system/sub-system-role/get-import-template',
    method: 'get',
    responseType: 'blob'
  })
}
