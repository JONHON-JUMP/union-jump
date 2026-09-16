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
  resolveCoveringLeaf,
  unwrapDirectHttpIframeLink,
  portalQueryBucket
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

/** 列表工具栏：只改筛选条件，不应新开 JUMP 页签 */
const LIST_TOOLBAR_CLICK = /^(刷新|重载|重新加载|重置|清空|查询|搜索|检索|筛选|过滤|选时段|选择时段|导出|导入|下载|上传|取消|关闭|返回|确定|确认|保存|提交|查看|更多|展开|收起)$/

/** 明确进入子页的按钮 */
const OPEN_CHILD_CLICK = /查看参数|查看详情|查看版本|查询详情|新增接口|新增版本|新增|新建|创建|编辑|修改|预览|复制/

const FILTER_QUERY_KEYS = {
  page: true, pagenum: true, pagesize: true, size: true, current: true,
  limit: true, offset: true, total: true, keyword: true, keywords: true,
  name: true, title: true, status: true, type: true, state: true,
  start: true, end: true, starttime: true, endtime: true, begintime: true,
  finishtime: true, from: true, to: true, date: true, time: true, range: true,
  year: true, month: true, week: true, t: true, _t: true, ts: true,
  timestamp: true, random: true, nonce: true, refresh: true, order: true,
  orderby: true, sort: true, sortby: true, field: true, q: true,
  search: true, filter: true, timerange: true, daterange: true
}

const ID_QUERY_KEYS = {
  id: true, ids: true, uuid: true, guid: true, pk: true,
  applyid: true, recordid: true, rowid: true, bizid: true, dataid: true,
  versionid: true, paramid: true
}

function clickLabelText(label) {
  return String(label || '').replace(/\s+/g, '')
}

function isOpenChildClick(label) {
  const t = clickLabelText(label)
  if (!t || t.length > 24) {
    return false
  }
  if (/接口管理平台|业务系统/.test(t)) {
    return false
  }
  if (OPEN_CHILD_CLICK.test(t)) {
    return true
  }
  return /详情/.test(t) && !/^(查询|搜索)$/.test(t)
}

function isListToolbarClick(label) {
  const t = clickLabelText(label)
  if (!t || isOpenChildClick(t)) {
    return false
  }
  return LIST_TOOLBAR_CLICK.test(t) || /刷新|选时段|时段|时间范围|日期范围/.test(t)
}

function pathnameOfHttp(url) {
  try {
    const raw = unwrapDirectHttpIframeLink(url) || url
    return new URL(String(raw)).pathname.replace(/\/+$/, '') || '/'
  } catch (e) {
    return ''
  }
}

function locPathname(loc) {
  const clientId = parsePortalClientId(loc && loc.path)
  const rest = extractPortalMenuRest(loc && loc.path, clientId)
  const http = slashIpPortRestToHttp(rest)
  return http ? pathnameOfHttp(http) : ''
}

function leafPathname(covering) {
  const link = covering && covering.entry && covering.entry.link
  return link ? pathnameOfHttp(link) : ''
}

function hashRoutePath(hash) {
  return String(hash || '').replace(/^#/, '').split('?')[0]
}

function hasDetailSegment(pathOrHash) {
  return /(^|\/)(detail|details|edit|add|create|new|preview|param|params|parameter|version)(\/|$)/i.test(
    String(pathOrHash || '')
  )
}

function isDeeperThanLeaf(loc, covering) {
  const locPath = locPathname(loc)
  const leafPath = leafPathname(covering)
  if (!locPath || !leafPath) {
    return hasDetailSegment(locPath) || hasDetailSegment(hashRoutePath(loc && loc.hash))
  }
  const a = locPath.replace(/\/index$/i, '')
  const b = leafPath.replace(/\/index$/i, '')
  if (a !== b && a.startsWith(b + '/')) {
    return true
  }
  const hashPath = hashRoutePath(loc && loc.hash)
  return !!(hashPath && hashPath !== '/' && hasDetailSegment(hashPath))
}

function queryKeys(query) {
  return Object.keys(query || {}).filter(k => {
    if (k === '_jump_child' || k === '_portal_t' || k === '_jump_camstar_warm') {
      return false
    }
    const val = query[k]
    return val != null && val !== ''
  })
}

function isFilterOnlyQuery(query) {
  const keys = queryKeys(query)
  if (!keys.length) {
    return true
  }
  return keys.every(k => FILTER_QUERY_KEYS[String(k).toLowerCase()] || FILTER_QUERY_KEYS[String(k).toLowerCase().replace(/[_-]/g, '')])
}

function hasIdentityQuery(query) {
  return queryKeys(query).some(k => {
    const n = String(k).toLowerCase().replace(/[_-]/g, '')
    return ID_QUERY_KEYS[n] || /id$/.test(n)
  })
}

/**
 * 是否要新开 JUMP 底栏子页签。
 * 以前：iframe 地址一带 ? 就 push。刷新 / 选时段 / 查看 都会误开。
 * 现在：只有详情类按钮，或路径明显深一层 / 带业务 id 的详情，才开。
 */
export function shouldOpenPortalChildPage(route, loc, clickLabel, covering, otherLeaf) {
  if (!loc) {
    return false
  }
  if (isListToolbarClick(clickLabel)) {
    return false
  }
  if (isOpenChildClick(clickLabel)) {
    return true
  }
  if (isDeeperThanLeaf(loc, covering) || hasDetailSegment(locPathname(loc))) {
    return true
  }
  if (otherLeaf) {
    return false
  }
  if (isFilterOnlyQuery(loc.query) && !hasDetailSegment(hashRoutePath(loc.hash))) {
    return false
  }
  if (hasIdentityQuery(loc.query) && portalQueryBucket(route) === 'root') {
    return true
  }
  return false
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
  const openChild = shouldOpenPortalChildPage(route, loc, title, covering, otherLeaf)

  if (backToParent) {
    loc.path = sessionParentPath
    loc.query = {}
    loc.hash = ''
    setPendingPortalIframeTitle(loc, '')
    setPendingPortalParent(loc, '', false)
  } else if (!openChild && !keepJumpChild) {
    return
  } else if (openChild || keepJumpChild) {
    loc.query = Object.assign({}, loc.query || {}, { _jump_child: '1' })
    setPendingPortalIframeTitle(loc, title)
    setPendingPortalParent(loc, sessionParentPath, true)
  } else {
    return
  }
  if (sameJumpLocation(route, loc)) {
    return
  }
  const payload = {
    path: loc.path,
    query: loc.query,
    hash: loc.hash || undefined
  }
  if (!portalTabsMatch(route, loc) && store && openChild) {
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
