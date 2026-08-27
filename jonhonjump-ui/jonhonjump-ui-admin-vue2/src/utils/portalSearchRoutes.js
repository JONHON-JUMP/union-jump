const CONSTANT_ROUTE_MARKERS = [
  '/redirect',
  '/login',
  '/sso',
  '/social-login',
  '/404',
  '/401'
]

function isConstantRoute(route) {
  const path = String(route && route.path || '')
  if (!path) {
    return false
  }
  return CONSTANT_ROUTE_MARKERS.some(marker => path === marker || path.startsWith(`${marker}/`))
}

function belongsToSubSystem(route, clientId) {
  if (!route || !clientId) {
    return false
  }
  if (route.meta && route.meta.clientId) {
    return route.meta.clientId === clientId
  }
  const path = String(route.path || '')
  const prefix = `/portal/${clientId}`
  return path === prefix || path.startsWith(`${prefix}/`)
}

function isMainBusinessRoute(route) {
  if (!route || route.hidden) {
    return false
  }
  if (isConstantRoute(route)) {
    return false
  }
  const path = String(route.path || '')
  if (path.startsWith('/portal/')) {
    return false
  }
  if (route.meta && route.meta.clientId) {
    return false
  }
  return true
}

function filterRoutesForSystem(routes, currentSystem) {
  const list = routes || []
  if (currentSystem === 'main') {
    return list.filter(route => isMainBusinessRoute(route) || (
      route && route.children && route.children.some(child => isMainBusinessRoute(child))
    ))
  }
  return list.filter(route => belongsToSubSystem(route, currentSystem) || (
    route && route.children && route.children.some(child => belongsToSubSystem(child, currentSystem))
  ))
}

/**
 * 门户搜索 / 全部应用：仅返回当前打开系统的菜单路由树。
 * 子系统菜单尚未预热完成时返回空，避免先闪主系统菜单再切到子系统。
 */
function resolveCurrentSystemSearchRoutes(state) {
  const currentSystem = state.portal.currentSystem || 'main'
  if (currentSystem === 'main') {
    const cachedMain = state.portal.mainSidebarRouters
    const defaultRoutes = state.permission.defaultRoutes
    const sidebar = state.permission.sidebarRouters
    const source = (cachedMain && cachedMain.length)
      ? cachedMain
      : (defaultRoutes && defaultRoutes.length)
        ? defaultRoutes
        : (sidebar || [])
    return filterRoutesForSystem(source, 'main')
  }

  const cachedSub = state.portal.subSystemSidebarCache[currentSystem]
  if (cachedSub && cachedSub.length) {
    return filterRoutesForSystem(cachedSub, currentSystem)
  }

  return []
}

function resolveCurrentSystemLabel(state) {
  if (state.portal.currentSystem === 'main') {
    return 'JUMP 主系统'
  }
  const current = state.portal.currentSystem
  const matched = (state.portal.systemList || []).find(item => item.clientId === current)
  if (matched) {
    return matched.systemName || matched.clientId
  }
  return current
}

module.exports = {
  resolveCurrentSystemSearchRoutes,
  resolveCurrentSystemLabel
}
