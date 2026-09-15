/**
 * 对齐 4200：登录后写 Nancal_Cam_SessionId = Base64(工号)，path=/。
 * 4200 成功路径：本机 setCookie → iframe 立刻挂 meta.link（同 hostname 不同端口 Cookie 共享）。
 * 不在打开路径上阻塞等待跨机种 bridge（bridge 未部署会拖死首开）。
 */
import Cookies from 'js-cookie'
import { getUsername } from '@/utils/auth'
import { isCamstarLikeUrl } from '@/utils/portalMenuKind'

const CAMSTAR_COOKIE = 'Nancal_Cam_SessionId'
const COOKIEKEY_STORE = 'JUMP_CAMSTAR_COOKIEKEY'
// Camstar 侧登录用户名（拼接车间的用户为 车间编号_工号；GetInfo 时预取写入）
const CAMSTAR_USERNAME_STORE = 'JUMP_CAMSTAR_USERNAME'
// 子系统 clientId / subSystemId → 对接用户名：打开系统时按"用户选择的系统"精确取该用户
// 在此系统的身份（my-list externalUsername，来源于花名册行的拼接标记），不依赖 URL 形态
const EXTERNAL_USERNAME_BY_CLIENT = {}
const EXTERNAL_USERNAME_BY_SUBSYSTEM = {}
// 子系统 origin(协议+主机+端口) → 对接用户名：同一 hostname 不同端口也部署系统（4200 模式），
// 而 Cookie 域不分端口，URL 兜底匹配必须用含端口的 origin
const EXTERNAL_USERNAME_BY_ORIGIN = {}
// hostname → 对接用户名：仅该主机只挂一个系统时可用作兜底；多系统共主机时置空避免带错身份
const EXTERNAL_USERNAME_BY_HOST = {}

function toBase64Utf8(text) {
  try {
    return btoa(unescape(encodeURIComponent(String(text || ''))))
  } catch (e) {
    return btoa(String(text || ''))
  }
}

function parseUrl(url) {
  try {
    return new URL(url, window.location.href)
  } catch (e) {
    return null
  }
}

function clearExternalUsernameMaps() {
  [EXTERNAL_USERNAME_BY_CLIENT, EXTERNAL_USERNAME_BY_SUBSYSTEM,
    EXTERNAL_USERNAME_BY_ORIGIN, EXTERNAL_USERNAME_BY_HOST].forEach(map => {
    Object.keys(map).forEach(k => {
      delete map[k]
    })
  })
}

/**
 * 注册子系统对接用户名（portal store 的 loadSystemList 成功后调用）。
 * systems 为 my-list 返回项：{ subSystemId, clientId, systemUrl, externalUsername, ... }。
 * clientId/subSystemId 为精确主键（打开/切换系统时用）；origin/host 仅作无系统上下文时的 URL 兜底。
 */
export function registerExternalUsernames(systems) {
  clearExternalUsernameMaps()
  ;(systems || []).forEach(item => {
    if (!item) {
      return
    }
    const name = resolveExternalUsernameFromRoster(item)
    if (!name) {
      return
    }
    // 无 systemUrl 也要登记：切换系统种 Cookie 只依赖 clientId，不能因缺 URL 丢身份
    if (item.clientId) {
      EXTERNAL_USERNAME_BY_CLIENT[String(item.clientId)] = name
    }
    if (item.subSystemId) {
      EXTERNAL_USERNAME_BY_SUBSYSTEM[String(item.subSystemId)] = name
    }
    if (!item.systemUrl) {
      return
    }
    const parsed = parseUrl(item.systemUrl)
    if (!parsed) {
      return
    }
    EXTERNAL_USERNAME_BY_ORIGIN[parsed.origin.toLowerCase()] = name
    const host = parsed.hostname.toLowerCase()
    const exist = EXTERNAL_USERNAME_BY_HOST[host]
    // 同 hostname 多个系统（不同端口共享 Cookie 域）且身份不同 → hostname 兜底歧义，置空
    if (exist === undefined) {
      EXTERNAL_USERNAME_BY_HOST[host] = name
    } else if (exist !== name) {
      EXTERNAL_USERNAME_BY_HOST[host] = ''
    }
  })
}

