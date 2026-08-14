import router, { constantRoutes } from '@/router'
import { getMyExternalSystemList, getMyPortalMenus, getMyPortalMenusVersion } from '@/api/system/subSystemUsers'
import { getUserPortalDefault } from '@/api/system/user/portalDefault'
import { getUserQuickNavList } from '@/api/system/user/quickNav'
import { getSubSystemUserQuickNavList } from '@/api/system/user/subSystemQuickNav'
import {
  isPortalSubSystemHomePath,
  isMainBusinessPath,
  parsePortalClientId,
  normalizeSubsystemIframeLink,
  isGenericPortalTitle,
  resolvePortalMenuTitle
} from '@/utils/portalRoute'
import {
  loadPersistedPortalCache,
  persistPortalCache,
  clearPersistedPortalCache
} from '@/utils/portalMenuCache'
import { resolveRuleBasedPortalDefault } from '@/utils/portalSubsystem'
import {
  buildQuickNavScopeKey,
  getQuickNavCache,
  setQuickNavCache,
  clearAllQuickNavCache
} from '@/utils/portalQuickNavCache'
import {
  collectCamstarPrefetchEntries,
  prepareCamstarSessionFromEntries
} from '@/utils/camstarPrefetch'
import cache from '@/plugins/cache'

const PORTAL_LAST_SYSTEM_KEY = 'portal_last_system'

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

function snapshotPortalCache(state) {
  return {
    currentSystem: state.currentSystem,
    loadedSubSystems: state.loadedSubSystems,
    subSystemMenuSignatures: state.subSystemMenuSignatures,
    subSystemRbacVersions: state.subSystemRbacVersions,
    subSystemEntryPaths: state.subSystemEntryPaths,
    subSystemSidebarCache: state.subSystemSidebarCache,
    subSystemPathLinkCache: state.subSystemPathLinkCache
  }
}

function createInitialPortalState() {
  const persisted = loadPersistedPortalCache()
  const lastChoice = cache.session.get(PORTAL_LAST_SYSTEM_KEY)
  // 刷新：优先恢复会话里当前系统；无会话记录时先落主系统，再由 bootstrap 按登录默认系统切
  let currentSystem = 'main'
  if (lastChoice === 'main') {
    currentSystem = 'main'
  } else if (lastChoice) {
    currentSystem = lastChoice
  } else if (persisted && persisted.currentSystem) {
    currentSystem = persisted.currentSystem
  }
  let pathLinkMap = {}
  if (currentSystem !== 'main' && persisted && persisted.subSystemPathLinkCache) {
    const cached = persisted.subSystemPathLinkCache[currentSystem]
    if (cached) {
      pathLinkMap = sanitizePathLinkMap(cached)
    }
  }
  // 刷新不恢复侧栏菜单树（F5 后重拉 my-menus）；pathLinkMap 可恢复以免 iframe 短暂白屏
  return {
    currentSystem,
    systemList: [],
    mainSidebarRouters: null,
    loadedSubSystems: {},
    subSystemMenuSignatures: {},
    /** clientId → 主系统 RBAC 版本；进入子系统时比对，变化才重拉 my-menus */
    subSystemRbacVersions: {},
    subSystemEntryPaths: (persisted && persisted.subSystemEntryPaths) || {},
    subSystemSidebarCache: {},
    subSystemPathLinkCache: (persisted && persisted.subSystemPathLinkCache) || {},
    pathLinkMap,
    loadingSubSystems: {},
    quickNavInFlight: {},
    /** 快捷导航拉取代数：保存/强制刷新后递增，丢弃过期 GET，防止旧响应盖掉新数据 */
    quickNavLoadEpoch: {},
    iframeSyncSuspended: false,
    preserveDockTabs: false,
    portalBootstrapped: false,
    /** 单飞：并发 beforeEach 共用同一个 bootstrap Promise */
    bootstrapInFlight: null,
    portalDefaultCache: null
  }
}

/** 切换系统时递增，用于作废尚未执行的后台预热，防止抢回默认子系统 */
let portalWarmGeneration = 0

function bumpPortalWarmGeneration() {
  portalWarmGeneration += 1
  return portalWarmGeneration
}

const state = createInitialPortalState()

