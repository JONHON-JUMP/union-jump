/** portal 路径中的 client_id 段（与 sub_system.client_id 一致） */
export const PORTAL_CLIENT_ID_RE = '[a-zA-Z][a-zA-Z0-9_-]*'

/** 静态 PortalFrame / 未命中菜单名时的占位标题，不能当真实菜单名用 */
const GENERIC_PORTAL_TITLES = {
  '业务页': true,
  '外部系统': true,
  'no-name': true
}

export function isGenericPortalTitle(title) {
  const t = String(title || '').trim()
  if (!t) {
    return true
  }
  if (GENERIC_PORTAL_TITLES[t]) {
    return true
  }
  // filterSubSystemRouter 改写后的内部 name，不能展示
  return /^SubMenu\d+_/i.test(t) || /Sub\d+_\d+$/i.test(t)
}

/** 从 pathLinkMap entry / route.meta 取真实菜单名 */
export function resolvePortalMenuTitle(...candidates) {
  for (let i = 0; i < candidates.length; i++) {
    const t = candidates[i]
    if (t != null && !isGenericPortalTitle(t)) {
      return String(t).trim()
    }
  }
  return ''
}

export const PORTAL_PATH_PREFIX_RE = new RegExp(`^/portal/${PORTAL_CLIENT_ID_RE}(?:/|$)`)

export function parsePortalClientId(path) {
  const match = (path || '').match(new RegExp(`^/portal/(${PORTAL_CLIENT_ID_RE})(?:/|$)`))
  return match ? match[1] : null
}

/** 兼容旧链接 /portal/1/... */
export function parseLegacyPortalSubSystemId(path) {
  const match = (path || '').match(/^\/portal\/(\d+)(?:\/|$)/)
  return match ? Number(match[1]) : null
}

export function buildPortalPath(clientId, menuPath) {
  const base = `/portal/${clientId}`
  if (!menuPath) {
    return base
  }
  const segment = String(menuPath).replace(/^\/+/, '')
  return segment ? `${base}/${segment}`.replace(/\/+/g, '/') : base
}

export function isPortalSystemPath(path, clientId) {
  return new RegExp(`^/portal/${clientId}(?:/|$)`).test(path || '')
}

const SUB_SYSTEM_PERSONAL_PREFIXES = ['/user/']

/** 子系统模式下允许访问的路径（门户首页、当前子系统 portal、个人中心等） */
export function isSubSystemAllowedPath(path, clientId) {
  const normalized = path || ''
  if (normalized === '/index' || normalized === '/') {
    return true
  }
  if (clientId && isPortalSystemPath(normalized, clientId)) {
    return true
  }
  return SUB_SYSTEM_PERSONAL_PREFIXES.some(prefix => normalized === prefix || normalized.startsWith(prefix))
}

/** 是否为主系统业务路由（非门户、非个人中心） */
export function isMainBusinessPath(path) {
  const normalized = path || ''
  if (!normalized || normalized === '/index' || normalized === '/') {
    return false
  }
  if (PORTAL_PATH_PREFIX_RE.test(normalized)) {
    return false
  }
  if (SUB_SYSTEM_PERSONAL_PREFIXES.some(prefix => normalized === prefix || normalized.startsWith(prefix))) {
    return false
  }
  const publicPaths = ['/login', '/sso', '/social-login', '/404', '/401', '/redirect']
  if (publicPaths.some(prefix => normalized === prefix || normalized.startsWith(`${prefix}/`))) {
    return false
  }
  return true
}

/** 子系统 iframe 首页路由（应统一复用门户 /index，不在 dock 展示） */
export const PORTAL_SUBSYSTEM_HOME_RE = /^\/portal\/[a-zA-Z][a-zA-Z0-9_-]*\/home(?:\/|$)/

export function isPortalSubSystemHomePath(path) {
  return PORTAL_SUBSYSTEM_HOME_RE.test(path || '')
}

export function findFirstPortalLeafPath(routes, parentPath = '') {
  for (const route of routes) {
    const currentPath = buildRoutePath(route.path, parentPath)
    if (route.meta && route.meta.link) {
      return currentPath
    }
    if (route.children && route.children.length) {
      const childPath = findFirstPortalLeafPath(route.children, currentPath)
      if (childPath) {
        return childPath
      }
    }
  }
  return null
}

