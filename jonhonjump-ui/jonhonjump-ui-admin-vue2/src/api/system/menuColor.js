import request from '@/utils/request'

export function getMenuColorPage(query) {
  return request({ url: '/system/menu-color/page', method: 'get', params: query })
}

export function getMenuColor(id) {
  return request({ url: '/system/menu-color/get', method: 'get', params: { id } })
}

export function getMenuColorSimpleList() {
  return request({ url: '/system/menu-color/simple-list', method: 'get' })
}

export function createMenuColor(data) {
  return request({ url: '/system/menu-color/create', method: 'post', data })
}

export function updateMenuColor(data) {
  return request({ url: '/system/menu-color/update', method: 'put', data })
}

export function deleteMenuColor(id) {
  return request({ url: '/system/menu-color/delete', method: 'delete', params: { id } })
}

export function deleteMenuColorList(ids) {
  return request({ url: '/system/menu-color/delete-list', method: 'delete', params: { ids: ids.join(',') } })
}