const mutations = {
  SET_CURRENT_SYSTEM(state, system) {
    state.currentSystem = system
    // 任意切壳路径都写入会话，避免只改 Vuex/cache 导致 F5 仍按旧 last_system 跳回
    persistPortalSystemChoice(system)
    persistPortalCache(snapshotPortalCache(state))
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
  SET_BOOTSTRAP_IN_FLIGHT(state, task) {
    state.bootstrapInFlight = task || null
  },
  SET_PORTAL_DEFAULT_CACHE(state, config) {
    state.portalDefaultCache = config || null
  },
  MARK_SUB_SYSTEM_LOADED(state, { clientId, signature, entryPath, rbacVersion }) {
    state.loadedSubSystems = {
      ...state.loadedSubSystems,
      [clientId]: true
    }
    state.subSystemMenuSignatures = {
      ...state.subSystemMenuSignatures,
      [clientId]: signature
    }
    if (rbacVersion != null) {
      state.subSystemRbacVersions = {
        ...state.subSystemRbacVersions,
        [clientId]: Number(rbacVersion) || 0
      }
    }
    if (entryPath) {
      state.subSystemEntryPaths = {
        ...state.subSystemEntryPaths,
        [clientId]: entryPath
      }
    }
    persistPortalCache(snapshotPortalCache(state))
  },
  SET_SUB_SYSTEM_SIDEBAR_CACHE(state, { clientId, sidebarRouters }) {
    state.subSystemSidebarCache = {
      ...state.subSystemSidebarCache,
      [clientId]: sidebarRouters
    }
    persistPortalCache(snapshotPortalCache(state))
  },
  SET_SUB_SYSTEM_PATH_LINK_CACHE(state, { clientId, pathLinkMap }) {
    state.subSystemPathLinkCache = {
      ...state.subSystemPathLinkCache,
      [clientId]: pathLinkMap
    }
    persistPortalCache(snapshotPortalCache(state))
  },
  RESET_PORTAL(state) {
    state.currentSystem = 'main'
    state.systemList = []
    state.mainSidebarRouters = null
    state.loadedSubSystems = {}
    state.subSystemMenuSignatures = {}
    state.subSystemRbacVersions = {}
    state.subSystemEntryPaths = {}
    state.subSystemSidebarCache = {}
    state.subSystemPathLinkCache = {}
    state.pathLinkMap = {}
    state.loadingSubSystems = {}
    state.quickNavInFlight = {}
    state.quickNavLoadEpoch = {}
    state.iframeSyncSuspended = false
    state.preserveDockTabs = false
    state.portalBootstrapped = false
    state.bootstrapInFlight = null
    state.portalDefaultCache = null
    bumpPortalWarmGeneration()
    clearPersistedPortalCache()
    clearPortalSystemChoice()
    // 换人/登出必须清快捷导航 session，避免串用户或无 apps 的旧缓存
    clearAllQuickNavCache()
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
   * 快捷导航单飞：Shell / 首页 Panel / bootstrap 预取共用，避免同屏双请求
   * @param payload.force 强制新开请求；配合 epoch 丢弃保存前发出的过期响应
   */
  loadQuickNavConfig({ state }, payload = {}) {
    const subSystemId = Number(payload.subSystemId) || 0
    const force = !!payload.force
    const cacheKey = subSystemId > 0
      ? buildQuickNavScopeKey('x', subSystemId)
      : 'main'
    if (!force && state.quickNavInFlight[cacheKey]) {
      return state.quickNavInFlight[cacheKey]
    }
    const epoch = (state.quickNavLoadEpoch[cacheKey] || 0) + 1
    state.quickNavLoadEpoch = {
      ...state.quickNavLoadEpoch,
      [cacheKey]: epoch
    }
    const request = subSystemId > 0
      ? getSubSystemUserQuickNavList(subSystemId)
      : getUserQuickNavList()
    const task = request.then(res => {
      // 过期响应：绝不回写 stale data，否则保存加星后会被旧 GET 立刻盖掉
      if (epoch !== state.quickNavLoadEpoch[cacheKey]) {
        if (state.quickNavInFlight[cacheKey] && state.quickNavInFlight[cacheKey] !== task) {
          return state.quickNavInFlight[cacheKey]
        }
        const cached = getQuickNavCache(cacheKey)
        if (cached && Array.isArray(cached.apps)) {
          return {
            menuIds: cached.menuIds || [],
            configured: !!cached.configured,
            lockedMenuIds: cached.lockedMenuIds || [],
            apps: cached.apps
          }
        }
        return { menuIds: [], configured: false, lockedMenuIds: [], apps: [] }
      }
      const raw = (res && res.data) || {}
      // 有 apps 字段则信接口（含空数组）；无字段视为旧后端，apps=null 允许侧栏过渡
      const hasAppsField = Object.prototype.hasOwnProperty.call(raw, 'apps')
      const apps = hasAppsField
        ? (Array.isArray(raw.apps) ? raw.apps : [])
        : null
      const config = {
        menuIds: raw.menuIds || [],
        configured: !!raw.configured,
        lockedMenuIds: raw.lockedMenuIds || [],
        apps
      }
      setQuickNavCache(
        cacheKey,
        config.menuIds,
        config.configured,
        config.lockedMenuIds,
        config.apps
      )
      return config
    }).finally(() => {
      if (state.quickNavInFlight[cacheKey] === task) {
        const next = { ...state.quickNavInFlight }
        delete next[cacheKey]
        state.quickNavInFlight = next
      }
    })
    state.quickNavInFlight = {
      ...state.quickNavInFlight,
      [cacheKey]: task
    }
    return task
  },

  /**
   * 应用保存接口返回的权威快捷导航（使当前 GET 代数失效，避免旧 list 回写）
   */
  applyQuickNavConfig({ state }, payload = {}) {
    const subSystemId = Number(payload.subSystemId) || 0
    const cacheKey = subSystemId > 0
      ? buildQuickNavScopeKey('x', subSystemId)
      : 'main'
    const raw = payload.config || {}
    const hasAppsField = Object.prototype.hasOwnProperty.call(raw, 'apps')
    const config = {
      menuIds: raw.menuIds || [],
      configured: !!raw.configured,
      lockedMenuIds: raw.lockedMenuIds || [],
      apps: hasAppsField ? (Array.isArray(raw.apps) ? raw.apps : []) : null
    }
    state.quickNavLoadEpoch = {
      ...state.quickNavLoadEpoch,
      [cacheKey]: (state.quickNavLoadEpoch[cacheKey] || 0) + 1
    }
    setQuickNavCache(
      cacheKey,
      config.menuIds,
      config.configured,
      config.lockedMenuIds,
      config.apps
    )
    return config
  },

  /**
   * 读取/缓存用户星标默认系统（PortalSystemSwitch 依赖此 action）
   */
  fetchPortalDefault({ state, commit }, options = {}) {
    if (!options.force && state.portalDefaultCache) {
      return Promise.resolve(state.portalDefaultCache)
    }
    return getUserPortalDefault().then(res => {
      const data = (res && res.data) || {}
      const config = {
        configured: !!data.configured,
        subSystemId: data.subSystemId != null ? data.subSystemId : null,
        defaultSystem: data.defaultSystem || 'main'
      }
      commit('SET_PORTAL_DEFAULT_CACHE', config)
      return config
    })
  },

  /**
   * 星标「登录后默认打开」仅写入服务端配置；不切当前壳、不改会话 last_system。
   * 当前会话在哪个系统，就留在哪个系统；刷新也跟 last_system，只有重新登录才用星标默认。
   */
  rememberSystemChoice() {
    return Promise.resolve()
  },

  /**
   * 门户加载产品约定（跨界快捷导航，菜单数据均在主系统）：
   * 1) 首次进入 = 默认系统：先加载该系统快捷导航（后端 Redis → 未命中再现场重建写回），
   *    全量菜单后台懒加载，绝不挡卡片。
   * 2) 切换系统：同样先查快捷导航（Redis / 重建），再切壳出卡片；全量菜单后台懒加载。
   * 3) 主系统、子系统同一套节奏，只换 subSystemId（0=主）。
   */
  bootstrapAfterAuth({ commit, dispatch, state }) {
    if (state.portalBootstrapped) {
      return Promise.resolve()
    }
    if (state.bootstrapInFlight) {
      return state.bootstrapInFlight
    }
    // 捕获世代：await 期间用户若已手动切系统，后续 apply 必须放弃
    const applyGen = portalWarmGeneration
    const task = Promise.all([
      dispatch('loadSystemList'),
      dispatch('fetchPortalDefault', { force: true }).catch(() => ({}))
    ]).then(([list, defaultConfig]) => {
      const targetSystem = resolveBootstrapTargetSystem(list, defaultConfig)
      const subSystemId = resolveQuickNavSubSystemIdFromList(list, targetSystem)
      // 默认系统快捷导航与切壳并行；都完成再放行首页，避免先进页再等卡片
      // 只等 quick-nav（通常 Redis），不等全量菜单
      return Promise.all([
        dispatch('loadQuickNavConfig', { subSystemId }).catch(err => {
          console.warn('[portal] bootstrap quick-nav prefetch failed:', err)
          return {}
        }),
        dispatch('applyPortalBootstrapSystem', { list, defaultConfig, applyGen })
      ])
    }).catch(err => {
      console.warn('[portal] bootstrapAfterAuth failed:', err)
    }).finally(() => {
      commit('SET_PORTAL_BOOTSTRAPPED', true)
      commit('SET_BOOTSTRAP_IN_FLIGHT', null)
    })
    commit('SET_BOOTSTRAP_IN_FLIGHT', task)
    return task
  },

  applyPortalBootstrapSystem({ dispatch, state }, { list, defaultConfig, applyGen }) {
    const isStale = () => applyGen != null && applyGen !== portalWarmGeneration
    if (isStale()) {
      return Promise.resolve()
    }
    const currentPath = router.currentRoute && router.currentRoute.path
    const onPortalHome = !currentPath || currentPath === '/' || currentPath === '/index'
    // 刷新落在主业务页：壳固定主系统并写入会话，不抢路由到子系统
    if (!onPortalHome && isMainBusinessPath(currentPath)) {
      if (isStale()) {
        return Promise.resolve()
      }
      if (state.currentSystem !== 'main') {
        return dispatch('enterMainSystem', { stayOnPortalHome: false, skipNavigate: true })
      }
      persistPortalSystemChoice('main')
      return Promise.resolve()
    }
    // 刷新落在子系统业务页：以 URL 为准同步壳/会话，菜单/SSO 仍由路由守卫拉取
    const routeClientId = !onPortalHome ? parsePortalClientId(currentPath) : null
    if (routeClientId) {
      if (isStale()) {
        return Promise.resolve()
      }
      if (state.currentSystem !== routeClientId) {
        return dispatch('activateSubSystemShell', { clientId: routeClientId })
      }
      persistPortalSystemChoice(routeClientId)
      return Promise.resolve()
    }
    const targetSystem = resolveBootstrapTargetSystem(list, defaultConfig)
    if (isStale()) {
      return Promise.resolve()
    }
    // 已在目标系统：只做后台预热，禁止重复 SET 造成芯片闪动
    if (state.currentSystem === targetSystem) {
      if (targetSystem === 'main') {
        schedulePortalBootstrap(() => dispatch('warmMainSystemInBackground'))
      } else {
        schedulePortalBootstrap(() => dispatch('warmSubSystemDefaultInBackground', targetSystem))
      }
      return Promise.resolve()
    }
    if (targetSystem === 'main') {
      return dispatch('enterMainSystem', { stayOnPortalHome: true, skipNavigate: true }).then(() => {
        if (isStale()) {
          return
        }
        schedulePortalBootstrap(() => dispatch('warmMainSystemInBackground'))
      })
    }
    return dispatch('activateSubSystemShell', {
      clientId: targetSystem
    }).then(() => {
      if (isStale()) {
        return
      }
      schedulePortalBootstrap(() => dispatch('warmSubSystemDefaultInBackground', targetSystem))
    })
  },

  /**
   * 后台：主系统角色/菜单/权限。
   * 先只读 Redis；未命中再延迟打库，避免与工作台首屏接口抢连接导致 30s 超时。
   */
  warmMainSystemInBackground({ dispatch, rootState }) {
    if (rootState.permission.defaultRoutes && rootState.permission.defaultRoutes.length) {
      return Promise.resolve()
    }
    return dispatch('LoadMainMenus', { redisOnly: true }, { root: true }).then(routes => {
      if (routes) {
        return routes
      }
      return new Promise(resolve => {
        setTimeout(() => {
          dispatch('LoadMainMenus', { redisOnly: false }, { root: true })
            .catch(err => {
              console.warn('[portal] warm main menus/perm failed:', err)
            })
            .then(resolve)
        }, 8000)
      })
    }).catch(err => {
      console.warn('[portal] warm main menus redis peek failed:', err)
    })
  },

  /**
   * 后台：子系统 my-menus/权限包，以及主系统角色菜单权限。
   * 只预热缓存，不抢当前壳；用户已切走时作废本次预热。
   */
  warmSubSystemDefaultInBackground({ dispatch }, clientId) {
    const key = normalizeSystemKey(clientId)
    const warmGen = portalWarmGeneration
    const subWarm = new Promise(resolve => {
      setTimeout(() => {
        if (warmGen !== portalWarmGeneration) {
          resolve()
          return
        }
        dispatch('ensureSubSystemLoaded', { clientId: key, activate: false })
          .catch(err => {
            console.warn('[portal] warm sub menus/perm failed:', err)
          })
          .then(resolve)
      }, 3000)
    })
    const mainWarm = dispatch('warmMainSystemInBackground')
    return Promise.all([subWarm, mainWarm])
  },

  /**
   * 只切换「当前系统」标记与已有缓存侧栏，不等待 my-menus / SSO。
   * 必须同步完成：禁止 await 任何请求，否则点菜单会被卡住、全屏 loading 锁死交互。
   */
  activateSubSystemShell({ commit, dispatch, state }, { clientId, skipPersist }) {
    const key = normalizeSystemKey(clientId)
    if (!skipPersist) {
      persistPortalSystemChoice(key)
    }
    commit('SET_CURRENT_SYSTEM', key)
    const cachedSidebar = state.subSystemSidebarCache[key]
    if (cachedSidebar && cachedSidebar.length) {
      commit('SET_SIDEBAR_ROUTERS', cachedSidebar, { root: true })
    }
    const cachedLinks = state.subSystemPathLinkCache[key]
    if (cachedLinks) {
      commit('SET_PORTAL_PATH_LINKS', sanitizePathLinkMap(cachedLinks))
    }
    // 主系统侧栏快照后台做，绝不挡本次打开
    dispatch('cacheMainSidebar').catch(() => {})
    return Promise.resolve(key)
  },

  /**
   * 兼容旧调用：后台预热子系统菜单/SSO（不阻塞首屏）
   */
  preloadSubSystemInBackground({ dispatch }, clientId) {
    return dispatch('warmSubSystemDefaultInBackground', clientId)
  },

  /**
   * 进入 / 切换到子系统时判断权限版本：变化才重拉 my-menus，否则复用内存树。
   * 签名兼容旧调用（可传 clientId 或 { clientId, skipSso }，skipSso 现为 noop）。
   */
  ensureSubSystemReady({ dispatch, state, commit }, clientIdOrOpts) {
    const opts = (typeof clientIdOrOpts === 'object' && clientIdOrOpts !== null) ? clientIdOrOpts : { clientId: clientIdOrOpts }
    const key = normalizeSystemKey(opts.clientId)
    return dispatch('resolveSubSystemReload', key).then(force => {
      return dispatch('ensureSubSystemLoaded', { clientId: key, activate: true, force })
        .then(() => key)
    })
  },

  /**
   * 与主系统 RBAC 版本比对：未加载或版本变化 → 需要重拉；否则复用内存树。
   */
  resolveSubSystemReload({ state }, clientId) {
    const key = normalizeSystemKey(clientId)
    if (!state.loadedSubSystems[key] || !state.subSystemSidebarCache[key]
        || !state.subSystemSidebarCache[key].length) {
      return Promise.resolve(true)
    }
    const target = findSystemByClientId(state, key)
    if (!target || target.subSystemId == null) {
      return Promise.resolve(true)
    }
    const localVersion = state.subSystemRbacVersions[key]
    return getMyPortalMenusVersion(Number(target.subSystemId)).then(res => {
      const remoteVersion = Number(res.data)
      const remote = Number.isFinite(remoteVersion) ? remoteVersion : 0
      if (localVersion == null || Number(localVersion) !== remote) {
        return true
      }
      return false
    }).catch(() => {
      // 版本比对请求失败（网络抖动/超时）时，若本地已有缓存版本则信任本地缓存，避免单次抖动误走
      // 全量重拉+iframe重载的慢路径（表现为“同一页面有时快有时慢”）。
      // 仅当本地从未拿到过版本号时，才保守地触发一次重拉以兜底。
      return localVersion == null
    })
  },

  /** 门户首页不预拉菜单；进入业务页时由 ensureSubSystemReady 负责 */
  ensureCurrentSubSystemMenusLoaded() {
    return Promise.resolve()
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

  /**
   * 切换系统：先快捷导航（主系统 Redis），再切壳；全量菜单后台懒加载。
   * 与首次进入默认系统同一节奏，禁止等 8s 全量菜单才出卡片。
   */
  switchSystem({ commit, dispatch, state }, payload) {
    const system = typeof payload === 'object' && payload !== null ? payload.system : payload
    const stayOnPortalHome = typeof payload === 'object' && payload !== null && payload.stayOnPortalHome === true
    const skipNavigate = typeof payload === 'object' && payload !== null && payload.skipNavigate === true
    const skipPersist = typeof payload === 'object' && payload !== null && payload.skipPersist === true

    bumpPortalWarmGeneration()

    if (system === 'main') {
      if (!skipPersist) {
        persistPortalSystemChoice('main')
      }
      // 先拉主系统快捷导航再切壳：切系统秒出卡片，绝不跟 8s 全量菜单绑在一起
      return dispatch('loadQuickNavConfig', { subSystemId: 0 }).catch(err => {
        console.warn('[portal] switch main quick-nav failed:', err)
        return {}
      }).then(() => dispatch('cacheMainSidebar')).then(() => {
        const switchingAway = state.currentSystem !== 'main'
        const enter = () => dispatch('enterMainSystem', {
          stayOnPortalHome: stayOnPortalHome || !skipNavigate,
          skipNavigate,
          clearDock: switchingAway
        })
        if (!switchingAway) {
          return enter().then(() => {
            schedulePortalBootstrap(() => dispatch('warmMainSystemInBackground'))
          })
        }
        commit('SET_IFRAME_SYNC_SUSPENDED', true)
        return dispatch('tagsView/clearDockBusinessTabs', null, { root: true }).then(() => {
          return enter().finally(() => {
            commit('SET_IFRAME_SYNC_SUSPENDED', false)
          })
        }).then(() => {
          schedulePortalBootstrap(() => dispatch('warmMainSystemInBackground'))
        })
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
      const subSystemId = Number(ref.subSystemId) || 0
      if (!skipPersist) {
        persistPortalSystemChoice(targetSystem)
      }
      // 先拉目标子系统快捷导航再切壳；全量 my-menus / SSO 仍后台
      return dispatch('loadQuickNavConfig', { subSystemId }).catch(err => {
        console.warn('[portal] switch sub quick-nav failed:', err)
        return {}
      }).then(() => dispatch('cacheMainSidebar')).then(() => {
        if (targetSystem === currentSystem) {
          schedulePortalBootstrap(() => dispatch('ensureSubSystemReady', targetSystem))
          if (skipNavigate) {
            return Promise.resolve()
          }
          return dispatch('navigateToPortalHome')
        }
        // 切系统：挂起 iframe/页签登记，清 dock + iframe，再回首页，避免旧路由把页签加回来
        commit('SET_IFRAME_SYNC_SUSPENDED', true)
        return dispatch('tagsView/clearDockBusinessTabs', null, { root: true }).then(() => {
          return dispatch('activateSubSystemShell', {
            clientId: targetSystem,
            skipPersist: true
          }).then(() => {
            schedulePortalBootstrap(() =>
              dispatch('ensureSubSystemReady', targetSystem).then(() =>
                dispatch('warmSubSystemDefaultInBackground', targetSystem)
              )
            )
            if (skipNavigate) {
              commit('SET_IFRAME_SYNC_SUSPENDED', false)
              return Promise.resolve()
            }
            return dispatch('navigateToPortalHome', { clearDock: true }).finally(() => {
              commit('SET_IFRAME_SYNC_SUSPENDED', false)
            })
          })
        })
      })
    })
  },

  enterMainSystem({ commit, dispatch, state, rootState }, payload) {
    const stayOnPortalHome = payload && payload.stayOnPortalHome
    const skipNavigate = payload && payload.skipNavigate
    const clearDock = payload && payload.clearDock === true
    const ensureMainMenus = () => {
      // 门户首页：快捷导航走独立接口，完整菜单树交给后台 warm，不挡首屏
      if (stayOnPortalHome) {
        return Promise.resolve()
      }
      if ((state.mainSidebarRouters && state.mainSidebarRouters.length)
          || (rootState.permission.defaultRoutes && rootState.permission.defaultRoutes.length)) {
        return Promise.resolve()
      }
      // 进入主系统业务页才允许打库构建菜单
      return dispatch('LoadMainMenus', { redisOnly: false }, { root: true }).then(() => {})
    }
    return ensureMainMenus().then(() => {
      commit('SET_CURRENT_SYSTEM', 'main')
      commit('SET_PORTAL_PATH_LINKS', {})
      if (state.mainSidebarRouters && state.mainSidebarRouters.length) {
        commit('SET_SIDEBAR_ROUTERS', state.mainSidebarRouters, { root: true })
      } else if (rootState.permission.defaultRoutes && rootState.permission.defaultRoutes.length) {
        commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(rootState.permission.defaultRoutes), { root: true })
      }
      if (skipNavigate) {
        return Promise.resolve()
      }
      if (stayOnPortalHome) {
        return dispatch('navigateToPortalHome', { clearDock })
      }
      return dispatch('tagsView/keepMainViews', null, { root: true }).then(() => {
        return dispatch('goMain')
      })
    })
  },

  goMain() {
    return goPortalIndex()
  },

  /** 回到门户 /index。clearDock=true：切系统场景，不保留上一系统页签 */
  navigateToPortalHome({ commit, dispatch, state }, payload) {
    const clearDock = payload && payload.clearDock === true
    if (state.currentSystem) {
      persistPortalSystemChoice(state.currentSystem)
    }
    // 切系统：禁止 preserve，落地 /index 时 beforeEach 还会再清一次
    commit('SET_PRESERVE_DOCK_TABS', !clearDock)
    if (clearDock) {
      commit('SET_IFRAME_SYNC_SUSPENDED', true)
    }
    const cachedLinks = state.currentSystem && state.currentSystem !== 'main'
      ? state.subSystemPathLinkCache[state.currentSystem]
      : null
    if (cachedLinks) {
      commit('SET_PORTAL_PATH_LINKS', sanitizePathLinkMap(cachedLinks))
    }
    const prune = clearDock
      ? dispatch('tagsView/clearDockBusinessTabs', null, { root: true })
      : dispatch('tagsView/prunePortalHomeViews', null, { root: true })
    return prune.then(() => {
      return goPortalIndex().finally(() => {
        if (clearDock) {
          // 路由已到首页后再清一次，杜绝旧 path 的 TagsView.addTags 回写
          return dispatch('tagsView/clearDockBusinessTabs', null, { root: true }).finally(() => {
            commit('SET_IFRAME_SYNC_SUSPENDED', false)
            commit('SET_PRESERVE_DOCK_TABS', false)
          })
        }
        commit('SET_PRESERVE_DOCK_TABS', false)
      })
    })
  },

  /** 关闭当前页签后回门户首页（不清理其它 dock 页签）；不改变当前系统 */
  returnToPortalHome({ commit, dispatch, state }) {
    if (state.currentSystem) {
      persistPortalSystemChoice(state.currentSystem)
    }
    commit('SET_IFRAME_SYNC_SUSPENDED', true)
    commit('SET_PRESERVE_DOCK_TABS', true)
    const cachedLinks = state.currentSystem && state.currentSystem !== 'main'
      ? state.subSystemPathLinkCache[state.currentSystem]
      : null
    if (cachedLinks) {
      commit('SET_PORTAL_PATH_LINKS', sanitizePathLinkMap(cachedLinks))
    }
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

  ensureSubSystemLoaded({ dispatch, state }, payload) {
    const key = normalizeSystemKey(
      payload && typeof payload === 'object' ? payload.clientId : payload
    )
    const activate = !(payload && typeof payload === 'object' && payload.activate === false)
    const force = !!(payload && typeof payload === 'object' && payload.force)
    const startGen = payload && typeof payload === 'object' && payload.startGen != null
      ? payload.startGen
      : portalWarmGeneration

    const startLoad = () => {
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
    }

    const ensureCached = () => {
      // 非强制：已加载则复用内存树（性能）
      if (!force && state.loadedSubSystems[key]) {
        return Promise.resolve(state.subSystemEntryPaths[key])
      }
      if (!force && state.loadingSubSystems[key]) {
        return state.loadingSubSystems[key]
      }
      // 强制刷新：若已有进行中的加载，等结束后再拉一次，保证拿到最新 my-menus
      if (force && state.loadingSubSystems[key]) {
        return state.loadingSubSystems[key].catch(() => null).then(() => startLoad())
      }
      return startLoad()
    }

    return ensureCached().then(entryPath => {
      if (!activate) {
        return entryPath
      }
      // 加载期间用户已切到其他系统：只保留缓存，禁止异步抢壳
      if (startGen !== portalWarmGeneration && state.currentSystem !== key) {
        return entryPath
      }
      return dispatch('activateSubSystem', { clientId: key, startGen })
        .then(() => state.subSystemEntryPaths[key] || entryPath)
    })
  },

  /**
   * 仅拉取并缓存子系统菜单/链路，不改变「当前系统」壳。
   */
  loadSubSystemPortal({ commit, dispatch, state }, clientId) {
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
        return Promise.all([
          dispatch('GenerateSubSystemRoutes', {
            subSystemId,
            clientId: target.clientId,
            menus,
            portalHome,
            applyToLive: false
          }, { root: true }),
          getMyPortalMenusVersion(subSystemId).catch(() => ({ data: 0 }))
        ]).then(([{ sidebarRoutes }, versionRes]) => {
          const sidebarRouters = sidebarRoutes || []
          const pathLinkMap = sanitizePathLinkMap(buildPortalPathLinkMap(sidebarRouters))
          if (Object.keys(pathLinkMap).length === 0) {
            return Promise.reject(new Error('外部系统菜单未配置有效链接'))
          }
          const entryPath = '/index'
          const rbacVersion = Number(versionRes && versionRes.data)
          commit('SET_SUB_SYSTEM_PATH_LINK_CACHE', { clientId: target.clientId, pathLinkMap })
          commit('MARK_SUB_SYSTEM_LOADED', {
            clientId: target.clientId,
            signature,
            entryPath,
            rbacVersion: Number.isFinite(rbacVersion) ? rbacVersion : 0
          })
          commit('SET_SUB_SYSTEM_SIDEBAR_CACHE', {
            clientId: target.clientId,
            sidebarRouters
          })
          // 若用户当前仍在该子系统，把缓存同步到活动侧栏/链路；绝不抢改 currentSystem
          if (state.currentSystem === target.clientId) {
            commit('SET_PORTAL_PATH_LINKS', pathLinkMap)
            commit('SET_SIDEBAR_ROUTERS', sidebarRouters, { root: true })
          }
          // 菜单就绪后后台预热 Camstar 页面壳（缩小与原生 4200 的首开差距）
          dispatch('prefetchCamstarShells', { clientId: target.clientId, pathLinkMap })
          return entryPath
        })
      })
    })
  },

  /**
   * 进入含 Camstar 的子系统后：本机 Cookie + 源站探活。
   * 禁止 CLEAR 保温 iframe（对齐 4200：已开页面用 v-show，再点应瞬间切回）。
   */
  prefetchCamstarShells(_ctx, { clientId, pathLinkMap, limit }) {
    const map = pathLinkMap || {}
    const entries = collectCamstarPrefetchEntries(map, limit == null ? 6 : limit)
    if (!entries.length) {
      return Promise.resolve(0)
    }
    return prepareCamstarSessionFromEntries(entries).then(count => {
      if (typeof console !== 'undefined' && console.log) {
        console.log(
          `%c[camstar-prefetch] cookie+origin client=${clientId || '-'} count=${count}`,
          'color:#909399',
          entries.map(e => e.path)
        )
      }
      return count
    })
  },

  activateSubSystem({ commit, state, dispatch }, payload) {
    const key = normalizeSystemKey(
      payload && typeof payload === 'object' ? payload.clientId : payload
    )
    const startGen = payload && typeof payload === 'object' && payload.startGen != null
      ? payload.startGen
      : undefined
    // 异步回调里若用户已切走，禁止把壳抢回去
    if (startGen != null && startGen !== portalWarmGeneration && state.currentSystem !== key) {
      return Promise.resolve(state.subSystemEntryPaths[key])
    }
    commit('SET_CURRENT_SYSTEM', key)
    const applyCachedShell = () => {
      const pathLinkMap = state.subSystemPathLinkCache[key]
      if (pathLinkMap) {
        commit('SET_PORTAL_PATH_LINKS', sanitizePathLinkMap(pathLinkMap))
        dispatch('prefetchCamstarShells', { clientId: key, pathLinkMap })
      }
      const sidebarRouters = state.subSystemSidebarCache[key]
      if (sidebarRouters && sidebarRouters.length) {
        commit('SET_SIDEBAR_ROUTERS', sidebarRouters, { root: true })
      }
      return state.subSystemEntryPaths[key]
    }
    if (state.loadedSubSystems[key] && state.subSystemSidebarCache[key]
        && state.subSystemSidebarCache[key].length) {
      return Promise.resolve(applyCachedShell())
    }
    return dispatch('ensureSubSystemLoaded', { clientId: key, activate: false }).then(() => applyCachedShell())
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
      // 停在门户首页：只切壳，不拉 my-menus / OAuth
      if (parsed.stayOnPortalHome) {
        return dispatch('activateSubSystemShell', {
          clientId,
          skipPersist: true
        }).then(() => {
          if (parsed.navigate === false) {
            return '/index'
          }
          return dispatch('navigateToPortalHome')
        })
      }
      return dispatch('ensureSubSystemReady', clientId).then(entryPath => {
        return dispatch('tagsView/keepPortalViews', clientId, { root: true }).then(() => entryPath)
      }).then(entryPath => {
        if (parsed.navigate === false) {
          return entryPath
        }
        return dispatch('navigateToPortalHome')
      })
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

function resolveBootstrapTargetSystem(systemList, defaultConfig) {
  const list = systemList || []
  // 同会话内：优先恢复用户当前所在系统（切换/关菜单后都靠它，绝不能回落到星标默认）
  const fromSession = cache.session.get(PORTAL_LAST_SYSTEM_KEY)
  const fromCache = (() => {
    const snap = loadPersistedPortalCache()
    return snap && snap.currentSystem ? snap.currentSystem : null
  })()
  const candidates = [fromSession, fromCache]
  for (let i = 0; i < candidates.length; i++) {
    const persisted = candidates[i]
    if (persisted === 'main') {
      return 'main'
    }
    if (persisted && list.some(item => item.clientId === persisted)) {
      return persisted
    }
  }
  // 仅「重新登录 / 会话已清空」时才用星标或规则默认
  return resolvePortalBootstrapSystem(defaultConfig, list)
}

/** 按系统 clientId 解析快捷导航接口所需的 subSystemId（主系统为 0） */
function resolveQuickNavSubSystemIdFromList(systemList, systemKey) {
  if (!systemKey || systemKey === 'main') {
    return 0
  }
  const sys = (systemList || []).find(item => item.clientId === systemKey)
  return sys && sys.subSystemId != null ? Number(sys.subSystemId) || 0 : 0
}

function resolvePortalBootstrapSystem(defaultConfig, systemList) {
  const list = systemList || []
  const ruleDefault = resolveRuleBasedPortalDefault(list)
  const preferred = (defaultConfig && defaultConfig.defaultSystem) || ruleDefault
  if (preferred === 'main') {
    return 'main'
  }
  return list.some(item => item.clientId === preferred) ? preferred : ruleDefault
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

function joinPortalMenuPath(base, segment) {
  if (!segment) {
    return base || ''
  }
  // 与 permission.joinPortalRoutePath 一致：勿把 Camstar http 拼进门户地址
  if (/^(https?:|mailto:|tel:)/i.test(segment) || /https?:\/\//i.test(segment)) {
    return base || ''
  }
  if (segment.startsWith('/')) {
    return segment.replace(/\/+/g, '/')
  }
  const normalizedBase = (base || '').replace(/\/+$/, '')
  return `${normalizedBase}/${segment}`.replace(/\/+/g, '/')
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
      const clientId = parsePortalClientId(currentPath) || (route.meta && route.meta.clientId)
      const title = resolvePortalMenuTitle(
        route.meta.menuTitle,
        route.meta.title,
        route.name
      ) || '业务页'
      const entry = {
        link: normalizeSubsystemIframeLink(route.meta.link, clientId),
        title,
        menuTitle: title,
        icon: route.meta.icon,
        // camstar=主系统直开（无 SSO）；ruoyi=子系统 OAuth
        kind: route.meta.portalKind || (String(route.meta.link).indexOf('#') >= 0 ? 'ruoyi' : 'camstar')
      }
      map[currentPath] = entry
      if (currentPath.endsWith('/index')) {
        const parentAlias = currentPath.replace(/\/index$/, '')
        if (parentAlias && !map[parentAlias]) {
          map[parentAlias] = entry
        }
      }
      // 旧书签 /portal/x/m{id} → 仍可命中，并带 canonicalPath 跳到 MES 对齐 path
      const menuId = route.meta.menuId
      if (clientId && menuId != null && menuId !== '') {
        const legacyEntry = { ...entry, canonicalPath: currentPath }
        const legacyShort = `/portal/${clientId}/m${menuId}`
        map[legacyShort] = legacyEntry
      }
    }
    if (route.children && route.children.length) {
      buildPortalPathLinkMap(route.children, currentPath, map)
    }
  })
  return map
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

function schedulePortalBootstrap(task) {
  const run = () => {
    if (typeof task === 'function') {
      task().catch(() => {})
    }
  }
  if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
    window.requestIdleCallback(run, { timeout: 3000 })
  } else {
    setTimeout(run, 0)
  }
}
