import request from '@/utils/request'

/**
 * 查询当前登录用户固定到门户首页的应用。
 *
 * 预期响应：
 * {
 *   code: 200,
 *   data: [
 *     { name: '角色管理', path: '/system/role', icon: 'peoples', group: '系统管理' }
 *   ]
 * }
 */
export function getPortalPinnedApps() {
  return request({
    url: '/portal/pinned-app/list',
    method: 'get'
  })
}

/**
 * 覆盖保存当前登录用户固定到门户首页的应用。
 *
 * 请求体：
 * {
 *   apps: [
 *     { name: '角色管理', path: '/system/role', icon: 'peoples', group: '系统管理' }
 *   ]
 * }
 */
export function savePortalPinnedApps(apps) {
  return request({
    url: '/portal/pinned-app/save',
    method: 'put',
    data: { apps }
  })
}
