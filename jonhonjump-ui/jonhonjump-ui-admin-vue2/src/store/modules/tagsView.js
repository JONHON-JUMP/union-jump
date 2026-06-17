import { isPortalSubSystemHomePath } from '@/utils/portalRoute'

const state = {
  visitedViews: [],
  cachedViews: [],
  iframeViews: [],
  recentViewPaths: []
}

const mutations = {
  ADD_IFRAME_VIEW: (state, view) => {
    const title = resolveViewTitle(view)
    const index = state.iframeViews.findIndex(v => v.path === view.path)
    if (index > -1) {
      state.iframeViews.splice(index, 1, Object.assign({}, state.iframeViews[index], view, { title }))
      return
    }
    state.iframeViews.push(Object.assign({}, view, { title }))
  },
  ADD_VISITED_VIEW: (state, view) => {
    const title = resolveViewTitle(view)
    const index = state.visitedViews.findIndex(v => v.path === view.path)
    if (index > -1) {
      state.visitedViews.splice(index, 1, Object.assign({}, state.visitedViews[index], view, { title }))
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
    state.iframeViews = state.iframeViews.filter(item => item.path !== view.path)
    state.recentViewPaths = state.recentViewPaths.filter(path => path !== view.path)
  },
  DEL_IFRAME_VIEW: (state, view) => {
    state.iframeViews = state.iframeViews.filter(item => item.path !== view.path)
  },
  DEL_CACHED_VIEW: (state, view) => {
    const index = state.cachedViews.indexOf(view.name)
    index > -1 && state.cachedViews.splice(index, 1)
  },

  DEL_OTHERS_VISITED_VIEWS: (state, view) => {
    state.visitedViews = state.visitedViews.filter(v => {
      return v.meta.affix || v.path === view.path
    })
    state.iframeViews = state.iframeViews.filter(item => item.path === view.path)
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
    const title = resolveViewTitle(view)
    state.visitedViews.splice(index, 1, Object.assign({}, state.visitedViews[index], view, { title }))
    const iframeIndex = state.iframeViews.findIndex(v => v.path === view.path)
    if (iframeIndex > -1) {
      state.iframeViews.splice(iframeIndex, 1, Object.assign({}, state.iframeViews[iframeIndex], view, { title }))
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
      if(item.meta.link) {
        const fi = state.iframeViews.findIndex(v => v.path === item.path)
        state.iframeViews.splice(fi, 1)
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
      if(item.meta.link) {
        const fi = state.iframeViews.findIndex(v => v.path === item.path)
        state.iframeViews.splice(fi, 1)
      }
      return false
    })
  },
  PRUNE_VIEWS: (state, keepFn) => {
    const removed = state.visitedViews.filter(view => !keepFn(view))
    state.visitedViews = state.visitedViews.filter(keepFn)
    removed.forEach(view => {
      state.iframeViews = state.iframeViews.filter(item => item.path !== view.path)
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
  /** 门户回首页 / 切换系统：dock 仅保留首页，清空全部业务页签 */
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
  if (view.meta && view.meta.title) {
    return view.meta.title
  }
  if (view.title) {
    return view.title
  }
  return 'no-name'
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
