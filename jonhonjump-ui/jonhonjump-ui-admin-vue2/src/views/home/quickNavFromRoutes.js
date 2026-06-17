import path from 'path'
import { isExternal } from '@/utils/validate'

const ICON_COLORS = [
  '#4a90e2', '#52c41a', '#fa8c16', '#722ed1', '#13c2c2', '#eb2f96',
  '#f5222d', '#2f54eb', '#fa541c', '#a0d911', '#1890ff', '#597ef7'
]

export { ICON_COLORS }

const SKIP_TOP_PATHS = new Set([
  '/redirect', '/login', '/sso', '/social-login', '/404', '/401',
  '/user', '/dict', '/job', '/codegen'
])

function resolveRoutePath(routePath, basePath = '') {
  if (isExternal(routePath)) {
    return routePath
  }
  if (isExternal(basePath)) {
    return basePath
  }
  if (!routePath) {
    return basePath
  }
  return path.resolve(basePath, routePath)
}

function isHomeAffixRoute(route) {
  return route.meta && route.meta.affix && (route.path === 'index' || route.meta.title === '首页')
}

function isPortalHomeRoute(route) {
  return route.meta && route.meta.portalHome
}

import { resolvePortalMenuIcon } from '@/utils/portalMenuIcon'

function toQuickNavItem(route, fullPath, color, group) {
  const rawIcon = route.meta.icon || route.icon || ''
  const resolved = resolvePortalMenuIcon(rawIcon, {
    name: route.meta.title,
    path: fullPath || route.path
  })
  const menuId = route.id || route.meta.menuId
  return {
    menuId,
    name: route.meta.title,
    group: group || '',
    path: fullPath,
    color,
    external: isExternal(fullPath),
    hasIcon: resolved.hasIcon,
    svgIcon: resolved.svgIcon,
    icon: resolved.icon
  }
}

function isRouteDirectory(route) {
  return !!(route.children && route.children.length)
}

/** ParentView 常与叶子同名，此类中间层不覆盖上级目录名 */
function resolveNextGroup(route, currentGroup) {
  if (!isRouteDirectory(route)) {
    return currentGroup
  }
  const title = route.meta && route.meta.title
  if (!title) {
    return currentGroup
  }
  const visibleChildren = route.children.filter(child => !child.hidden && child.meta && child.meta.title)
  if (visibleChildren.length === 1) {
    const child = visibleChildren[0]
    if (child.meta.title === title && isRouteDirectory(child)) {
      return currentGroup
    }
  }
  return title
}

function collectMenuItems(routes, basePath = '', colorStart = 0, group = '') {
  const items = []
  if (!routes || !routes.length) {
    return { items, nextColorIndex: colorStart }
  }

  let colorIndex = colorStart
  routes.forEach(route => {
    if (route.hidden || !route.meta || !route.meta.title) {
      return
    }

    const fullPath = resolveRoutePath(route.path, basePath)
    if (isRouteDirectory(route)) {
      const nextGroup = resolveNextGroup(route, group)
      const childResult = collectMenuItems(route.children, fullPath, colorIndex, nextGroup)
      items.push(...childResult.items)
      colorIndex = childResult.nextColorIndex
      return
    }

    if (isHomeAffixRoute(route) || isPortalHomeRoute(route)) {
      return
    }

    items.push(toQuickNavItem(route, fullPath, ICON_COLORS[colorIndex % ICON_COLORS.length], group))
    colorIndex++
  })

  return { items, nextColorIndex: colorIndex }
}

function buildAllQuickNavItems(sidebarRouters) {
  const items = []
  if (!sidebarRouters || !sidebarRouters.length) {
    return items
  }

  let colorIndex = 0
  sidebarRouters.forEach(route => {
    if (route.hidden) {
      return
    }

    const topPath = route.path || ''
    if (SKIP_TOP_PATHS.has(topPath)) {
      return
    }

    if (topPath === '' && route.children && route.children.every(child => child.hidden || isHomeAffixRoute(child))) {
      return
    }

    const basePath = topPath === '' ? '/' : topPath
    const moduleGroup = route.meta && route.meta.title ? route.meta.title : ''
    const result = collectMenuItems(route.children, basePath, colorIndex, moduleGroup)
    items.push(...result.items)
    colorIndex = result.nextColorIndex
  })

  return items
}

/**
 * 根据用户已选 menuIds 构建扁平快捷导航列表（无分组）
 */
export function buildQuickNavItems(sidebarRouters, menuIds) {
  if (!menuIds || !menuIds.length) {
    return []
  }

  const orderMap = new Map(menuIds.map((id, index) => [Number(id), index]))
  return buildAllQuickNavItems(sidebarRouters)
    .filter(item => item.menuId != null && orderMap.has(Number(item.menuId)))
    .sort((a, b) => orderMap.get(Number(a.menuId)) - orderMap.get(Number(b.menuId)))
}

/**
 * 根据侧边栏菜单构建全部可快捷入口（用于门户首页切换子系统后的快捷应用）
 */
export function buildSidebarQuickApps(sidebarRouters) {
  return buildAllQuickNavItems(sidebarRouters)
}

/**
 * 构建外部系统快捷导航列表
 */
export function buildExternalNavItems(list) {
  if (!list || !list.length) {
    return []
  }
  return list.map((item, index) => ({
    id: item.id,
    subSystemId: item.subSystemId,
    clientId: item.clientId,
    name: item.clientName || item.clientId,
    path: item.ssoUrl,
    logo: item.logo,
    color: ICON_COLORS[index % ICON_COLORS.length],
    sso: true
  }))
}
