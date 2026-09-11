import {
  parsePortalClientId,
  extractPortalMenuRest,
  slashIpPortRestToHttp,
  httpUrlToPortalLocation,
  isPortalPathDescendant,
  portalPathAliasKey,
  portalTabsMatch,
  setPendingPortalIframeTitle,
  setPendingPortalParent,
  resolveCoveringLeaf
} from '@/utils/portalRoute'

export const JUMP_PORTAL_LOCATION = 'JUMP_PORTAL_LOCATION'

function queryEqual(a, b) {
  const aa = a || {}
  const bb = b || {}
  const keys = Object.keys(aa)
  if (keys.length !== Object.keys(bb).length) {
    return false
  }
  for (let i = 0; i < keys.length; i++) {
    const k = keys[i]
    if (String(aa[k] == null ? '' : aa[k]) !== String(bb[k] == null ? '' : bb[k])) {
      return false
    }
  }
  return true
}

function sameJumpLocation(route, loc) {
  if (!route || !loc) {
    return false
  }
  if (portalPathAliasKey(route.path) !== portalPathAliasKey(loc.path)) {
    return false
  }
  if (!queryEqual(route.query, loc.query)) {
    return false
  }
  const rh = String(route.hash || '')
  const lh = String(loc.hash || '')
  return rh === lh || rh === lh.replace(/^#/, '') || ('#' + rh.replace(/^#/, '')) === lh
}

function isPrivateOrLocalHost(hostname) {
  const h = String(hostname || '')
  return h === 'localhost'
    || h === '127.0.0.1'
    || /^10\./.test(h)
    || /^192\.168\./.test(h)
    || /^172\.(1[6-9]|2\d|3[0-1])\./.test(h)
}

function sameBusinessOrigin(href, route) {
  const clientId = parsePortalClientId(route && route.path)
  if (!clientId) {
    return false
  }
  try {
    const hrefHost = new URL(href).hostname
    const rest = extractPortalMenuRest(route.path, clientId)
    const currentHttp = slashIpPortRestToHttp(rest)
    if (currentHttp) {
      if (new URL(href).origin === new URL(currentHttp).origin) {
        return true
      }
    }
    // 菜单写 129、实际跳到 128 时仍允许（同一内网业务机）
    return isPrivateOrLocalHost(hrefHost)
  } catch (e) {
    return false
  }
}

export function findCoveringPortalView(list, childPath) {
  if (!childPath || !list || !list.length) {
    return null
  }
  let best = null
  let bestLen = -1
  for (let i = 0; i < list.length; i++) {
    const view = list[i]
    if (!view || !view.path || !isPortalPathDescendant(childPath, view.path)) {
      continue
    }
    const len = portalPathAliasKey(view.path).length
    if (len > bestLen) {
      best = view
      bestLen = len
    }
  }
  return best
}

/**
 * iframe 内跳到子页 / 带 ? 参数时，按通知详情方式：新开 JUMP 路由（push），底栏出子页签。
 * 不得把 iframe 里的其它叶子菜单（如接口资产信息）当成 JUMP 新开的根页签。
 */
export function applyIframeHrefToJump(router, href, clientIdHint, title, messageEvent) {
  if (!router || !href || !/^https?:\/\//i.test(href)) {
    return
  }
  if (/camstar-cookie-bridge|jump-portal-sync/i.test(href)) {
    return
  }
  const store = router.app && router.app.$store
  if (store && store.state.portal && store.state.portal.iframeSyncSuspended) {
    return
  }
  if (messageEvent && !isMessageFromVisiblePortalIframe(messageEvent)) {
    return
  }
  const route = router.currentRoute
  const clientId = parsePortalClientId(route && route.path) || clientIdHint
  if (!clientId || !route || !parsePortalClientId(route.path)) {
    return
  }
  if (!sameBusinessOrigin(href, route)) {
    return
  }
  const loc = httpUrlToPortalLocation(clientId, href)
  if (!loc) {
    return
  }
  const pathLinkMap = store && store.state.portal && store.state.portal.pathLinkMap
  const covering = resolveCoveringLeaf(route.path, pathLinkMap)
  const nextLeaf = resolveCoveringLeaf(loc.path, pathLinkMap)
  const currentView = store && store.state.tagsView && (store.state.tagsView.visitedViews || []).find(v => portalTabsMatch(v, route))
  const sessionParentPath = (currentView && currentView.meta && currentView.meta.portalParentPath)
    || (covering && covering.key)
    || ''
  const otherLeaf = !!(covering && nextLeaf && covering.key && nextLeaf.key && covering.key !== nextLeaf.key)
  const samePath = portalPathAliasKey(route.path) === portalPathAliasKey(loc.path)
  const keepJumpChild = !!(route.query && route.query._jump_child && samePath)
  const backToParent = !!(otherLeaf && sessionParentPath && nextLeaf
    && portalPathAliasKey(nextLeaf.key) === portalPathAliasKey(sessionParentPath))
  if (backToParent) {
    loc.path = sessionParentPath
    loc.query = {}
    loc.hash = ''
    setPendingPortalIframeTitle(loc, '')
    setPendingPortalParent(loc, '', false)
  } else if (otherLeaf) {
    loc.query = Object.assign({}, loc.query || {}, { _jump_child: '1' })
    setPendingPortalIframeTitle(loc, title)
    setPendingPortalParent(loc, sessionParentPath, true)
  } else if (keepJumpChild) {
    loc.query = Object.assign({}, loc.query || {}, { _jump_child: '1' })
    setPendingPortalIframeTitle(loc, title)
    setPendingPortalParent(loc, sessionParentPath, true)
  } else {
    setPendingPortalIframeTitle(loc, title)
    setPendingPortalParent(loc, sessionParentPath, false)
  }
  if (sameJumpLocation(route, loc)) {
    return
  }
  const payload = {
    path: loc.path,
    query: loc.query,
    hash: loc.hash || undefined
  }
  if (!portalTabsMatch(route, loc) && store) {
    restoreParentPortalIframe(store, sessionParentPath || (covering && covering.key) || '')
  }
  const nav = portalTabsMatch(route, loc) ? router.replace.bind(router) : router.push.bind(router)
  nav(payload).catch(() => {})
}

function restoreParentPortalIframe(store, parentPath) {
  if (!parentPath) {
    return
  }
  store.commit('tagsView/BUMP_COVERING_PORTAL_IFRAME', { path: parentPath, query: {}, hash: '' })
}

function isMessageFromVisiblePortalIframe(event) {
  if (!event || !event.source || typeof document === 'undefined') {
    return false
  }
  const nodes = document.querySelectorAll('.iframe-toggle__frames .inner-link-root')
  if (!nodes.length) {
    return true
  }
  for (let i = 0; i < nodes.length; i++) {
    const root = nodes[i]
    const hidden = window.getComputedStyle(root).display === 'none'
    if (hidden) {
      continue
    }
    const iframe = root.querySelector('iframe')
    if (iframe && iframe.contentWindow === event.source) {
      return true
    }
  }
  return false
}

export function isJumpPortalLocationMessage(event) {
  const data = event && event.data
  return !!(data && data.type === JUMP_PORTAL_LOCATION && data.href)
}

let portalLocListenerBound = false

/** 全局监听业务 iframe 的地址上报（不依赖 IframeToggle 是否已挂上） */
export function installJumpPortalLocationListener(router) {
  if (portalLocListenerBound || !router || typeof window === 'undefined') {
    return
  }
  portalLocListenerBound = true
  window.addEventListener('message', function (event) {
    if (!isJumpPortalLocationMessage(event)) {
      return
    }
    const data = event.data
    applyIframeHrefToJump(router, data.href, null, data.clickLabel || data.title, event)
  })
}
