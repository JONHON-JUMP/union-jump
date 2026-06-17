import router, { constantRoutes } from '@/router'
import { authorize } from '@/api/login'
import { getMyExternalSystemList, getMyPortalMenus } from '@/api/system/subSystemUsers'
import { isPortalSubSystemHomePath, isMainBusinessPath } from '@/utils/portalRoute'
import { getUniqueSubSystem } from '@/utils/portalSubsystem'
import cache from '@/plugins/cache'

const PORTAL_LAST_SYSTEM_KEY = 'portal_last_system'

const state = {
  currentSystem: 'main',
  systemList: [],
  mainSidebarRouters: null,
  loadedSubSystems: {},
  subSystemMenuSignatures: {},
  subSystemEntryPaths: {},
  subSystemSidebarCache: {},
  subSystemPathLinkCache: {},
  pathLinkMap: {},
  ssoDone: {},
  loadingSubSystems: {},
  iframeSyncSuspended: false,
  preserveDockTabs: false,
  portalBootstrapped: false
}

const mutations = {
  SET_CURRENT_SYSTEM(state, system) {
    state.currentSystem = system
  },
  SET_SYSTEM_LIST(state, list) {
    state.systemList = list || []
  },
  SET_MAIN_SIDEBAR_ROUTERS(state, routes) {
    state.mainSidebarRouters = routes
  },
  SET_PORTAL_PATH_LINKS(state, pathLinkMap) {
    state.pathLinkMap = pathLinkMap || {}
  },
  SET_IFRAME_SYNC_SUSPENDED(state, suspended) {
    state.iframeSyncSuspended = suspended === true
  },
  SET_PRESERVE_DOCK_TABS(state, preserve) {
    state.preserveDockTabs = preserve === true
  },
  SET_PORTAL_BOOTSTRAPPED(state, bootstrapped) {
    state.portalBootstrapped = bootstrapped === true
  },
  MARK_SUB_SYSTEM_LOADED(state, { clientId, signature, entryPath }) {
    state.loadedSubSystems = {
      ...state.loadedSubSystems,
      [clientId]: true
    }
    state.subSystemMenuSignatures = {
      ...state.subSystemMenuSignatures,
      [clientId]: signature
    }
    if (entryPath) {
      state.subSystemEntryPaths = {
        ...state.subSystemEntryPaths,
        [clientId]: entryPath
      }
    }
  },
  SET_SUB_SYSTEM_SIDEBAR_CACHE(state, { clientId, sidebarRouters }) {
    state.subSystemSidebarCache = {
      ...state.subSystemSidebarCache,
      [clientId]: sidebarRouters
    }
  },
  SET_SUB_SYSTEM_PATH_LINK_CACHE(state, { clientId, pathLinkMap }) {
    state.subSystemPathLinkCache = {
      ...state.subSystemPathLinkCache,
      [clientId]: pathLinkMap
    }
  },
  MARK_SSO_DONE(state, clientId) {
    state.ssoDone = {
      ...state.ssoDone,
      [clientId]: true
    }
  },
  RESET_PORTAL(state) {
    state.currentSystem = 'main'
    state.systemList = []
    state.mainSidebarRouters = null
    state.loadedSubSystems = {}
    state.subSystemMenuSignatures = {}
    state.subSystemEntryPaths = {}
    state.subSystemSidebarCache = {}
    state.subSystemPathLinkCache = {}
    state.pathLinkMap = {}
    state.ssoDone = {}
    state.loadingSubSystems = {}
    state.iframeSyncSuspended = false
    state.preserveDockTabs = false
    state.portalBootstrapped = false
    clearPortalSystemChoice()
  }
}

