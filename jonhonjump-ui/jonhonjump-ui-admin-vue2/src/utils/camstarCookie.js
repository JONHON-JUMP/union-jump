/**
 * 对齐 4200：登录后写 Nancal_Cam_SessionId = Base64(工号)，path=/。
 * 4200 成功路径：本机 setCookie → iframe 立刻挂 meta.link（同 hostname 不同端口 Cookie 共享）。
 * 不在打开路径上阻塞等待跨机种 bridge（bridge 未部署会拖死首开）。
 */
import Cookies from 'js-cookie'
import { getUsername } from '@/utils/auth'

const CAMSTAR_COOKIE = 'Nancal_Cam_SessionId'
const COOKIEKEY_STORE = 'JUMP_CAMSTAR_COOKIEKEY'

function toBase64Utf8(text) {
  try {
    return btoa(unescape(encodeURIComponent(String(text || ''))))
  } catch (e) {
    return btoa(String(text || ''))
  }
}

export function saveCamstarCookieKey(key) {
  if (!key) {
    return
  }
  sessionStorage.setItem(COOKIEKEY_STORE, String(key))
  Cookies.set(CAMSTAR_COOKIE, String(key), { path: '/' })
}

export function resolveCamstarCookieKey() {
  // 当前登录用户永远最权威：换号登录/刷新会话时自动纠偏旧 Cookie，避免残留上一个账号的 key
  const username = getUsername()
  const fromUser = username ? toBase64Utf8(username) : ''
  if (fromUser) {
    return fromUser
  }
  const saved = sessionStorage.getItem(COOKIEKEY_STORE)
  if (saved) {
    return saved
  }
  const existing = Cookies.get(CAMSTAR_COOKIE)
  if (existing) {
    return existing
  }
  return ''
}

/** 退出登录时清残留：同一浏览器换号后不得再带上一账号的 Camstar 会话 */
export function clearCamstarCookie() {
  sessionStorage.removeItem(COOKIEKEY_STORE)
  Cookies.remove(CAMSTAR_COOKIE, { path: '/' })
}

/** 对齐 4200 setCookie：在当前页 host 写入 Cookie（打开 Camstar 前调用即可） */
export function ensureLocalCamstarCookie() {
  const key = resolveCamstarCookieKey()
  if (!key) {
    return ''
  }
  Cookies.set(CAMSTAR_COOKIE, key, { path: '/' })
  return key
}

/**
 * 可选：后台尝试给其它 host 种 Cookie（需目标机已部署 camstar-cookie-bridge.html）。
 * 不阻塞业务打开；失败静默忽略。
 */
export function seedCamstarCookieForUrlInBackground(httpUrl) {
  const key = ensureLocalCamstarCookie()
  if (!key || !httpUrl) {
    return
  }
  let parsed
  try {
    parsed = new URL(httpUrl, window.location.href)
  } catch (e) {
    return
  }
  if (parsed.hostname === window.location.hostname) {
    return
  }
  const bridges = []
  if (/camstarportal/i.test(parsed.pathname || '')) {
    bridges.push(`${parsed.origin}/CamstarPortal/camstar-cookie-bridge.html`)
  }
  bridges.push(`${parsed.origin}/camstar-cookie-bridge.html`)
  bridges.forEach((base, i) => {
    window.setTimeout(() => {
      try {
        const iframe = document.createElement('iframe')
        iframe.setAttribute('title', 'camstar-cookie-bridge')
        iframe.style.cssText = 'position:absolute;width:0;height:0;border:0;visibility:hidden'
        iframe.src = `${base}?v=${encodeURIComponent(key)}&_t=${Date.now()}`
        document.body.appendChild(iframe)
        window.setTimeout(() => {
          if (iframe.parentNode) {
            iframe.parentNode.removeChild(iframe)
          }
        }, 3000)
      } catch (e) { /* ignore */ }
    }, i * 200)
  })
}
