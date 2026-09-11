import {
  isPortalSubSystemHomePath,
  isGenericPortalTitle,
  resolvePortalMenuTitle,
  portalTabsMatch,
  portalTabKey,
  portalQueryBucket,
  isPortalPathDescendant
} from '@/utils/portalRoute'

/** Camstar 关页保温上限（只缓存页面壳，不缓存业务数据） */
const MAX_WARM_CAMSTAR_IFRAMES = 8

function maxWarmCamstarIframes() {
  // Chrome <90：隐藏保温帧越多，切系统/收 dock 时主线程卸 DOM 越卡
  if (typeof document !== 'undefined' && document.documentElement.classList.contains('legacy-anim')) {
    return 2
  }
  return MAX_WARM_CAMSTAR_IFRAMES
}

const state = {
  visitedViews: [],
  cachedViews: [],
  iframeViews: [],
  /**
   * 已关页签但仍挂着的 Camstar iframe（隐藏保温）。
   * 再开同菜单时按 path 别名 revive，:key=原 path 不变 → 秒开。
   */
  warmIframeViews: [],
  recentViewPaths: []
}

function isCamstarIframeView(view) {
  const link = (view && view.meta && view.meta.link) || ''
  return /^https?:\/\//i.test(link) && link.indexOf('/#/') < 0 && link.indexOf('#') < 0
}

/** 仅 /index 别名；禁止用业务 URL 认同一页（会串菜单） */
function portalPathAliasKey(path) {
  return String(path || '').replace(/\/index\/?$/, '').replace(/\/$/, '')
}

function asViewRef(viewOrPath) {
  if (viewOrPath && typeof viewOrPath === 'object') {
    return viewOrPath
  }
  return { path: viewOrPath, query: {}, hash: '' }
}

function findViewIndexByPortalTab(list, view) {
  if (!view || !list || !list.length) {
    return -1
  }
  return list.findIndex(v => v && portalTabsMatch(v, view))
}

/** Camstar 关页：挪入保温池；若依：直接删除 */
function parkOrRemoveIframe(state, viewOrPath) {
  const ref = asViewRef(viewOrPath)
  if (!ref.path) {
    return
  }
  const idx = findViewIndexByPortalTab(state.iframeViews, ref)
  if (idx < 0) {
    const dropKey = portalTabKey(ref)
    state.warmIframeViews = (state.warmIframeViews || []).filter(
      v => portalTabKey(v) !== dropKey
    )
    return
  }
  const item = state.iframeViews[idx]
  state.iframeViews.splice(idx, 1)
  if (!isCamstarIframeView(item)) {
    return
  }
  const keepKey = portalTabKey(item)
  state.warmIframeViews = (state.warmIframeViews || []).filter(
    v => portalTabKey(v) !== keepKey
  )
  state.warmIframeViews.push(item)
  while (state.warmIframeViews.length > maxWarmCamstarIframes()) {
    state.warmIframeViews.shift()
  }
}

function reviveCamstarIframe(state, view) {
  const ref = asViewRef(view)
  const warm = state.warmIframeViews || []
  const w = findViewIndexByPortalTab(warm, ref)
  if (w < 0) {
    return null
  }
  const [item] = state.warmIframeViews.splice(w, 1)
  return item
}

