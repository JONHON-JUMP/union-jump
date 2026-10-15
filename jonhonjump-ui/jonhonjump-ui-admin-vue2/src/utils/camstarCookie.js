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

function replaceMapContents(target, source) {
  Object.keys(target).forEach(k => {
    delete target[k]
  })
  Object.keys(source || {}).forEach(k => {
    target[k] = source[k]
  })
}

/** 本域强制重写 Cookie（先删再写，避免旧属性残留） */
function writeLocalCamstarCookie(key) {
  if (!key) {
    return ''
  }
  Cookies.remove(CAMSTAR_COOKIE, { path: '/' })
  Cookies.set(CAMSTAR_COOKIE, String(key), { path: '/' })
  sessionStorage.setItem(COOKIEKEY_STORE, String(key))
  return String(key)
}

/**
 * 注册子系统对接用户名（portal store 的 loadSystemList 成功后调用）。
 * systems 为 my-list 返回项：{ subSystemId, clientId, systemUrl, externalUsername, ... }。
 * clientId/subSystemId 为精确主键（打开/切换系统时用）；origin/host 仅作单系统上下文时的 URL 兜底。
 * 先建新表再整体替换，避免 clear→fill 空窗期把 Cookie 种成裸工号。
 */
export function registerExternalUsernames(systems) {
  const byClient = {}
  const bySubsystem = {}
  const byOrigin = {}
  const byHost = {}
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
      byClient[String(item.clientId)] = name
    }
    const mesClientId = item.mesClientId || item.mes_client_id
    const workshopId = String(item.workshopId || '').trim()
    if (mesClientId && workshopId) {
      byClient[String(mesClientId) + '_' + workshopId] = name
    }
    // 同 subSystemId 多叶子身份不同：不能互盖，置空迫使走 clientId
    if (item.subSystemId != null && item.subSystemId !== '') {
      const ssKey = String(item.subSystemId)
      const existSs = bySubsystem[ssKey]
      if (existSs === undefined) {
        bySubsystem[ssKey] = name
      } else if (existSs !== name) {
        bySubsystem[ssKey] = ''
      }
    }
    if (!item.systemUrl) {
      return
    }
    const parsed = parseUrl(item.systemUrl)
    if (!parsed) {
      return
    }
    const origin = parsed.origin.toLowerCase()
    const existOrigin = byOrigin[origin]
    // 同址多车间（4200/5500 共 :8888）：origin 歧义置空，必须靠 clientId 切换写 Cookie
    if (existOrigin === undefined) {
      byOrigin[origin] = name
    } else if (existOrigin !== name) {
      byOrigin[origin] = ''
    }
    const host = parsed.hostname.toLowerCase()
    const exist = byHost[host]
    // 同 hostname 多个系统（不同端口共享 Cookie 域）且身份不同 → hostname 兜底歧义，置空
    if (exist === undefined) {
      byHost[host] = name
    } else if (exist !== name) {
      byHost[host] = ''
    }
  })
  replaceMapContents(EXTERNAL_USERNAME_BY_CLIENT, byClient)
  replaceMapContents(EXTERNAL_USERNAME_BY_SUBSYSTEM, bySubsystem)
  replaceMapContents(EXTERNAL_USERNAME_BY_ORIGIN, byOrigin)
  replaceMapContents(EXTERNAL_USERNAME_BY_HOST, byHost)
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
  writeLocalCamstarCookie(String(key))
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
 * 3. 若已指定 systemId 仍未命中：保留当前已种身份（刚 refresh 的车间_工号 / 已有 Cookie），
 *    绝不用裸登录工号覆盖——Camstar 会拒掉并打出 JsonRequestBehavior 黄页
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
  // 已点名目标系统却未登记：保留现有身份，禁止裸工号盖掉车间_工号
  if (systemId) {
    const camstarUsername = getCamstarUsername()
    if (camstarUsername) {
      return toBase64Utf8(camstarUsername)
    }
    const saved = sessionStorage.getItem(COOKIEKEY_STORE)
    if (saved) {
      return saved
    }
    const existing = Cookies.get(CAMSTAR_COOKIE)
    return existing || ''
  }
  const camstarUsername = getCamstarUsername()
  if (camstarUsername) {
    return toBase64Utf8(camstarUsername)
  }
  const saved = sessionStorage.getItem(COOKIEKEY_STORE)
  if (saved) {
    return saved
  }
  const existing = Cookies.get(CAMSTAR_COOKIE)
  if (existing) {
    return existing
  }
  const username = getUsername()
  return username ? toBase64Utf8(username) : ''
}

