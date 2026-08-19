/**
 * 多标签页会话协调：按身份精准踢，不误杀其它账号的并行会话。
 * Token 在 sessionStorage（按标签隔离），用 localStorage 事件跨页广播。
 */
import { removeToken, removeUsername, getUsername } from '@/utils/auth'
import { clearCamstarCookie } from '@/utils/camstarCookie'
import { destroySessionGuard } from '@/utils/sessionGuard'

const FORCE_LOGIN_KEY = 'portal_force_login_home'

function buildLoginHomeUrl() {
  return `/login/?redirect=${encodeURIComponent('/index')}`
}

/**
 * 广播强制回登录页。
 * @param {object} [options]
 * @param {boolean} [options.kickAll=false] true=踢掉所有标签页（空闲锁屏等整机无人场景）；
 *                                          默认只踢与登出者同账号的标签页，其它账号会话不受影响
 */
export function broadcastForceLoginHome(options) {
  const kickAll = !!(options && options.kickAll)
  try {
    localStorage.setItem(FORCE_LOGIN_KEY, JSON.stringify({
      kickAll,
      user: getUsername() || '',
      t: Date.now()
    }))
  } catch (e) { /* ignore */ }
}

function parseBroadcast(newValue) {
  try {
    const parsed = JSON.parse(newValue)
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch (e) {
    return null
  }
}

export function installPortalLogoutBroadcast() {
  window.addEventListener('storage', (event) => {
    if (event.key !== FORCE_LOGIN_KEY || !event.newValue) {
      return
    }
    const msg = parseBroadcast(event.newValue)
    // 消息解析失败（旧格式/异常）时保守起见全踢，兼容“现场换人”安全需求
    const kickAll = !msg || !!msg.kickAll
    if (!kickAll) {
      const self = getUsername()
      // 同账号标签页其 Token 已随本次登出语义失效，必须踢；
      // 其它账号是独立有效会话，不误杀（多标签页并行登录场景）
      if (!self || self !== msg.user) {
        return
      }
    }
    // 本标签仍可能留着旧 Token / 业务页：强制回默认首页登录
    try {
      destroySessionGuard()
      removeToken()
      removeUsername()
      clearCamstarCookie()
    } catch (e) { /* ignore */ }
    try {
      const topWin = window.top && window.top !== window.self ? window.top : window
      topWin.location.href = buildLoginHomeUrl()
    } catch (e) {
      window.location.href = buildLoginHomeUrl()
    }
  })
}
