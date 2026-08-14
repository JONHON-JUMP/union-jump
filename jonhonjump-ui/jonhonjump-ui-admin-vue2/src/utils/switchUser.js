import { MessageBox } from 'element-ui'
import { destroySessionGuard } from '@/utils/sessionGuard'
import { broadcastForceLoginHome } from '@/utils/portalLogoutBroadcast'

/** 尽量拿到顶层窗口，避免登录页被套在 iframe / 门户壳里 */
export function getTopWindow() {
  try {
    if (window.top && window.top !== window.self) {
      return window.top
    }
  } catch (e) { /* cross-origin */ }
  return window
}

function buildLoginUrl(redirectPath) {
  const redirect = encodeURIComponent(redirectPath || '/index')
  return `/login/?redirect=${redirect}`
}

/** 切换用户 / 退出登录：清登录态并跳转登录页 */
export function switchUser(store, redirectPath) {
  destroySessionGuard()
  // 通知其它标签页：勿带着旧业务页 redirect 去登录
  broadcastForceLoginHome()
  return store.dispatch('LogOut').finally(() => {
    getTopWindow().location.href = buildLoginUrl(redirectPath)
  })
}

/**
 * 切换用户：固定回门户默认首页 /index（按新用户星标默认系统进入）。
 * 故意忽略业务页 path，避免换人后落到上一账号的页面。
 */
export function confirmSwitchUser(store) {
  return MessageBox.confirm(
    '切换用户将退出当前账号并返回登录页，是否继续？',
    '切换用户',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => switchUser(store, '/index')).catch(() => {})
}

/** 退出登录：同样固定 /index，避免下次登录落到上一业务页 */
export function confirmLogout(store) {
  return MessageBox.confirm(
    '确定退出登录吗？',
    '退出登录',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => switchUser(store, '/index')).catch(() => {})
}

export function redirectToLogin(redirectPath) {
  getTopWindow().location.href = buildLoginUrl(redirectPath)
}

/**
 * 需要重新登录：先提示，确认后顶层跳转门户登录页（禁止在壳内嵌登录页）
 * @param {object} options
 * @param {boolean} [options.force=false] 强制重新登录：无取消按钮，关窗/ESC 也会跳转登录
 */
export function promptReloginAndRedirect(options = {}) {
  const {
    message = '登录状态已过期，请重新登录',
    title = '系统提示',
    redirectPath = '/index',
    store = null,
    force = false
  } = options
  const go = () => {
    const jump = () => redirectToLogin(redirectPath)
    if (store && typeof store.dispatch === 'function') {
      broadcastForceLoginHome()
      return store.dispatch('LogOut').then(jump).catch(jump)
    }
    jump()
  }
  if (force) {
    return MessageBox.alert(message, title, {
      confirmButtonText: '重新登录',
      type: 'warning',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false,
      distinguishCancelAndClose: true
    }).then(go).catch(go)
  }
  return MessageBox.confirm(message, title, {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning',
    closeOnClickModal: false
  }).then(go).catch(() => {})
}

/** 权限变更等场景：强制回登录，不给取消，避免白屏卡死 */
export function forceReloginAndRedirect(options = {}) {
  return promptReloginAndRedirect({ ...options, force: true })
}
