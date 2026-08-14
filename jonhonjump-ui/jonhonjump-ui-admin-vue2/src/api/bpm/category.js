import request from '@/utils/request'

export function getCategoryPage(query) {
  return request({
    url: '/bpm/category/page',
    method: 'get',
    params: query
  })
}

export function getCategorySimpleList() {
  return request({
    url: '/bpm/category/simple-list',
    method: 'get'
  })
}

export function getCategory(id) {
  return request({
    url: '/bpm/category/get?id=' + id,
    method: 'get'
  })
}

export function createCategory(data) {
  return request({
    url: '/bpm/category/create',
    method: 'post',
    data: data
  })
}

export function updateCategory(data) {
  return request({
    url: '/bpm/category/update',
    method: 'put',
    data: data
  })
}

export function deleteCategory(id) {
  return request({
    url: '/bpm/category/delete?id=' + id,
    method: 'delete'
  })
}
