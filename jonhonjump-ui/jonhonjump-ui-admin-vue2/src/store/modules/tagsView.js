import { isPortalSubSystemHomePath, isGenericPortalTitle, resolvePortalMenuTitle } from '@/utils/portalRoute'

/** Camstar 关页保温上限（只缓存页面壳，不缓存业务数据） */
const MAX_WARM_CAMSTAR_IFRAMES = 8

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

function findViewIndexByPathAlias(list, path) {
  if (!path || !list || !list.length) {
    return -1
  }
  const exact = list.findIndex(v => v && v.path === path)
  if (exact >= 0) {
    return exact
  }
  const key = portalPathAliasKey(path)
  return list.findIndex(v => v && portalPathAliasKey(v.path) === key)
}

/** Camstar 关页：挪入保温池；若依：直接删除 */
function parkOrRemoveIframe(state, path) {
  if (!path) {
    return
  }
  const idx = findViewIndexByPathAlias(state.iframeViews, path)
  if (idx < 0) {
    state.warmIframeViews = (state.warmIframeViews || []).filter(
      v => portalPathAliasKey(v.path) !== portalPathAliasKey(path)
    )
    return
  }
  const item = state.iframeViews[idx]
  state.iframeViews.splice(idx, 1)
  if (!isCamstarIframeView(item)) {
    return
  }
  const keepKey = portalPathAliasKey(item.path)
  state.warmIframeViews = (state.warmIframeViews || []).filter(
    v => portalPathAliasKey(v.path) !== keepKey
  )
  state.warmIframeViews.push(item)
  while (state.warmIframeViews.length > MAX_WARM_CAMSTAR_IFRAMES) {
    state.warmIframeViews.shift()
  }
}

function reviveCamstarIframe(state, path) {
  const warm = state.warmIframeViews || []
  const w = findViewIndexByPathAlias(warm, path)
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
    // 保温命中：挪回活跃，冻结原 path + link（禁止换 :key / 改 src）
    const revived = reviveCamstarIframe(state, view.path)
    if (revived) {
      const prevLink = (revived.meta && revived.meta.link) || ''
      const keepLink = isCamstarIframeView(revived) ? (prevLink || nextLink) : (nextLink || prevLink)
      const merged = Object.assign({}, revived, {
        title,
        path: revived.path,
        meta: { ...(revived.meta || {}), ...(view.meta || {}), link: keepLink }
      })
      const activeIdx = findViewIndexByPathAlias(state.iframeViews, revived.path)
      if (activeIdx > -1) {
        return
      }
      state.iframeViews.push(merged)
      return
    }
    // 活跃判重：只认 path / /index 别名
    const index = findViewIndexByPathAlias(state.iframeViews, view.path)
    if (index > -1) {
      const prev = state.iframeViews[index]
      const prevLink = (prev.meta && prev.meta.link) || ''
      // Camstar：同 path 已挂过则冻结，禁止 pathLinkMap 回填改 src
      if (isCamstarIframeView(prev)) {
        if (prev.title !== title && title && title !== 'no-name') {
          state.iframeViews.splice(index, 1, Object.assign({}, prev, { title }))
        }
        return
      }
      if (prevLink === nextLink && prev.title === title) {
        return
      }
      if (prevLink === nextLink) {
        state.iframeViews.splice(index, 1, Object.assign({}, prev, {
          title,
          meta: { ...prev.meta, ...(view.meta || {}), link: prevLink }
        }))
        return
      }
      state.iframeViews.splice(index, 1, Object.assign({}, prev, view, { title }))
      return
    }
    state.iframeViews.push(Object.assign({}, view, { title }))
  },
  ADD_VISITED_VIEW: (state, view) => {
    let title = resolveViewTitle(view)
    const index = state.visitedViews.findIndex(v => v.path === view.path)
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
      if (v.path === view.path) {
        state.visitedViews.splice(i, 1)
        break
      }
    }
    // Camstar 进保温；若依直接删
    parkOrRemoveIframe(state, view.path)
    state.recentViewPaths = state.recentViewPaths.filter(path => path !== view.path)
  },
  DEL_IFRAME_VIEW: (state, view) => {
    parkOrRemoveIframe(state, view && view.path)
  },
  CLEAR_WARM_IFRAME_VIEWS: (state) => {
    state.warmIframeViews = []
  },
  /** 切系统：活跃 + 保温 iframe 一并清空 */
  CLEAR_ALL_IFRAME_FRAMES: (state) => {
    state.iframeViews = []
    state.warmIframeViews = []
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
      return v.meta.affix || v.path === view.path
    })
    const keepKey = portalPathAliasKey(view.path)
    ;(state.iframeViews || []).slice().forEach(item => {
      if (portalPathAliasKey(item.path) !== keepKey) {
        parkOrRemoveIframe(state, item.path)
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
    const index = state.visitedViews.findIndex(v => v.path === view.path)
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
    // Camstar：页签 meta.link 也冻结，避免二次打开被 sync 改写
    const visitLink = prevVisitHttp ? prevVisitLink : (nextVisitLink || prevVisitLink)
    state.visitedViews.splice(index, 1, Object.assign({}, prev, view, {
      title,
      meta: { ...(prev.meta || {}), ...(view.meta || {}), title, menuTitle: title, link: visitLink || undefined }
    }))
    const iframeIndex = findViewIndexByPathAlias(state.iframeViews, view.path)
    if (iframeIndex > -1) {
      const iframePrev = state.iframeViews[iframeIndex]
      const prevLink = (iframePrev.meta && iframePrev.meta.link) || ''
      const nextLink = (view.meta && view.meta.link) || ''
      // Camstar / 已有 http 直链：永远不换 link
      if (isCamstarIframeView(iframePrev) || (prevLink && (!nextLink || nextLink === prevLink))) {
        if (iframePrev.title !== title) {
          state.iframeViews.splice(iframeIndex, 1, Object.assign({}, iframePrev, { title }))
        }
        return
      }
      state.iframeViews.splice(iframeIndex, 1, Object.assign({}, iframePrev, view, { title }))
    }
  },
  DEL_RIGHT_VIEWS: (state, view) => {
    const index = state.visitedViews.findIndex(v => v.path === view.path)
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
        parkOrRemoveIframe(state, item.path)
      }
      return false
    })
  },
  DEL_LEFT_VIEWS: (state, view) => {
    const index = state.visitedViews.findIndex(v => v.path === view.path)
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
        parkOrRemoveIframe(state, item.path)
      }
      return false
    })
  },
  PRUNE_VIEWS: (state, keepFn) => {
    const removed = state.visitedViews.filter(view => !keepFn(view))
    state.visitedViews = state.visitedViews.filter(keepFn)
    removed.forEach(view => {
      parkOrRemoveIframe(state, view.path)
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
