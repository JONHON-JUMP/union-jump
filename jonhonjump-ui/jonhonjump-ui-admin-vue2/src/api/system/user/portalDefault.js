import request from '@/utils/request'

export function getUserPortalDefault() {
  return request({
    url: '/system/user/portal-default/get',
    method: 'get',
    // 全局请求：门户启动/切系统依赖，路由切换不取消
    isGlobal: true
  })
}

export function saveUserPortalDefault(data) {
  return request({
    url: '/system/user/portal-default/save',
    method: 'put',
    data
  })
}

export function clearUserPortalDefault() {
  return request({
    url: '/system/user/portal-default/clear',
    method: 'delete'
  })
}