export function findPortalRoutePath(routes, matcher, parentPath = '') {
  for (const route of routes) {
    const currentPath = buildRoutePath(route.path, parentPath)
    if (matcher(route, currentPath)) {
      return currentPath
    }
    if (route.children && route.children.length) {
      const childPath = findPortalRoutePath(route.children, matcher, currentPath)
      if (childPath) {
        return childPath
      }
    }
  }
  return null
}

export function resolvePortalEntryPath(routes, { homeMenuId, homePageUrl, systemUrl, subSystemId, clientId }) {
  // 所有子系统首页统一复用门户 /index，仅切换菜单与快捷导航
  return '/index'
}

export function buildPortalHomeMenu(subSystemId, { title, link }) {
  return {
    id: -subSystemId,
    parentId: 0,
    name: title || '首页',
    path: 'home',
    icon: 'dashboard',
    visible: true,
    keepAlive: false,
    alwaysShow: false,
    component: 'system/subSystem/portal/Empty',
    componentName: `SubSystemHome${subSystemId}`,
    link,
    portalHome: true
  }
}

function buildRoutePath(path, parentPath) {
  if (!path) {
    return parentPath
  }
  if (path.startsWith('/')) {
    return path
  }
  return `${parentPath}/${path}`.replace(/\/+/g, '/')
}

/** 可写副本，避免把 vue-router 冻结对象直接丢给 tagsView 再赋值 */
function plainPortalView(route, patch = {}) {
  const baseMeta = route && route.meta && typeof route.meta === 'object' ? { ...route.meta } : {}
  const patchMeta = patch.meta && typeof patch.meta === 'object' ? patch.meta : null
  return {
    name: route.name,
    path: route.path,
    fullPath: route.fullPath || route.path,
    hash: route.hash,
    query: route.query ? { ...route.query } : {},
    params: route.params ? { ...route.params } : {},
    title: patch.title != null ? patch.title : route.title,
    meta: patchMeta ? { ...baseMeta, ...patchMeta } : baseMeta
  }
}

/** 静态 PortalFrame 路由匹配时，用 pathLinkMap 还原菜单标题与 iframe 链接 */
export function resolvePortalFrameRoute(route, pathLinkMap, systemList) {
  if (!route || (route.name !== 'PortalFrame' && route.name !== 'PortalFrameLegacy')) {
    return route
  }
  const clientId = parsePortalClientId(route.path)
  const existingTitle = resolvePortalMenuTitle(
    route.meta && route.meta.menuTitle,
    route.meta && route.meta.title,
    route.title
  )
  const entry = lookupPathLinkEntry(route.path, pathLinkMap)
  if (entry) {
    const title = resolvePortalMenuTitle(entry.title, entry.menuTitle, existingTitle) || '业务页'
    return plainPortalView(route, {
      title,
      meta: {
        title,
        menuTitle: title,
        link: normalizeSubsystemIframeLink(entry.link, clientId),
        icon: entry.icon || (route.meta && route.meta.icon),
        // Camstar：主系统直开能力，与子系统 OAuth 无关
        portalKind: entry.kind || (route.meta && route.meta.portalKind)
      }
    })
  }
  let rest = extractPortalMenuRest(route.path, clientId)
  const systemUrl = resolveSystemUrl(systemList, clientId)
  if (!rest || !systemUrl) {
    if (existingTitle) {
      return plainPortalView(route, {
        title: existingTitle,
        meta: { title: existingTitle, menuTitle: existingTitle }
      })
    }
    return plainPortalView(route)
  }
  if (/^m\d+$/i.test(rest) || /(^|\/)m\d+(\/|$)/i.test(rest)) {
    return plainPortalView(route)
  }

  // 错误壳 path：…/129/8088/mes4200 — 是门户自己，不是业务机
  if (isEncodedSystemUrlRest(rest, systemUrl)) {
    return plainPortalView(route)
  }

  // Camstar：IP:port 编码 → http 直链；若依：systemUrl/#/
  const directHttp = slashIpPortRestToHttp(rest)
  // 直链拼出后，再按 link 反查菜单名（壳 path 带/不带 15 都能命中）
  const byLink = directHttp ? lookupPathLinkEntry(route.path, pathLinkMap, directHttp) : null
  let link
  if (directHttp
    && !isEncodedSystemUrlRest(encodePureHttpToShell(directHttp), systemUrl)
    && encodePureHttpToShell(directHttp) !== encodePureHttpToShell(systemUrl)) {
    link = directHttp
  } else {
    link = `${systemUrl}/#/${String(rest).replace(/:/g, '/')}`
  }
  const title = resolvePortalMenuTitle(
    byLink && byLink.title,
    byLink && byLink.menuTitle,
    existingTitle
  ) || '业务页'
  const kind = (byLink && byLink.kind)
    || (directHttp ? 'camstar' : 'ruoyi')
  return plainPortalView(route, {
    title,
    meta: {
      title,
      menuTitle: title,
      link: normalizeSubsystemIframeLink((byLink && byLink.link) || link, clientId),
      icon: (byLink && byLink.icon) || (route.meta && route.meta.icon),
      portalKind: kind
    }
  })
}

