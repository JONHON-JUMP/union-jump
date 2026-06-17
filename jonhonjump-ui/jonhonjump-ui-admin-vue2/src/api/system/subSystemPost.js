import request from '@/utils/request'
import { getSubSystemClientSimpleList } from '@/api/system/subSystemUsers'

export { getSubSystemClientSimpleList }

export function getSubSystemPostPage(query) {
  return request({
    url: '/system/sub-system-post/page',
    method: 'get',
    params: query
  })
}

export function getSubSystemPost(id) {
  return request({
    url: '/system/sub-system-post/get?id=' + id,
    method: 'get'
  })
}

export function createSubSystemPost(data) {
  return request({
    url: '/system/sub-system-post/create',
    method: 'post',
    data: data
  })
}

export function updateSubSystemPost(data) {
  return request({
    url: '/system/sub-system-post/update',
    method: 'put',
    data: data
  })
}

export function deleteSubSystemPost(id) {
  return request({
    url: '/system/sub-system-post/delete?id=' + id,
    method: 'delete'
  })
}

export function deleteSubSystemPostList(ids) {
  return request({
    url: '/system/sub-system-post/delete-list?ids=' + ids.join(','),
    method: 'delete'
  })
}
