import { resolveMenuIconFields } from '@/utils/menuIcon'
import { resolveMenuColors } from '@/utils/menuIconStyle'

function isExternal(path) {
  return typeof path === 'string' && /^(https?:|mailto:|tel:)/.test(path)
}

function resolveRoutePath(basePath, routePath) {
  if (!routePath) return basePath || '/'
  if (isExternal(routePath) || routePath.charAt(0) === '/') return routePath
  return `${basePath}/${routePath}`.replace(/\/+/g, '/')
}

function toQuickNavItem(route, fullPath, group) {
  const title = route.meta && route.meta.title
  const iconFields = resolveMenuIconFields((route.meta && route.meta.icon) || '', {
    title,
    path: fullPath
  })
  const { color, shape } = resolveMenuColors(route.meta || {})
  return {
    menuId: route.meta && route.meta.menuId,
    manualUrl: route.meta && route.meta.manualUrl,
    name: title,
    subtitle: '',
    path: fullPath,
    svgIcon: iconFields.svgIcon,
    icon: iconFields.icon,
    color,
    shape,
    keywords: `${title} ${group}`,
    external: isExternal(fullPath)
  }
}

function collectMenuItems(routes, basePath = '', group = '') {
  const items = []
  if (!Array.isArray(routes)) return items
  routes.forEach(route => {
    if (!route || route.hidden || route.path === '*' || route.path === '/404') return
    const title = route.meta && route.meta.title
    const fullPath = resolveRoutePath(basePath, route.path)
    const hasChildren = route.children && route.children.length > 0
    const nextGroup = title || group
    if (title && fullPath !== '/index' && route.redirect !== 'noRedirect' && !hasChildren) {
      items.push(toQuickNavItem(route, fullPath, group))
    }
    if (hasChildren) {
      items.push(...collectMenuItems(route.children, fullPath, nextGroup))
    }
  })
  return items
}

export function buildQuickNavItems(routes, menuIds) {
  const ids = Array.isArray(menuIds) ? menuIds : []
  if (!ids.length) return []
  const idSet = new Set(ids.map(id => Number(id)))
  const all = collectMenuItems(routes)
  const byId = new Map(all.filter(item => item.menuId != null).map(item => [Number(item.menuId), item]))
  return ids.map(id => byId.get(Number(id))).filter(Boolean)
}

export function buildSidebarQuickApps(routes) {
  return collectMenuItems(routes).slice(0, 12)
}

export function buildExternalNavItems(items) {
  if (!Array.isArray(items)) return []
  return items.map((item, index) => {
    const { color, shape } = resolveMenuColors(item)
    return {
      menuId: `external-${item.id != null ? item.id : index}`,
      name: item.name || item.systemName || '外部系统',
      subtitle: item.subtitle || '',
      path: item.url || item.path || '',
      svgIcon: null,
      icon: item.icon || 'link',
      color,
      shape,
      logo: item.logo,
      keywords: item.name || '',
      external: true
    }
  })
}
