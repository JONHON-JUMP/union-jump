import request from '@/utils/request'



export const DEFAULT_MENU_ICON_RADIUS = '22px'



export function getMenuStylePage(query) {

  return request({ url: '/system/menu-style/page', method: 'get', params: query })

}



export function getMenuStyle(id) {

  return request({ url: '/system/menu-style/get', method: 'get', params: { id } })

}



export function getMenuStyleSimpleList() {

  return request({ url: '/system/menu-style/simple-list', method: 'get' })

}



export function getMenuStyleDefault() {

  return request({ url: '/system/menu-style/default', method: 'get' })

}



export function createMenuStyle(data) {

  return request({ url: '/system/menu-style/create', method: 'post', data })

}



export function updateMenuStyle(data) {

  return request({ url: '/system/menu-style/update', method: 'put', data })

}



export function deleteMenuStyle(id) {

  return request({ url: '/system/menu-style/delete', method: 'delete', params: { id } })

}



export function deleteMenuStyleList(ids) {

  return request({ url: '/system/menu-style/delete-list', method: 'delete', params: { ids: ids.join(',') } })

}



export function menuIconRadius(shape) {
  if (shape === 'circle') return '50%'
  if (shape === 'square') return '4px'
  return DEFAULT_MENU_ICON_RADIUS
}

