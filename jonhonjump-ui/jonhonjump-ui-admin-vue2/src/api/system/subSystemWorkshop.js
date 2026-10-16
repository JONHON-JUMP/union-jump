import request from '@/utils/request'
import { getSubSystemClientSimpleList } from '@/api/system/subSystemUsers'

export { getSubSystemClientSimpleList }

/** 业务系统子表叶子列表（启用态字典/辅助筛选） */
export function getSubSystemWorkshopList() {
  return request({
    url: '/system/sub-system-workshop/list',
    method: 'get'
  })
}

/** 人员可挂外部树：仅子表叶子可选 */
export function getHangableExternalTree() {
  return request({
    url: '/system/sub-system-workshop/hangable-tree',
    method: 'get'
  })
}

export function getSubSystemWorkshopPage(query) {
  return request({
    url: '/system/sub-system-workshop/page',
    method: 'get',
    params: query
  })
}

export function getSubSystemWorkshop(id) {
  return request({
    url: '/system/sub-system-workshop/get?id=' + id,
    method: 'get'
  })
}

export function createSubSystemWorkshop(data) {
  return request({
    url: '/system/sub-system-workshop/create',
    method: 'post',
    data: data
  })
}

export function updateSubSystemWorkshop(data) {
  return request({
    url: '/system/sub-system-workshop/update',
    method: 'put',
    data: data
  })
}

export function deleteSubSystemWorkshop(id) {
  return request({
    url: '/system/sub-system-workshop/delete?id=' + id,
    method: 'delete'
  })
}

export function deleteSubSystemWorkshopList(ids) {
  return request({
    url: '/system/sub-system-workshop/delete-list?ids=' + ids.join(','),
    method: 'delete'
  })
}
