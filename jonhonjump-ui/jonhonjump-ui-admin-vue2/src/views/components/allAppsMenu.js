const DEFAULT_ICON = 'component'
const EXCLUDED_PATHS = new Set(['/404', '/index'])

function isExternalPath(path) {
  return /^(https?:|mailto:|tel:)/.test(path || '')
}

function resolveMenuPath(basePath, routePath) {
  if (!routePath) return basePath || '/'
  if (isExternalPath(routePath) || routePath.charAt(0) === '/') return routePath

  const base = basePath && basePath !== '/' ? basePath.replace(/\/+$/, '') : ''
  if (isExternalPath(base)) return `${base}/${routePath.replace(/^\/+/, '')}`
  return `${base}/${routePath}`.replace(/\/+/g, '/')
}

function isVisibleRoute(route, path) {
  return Boolean(route) &&
    !route.hidden &&
    route.path !== '*' &&
    !EXCLUDED_PATHS.has(path)
}

function routeTitle(route) {
  return route.meta && route.meta.title
}

function routeIcon(route) {
  return (route.meta && route.meta.icon) || DEFAULT_ICON
}

function menuKey(type, path, name) {
  return `${type}:${path}:${name}`
}

function isOpenableRoute(route) {
  return Boolean(route.path) && route.redirect !== 'noRedirect'
}

function createLeaf(route, path) {
  const name = routeTitle(route)
  return {
    type: 'leaf',
    key: menuKey('leaf', path, name),
    name,
    icon: routeIcon(route),
    path
  }
}

function collectLeaves(routes, basePath) {
  const leaves = []
  const routeList = routes || []

  routeList.forEach(route => {
    const path = resolveMenuPath(basePath, route.path)
    if (!isVisibleRoute(route, path)) return

    const title = routeTitle(route)
    const descendants = collectLeaves(route.children, path)

    if (descendants.length) {
      leaves.push(...descendants)
    } else if (title && isOpenableRoute(route)) {
      leaves.push(createLeaf(route, path))
    }
  })

  return leaves
}

function collectGroupChildren(routes, basePath) {
  const items = []
  const routeList = routes || []

  routeList.forEach(route => {
    const path = resolveMenuPath(basePath, route.path)
    if (!isVisibleRoute(route, path)) return

    const title = routeTitle(route)
    const descendants = collectLeaves(route.children, path)

    if (!title) {
      items.push(...collectGroupChildren(route.children, path))
    } else if (descendants.length) {
      items.push({
        type: 'folder',
        key: menuKey('folder', path, title),
        name: title,
        icon: routeIcon(route),
        children: descendants
      })
    } else if (isOpenableRoute(route)) {
      items.push(createLeaf(route, path))
    }
  })

  return items
}

function normalizeTopRoutes(routes, basePath) {
  const groups = []
  const routeList = routes || []

  routeList.forEach(route => {
    const path = resolveMenuPath(basePath, route.path)
    if (!isVisibleRoute(route, path)) return

    const title = routeTitle(route)

    if (!title) {
      groups.push(...normalizeTopRoutes(route.children, path))
      return
    }

    const children = collectGroupChildren(route.children, path)
    if (!children.length && isOpenableRoute(route)) {
      children.push(createLeaf(route, path))
    }
    if (!children.length) return

    groups.push({
      type: 'group',
      key: menuKey('group', path, title),
      name: title,
      icon: routeIcon(route),
      children
    })
  })

  return groups
}

function normalizeMenuTree(routes) {
  return normalizeTopRoutes(routes, '')
}

function searchMenus(groups, keyword) {
  const query = String(keyword || '').trim().toLowerCase()
  if (!query) return []

  const matches = []
  const matchedKeys = new Set()
  const groupList = groups || []

  function addMatch(item) {
    if (!matchedKeys.has(item.key)) {
      matchedKeys.add(item.key)
      matches.push(item)
    }
  }

  groupList.forEach(group => {
    const children = group.children || []
    if (String(group.name).toLowerCase().includes(query)) {
      children.forEach(addMatch)
      return
    }

    children.forEach(item => {
      if (String(item.name).toLowerCase().includes(query)) addMatch(item)
      if (item.type === 'folder') {
        item.children.forEach(leaf => {
          if (String(leaf.name).toLowerCase().includes(query)) addMatch(leaf)
        })
      }
    })
  })
  return matches
}

function folderPreviewStyle(count) {
  const size = Math.max(0, Number.isFinite(count) ? Math.floor(count) : 0)
  return {
    columns: Math.max(1, Math.ceil(Math.sqrt(size))),
    size
  }
}

module.exports = {
  normalizeMenuTree,
  searchMenus,
  folderPreviewStyle,
  resolveMenuPath
}