/** 按系统标识取该系统的对接用户名（clientId 优先，subSystemId 兜底） */
export function resolveUsernameForSystem(clientIdOrSubSystemId) {
  if (!clientIdOrSubSystemId) {
    return ''
  }
  const key = String(clientIdOrSubSystemId)
  return EXTERNAL_USERNAME_BY_CLIENT[key] || EXTERNAL_USERNAME_BY_SUBSYSTEM[key] || ''
}

/** 按目标 URL 解析该系统的对接用户名：origin 精确匹配 > hostname 唯一兜底；未注册返回空 */
export function resolveUsernameForUrl(url) {
  const parsed = parseUrl(url)
  if (!parsed) {
    return ''
  }
  const byOrigin = EXTERNAL_USERNAME_BY_ORIGIN[parsed.origin.toLowerCase()]
  if (byOrigin) {
    return byOrigin
  }
  const byHost = EXTERNAL_USERNAME_BY_HOST[parsed.hostname.toLowerCase()]
  return byHost || ''
}

export function saveCamstarCookieKey(key) {
  if (!key) {
    return
  }
  sessionStorage.setItem(COOKIEKEY_STORE, String(key))
  Cookies.set(CAMSTAR_COOKIE, String(key), { path: '/' })
}

/** 缓存 Camstar 侧登录用户名（车间_工号 与工号不同名时） */
export function saveCamstarUsername(name) {
  if (!name) {
    return
  }
  sessionStorage.setItem(CAMSTAR_USERNAME_STORE, String(name))
}

export function getCamstarUsername() {
  return sessionStorage.getItem(CAMSTAR_USERNAME_STORE) || ''
}

/**
 * 解析 Camstar Cookie 值。可选传入系统标识与目标 URL，优先级：
 * 1. clientId/subSystemId 精确命中（打开/切换系统时的权威方式）
 * 2. URL origin 精确匹配 > hostname 唯一兜底
 * 3. 若已指定 systemId 仍未命中：只用主登录工号（禁止用全局「车间_工号」兜底，避免切到不拼接系统仍带旧身份）
 * 4. 未指定 systemId：预取的 Camstar 用户名 > 主登录工号 > 旧缓存
 */
export function resolveCamstarCookieKey(url, systemId) {
  const bySystem = resolveUsernameForSystem(systemId)
  if (bySystem) {
    return toBase64Utf8(bySystem)
  }
  const forUrl = resolveUsernameForUrl(url)
  if (forUrl) {
    return toBase64Utf8(forUrl)
  }
  const username = getUsername()
  const fromUser = username ? toBase64Utf8(username) : ''
  // 指定了目标系统却没命中注册表：绝不能回落到其它系统的「车间_工号」
  if (systemId) {
    return fromUser || ''
  }
  const camstarUsername = getCamstarUsername()
  if (camstarUsername) {
    return toBase64Utf8(camstarUsername)
  }
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
  sessionStorage.removeItem(CAMSTAR_USERNAME_STORE)
  clearExternalUsernameMaps()
  Cookies.remove(CAMSTAR_COOKIE, { path: '/' })
}

/**
 * 按花名册字段算对接用户名：关联车间编号=是 → 车间_工号，否则 → 工号。
 * 有 usernameWithWorkshop 时本地重算，不盲信可能过期的 externalUsername。
 */
export function resolveExternalUsernameFromRoster(system) {
  if (!system) {
    return ''
  }
  const flag = system.usernameWithWorkshop
  const withWorkshop = flag === '1' || flag === 1 || flag === true
  const explicitlyNo = flag === '0' || flag === 0 || flag === false
  const workshopId = String(system.workshopId || '').trim()
  const bare = String(system.username || system.userName || getUsername() || '').trim()
  if (withWorkshop && workshopId && bare) {
    if (bare.indexOf(workshopId + '_') === 0) {
      return bare
    }
    return workshopId + '_' + bare
  }
  if (explicitlyNo && bare) {
    return bare
  }
  const fromApi = String(system.externalUsername || '').trim()
  return fromApi || bare
}

