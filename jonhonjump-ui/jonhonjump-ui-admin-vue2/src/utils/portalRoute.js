/** portal 路径中的 client_id 段（与 sub_system.client_id 一致） */
export const PORTAL_CLIENT_ID_RE = '[a-zA-Z][a-zA-Z0-9_-]*'

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

/** 静态 PortalFrame 路由匹配时，用 pathLinkMap 还原菜单标题与 iframe 链接 */
export function resolvePortalFrameRoute(route, pathLinkMap) {
  if (!route || (route.name !== 'PortalFrame' && route.name !== 'PortalFrameLegacy')) {
    return route
  }
  const entry = pathLinkMap && pathLinkMap[route.path]
  if (!entry) {
    return route
  }
  const clientId = parsePortalClientId(route.path)
  return {
    ...route,
    meta: {
      ...(route.meta || {}),
      title: entry.title || route.meta.title,
      link: normalizeSubsystemIframeLink(entry.link, clientId),
      icon: entry.icon || route.meta.icon
    }
  }
}

/**
 * 子系统 iframe 须与门户同源（如 /scada/#/...），否则 SSO Cookie 无法带入 iframe，页面会空数据。
 */
export function normalizeSubsystemIframeLink(link, clientId) {
  if (!link || !clientId) {
    return link
  }
  const hashIndex = link.indexOf('/#/')
  if (hashIndex < 0) {
    return link
  }
  const hashPart = link.substring(hashIndex)
  try {
    const parsed = new URL(link, window.location.origin)
    if (parsed.origin !== window.location.origin) {
      return `${window.location.origin}/${clientId}${hashPart}`
    }
  } catch (e) {
    return link
  }
  return link
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