/**
 * pathLinkMap 命中：精确路径、/index 别名、去掉一层数字目录后再精确比。
 * 禁止 endsWith 模糊匹配（会把若依页误挂成 Camstar 直链 → 跳过 SSO → 全白屏）。
 */
export function lookupPathLinkEntry(path, pathLinkMap, linkHint) {
  if (!path || !pathLinkMap) {
    return null
  }
  if (pathLinkMap[path]) {
    return pathLinkMap[path]
  }
  const withIndex = path.endsWith('/index') ? path : `${path.replace(/\/$/, '')}/index`
  const withoutIndex = path.replace(/\/index$/, '')
  if (pathLinkMap[withIndex]) {
    return pathLinkMap[withIndex]
  }
  if (withoutIndex !== path && pathLinkMap[withoutIndex]) {
    return pathLinkMap[withoutIndex]
  }

  const clientId = parsePortalClientId(path)
  const rest = extractPortalMenuRest(path, clientId)
  if (rest) {
    // 仅：去一层数字前缀后再做「整段相等」（15/192/... ↔ 192/...）
    const restAlt = rest.replace(/^\d+\//, '')
    const keys = Object.keys(pathLinkMap)
    for (let i = 0; i < keys.length; i++) {
      const key = keys[i]
      const entry = pathLinkMap[key]
      if (!entry) {
        continue
      }
      const kr = extractPortalMenuRest(key, parsePortalClientId(key) || clientId)
      if (!kr) {
        continue
      }
      const krAlt = kr.replace(/^\d+\//, '')
      if (kr === rest || krAlt === rest || kr === restAlt || krAlt === restAlt) {
        return entry
      }
    }
  }
  return linkHint ? findPathLinkByHttp(pathLinkMap, linkHint) : null
}

function findPathLinkByHttp(pathLinkMap, httpUrl) {
  const want = normalizeHttpForMatch(httpUrl)
  if (!want) {
    return null
  }
  const wantPath = pathnameOnly(want)
  // 路径太短不做模糊，避免误伤若依
  if (!wantPath || wantPath.length < 8) {
    return null
  }
  const keys = Object.keys(pathLinkMap || {})
  for (let i = 0; i < keys.length; i++) {
    const entry = pathLinkMap[keys[i]]
    if (!entry || !entry.link) {
      continue
    }
    const got = normalizeHttpForMatch(entry.link)
    if (!got) {
      continue
    }
    if (got === want) {
      return entry
    }
    const gotPath = pathnameOnly(got)
    if (gotPath && gotPath === wantPath) {
      return entry
    }
  }
  return null
}

function normalizeHttpForMatch(url) {
  const raw = unwrapDirectHttpIframeLink(url) || url
  if (!raw || !/^https?:\/\//i.test(raw)) {
    return ''
  }
  return String(raw).replace(/\/+$/, '').toLowerCase()
}

function pathnameOnly(httpUrl) {
  try {
    return new URL(httpUrl).pathname.replace(/\/+$/, '').toLowerCase()
  } catch (e) {
    return ''
  }
}

/**
 * 门户壳 rest → 业务 http：
 * - 新：192.168.240.12794200[/path]（点分 IP + 9 + 端口，避免 Vue Router 把 :4200 当参数）
 * - 旧：15/192/168/240/126/43061/... 或 192/168/.../127/4200/...
 */
export function slashIpPortRestToHttp(rest) {
  const raw0 = String(rest || '').replace(/^\/+/, '')
  // 先认点分 IP9port（勿先把点改成 /）
  {
    const re9 = /(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})9(\d{2,5})(?=\/|$)/g
    let m
    let best9 = null
    while ((m = re9.exec(raw0)) !== null) {
      const parts = m[1].split('.').map(Number)
      const port = Number(m[2])
      if (parts.some(x => x > 255) || port <= 255) {
        continue
      }
      const after = raw0.substring(m.index + m[0].length).replace(/^\/+/, '')
      best9 = { host: m[1], port, path: after }
    }
    if (best9) {
      return best9.path
        ? `http://${best9.host}:${best9.port}/${best9.path}`
        : `http://${best9.host}:${best9.port}/`
    }
  }
  let raw = raw0.replace(/:/g, '/')
  raw = raw.replace(/(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})/g, '$1/$2/$3/$4')
  const re = /(\d{1,3})\/(\d{1,3})\/(\d{1,3})\/(\d{1,3})\/(\d{2,5})(?=\/|$)/g
  let m
  let best = null
  while ((m = re.exec(raw)) !== null) {
    const a = Number(m[1])
    const b = Number(m[2])
    const c = Number(m[3])
    const d = Number(m[4])
    const port = Number(m[5])
    if (a > 255 || b > 255 || c > 255 || d > 255 || port <= 255) {
      re.lastIndex = m.index + String(m[1]).length + 1
      continue
    }
    const after = raw.substring(m.index + m[0].length).replace(/^\/+/, '')
    best = { host: `${a}.${b}.${c}.${d}`, port, path: after }
  }
  if (!best) {
    return ''
  }
  return best.path
    ? `http://${best.host}:${best.port}/${best.path}`
    : `http://${best.host}:${best.port}/`
}