const mutations = {
  ADD_IFRAME_VIEW: (state, view) => {
    const title = resolveViewTitle(view)
    const nextLink = (view.meta && view.meta.link) || ''
    const isChild = portalQueryBucket(view) === 'child' || !!(view.meta && view.meta.portalChild)
    // 保温命中：挪回活跃；子页不冻结 link，否则看不到 ? 参数页
    const revived = reviveCamstarIframe(state, view)
    if (revived) {
      const prevLink = (revived.meta && revived.meta.link) || ''
      const homeLink = (revived.meta && revived.meta.portalHomeLink) || (!isChild ? (prevLink || nextLink) : '')
      const keepLink = (!isChild && isCamstarIframeView(revived))
        ? (homeLink || prevLink || nextLink)
        : (nextLink || prevLink)
      const merged = Object.assign({}, revived, view, {
        title,
        path: revived.path,
        meta: { ...(revived.meta || {}), ...(view.meta || {}), link: keepLink, portalHomeLink: homeLink || keepLink }
      })
      const activeIdx = findViewIndexByPortalTab(state.iframeViews, revived)
      if (activeIdx > -1) {
        return
      }
      state.iframeViews.push(merged)
      return
    }
    const index = findViewIndexByPortalTab(state.iframeViews, view)
    if (index > -1) {
      const prev = state.iframeViews[index]
      const prevLink = (prev.meta && prev.meta.link) || ''
      const homeLink = (prev.meta && prev.meta.portalHomeLink) || (!isChild ? (prevLink || nextLink) : '')
      // 主菜单 Camstar：同页签冻结 src；子页允许换成带 ? 的地址
      if (!isChild && isCamstarIframeView(prev)) {
        const frozen = Object.assign({}, prev, {
          title: (title && title !== 'no-name') ? title : prev.title,
          meta: { ...(prev.meta || {}), portalHomeLink: homeLink || prevLink }
        })
        if (frozen.title !== prev.title || !(prev.meta && prev.meta.portalHomeLink)) {
          state.iframeViews.splice(index, 1, frozen)
        }
        return
      }
      if (prevLink === nextLink && prev.title === title) {
        return
      }
      state.iframeViews.splice(index, 1, Object.assign({}, prev, view, {
        title,
        meta: { ...(prev.meta || {}), ...(view.meta || {}), portalHomeLink: homeLink || (view.meta && view.meta.portalHomeLink) }
      }))
      return
    }
    state.iframeViews.push(Object.assign({}, view, {
      title,
      meta: {
        ...(view.meta || {}),
        portalHomeLink: isChild
          ? (view.meta && view.meta.portalHomeLink)
          : ((view.meta && view.meta.portalHomeLink) || nextLink)
      }
    }))
  },
  ADD_VISITED_VIEW: (state, view) => {
    let title = resolveViewTitle(view)
    const index = state.visitedViews.findIndex(v => portalTabsMatch(v, view))
    if (index > -1) {
      const prev = state.visitedViews[index]
      // 泛化标题不得覆盖已有真实菜单名（Camstar 静态 PortalFrame 常见）
      if (isGenericPortalTitle(title) && prev && !isGenericPortalTitle(prev.title)) {
        title = prev.title
      }
      state.visitedViews.splice(index, 1, Object.assign({}, prev, view, {
        title,
        meta: { ...(prev.meta || {}), ...(view.meta || {}), title, menuTitle: title }
      }))
      return
    }
    state.visitedViews.push(Object.assign({}, view, { title }))
  },
  TOUCH_VISITED_VIEW: (state, view) => {
    state.recentViewPaths = state.recentViewPaths.filter(path => path !== view.path)
    state.recentViewPaths.push(view.path)
  },
  ADD_CACHED_VIEW: (state, view) => {
    if (state.cachedViews.includes(view.name)) return
    if (view.meta && !view.meta.noCache) {
      state.cachedViews.push(view.name)
    }
  },
  DEL_VISITED_VIEW: (state, view) => {
    for (const [i, v] of state.visitedViews.entries()) {
      if (portalTabsMatch(v, view)) {
        state.visitedViews.splice(i, 1)
        break
      }
    }
    // Camstar 进保温；若依直接删
    parkOrRemoveIframe(state, view)
    state.recentViewPaths = state.recentViewPaths.filter(path => path !== view.path)
  },
  DEL_IFRAME_VIEW: (state, view) => {
    parkOrRemoveIframe(state, view)
  },
  CLEAR_WARM_IFRAME_VIEWS: (state) => {
    state.warmIframeViews = []
  },
  /** 切系统：活跃 + 保温 iframe 一并清空 */
  CLEAR_ALL_IFRAME_FRAMES: (state) => {
    state.iframeViews = []
    state.warmIframeViews = []
  },
  /** 子页从列表 iframe 点开后，把列表帧重新加载回原菜单地址 */
  BUMP_COVERING_PORTAL_IFRAME: (state, childLoc) => {
    if (!childLoc || !childLoc.path) {
      return
    }
    const views = state.iframeViews || []
    let best = -1
    let bestLen = -1
    for (let i = 0; i < views.length; i++) {
      const v = views[i]
      if (!v || portalQueryBucket(v) !== 'root') {
        continue
      }
      if (portalPathAliasKey(v.path) !== portalPathAliasKey(childLoc.path)
        && !isPortalPathDescendant(childLoc.path, v.path)) {
        continue
      }
      const len = portalPathAliasKey(v.path).length
      if (len > bestLen) {
        best = i
        bestLen = len
      }
    }
    if (best < 0) {
      return
    }
    const prev = views[best]
    views.splice(best, 1, Object.assign({}, prev, {
      meta: { ...(prev.meta || {}), portalSrcNonce: Date.now() }
    }))
  },
  /** 点回叶子菜单：iframe src 拉回原菜单地址并重建，避免仍停在详情 */
  RESTORE_PORTAL_IFRAME: (state, tab) => {
    const index = findViewIndexByPortalTab(state.iframeViews, tab)
    if (index < 0) {
      return
    }
    const prev = state.iframeViews[index]
    const home = (prev.meta && (prev.meta.portalHomeLink || prev.meta.link)) || ''
    if (!home) {
      return
    }
    state.iframeViews.splice(index, 1, Object.assign({}, prev, {
      meta: {
        ...(prev.meta || {}),
        link: home,
        portalHomeLink: home,
        portalSrcNonce: Date.now()
      }
    }))
  },
  /** 打开前丢掉该 path 的保温壳，避免复用已缓存的 Camstar 登录页 */
  EVICT_WARM_IFRAME_PATH: (state, path) => {
    if (!path) {
      return
    }
    const key = portalPathAliasKey(path)
    state.warmIframeViews = (state.warmIframeViews || []).filter(
      v => portalPathAliasKey(v.path) !== key
    )
  },
  /** 预挂默认关闭：未种 Cookie 时预挂易保温登录页；保留空实现兼容旧 dispatch */
  PREFETCH_WARM_IFRAMES: () => {},
  DEL_CACHED_VIEW: (state, view) => {
    const index = state.cachedViews.indexOf(view.name)
    index > -1 && state.cachedViews.splice(index, 1)
  },

  DEL_OTHERS_VISITED_VIEWS: (state, view) => {
    state.visitedViews = state.visitedViews.filter(v => {
      return (v.meta && v.meta.affix) || portalTabsMatch(v, view)
    })
    ;(state.iframeViews || []).slice().forEach(item => {
      if (!portalTabsMatch(item, view)) {
        parkOrRemoveIframe(state, item)
      }
    })
    state.recentViewPaths = state.recentViewPaths.filter(path => path === view.path)
  },
  DEL_OTHERS_CACHED_VIEWS: (state, view) => {
    const index = state.cachedViews.indexOf(view.name)
    if (index > -1) {
      state.cachedViews = state.cachedViews.slice(index, index + 1)
    } else {
      state.cachedViews = []
    }
  },
  DEL_ALL_VISITED_VIEWS: state => {
    // keep affix tags
    const affixTags = state.visitedViews.filter(tag => tag.meta.affix)
    state.visitedViews = affixTags
    state.iframeViews = []
    state.warmIframeViews = []
    state.recentViewPaths = []
  },
  DEL_ALL_CACHED_VIEWS: state => {
    state.cachedViews = []
  },
  UPDATE_VISITED_VIEW: (state, view) => {
    const index = state.visitedViews.findIndex(v => portalTabsMatch(v, view))
    if (index === -1) {
      return
    }
    const prev = state.visitedViews[index]
    let title = resolveViewTitle(view)
    if (isGenericPortalTitle(title) && prev && !isGenericPortalTitle(prev.title)) {
      title = prev.title
    }
    const prevVisitLink = (prev.meta && prev.meta.link) || ''
    const nextVisitLink = (view.meta && view.meta.link) || ''
    const prevVisitHttp = /^https?:\/\//i.test(prevVisitLink) && prevVisitLink.indexOf('#') < 0
    const isChild = portalQueryBucket(view) === 'child' || !!(view.meta && view.meta.portalChild)
    // 主菜单 Camstar：冻结 link；子页必须换成带 ? 的地址
    const visitLink = (prevVisitHttp && !isChild)
      ? prevVisitLink
      : (nextVisitLink || prevVisitLink)
    state.visitedViews.splice(index, 1, Object.assign({}, prev, view, {
      title,
      meta: { ...(prev.meta || {}), ...(view.meta || {}), title, menuTitle: title, link: visitLink || undefined }
    }))
    const iframeIndex = findViewIndexByPortalTab(state.iframeViews, view)
    if (iframeIndex > -1) {
      const iframePrev = state.iframeViews[iframeIndex]
      const prevLink = (iframePrev.meta && iframePrev.meta.link) || ''
      const nextLink = (view.meta && view.meta.link) || ''
      if (!isChild && (isCamstarIframeView(iframePrev) || (prevLink && (!nextLink || nextLink === prevLink)))) {
        if (iframePrev.title !== title) {
          state.iframeViews.splice(iframeIndex, 1, Object.assign({}, iframePrev, { title }))
        }
        return
      }
      state.iframeViews.splice(iframeIndex, 1, Object.assign({}, iframePrev, view, { title }))
    }
  },
  DEL_RIGHT_VIEWS: (state, view) => {
    const index = state.visitedViews.findIndex(v => portalTabsMatch(v, view))
    if (index === -1) {
      return
    }
    state.visitedViews = state.visitedViews.filter((item, idx) => {
      if (idx <= index || (item.meta && item.meta.affix)) {
        return true
      }
      const i = state.cachedViews.indexOf(item.name)
      if (i > -1) {
        state.cachedViews.splice(i, 1)
      }
      if (item.meta && item.meta.link) {
        parkOrRemoveIframe(state, item)
      }
      return false
    })
  },
  DEL_LEFT_VIEWS: (state, view) => {
    const index = state.visitedViews.findIndex(v => portalTabsMatch(v, view))
    if (index === -1) {
      return
    }
    state.visitedViews = state.visitedViews.filter((item, idx) => {
      if (idx >= index || (item.meta && item.meta.affix)) {
        return true
      }
      const i = state.cachedViews.indexOf(item.name)
      if (i > -1) {
        state.cachedViews.splice(i, 1)
      }
      if (item.meta && item.meta.link) {
        parkOrRemoveIframe(state, item)
      }
      return false
    })
  },
  PRUNE_VIEWS: (state, keepFn) => {
    const removed = state.visitedViews.filter(view => !keepFn(view))
    state.visitedViews = state.visitedViews.filter(keepFn)
    removed.forEach(view => {
      parkOrRemoveIframe(state, view)
      state.recentViewPaths = state.recentViewPaths.filter(path => path !== view.path)
      if (view.name) {
        const index = state.cachedViews.indexOf(view.name)
        if (index > -1) {
          state.cachedViews.splice(index, 1)
        }
      }
    })
  }
}

