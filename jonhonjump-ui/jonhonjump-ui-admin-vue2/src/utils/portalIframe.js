import {
  resolvePortalFrameRoute,
  resolveCanonicalPortalPath,
  isPortalSubSystemHomePath,
  isGenericPortalTitle,
  resolvePortalMenuTitle,
  portalTabsMatch,
  parsePortalClientId,
  extractPortalMenuRest,
  slashIpPortRestToHttp
} from '@/utils/portalRoute'
import { ensureLocalCamstarCookie } from '@/utils/camstarCookie'

/**
 * 把路由收成可写的普通对象。vue-router 的 to / matched 常不可扩展，
 * 直接 view.title = xxx 会抛 TypeError，导致 addIframeView 中断。
 */
function toMutablePortalView(route) {
  if (!route) {
    return route
  }
  const meta = route.meta && typeof route.meta === 'object' ? { ...route.meta } : {}
  return {
    name: route.name,
    path: route.path,
    fullPath: route.fullPath || route.path,
    hash: route.hash,
    query: route.query ? { ...route.query } : {},
    params: route.params ? { ...route.params } : {},
    title: route.title,
    meta
  }
}

function portalPathAliasKey(path) {
  return String(path || '').replace(/\/index\/?$/, '').replace(/\/$/, '')
}

function isCamstarLink(link) {
  const s = String(link || '')
  return /^https?:\/\//i.test(s) && s.indexOf('/#/') < 0 && s.indexOf('#') < 0
}

/** 壳 path 还原业务 http，补上 resolve 未给出的 meta.link */
function fallbackLinkFromShell(route) {
  const clientId = parsePortalClientId(route && route.path)
  if (!clientId) {
    return ''
  }
  const rest = extractPortalMenuRest(route.path, clientId)
  if (!rest || /^menu\d+/i.test(rest)) {
    return ''
  }
  return slashIpPortRestToHttp(String(rest).replace(/:/g, '/')) || ''
}

/**
 * 门户 iframe 唯一登记入口。
 * 子页（带 ? 或更深路径）与主菜单分开登记，对齐通知公告 / 通知详情。
 */
export function syncPortalIframeView(store, route) {
  if (store.state.portal.iframeSyncSuspended) {
    return route
  }
  if (isPortalSubSystemHomePath(route.path)) {
    return route
  }
  const pathLinkMap = store.state.portal.pathLinkMap
  const resolved = resolvePortalFrameRoute(route, pathLinkMap, store.state.portal.systemList)
  const view = toMutablePortalView(resolved)
  if (!view || !view.name) {
    return view || route
  }
  if (!(view.meta && view.meta.link)) {
    const shellLink = fallbackLinkFromShell(route)
    if (shellLink) {
      view.meta = { ...(view.meta || {}), link: shellLink }
    }
  }
  const hasLink = !!(view.meta && view.meta.link)
  const isChild = !!(view.meta && view.meta.portalChild)

  const canonical = hasLink && !isChild ? resolveCanonicalPortalPath(view.path, pathLinkMap) : null
  if (canonical && canonical !== view.path) {
    view.path = canonical
  }

  const visited = store.state.tagsView.visitedViews || []
  const prevSameTab = visited.find(v => portalTabsMatch(v, view))
  const prev = prevSameTab || visited.find(v =>
    v.path === view.path || portalPathAliasKey(v.path) === portalPathAliasKey(view.path)
  )
  const prevTitle = resolvePortalMenuTitle(
    prev && prev.title,
    prev && prev.meta && prev.meta.menuTitle,
    prev && prev.meta && prev.meta.title
  )
  const prevChildTitle = resolvePortalMenuTitle(
    prevSameTab && prevSameTab.title,
    prevSameTab && prevSameTab.meta && prevSameTab.meta.menuTitle,
    prevSameTab && prevSameTab.meta && prevSameTab.meta.title
  )
  const nextTitle = resolvePortalMenuTitle(
    view.meta && view.meta.menuTitle,
    view.meta && view.meta.title,
    view.title
  )
  if (isChild) {
    const parentPath = (view.meta && view.meta.portalParentPath)
      || (prevSameTab && prevSameTab.meta && prevSameTab.meta.portalParentPath)
    if (parentPath) {
      view.meta = { ...(view.meta || {}), portalParentPath: parentPath }
    }
    const childTitle = (view.meta && view.meta.portalChildTitle)
      || prevChildTitle
      || nextTitle
    if (childTitle) {
      view.title = childTitle
      view.meta = {
        ...(view.meta || {}),
        title: childTitle,
        menuTitle: childTitle,
        portalChildTitle: childTitle
      }
    }
  } else if (!nextTitle && prevTitle) {
    view.title = prevTitle
    view.meta = { ...(view.meta || {}), title: prevTitle, menuTitle: prevTitle }
  } else if (nextTitle && isGenericPortalTitle(view.meta && view.meta.title) && prevTitle) {
    view.title = prevTitle
    view.meta = { ...(view.meta || {}), title: prevTitle, menuTitle: prevTitle }
  } else if (nextTitle) {
    view.title = nextTitle
    view.meta = { ...(view.meta || {}), title: nextTitle, menuTitle: nextTitle }
  }

  if (hasLink && isCamstarLink(view.meta && view.meta.link)) {
    ensureLocalCamstarCookie()
  }

  store.dispatch('tagsView/addView', view)

  if (view.meta && view.meta.link) {
    store.dispatch('tagsView/addIframeView', view)
  }

  store.dispatch('tagsView/updateVisitedView', view)
  if (view.meta && view.meta.title && !isGenericPortalTitle(view.meta.title)) {
    store.dispatch('settings/setTitle', view.meta.title)
  }
  return view
}
