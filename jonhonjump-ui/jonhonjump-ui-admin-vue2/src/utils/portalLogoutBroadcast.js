/**
 * 多标签页：某一页切换用户/退出后，其它标签不得带着旧业务页去登录。
 * Token 在 sessionStorage（按标签隔离），用 localStorage 事件跨页广播。
 */
import { removeToken } from '@/utils/auth'
import { destroySessionGuard } from '@/utils/sessionGuard'

const FORCE_LOGIN_KEY = 'portal_force_login_home'

function buildLoginHomeUrl() {
  return `/login/?redirect=${encodeURIComponent('/index')}`
}

export function broadcastForceLoginHome() {
  try {
    localStorage.setItem(FORCE_LOGIN_KEY, String(Date.now()))
  } catch (e) { /* ignore */ }
}

export function installPortalLogoutBroadcast() {
  window.addEventListener('storage', (event) => {
    if (event.key !== FORCE_LOGIN_KEY || !event.newValue) {
      return
    }
    // 本标签仍可能留着旧 Token / 业务页：强制回默认首页登录
    try {
      destroySessionGuard()
      removeToken()
    } catch (e) { /* ignore */ }
    try {
      const topWin = window.top && window.top !== window.self ? window.top : window
      topWin.location.href = buildLoginHomeUrl()
    } catch (e) {
      window.location.href = buildLoginHomeUrl()
    }
  })
}
