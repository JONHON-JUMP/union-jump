import {
  resolvePortalFrameRoute,
  resolveCanonicalPortalPath,
  isPortalSubSystemHomePath,
  isGenericPortalTitle,
  resolvePortalMenuTitle
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

function findFrameByPathAlias(list, path) {
  if (!path || !list || !list.length) {
    return null
  }
  const exact = list.find(v => v && v.path === path)
  if (exact) {
    return exact
  }
  const key = portalPathAliasKey(path)
  return list.find(v => v && portalPathAliasKey(v.path) === key) || null
}

function isCamstarLink(link) {
  const s = String(link || '')
  return /^https?:\/\//i.test(s) && s.indexOf('/#/') < 0 && s.indexOf('#') < 0
}

/**
 * 门户 iframe 唯一登记入口。
 * 不变量：页签用当前/canonical path；iframe :key 优先保温帧原 path；Camstar link 冻结。
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
  // 即使 link 暂时为空（pathLinkMap 还没加载好），也先注册到 visitedViews，
  // 这样底部 Dock 能显示页签；link 等 pathLinkMap 更新后由 AppMain watch 补上
  const hasLink = !!(view.meta && view.meta.link)

  // 打开时规范到 pathLinkMap canonical，减少 /index 分叉
  const canonical = hasLink ? resolveCanonicalPortalPath(view.path, pathLinkMap) : null
  if (canonical && canonical !== view.path) {
    view.path = canonical
    view.fullPath = canonical
  }

  const prev = (store.state.tagsView.visitedViews || []).find(v =>
    v.path === view.path || portalPathAliasKey(v.path) === portalPathAliasKey(view.path)
  )
  const prevTitle = resolvePortalMenuTitle(
    prev && prev.title,
    prev && prev.meta && prev.meta.menuTitle,
    prev && prev.meta && prev.meta.title
  )
  const nextTitle = resolvePortalMenuTitle(
    view.meta && view.meta.menuTitle,
    view.meta && view.meta.title,
    view.title
  )
  if (!nextTitle && prevTitle) {
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
    const existing = findFrameByPathAlias(
      [].concat(store.state.tagsView.iframeViews || [], store.state.tagsView.warmIframeViews || []),
      view.path
    )
    // iframe 复用必须用原 path 作 :key；页签仍跟 canonical/当前 path
    const iframeView = existing
      ? Object.assign({}, view, {
        path: existing.path,
        meta: {
          ...(view.meta || {}),
          link: isCamstarLink(existing.meta && existing.meta.link)
            ? existing.meta.link
            : view.meta.link
        }
      })
      : view
    store.dispatch('tagsView/addIframeView', iframeView)
  }

  store.dispatch('tagsView/updateVisitedView', view)
  if (view.meta && view.meta.title && !isGenericPortalTitle(view.meta.title)) {
    store.dispatch('settings/setTitle', view.meta.title)
  }
  return view
}
