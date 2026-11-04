import request from '@/utils/request'

export function getUserQuickNavList() {
  return request({
    url: '/system/user/quick-nav/list',
    method: 'get',
    // 全局请求：门户首页快捷导航，路由切换不取消
    isGlobal: true
  })
}

export function saveUserQuickNav(data) {
  return request({
    url: '/system/user/quick-nav/save',
    method: 'put',
    data
  })
}

/** 单项加入（增量，只动这一项；星标开关专用，全量 save 列表不完整时会覆盖丢项） */
export function addUserQuickNavMenu(menuId) {
  return request({
    url: '/system/user/quick-nav/add-menu',
    method: 'put',
    params: { menuId }
  })
}

/** 单项移除（增量，只动这一项） */
export function removeUserQuickNavMenu(menuId) {
  return request({
    url: '/system/user/quick-nav/remove-menu',
    method: 'put',
    params: { menuId }
  })
}

export function syncUserQuickNavFromRole() {
  return request({
    url: '/system/user/quick-nav/sync-from-role',
    method: 'post'
  })
}