const actions = {
  addView({ dispatch }, view) {
    dispatch('addVisitedView', view)
    dispatch('addCachedView', view)
  },
  addIframeView({ commit }, view) {
    commit('ADD_IFRAME_VIEW', view)
  },
  addVisitedView({ commit }, view) {
    commit('ADD_VISITED_VIEW', view)
  },
  touchVisitedView({ commit }, view) {
    commit('TOUCH_VISITED_VIEW', view)
  },
  addCachedView({ commit }, view) {
    commit('ADD_CACHED_VIEW', view)
  },
  delView({ dispatch, state }, view) {
    return new Promise(resolve => {
      dispatch('delVisitedView', view)
      dispatch('delCachedView', view)
      resolve({
        visitedViews: [...state.visitedViews],
        cachedViews: [...state.cachedViews]
      })
    })
  },
  delVisitedView({ commit, state }, view) {
    return new Promise(resolve => {
      commit('DEL_VISITED_VIEW', view)
      resolve([...state.visitedViews])
    })
  },
  delIframeView({ commit, state }, view) {
    return new Promise(resolve => {
      commit('DEL_IFRAME_VIEW', view)
      resolve([...state.iframeViews])
    })
  },
  delCachedView({ commit, state }, view) {
    return new Promise(resolve => {
      commit('DEL_CACHED_VIEW', view)
      resolve([...state.cachedViews])
    })
  },
  delOthersViews({ dispatch, state }, view) {
    return new Promise(resolve => {
      dispatch('delOthersVisitedViews', view)
      dispatch('delOthersCachedViews', view)
      resolve({
        visitedViews: [...state.visitedViews],
        cachedViews: [...state.cachedViews]
      })
    })
  },
  delOthersVisitedViews({ commit, state }, view) {
    return new Promise(resolve => {
      commit('DEL_OTHERS_VISITED_VIEWS', view)
      resolve([...state.visitedViews])
    })
  },
  delOthersCachedViews({ commit, state }, view) {
    return new Promise(resolve => {
      commit('DEL_OTHERS_CACHED_VIEWS', view)
      resolve([...state.cachedViews])
    })
  },
  delAllViews({ dispatch, state }, view) {
    return new Promise(resolve => {
      dispatch('delAllVisitedViews', view)
      dispatch('delAllCachedViews', view)
      resolve({
        visitedViews: [...state.visitedViews],
        cachedViews: [...state.cachedViews]
      })
    })
  },
  delAllVisitedViews({ commit, state }) {
    return new Promise(resolve => {
      commit('DEL_ALL_VISITED_VIEWS')
      resolve([...state.visitedViews])
    })
  },
  delAllCachedViews({ commit, state }) {
    return new Promise(resolve => {
      commit('DEL_ALL_CACHED_VIEWS')
      resolve([...state.cachedViews])
    })
  },
  updateVisitedView({ commit }, view) {
    commit('UPDATE_VISITED_VIEW', view)
  },
  delRightTags({ commit }, view) {
    return new Promise(resolve => {
      commit('DEL_RIGHT_VIEWS', view)
      resolve([...state.visitedViews])
    })
  },
  delLeftTags({ commit }, view) {
    return new Promise(resolve => {
      commit('DEL_LEFT_VIEWS', view)
      resolve([...state.visitedViews])
    })
  },
  keepPortalViews({ commit, state }, clientId) {
    const prefix = `/portal/${clientId}`
    commit('PRUNE_VIEWS', view => view.path === prefix || view.path.startsWith(`${prefix}/`))
    return Promise.resolve([...state.visitedViews])
  },
  keepMainViews({ commit, state }) {
    commit('PRUNE_VIEWS', view => !/^\/portal\/[^/]+(?:\/|$)/.test(view.path))
    if (!state.visitedViews.some(view => view.path === '/index')) {
      commit('ADD_VISITED_VIEW', {
        path: '/index',
        fullPath: '/index',
        name: '首页',
        meta: { title: '首页', affix: true }
      })
    }
    return Promise.resolve([...state.visitedViews])
  },
  prunePortalHomeViews({ commit, state }) {
    commit('PRUNE_VIEWS', view => {
      if (view.meta && view.meta.affix) return true
      if (isPortalSubSystemHomePath(view.path)) return false
      if (view.meta && view.meta.portalHome) return false
      return true
    })
    return Promise.resolve([...state.visitedViews])
  },
  /** 门户回首页 / 切换系统：dock 仅保留首页，并清掉全部业务 iframe（含 Camstar 保温） */
  clearDockBusinessTabs({ commit, state }) {
    const legacy = typeof document !== 'undefined'
      && document.documentElement.classList.contains('legacy-anim')

    if (legacy) {
      // Chrome <90：PRUNE 会先 park Camstar→warm 再 CLEAR 全删，等于两次卸 DOM。
      // 切系统本来就要清空，单次 prune 页签 + 直接 CLEAR，避免中间态重绘。
      // 不可把 CLEAR 拖到 rAF 之后：skipNavigate 紧接着 push 新页时会误删新 iframe。
      const removed = []
      state.visitedViews = state.visitedViews.filter(view => {
        if (view.path === '/index' || view.path === '/') {
          return true
        }
        if (view.meta && view.meta.affix) {
          return true
        }
        removed.push(view)
        return false
      })
      removed.forEach(view => {
        if (!view || !view.name) {
          return
        }
        const index = state.cachedViews.indexOf(view.name)
        if (index > -1) {
          state.cachedViews.splice(index, 1)
        }
      })
      commit('CLEAR_ALL_IFRAME_FRAMES')
      state.recentViewPaths = []
      return Promise.resolve([...state.visitedViews])
    }

    commit('PRUNE_VIEWS', view => {
      if (view.path === '/index' || view.path === '/') {
        return true
      }
      if (view.meta && view.meta.affix) {
        return true
      }
      return false
    })
    // 切系统不得保留上一系统页签/保温壳，否则会出现在新系统 dock 下
    commit('CLEAR_ALL_IFRAME_FRAMES')
    state.recentViewPaths = []
    return Promise.resolve([...state.visitedViews])
  },
  /** @deprecated 使用 clearDockBusinessTabs */
  clearPortalDockTabs({ dispatch }) {
    return dispatch('clearDockBusinessTabs')
  },
}

function resolveViewTitle(view) {
  if (!view) {
    return 'no-name'
  }
  const title = resolvePortalMenuTitle(
    view.meta && view.meta.menuTitle,
    view.meta && view.meta.title,
    view.title
  )
  if (title) {
    return title
  }
  // 仍无真实名时保留原 meta（便于后续 sync 用 pathLinkMap 覆盖），勿一律写成业务页
  const raw = (view.meta && view.meta.title) || view.title
  if (raw && !isGenericPortalTitle(raw)) {
    return raw
  }
  return raw || 'no-name'
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
