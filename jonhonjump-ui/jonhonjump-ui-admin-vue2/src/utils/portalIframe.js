import { resolvePortalFrameRoute, isPortalSubSystemHomePath } from '@/utils/portalRoute'

/**
 * 与旧版 TagsView.addTags / permission.afterEach 一致：在 pathLinkMap 就绪后注册门户 iframe。
 */
export function syncPortalIframeView(store, route) {
  if (store.state.portal.iframeSyncSuspended) {
    return route
  }
  if (isPortalSubSystemHomePath(route.path)) {
    return route
  }
  const view = resolvePortalFrameRoute(route, store.state.portal.pathLinkMap)
  if (!view.name) {
    return view
  }
  if (isUnresolvedPortalFrame(view, route.path, store.state.portal.pathLinkMap)) {
    return view
  }
  store.dispatch('tagsView/addView', view)
  if (view.meta && view.meta.link) {
    store.dispatch('tagsView/addIframeView', view)
  }
  store.dispatch('tagsView/updateVisitedView', view)
  if (view.meta && view.meta.title) {
    store.dispatch('settings/setTitle', view.meta.title)
  }
  return view
}

function isUnresolvedPortalFrame(view, path, pathLinkMap) {
  if (view.name !== 'PortalFrame' && view.name !== 'PortalFrameLegacy') {
    return false
  }
  return !(pathLinkMap && pathLinkMap[path])
}
