import request from '@/utils/request'
import { getSubSystemClientSimpleList } from '@/api/system/subSystemUsers'

export { getSubSystemClientSimpleList }

export function getSubSystemTeamPage(query) {
  return request({
    url: '/system/sub-system-team/page',
    method: 'get',
    params: query
  })
}

export function getSubSystemTeam(id) {
  return request({
    url: '/system/sub-system-team/get?id=' + id,
    method: 'get'
  })
}

export function createSubSystemTeam(data) {
  return request({
    url: '/system/sub-system-team/create',
    method: 'post',
    data: data
  })
}

export function updateSubSystemTeam(data) {
  return request({
    url: '/system/sub-system-team/update',
    method: 'put',
    data: data
  })
}

export function deleteSubSystemTeam(id) {
  return request({
    url: '/system/sub-system-team/delete?id=' + id,
    method: 'delete'
  })
}

export function deleteSubSystemTeamList(ids) {
  return request({
    url: '/system/sub-system-team/delete-list?ids=' + ids.join(','),
    method: 'delete'
  })
}

export function getSubSystemTeamUserSimpleList(subSystemId) {
  return request({
    url: '/system/sub-system-team/user-simple-list',
    method: 'get',
    params: { subSystemId }
  })
}

/** 下载外部系统班组导入模板 */
export function importSubSystemTeamTemplate() {
  return request({
    url: '/system/sub-system-team/get-import-template',
    method: 'get',
    responseType: 'blob'
  })
}