/**
 * 若 link 被误套成 {mes}/#/192/168/.../4200/...，还原成原始 http 直链。
 * 纯 http（无 /#/）原样返回；若依 hash（非 IP:port）不动。
 */
export function unwrapDirectHttpIframeLink(link) {
  if (!link) {
    return link
  }
  const s = String(link)
  if (/^https?:\/\//i.test(s) && s.indexOf('/#/') < 0 && s.indexOf('#') < 0) {
    return s
  }
  const hashIdx = s.indexOf('/#/')
  const hashIdx2 = hashIdx >= 0 ? hashIdx : s.indexOf('#')
  if (hashIdx2 < 0) {
    return s
  }
  const hashPart = s.substring(hashIdx2).replace(/^\/?#\/?/, '').replace(/:/g, '/')
  const asHttp = slashIpPortRestToHttp(hashPart)
  return asHttp || s
}

function encodePureHttpToShell(httpUrl) {
  let s = String(httpUrl || '').trim()
  const hashPos = s.indexOf('#')
  if (hashPos >= 0) {
    s = s.substring(0, hashPos)
  }
  // 点分 IP + 显式端口 → 192/168/240/127/4200[/path]（纯斜杠，避免壳 path 里出现 :4200 触发 Vue Router 动态参数）
  try {
    const withProto = /^https?:\/\//i.test(s) ? s : (/^\d{1,3}(\.\d{1,3}){3}(:\d+)?(\/|$)/.test(s) ? `http://${s}` : '')
    if (withProto) {
      const u = new URL(withProto)
      if (/^\d{1,3}(\.\d{1,3}){3}$/.test(u.hostname) && u.port) {
        const hostSlash = u.hostname.replace(/\./g, '/')
        const path = (u.pathname || '/').replace(/^\/+/, '').replace(/\/$/, '')
        const q = u.search || ''
        return `${hostSlash}/${u.port}${path ? `/${path}` : ''}${q}`
      }
    }
  } catch (e) { /* fallback */ }
  return s
    .replace(/^https?:\/\//i, '')
    .replace(/^www\./i, '')
    .replace(/\./g, '/')
    .replace(/:/g, '/')
    .replace(/\/+/g, '/')
    .replace(/\/$/, '')
}

/** 门户壳 path：业务直链 http → 192/168/240/127/4200/...（纯斜杠；旧 点+9 编码仍可解码） */
export function encodeHttpToMesPath(httpUrl) {
  const raw = String(httpUrl || '')
  const unwrapped = unwrapDirectHttpIframeLink(raw)
  if (unwrapped && /^https?:\/\//i.test(unwrapped) && unwrapped.indexOf('#') < 0) {
    return encodePureHttpToShell(unwrapped)
  }
  return encodePureHttpToShell(raw)
}

/** rest 是否其实是 systemUrl 自己被点改斜杠（错误书签） */
export function isEncodedSystemUrlRest(rest, systemUrl) {
  if (!rest || !systemUrl) {
    return false
  }
  const enc = encodePureHttpToShell(String(systemUrl).replace(/\/#\/.*$/, '').replace(/#.*$/, ''))
  if (!enc) {
    return false
  }
  const r = String(rest).replace(/\/index$/, '').replace(/\/$/, '')
  return r === enc || r.startsWith(enc + '/')
}

function extractPortalMenuRest(path, clientId) {
  if (!path || !clientId) {
    return ''
  }
  const prefix = `/portal/${clientId}/`
  if (!path.startsWith(prefix)) {
    return ''
  }
  return path.substring(prefix.length).replace(/^\/+/, '').replace(/\/index$/, '')
}

function resolveSystemUrl(systemList, clientId) {
  if (!clientId || !systemList || !systemList.length) {
    return ''
  }
  const hit = systemList.find(item => item && item.clientId === clientId)
  const url = (hit && (hit.systemUrl || hit.homePageUrl)) || ''
  return String(url || '').replace(/\/+$/, '')
}

function joinUrl(base, rest) {
  const b = String(base || '').replace(/\/+$/, '')
  let r = String(rest || '').replace(/^\/+/, '')
  if (!b) {
    return r
  }
  if (!r) {
    return b
  }
  if (/^https?:\/\//i.test(r)) {
    r = r
      .replace(/^https?:\/\//i, '')
      .replace(/^www\./i, '')
      .replace(/\./g, '/')
      .replace(/:/g, '/')
  } else {
    r = r.replace(/:/g, '/')
  }
  return `${b}/#/${r}`
}

/**
 * 非法门户 path / 旧 m{id} 短码 → 改成与 MES #/ 后一致的路径。
 */
export function resolvePortalShortPathAlias(path, pathLinkMap) {
  if (!path || !path.startsWith('/portal/')) {
    return null
  }
  // 旧包：嵌了 http://
  const embedded = resolvePortalEmbeddedHttpAlias(path, pathLinkMap)
  if (embedded) {
    return embedded
  }
  // 旧包：/portal/x/15/m221256 → /portal/x/15/192/168/...
  const legacy = resolvePortalLegacyMenuIdAlias(path, pathLinkMap)
  if (legacy) {
    return legacy
  }
  // 非法冒号：点分 IP:端口 → 纯斜杠（IP 的点也换成 /），与 encodePureHttpToShell 一致
  if (/(\d{1,3}(?:\.\d{1,3}){3}):(\d{2,5})/.test(path)) {
    return path.replace(/(\d{1,3}(?:\.\d{1,3}){3}):(\d{2,5})/g, (m, ip, port) =>
      ip.replace(/\./g, '/') + '/' + port)
  }
  if (/:\d+/.test(path)) {
    return path.replace(/:/g, '/')
  }
  return null
}

/** /portal/client/.../m123 → 正式 MES 对齐 path */
export function resolvePortalLegacyMenuIdAlias(path, pathLinkMap) {
  const m = String(path || '').match(/^\/portal\/([a-zA-Z][a-zA-Z0-9_-]*)\/(?:.*\/)?(m\d+)(?:\/index)?$/i)
  if (!m || !pathLinkMap) {
    return null
  }
  const clientId = m[1]
  const shortKey = `/portal/${clientId}/${m[2]}`
  const entry = pathLinkMap[shortKey] || pathLinkMap[path]
  if (entry && entry.canonicalPath && entry.canonicalPath !== path) {
    return entry.canonicalPath
  }
  return null
}

/** /portal/client/http://host/... → 改成与 MES 一致的 /portal/client/15/192/... 路径 */
export function resolvePortalEmbeddedHttpAlias(path, pathLinkMap) {
  const match = String(path || '').match(/^\/portal\/([a-zA-Z][a-zA-Z0-9_-]*)\/(https?:\/\/.+)$/i)
  if (!match || !pathLinkMap) {
    return null
  }
  const clientId = match[1]
  const camstarHttp = match[2]
  const inner = encodeHttpToMesPath(camstarHttp)
  const prefix = `/portal/${clientId}/`
  const preferred = `${prefix}${inner}`
  if (pathLinkMap[preferred]) {
    return preferred
  }
  const keys = Object.keys(pathLinkMap)
  let fallback = null
  for (let i = 0; i < keys.length; i++) {
    const key = keys[i]
    if (!key.startsWith(prefix) || /https?:\/\//i.test(key) || /\/m\d+(\/|$)/.test(key)) {
      continue
    }
    const item = pathLinkMap[key]
    const link = item && item.link
    if (!link) {
      continue
    }
    if (link === camstarHttp || link.indexOf(inner) >= 0 || key.indexOf(inner) >= 0) {
      if (key.indexOf(inner) >= 0) {
        return key
      }
      fallback = fallback || key
    }
  }
  return fallback
}

function sanitizeMesCamstarHash(hashPart) {
  // 只把端口冒号改成 /；保留 15/ 等目录前缀（4200 原生挂在工艺管理下时需要）
  return String(hashPart || '').replace(/:/g, '/')
}

/**
 * 纯 http(s) 直链原样返回（Camstar 直开）；误套 mes/#/IP:port 时解套。
 * 带 /#/ 的若依/MES link 保持清洗端口冒号。
 */
export function normalizeSubsystemIframeLink(link, clientId) {
  if (!link) {
    return link
  }
  const unwrapped = unwrapDirectHttpIframeLink(link)
  if (/^https?:\/\//i.test(unwrapped) && unwrapped.indexOf('/#/') < 0 && unwrapped.indexOf('#') < 0) {
    return unwrapped
  }
  if (!clientId) {
    return link
  }
  const hashIndex = link.indexOf('/#/')
  if (hashIndex < 0) {
    return link
  }
  const base = link.substring(0, hashIndex)
  let hashPart = link.substring(hashIndex)
  if (/\/\d+\/\d+\/\d+\/\d+/.test(hashPart) || /:\d+/.test(hashPart)) {
    hashPart = sanitizeMesCamstarHash(hashPart)
  }
  try {
    const parsed = new URL(link, window.location.origin)
    if (parsed.origin !== window.location.origin) {
      return base + hashPart
    }
  } catch (e) {
    return base + hashPart
  }
  return base + hashPart
}

/**
 * pathLinkMap 中菜单 canonical 路径以 /index 结尾，无 /index 的为别名。
 * 访问别名时重定向到 canonical，保证 PortalFrame 与 iframe 链接一致。
 */
export function shouldNormalizePortalPath(path, pathLinkMap) {
  if (!path || !pathLinkMap || !pathLinkMap[path]) {
    return false
  }
  if (path.endsWith('/index')) {
    return false
  }
  const canonical = `${path.replace(/\/$/, '')}/index`
  return !!pathLinkMap[canonical]
}

export function resolveCanonicalPortalPath(path, pathLinkMap) {
  if (!path) {
    return path
  }
  if (path.endsWith('/index') && pathLinkMap && pathLinkMap[path]) {
    return path
  }
  const canonical = `${path.replace(/\/$/, '')}/index`
  if (pathLinkMap && pathLinkMap[canonical]) {
    return canonical
  }
  return path
}
