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