const actions = {
  loadSystemList({ commit }) {
    return getMyExternalSystemList().then(res => {
      commit('SET_SYSTEM_LIST', res.data || [])
      return res.data || []
    })
  },

  /**
   * 登录后门户初始化：仅关联一个外部子系统时默认进入该子系统首页；
   * 本会话内用户手动切换过的系统会记住（刷新后保持）。
   */
  bootstrapAfterAuth({ commit, dispatch, state }) {
    if (state.portalBootstrapped) {
      return Promise.resolve()
    }
    return dispatch('loadSystemList').then(list => {
      const persisted = cache.session.get(PORTAL_LAST_SYSTEM_KEY)
      if (persisted === 'main') {
        return Promise.resolve()
      }
      if (persisted) {
        const exists = (list || []).some(item => item.clientId === persisted)
        if (exists && state.currentSystem !== persisted) {
          return dispatch('switchSystem', {
            system: persisted,
            stayOnPortalHome: true,
            skipNavigate: true,
            skipPersist: true
          })
        }
        return Promise.resolve()
      }
      const unique = getUniqueSubSystem(list)
      if (unique && state.currentSystem === 'main') {
        return dispatch('switchSystem', {
          system: unique.clientId,
          stayOnPortalHome: true,
          skipNavigate: true,
          skipPersist: true
        })
      }
      return Promise.resolve()
    }).catch(() => {}).finally(() => {
      commit('SET_PORTAL_BOOTSTRAPPED', true)
    })
  },

  cacheMainSidebar({ commit, state, rootState }) {
    if (state.mainSidebarRouters && state.mainSidebarRouters.length) {
      return Promise.resolve()
    }
    if (state.currentSystem === 'main' && rootState.permission.sidebarRouters.length) {
      commit('SET_MAIN_SIDEBAR_ROUTERS', rootState.permission.sidebarRouters)
    }
    return Promise.resolve()
  },

  ensureMainSidebarCached({ commit, state, rootState }) {
    if (state.mainSidebarRouters && state.mainSidebarRouters.length) {
      return
    }
    if (state.currentSystem === 'main' && rootState.permission.sidebarRouters.length) {
      commit('SET_MAIN_SIDEBAR_ROUTERS', rootState.permission.sidebarRouters)
    }
  },

  switchSystem({ dispatch, state }, payload) {
    const system = typeof payload === 'object' && payload !== null ? payload.system : payload
    const stayOnPortalHome = typeof payload === 'object' && payload !== null && payload.stayOnPortalHome === true
    const skipNavigate = typeof payload === 'object' && payload !== null && payload.skipNavigate === true
    const skipPersist = typeof payload === 'object' && payload !== null && payload.skipPersist === true

    if (system === 'main') {
      if (!skipPersist) {
        persistPortalSystemChoice('main')
      }
      return dispatch('cacheMainSidebar').then(() => {
        if (state.currentSystem !== 'main') {
          return dispatch('tagsView/clearDockBusinessTabs', null, { root: true }).then(() => {
            return dispatch('enterMainSystem', { stayOnPortalHome })
          })
        }
        return dispatch('enterMainSystem', { stayOnPortalHome })
      })
    }
    const ensureList = state.systemList.length > 0
      ? Promise.resolve()
      : dispatch('loadSystemList')
    return ensureList.then(() => {
      const ref = resolveSystemRef(state, system)
      if (!ref) {
        return Promise.reject(new Error('无效的外部系统'))
      }
      const targetSystem = ref.clientId
      const currentSystem = state.currentSystem
      if (!skipPersist) {
        persistPortalSystemChoice(targetSystem)
      }
      return dispatch('cacheMainSidebar').then(() => {
        if (targetSystem === currentSystem) {
          if (stayOnPortalHome) {
            return Promise.resolve()
          }
          return dispatch('runSilentSso', targetSystem).then(() => {
            return dispatch('navigateToPortalHome')
          })
        }
        return dispatch('tagsView/clearDockBusinessTabs', null, { root: true }).then(() => {
          return dispatch('enterSubSystem', {
            clientId: targetSystem,
            navigate: false,
            stayOnPortalHome: true
          }).then(() => {
            if (isMainBusinessPath(router.currentRoute.path)) {
              return dispatch('navigateToPortalHome')
            }
            if (skipNavigate || stayOnPortalHome) {
              return Promise.resolve()
            }
            return dispatch('navigateToPortalHome')
          })
        })
      })
    })
  },

  enterMainSystem({ commit, dispatch, state, rootState }, payload) {
    const stayOnPortalHome = payload && payload.stayOnPortalHome
    commit('SET_CURRENT_SYSTEM', 'main')
    if (state.mainSidebarRouters && state.mainSidebarRouters.length) {
      commit('SET_SIDEBAR_ROUTERS', state.mainSidebarRouters, { root: true })
    } else if (rootState.permission.defaultRoutes && rootState.permission.defaultRoutes.length) {
      commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(rootState.permission.defaultRoutes), { root: true })
    }
    if (stayOnPortalHome) {
      commit('SET_PORTAL_PATH_LINKS', {})
      return dispatch('navigateToPortalHome')
    }
    return dispatch('tagsView/keepMainViews', null, { root: true }).then(() => {
      return dispatch('goMain')
    })
  },

  goMain() {
    return goPortalIndex()
  },

  /** 回到门户 /index，保留 dock 已打开页签（仅隐藏当前页） */
  navigateToPortalHome({ commit, dispatch }) {
    commit('SET_PRESERVE_DOCK_TABS', true)
    commit('SET_PORTAL_PATH_LINKS', {})
    return dispatch('tagsView/prunePortalHomeViews', null, { root: true }).then(() => {
      return goPortalIndex().finally(() => {
        commit('SET_PRESERVE_DOCK_TABS', false)
      })
    })
  },

  /** 关闭当前页签后回门户首页（不清理其它 dock 页签） */
  returnToPortalHome({ commit, dispatch }) {
    commit('SET_IFRAME_SYNC_SUSPENDED', true)
    commit('SET_PRESERVE_DOCK_TABS', true)
    commit('SET_PORTAL_PATH_LINKS', {})
    return dispatch('tagsView/prunePortalHomeViews', null, { root: true }).then(() => {
      return goPortalIndex().finally(() => {
        commit('SET_IFRAME_SYNC_SUSPENDED', false)
        commit('SET_PRESERVE_DOCK_TABS', false)
      })
    })
  },

  closePortalTab({ commit, dispatch, rootState }, { tab, active }) {
    commit('SET_IFRAME_SYNC_SUSPENDED', true)
    return dispatch('tagsView/delView', tab, { root: true }).then(() => {
      if (!active) {
        commit('SET_IFRAME_SYNC_SUSPENDED', false)
        return
      }
      const remaining = rootState.tagsView.visitedViews.filter(view => {
        if (view.path === '/index' || view.path === '/') return false
        if (!view.title || !view.name) return false
        if (view.title === '外部系统') return false
        if (view.meta && view.meta.portalHome) return false
        if (isPortalSubSystemHomePath(view.path)) return false
        return true
      })
      if (remaining.length > 0) {
        commit('SET_IFRAME_SYNC_SUSPENDED', false)
        const nextTab = remaining[remaining.length - 1]
        return router.push(nextTab.fullPath || nextTab.path)
      }
      return dispatch('returnToPortalHome')
    }).catch(err => {
      commit('SET_IFRAME_SYNC_SUSPENDED', false)
      return Promise.reject(err)
    })
  },

  goPortal({ state, dispatch }, payload) {
    const clientId = resolvePortalClientId(state, payload)
    if (!clientId) {
      return Promise.reject(new Error('无效的外部系统'))
    }
    const path = (typeof payload === 'object' && payload.path)
      || state.subSystemEntryPaths[clientId]
      || '/index'
    if (path === '/index' || isPortalSubSystemHomePath(path)) {
      return dispatch('navigateToPortalHome')
    }
    if (router.currentRoute.path === path) {
      return Promise.resolve(path)
    }
    return navigateReplace(path)
  },

  ensureSubSystemLoaded({ dispatch, state }, clientId) {
    const key = normalizeSystemKey(clientId)
    if (state.loadedSubSystems[key]) {
      return dispatch('activateSubSystem', key).then(() => state.subSystemEntryPaths[key])
    }
    if (state.loadingSubSystems[key]) {
      return state.loadingSubSystems[key]
    }
    const task = dispatch('loadSubSystemPortal', key).finally(() => {
      const nextMap = { ...state.loadingSubSystems }
      delete nextMap[key]
      state.loadingSubSystems = nextMap
    })
    state.loadingSubSystems = {
      ...state.loadingSubSystems,
      [key]: task
    }
    return task
  },

  loadSubSystemPortal({ commit, dispatch, state, rootState }, clientId) {
    const key = normalizeSystemKey(clientId)
    const ensureList = state.systemList.length > 0
      ? Promise.resolve()
      : dispatch('loadSystemList')
    return ensureList.then(() => {
      const target = findSystemByClientId(state, key)
      if (!target) {
        return Promise.reject(new Error('无权访问该外部系统'))
      }
      const subSystemId = Number(target.subSystemId)
      return getMyPortalMenus(subSystemId).then(res => {
        const menus = res.data || []
        if (menus.length === 0) {
          return Promise.reject(new Error('该外部系统暂无可用菜单'))
        }
        const portalHome = null
        const signature = buildMenuSignature(menus, portalHome)
        dispatch('ensureMainSidebarCached')
        return dispatch('GenerateSubSystemRoutes', {
          subSystemId,
          clientId: target.clientId,
          menus,
          portalHome
        }, { root: true }).then(({ nestedRewrite }) => {
          const sidebarRouters = rootState.permission.sidebarRouters
          const pathLinkMap = sanitizePathLinkMap(buildPortalPathLinkMap(sidebarRouters))
          if (Object.keys(pathLinkMap).length === 0) {
            return Promise.reject(new Error('外部系统菜单未配置有效链接'))
          }
          const entryPath = '/index'
          commit('SET_PORTAL_PATH_LINKS', pathLinkMap)
          commit('SET_SUB_SYSTEM_PATH_LINK_CACHE', { clientId: target.clientId, pathLinkMap })
          commit('MARK_SUB_SYSTEM_LOADED', { clientId: target.clientId, signature, entryPath })
          commit('SET_SUB_SYSTEM_SIDEBAR_CACHE', {
            clientId: target.clientId,
            sidebarRouters
          })
          commit('SET_CURRENT_SYSTEM', target.clientId)
          return entryPath
        })
      })
    })
  },

  activateSubSystem({ commit, state, dispatch }, clientId) {
    const key = normalizeSystemKey(clientId)
    commit('SET_CURRENT_SYSTEM', key)
    const pathLinkMap = state.subSystemPathLinkCache[key]
    if (pathLinkMap) {
      commit('SET_PORTAL_PATH_LINKS', sanitizePathLinkMap(pathLinkMap))
    }
    const sidebarRouters = state.subSystemSidebarCache[key]
    if (sidebarRouters && sidebarRouters.length) {
      commit('SET_SIDEBAR_ROUTERS', sidebarRouters, { root: true })
      return Promise.resolve(state.subSystemEntryPaths[key])
    }
    return dispatch('loadSubSystemPortal', key)
  },

  enterSubSystem({ dispatch, state }, payload) {
    const parsed = parseEnterSubSystemPayload(payload)
    const ensureList = state.systemList.length > 0
      ? Promise.resolve()
      : dispatch('loadSystemList')
    return ensureList.then(() => {
      const ref = resolveSystemRef(state, parsed.clientId || parsed.subSystemId)
      if (!ref) {
        return Promise.reject(new Error('无效的外部系统'))
      }
      const clientId = ref.clientId
      return dispatch('ensureSubSystemLoaded', clientId).then(entryPath => {
        return dispatch('runSilentSso', clientId).then(() => entryPath)
      }).then(entryPath => {
        if (parsed.stayOnPortalHome) {
          return entryPath
        }
        return dispatch('tagsView/keepPortalViews', clientId, { root: true }).then(() => entryPath)
      }).then(entryPath => {
        if (parsed.navigate === false) {
          return entryPath
        }
        return dispatch('navigateToPortalHome')
      })
    })
  },

  runSilentSso({ commit, state }, clientId) {
    const key = normalizeSystemKey(clientId)
    if (state.ssoDone[key]) {
      return Promise.resolve()
    }
    const ref = findSystemByClientId(state, key)
    if (!ref) {
      return Promise.reject(new Error('无效的外部系统'))
    }
    const oauthClientId = ref.clientId
    const redirectUri = `${window.location.origin}/scada/sso/callback`
    const scopes = ['user.read']
    return authorize('code', oauthClientId, redirectUri, oauthClientId, true, scopes, []).then(res => {
      const href = res && res.data
      if (!href) {
        return Promise.reject(new Error('SSO 自动授权未通过，请检查 OAuth2 应用 scada 的 redirect_uris 与 auto_approve_scopes'))
      }
      return loadHiddenSsoIframe(href)
    }).then(() => {
      commit('MARK_SSO_DONE', key)
    })
  },

  preAuthSso({ dispatch }, system) {
    if (!system || (!system.clientId && !system.subSystemId)) {
      return Promise.resolve()
    }
    const clientId = system.clientId || system.subSystemId
    return dispatch('runSilentSso', clientId).catch(() => {})
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

function loadHiddenSsoIframe(src) {
  return new Promise((resolve, reject) => {
    const iframe = document.createElement('iframe')
    iframe.style.cssText = 'position:fixed;width:0;height:0;border:0;opacity:0;pointer-events:none'
    let finished = false
    const cleanup = () => {
      window.removeEventListener('message', onMessage)
      clearTimeout(timer)
      if (iframe.parentNode) {
        iframe.parentNode.removeChild(iframe)
      }
    }
    const finish = (success, err) => {
      if (finished) {
        return
      }
      finished = true
      cleanup()
      if (success) {
        resolve()
      } else {
        reject(err || new Error('SCADA SSO 登录失败'))
      }
    }
    const onMessage = (event) => {
      const data = event && event.data
      if (!data || data.type !== 'scada-sso-done') {
        return
      }
      finish(!!data.success, data.success ? null : new Error('SCADA SSO 登录失败，请确认门户账号在 SCADA 中存在'))
    }
    window.addEventListener('message', onMessage)
    const timer = setTimeout(() => {
      finish(false, new Error('SCADA SSO 登录超时'))
    }, 20000)
    iframe.onerror = () => finish(false, new Error('SCADA SSO iframe 加载失败'))
    document.body.appendChild(iframe)
    iframe.src = src
  })
}

function isPortalIndexPath(path) {
  return path === '/index' || path === '/'
}

function isNavigationFailure(err) {
  if (!err) {
    return true
  }
  if (err.name === 'NavigationDuplicated') {
    return true
  }
  const message = err.message || ''
  return message.indexOf('NavigationDuplicated') >= 0 ||
    message.indexOf('Avoided redundant navigation') >= 0
}

function goPortalIndex() {
  if (isPortalIndexPath(router.currentRoute.path)) {
    return Promise.resolve(router.currentRoute)
  }
  return navigateReplace('/index')
}

function navigateReplace(path) {
  return router.replace({ path }).catch(err => {
    if (isNavigationFailure(err)) {
      return Promise.resolve(router.currentRoute)
    }
    console.error('[portal] navigate failed:', path, err)
    return Promise.reject(new Error(`无法打开页面: ${path}`))
  })
}

function sanitizePathLinkMap(pathLinkMap) {
  const result = {}
  Object.keys(pathLinkMap || {}).forEach(path => {
    if (isPortalSubSystemHomePath(path)) {
      return
    }
    result[path] = pathLinkMap[path]
  })
  return result
}

function buildPortalPathLinkMap(routes, parentPath = '', map = {}) {
  ;(routes || []).forEach(route => {
    const currentPath = joinPortalMenuPath(parentPath, route.path)
    if (route.meta && route.meta.portalHome) {
      return
    }
    if (isPortalSubSystemHomePath(currentPath)) {
      return
    }
    if (route.meta && route.meta.link) {
      map[currentPath] = {
        link: route.meta.link,
        title: route.meta.title || route.name || '外部系统',
        icon: route.meta.icon
      }
    }
    if (route.children && route.children.length) {
      buildPortalPathLinkMap(route.children, currentPath, map)
    }
  })
  return map
}

function joinPortalMenuPath(base, segment) {
  if (!segment) {
    return base || ''
  }
  if (segment.startsWith('/')) {
    return segment.replace(/\/+/g, '/')
  }
  const normalizedBase = (base || '').replace(/\/+$/, '')
  return `${normalizedBase}/${segment}`.replace(/\/+/g, '/')
}

function parseSsoParams(ssoUrl) {
  try {
    const absoluteUrl = ssoUrl.startsWith('http')
      ? ssoUrl
      : `${window.location.origin}${ssoUrl}`
    const url = new URL(absoluteUrl)
    const scope = url.searchParams.get('scope') || 'user.read'
    return {
      responseType: url.searchParams.get('response_type') || 'code',
      clientId: url.searchParams.get('client_id'),
      redirectUri: url.searchParams.get('redirect_uri'),
      state: url.searchParams.get('state') || undefined,
      scopes: scope.split(' ').filter(Boolean)
    }
  } catch (e) {
    return null
  }
}

function parseEnterSubSystemPayload(payload) {
  if (typeof payload === 'object' && payload !== null) {
    return {
      clientId: payload.clientId,
      subSystemId: payload.subSystemId,
      navigate: payload.navigate !== false,
      stayOnPortalHome: payload.stayOnPortalHome === true
    }
  }
  return {
    clientId: payload,
    subSystemId: undefined,
    navigate: true,
    stayOnPortalHome: false
  }
}

function normalizeSystemKey(system) {
  return system === 'main' ? 'main' : String(system)
}

function findSystemByClientId(state, clientId) {
  return (state.systemList || []).find(item => item.clientId === clientId) || null
}

function findSystemBySubSystemId(state, subSystemId) {
  return (state.systemList || []).find(item => Number(item.subSystemId) === Number(subSystemId)) || null
}

function resolveSystemRef(state, ref) {
  if (ref === 'main') {
    return { key: 'main', clientId: 'main' }
  }
  if (ref === null || ref === undefined || ref === '') {
    return null
  }
  if (typeof ref === 'string') {
    const sys = findSystemByClientId(state, ref)
    if (sys) {
      return {
        key: sys.clientId,
        clientId: sys.clientId,
        subSystemId: Number(sys.subSystemId),
        system: sys
      }
    }
    return null
  }
  const num = Number(ref)
  if (!Number.isNaN(num)) {
    const sys = findSystemBySubSystemId(state, num)
    if (sys) {
      return {
        key: sys.clientId,
        clientId: sys.clientId,
        subSystemId: num,
        system: sys
      }
    }
  }
  return null
}

function resolvePortalClientId(state, payload) {
  if (typeof payload === 'object' && payload !== null) {
    if (payload.clientId) {
      return normalizeSystemKey(payload.clientId)
    }
    if (payload.subSystemId !== undefined && payload.subSystemId !== null) {
      const sys = findSystemBySubSystemId(state, payload.subSystemId)
      return sys ? sys.clientId : null
    }
    return null
  }
  const ref = resolveSystemRef(state, payload)
  return ref && ref.clientId !== 'main' ? ref.clientId : null
}

function buildMenuSignature(menus, portalHome) {
  const ids = []
  const walk = list => {
    ;(list || []).forEach(item => {
      ids.push(item.id)
      walk(item.children)
    })
  }
  walk(menus)
  if (portalHome && portalHome.link) {
    ids.push('home')
  }
  return ids.sort((a, b) => String(a).localeCompare(String(b))).join(',')
}

function persistPortalSystemChoice(system) {
  if (system) {
    cache.session.set(PORTAL_LAST_SYSTEM_KEY, system)
  }
}

function clearPortalSystemChoice() {
  cache.session.remove(PORTAL_LAST_SYSTEM_KEY)
}

function buildPortalHomeConfig(target, menus) {
  if (target.homeMenuId) {
    const link = findMenuLinkById(menus, target.homeMenuId)
    if (link) {
      return null
    }
  }
  const link = target.homePageUrl || target.systemUrl
  if (!link) {
    return null
  }
  return {
    title: target.homePageName || '首页',
    link
  }
}

function findMenuLinkById(menus, menuId) {
  for (const menu of menus || []) {
    if (menu.id === menuId) {
      if (menu.link) {
        return menu.link
      }
      return findMenuLinkById(menu.children, menuId)
    }
    const childLink = findMenuLinkById(menu.children, menuId)
    if (childLink) {
      return childLink
    }
  }
  return null
}
