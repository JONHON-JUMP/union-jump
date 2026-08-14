import request from '@/utils/request'
import { getSubSystemClientSimpleList } from '@/api/system/subSystemUsers'

export { getSubSystemClientSimpleList }

export function getSubSystemMenuList(query) {
  return request({
    url: '/system/sub-system-menu/list',
    method: 'get',
    params: query
  })
}

export function getSubSystemMenu(id) {
  return request({
    url: '/system/sub-system-menu/get?id=' + id,
    method: 'get'
  })
}

export function createSubSystemMenu(data) {
  return request({
    url: '/system/sub-system-menu/create',
    method: 'post',
    data: data
  })
}

export function updateSubSystemMenu(data) {
  return request({
    url: '/system/sub-system-menu/update',
    method: 'put',
    data: data
  })
}

export function deleteSubSystemMenu(id) {
  return request({
    url: '/system/sub-system-menu/delete?id=' + id,
    method: 'delete'
  })
}

export function deleteSubSystemMenuList(ids) {
  return request({
    url: '/system/sub-system-menu/delete-list?ids=' + ids.join(','),
    method: 'delete'
  })
}

/** 清门户 my-menus Redis 缓存（改菜单后仍见旧路由时手动调用） */
export function clearPortalMenuCache(subSystemId) {
  return request({
    url: '/system/sub-system-menu/clear-portal-cache',
    method: 'post',
    params: { subSystemId }
  })
}

/** 下载外部系统菜单导入模板 */
export function importSubSystemMenuTemplate() {
  return request({
    url: '/system/sub-system-menu/get-import-template',
    method: 'get',
    responseType: 'blob'
  })
}