/**
 * 按目标业务系统重写本域 Camstar Cookie（切换系统时调用）。
 * 规则只认花名册：关联车间编号=是 → 车间_工号；否 → 工号。直接写 Cookie，不绕 map 兜底。
 */
export function refreshCamstarCookieForSystem(system) {
  if (!system) {
    return ''
  }
  const clientId = system.clientId || system.client_id
  const subSystemId = system.subSystemId || system.id
  const name = resolveExternalUsernameFromRoster(system)
  if (!name) {
    return ''
  }
  // 同步进查找表，后续打开 iframe 的 ensureLocalCamstarCookie(url, clientId) 也能命中
  if (clientId) {
    EXTERNAL_USERNAME_BY_CLIENT[String(clientId)] = name
  }
  if (subSystemId != null && subSystemId !== '') {
    EXTERNAL_USERNAME_BY_SUBSYSTEM[String(subSystemId)] = name
  }
  const url = system.systemUrl || system.url || ''
  if (url) {
    const parsed = parseUrl(url)
    if (parsed) {
      EXTERNAL_USERNAME_BY_ORIGIN[parsed.origin.toLowerCase()] = name
    }
  }
  sessionStorage.removeItem(COOKIEKEY_STORE)
  saveCamstarUsername(name)
  const key = toBase64Utf8(name)
  Cookies.set(CAMSTAR_COOKIE, key, { path: '/' })
  sessionStorage.setItem(COOKIEKEY_STORE, key)
  if (url) {
    seedCamstarCookieForUrlInBackground(url, clientId || subSystemId)
  }
  return key
}

/**
 * 回主系统 / 无子系统上下文：Cookie 恢复为主登录工号，清掉上一系统的车间拼接身份缓存。
 */
export function resetCamstarCookieToLoginUser() {
  sessionStorage.removeItem(COOKIEKEY_STORE)
  sessionStorage.removeItem(CAMSTAR_USERNAME_STORE)
  const username = getUsername()
  if (!username) {
    Cookies.remove(CAMSTAR_COOKIE, { path: '/' })
    return ''
  }
  const key = toBase64Utf8(username)
  Cookies.set(CAMSTAR_COOKIE, key, { path: '/' })
  sessionStorage.setItem(COOKIEKEY_STORE, key)
  return key
}

/** 对齐 4200 setCookie：在当前页 host 写入 Cookie（打开系统前调用；systemId/url 可选，按目标系统身份写） */
export function ensureLocalCamstarCookie(url, systemId) {
  const key = resolveCamstarCookieKey(url, systemId)
  if (!key) {
    return ''
  }
  Cookies.set(CAMSTAR_COOKIE, key, { path: '/' })
  sessionStorage.setItem(COOKIEKEY_STORE, key)
  return key
}

/**
 * 可选：后台尝试给其它 host 种 Cookie（需目标机已部署 camstar-cookie-bridge.html）。
 * 不阻塞业务打开；失败静默忽略。
 * Cookie 值优先按系统标识（clientId/subSystemId）取该系统的对接用户名，URL 匹配兜底；
 * 跨 host 只种目标 host，不改写本域 Cookie（本域身份由打开系统时的 ensureLocalCamstarCookie 写）。
 */
export function seedCamstarCookieForUrlInBackground(httpUrl, systemId) {
  const key = resolveCamstarCookieKey(httpUrl, systemId)
  if (!key || !httpUrl) {
    return
  }
  // 非 4200/CamstarPortal 的外链直开页不要去拉 cookie-bridge，
  // 否则隐藏 iframe 会把对方整站拉起来（打开任一页都打用户/角色/菜单等接口）
  if (!isCamstarLikeUrl(httpUrl)) {
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