/** 退出登录时清残留：同一浏览器换号后不得再带上一账号的 Camstar 会话 */
export function clearCamstarCookie() {
  sessionStorage.removeItem(COOKIEKEY_STORE)
  sessionStorage.removeItem(CAMSTAR_USERNAME_STORE)
  clearExternalUsernameMaps()
  Cookies.remove(CAMSTAR_COOKIE, { path: '/' })
}

/**
 * 按花名册字段算对接用户名（只认表字段，不猜）：
 * - 优先用 my-list 已算好的 externalUsername（后端按叶子车间拼好）
 * - 否则 usernameWithWorkshop=1 且车间编号非空 → 车间编号_工号
 * - 车间编号为空，或标识为否 → 工号
 */
export function resolveExternalUsernameFromRoster(system) {
  if (!system) {
    return ''
  }
  const fromApi = String(system.externalUsername || '').trim()
  if (fromApi) {
    return fromApi
  }
  const flag = system.usernameWithWorkshop
  const withWorkshop = flag === '1' || flag === 1 || flag === true
  const workshopId = String(system.workshopId || '').trim()
  const bare = String(system.username || system.userName || getUsername() || '').trim()
  if (withWorkshop && workshopId && bare) {
    if (bare.indexOf(workshopId + '_') === 0) {
      return bare
    }
    return workshopId + '_' + bare
  }
  return bare
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
  const url = system.systemUrl || system.url || ''
  // 同 MES 多叶子（mesClientId ≠ clientId）：只写 clientId，禁止用当前叶子盖掉 SUBSYSTEM/ORIGIN
  const mesClientId = system.mesClientId || system.mes_client_id
  const workshopId = String(system.workshopId || '').trim()
  const isLeafSplit = !!(mesClientId && clientId && String(mesClientId) !== String(clientId))
    || !!(mesClientId && workshopId && clientId && String(clientId).indexOf('_') >= 0)
  // 叶子别名：mesClientId_车间 与 portal clientId 不一致时也能命中
  if (mesClientId && workshopId) {
    EXTERNAL_USERNAME_BY_CLIENT[String(mesClientId) + '_' + workshopId] = name
  }
  if (!isLeafSplit && subSystemId != null && subSystemId !== '') {
    EXTERNAL_USERNAME_BY_SUBSYSTEM[String(subSystemId)] = name
  } else if (isLeafSplit && subSystemId != null && subSystemId !== '') {
    // 歧义：清空迫使后续解析走 clientId
    EXTERNAL_USERNAME_BY_SUBSYSTEM[String(subSystemId)] = ''
  }
  if (url) {
    const parsed = parseUrl(url)
    if (parsed) {
      if (isLeafSplit) {
        EXTERNAL_USERNAME_BY_ORIGIN[parsed.origin.toLowerCase()] = ''
        EXTERNAL_USERNAME_BY_HOST[parsed.hostname.toLowerCase()] = ''
      } else {
        EXTERNAL_USERNAME_BY_ORIGIN[parsed.origin.toLowerCase()] = name
      }
    }
  }
  // 切换叶子时全局预取身份也改成当前叶子，避免后续无 systemId 的 ensure 又写回上一车间
  saveCamstarUsername(name)
  const key = writeLocalCamstarCookie(toBase64Utf8(name))
  if (url) {
    seedCamstarCookieForUrlInBackground(url, clientId || (mesClientId && workshopId
      ? (String(mesClientId) + '_' + workshopId) : subSystemId))
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
  return writeLocalCamstarCookie(toBase64Utf8(username))
}

/** 对齐 4200 setCookie：在当前页 host 写入 Cookie（打开系统前调用；systemId/url 可选，按目标系统身份写） */
export function ensureLocalCamstarCookie(url, systemId) {
  const key = resolveCamstarCookieKey(url, systemId)
  if (!key) {
    // 解析不到正确身份时不要动现有 Cookie（避免切系统瞬间被裸工号污染）
    return ''
  }
  return writeLocalCamstarCookie(key)
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
