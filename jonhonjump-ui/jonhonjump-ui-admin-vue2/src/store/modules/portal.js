import router, { constantRoutes } from '@/router'
import { authorize } from '@/api/login'
import { getMyExternalSystemList, getMyPortalMenus } from '@/api/system/subSystemUsers'
import { getUserPortalDefault } from '@/api/system/user/portalDefault'
import { getUserQuickNavList } from '@/api/system/user/quickNav'
import { getSubSystemUserQuickNavList } from '@/api/system/user/subSystemQuickNav'
import { isPortalSubSystemHomePath, isMainBusinessPath, parsePortalClientId } from '@/utils/portalRoute'
import {
  buildSubsystemSsoCallbackUri,
  isSubsystemSsoDoneMessage,
  loadPersistedSsoDone,
  persistSsoDoneMap,
  clearPersistedSsoDone,
  loadPersistedPortalCache,
  persistPortalCache,
  clearPersistedPortalCache
} from '@/utils/portalSso'
import { pingSubsystemUi } from '@/utils/subsystemHealth'
import { resolveRuleBasedPortalDefault } from '@/utils/portalSubsystem'
import {
  buildQuickNavScopeKey,
  setQuickNavCache
} from '@/utils/portalQuickNavCache'
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
    subSystemEntryPaths: (persisted && persisted.subSystemEntryPaths) || {},
    subSystemSidebarCache: {},
    subSystemPathLinkCache: (persisted && persisted.subSystemPathLinkCache) || {},
    pathLinkMap,
    ssoDone: loadPersistedSsoDone(),
    loadingSubSystems: {},
    loadingSso: {},
    quickNavInFlight: {},
    /** 快捷导航拉取代数：保存/强制刷新后递增，丢弃过期 GET，防止旧响应盖掉新数据 */
    quickNavLoadEpoch: {},
    iframeSyncSuspended: false,
    preserveDockTabs: false,
    portalBootstrapped: false,
    /** 单飞：并发 beforeEach 共用同一个 bootstrap Promise */
    bootstrapInFlight: null,
    portalDefaultCache: null,
    /** clientId → true|false|undefined；false 表示健康探测失败 */
    subsystemHealthy: {},
    /** clientId → 错误文案；SSO 失败时展示 */
    ssoError: {},
    /** clientId → 递增序号；会话重绑后强制重挂业务 iframe */
    iframeReloadNonce: {},
    /** clientId → Promise；避免 401 风暴重复重认证 */
    reauthInFlight: {}
  }
}

const HEALTH_POLL_MS = 30000
const HEALTH_FAIL_THRESHOLD = 2
/** @type {Record<string, { timer: any, fails: number }>} */
const healthProbeRuntime = {}
/** @type {Record<string, number>} 401 重认证冷却截止时间 */
const sessionExpiredCooldownUntil = {}
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
  MARK_SSO_DONE(state, clientId) {
    state.ssoDone = {
      ...state.ssoDone,
      [clientId]: true
    }
    persistSsoDoneMap(state.ssoDone)
  },
  /** 清除单个子系统 SSO 标记，用于发版/会话失效后重新静默登录 */
  CLEAR_SSO_DONE(state, clientId) {
    const key = normalizeSystemKey(clientId)
    if (!key || !state.ssoDone[key]) {
      return
    }
    const next = { ...state.ssoDone }
    delete next[key]
    state.ssoDone = next
    persistSsoDoneMap(state.ssoDone)
  },
  SET_SUBSYSTEM_HEALTHY(state, { clientId, healthy }) {
    const key = normalizeSystemKey(clientId)
    if (!key) {
      return
    }
    state.subsystemHealthy = {
      ...state.subsystemHealthy,
      [key]: healthy === true
    }
  },
  SET_SSO_ERROR(state, { clientId, message }) {
    const key = normalizeSystemKey(clientId)
    if (!key) {
      return
    }
    state.ssoError = {
      ...state.ssoError,
      [key]: message || '子系统登录失败'
    }
  },
  CLEAR_SSO_ERROR(state, clientId) {
    const key = normalizeSystemKey(clientId)
    if (!key || !state.ssoError[key]) {
      return
    }
    const next = { ...state.ssoError }
    delete next[key]
    state.ssoError = next
  },
  BUMP_IFRAME_RELOAD(state, clientId) {
    const key = normalizeSystemKey(clientId)
    if (!key) {
      return
    }
    state.iframeReloadNonce = {
      ...state.iframeReloadNonce,
      [key]: (state.iframeReloadNonce[key] || 0) + 1
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
    state.loadingSso = {}
    state.quickNavInFlight = {}
    state.quickNavLoadEpoch = {}
    state.iframeSyncSuspended = false
    state.preserveDockTabs = false
    state.portalBootstrapped = false
    state.bootstrapInFlight = null
    state.portalDefaultCache = null
    state.subsystemHealthy = {}
    state.ssoError = {}
    state.iframeReloadNonce = {}
    state.reauthInFlight = {}
    bumpPortalWarmGeneration()
    stopAllHealthProbes()
    clearPersistedSsoDone()
    clearPersistedPortalCache()
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
   * 快捷导航单飞：Shell 与首页 Panel 共用，避免同屏双请求 + 丢 action 后退化成侧栏假菜单
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
      // 过期响应不写缓存，改跟当前 in-flight（若有）或直接丢弃 data
      if (epoch !== state.quickNavLoadEpoch[cacheKey]) {
        return state.quickNavInFlight[cacheKey] || ((res && res.data) || {})
      }
      const config = (res && res.data) || {}
      setQuickNavCache(
        cacheKey,
        config.menuIds || [],
        !!config.configured,
        config.lockedMenuIds || []
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
    const config = payload.config || {}
    state.quickNavLoadEpoch = {
      ...state.quickNavLoadEpoch,
      [cacheKey]: (state.quickNavLoadEpoch[cacheKey] || 0) + 1
    }
    setQuickNavCache(
      cacheKey,
      config.menuIds || [],
      !!config.configured,
      config.lockedMenuIds || []
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
   * 登录后门户初始化（与产品约定一致）：
   * 【仅首次登录/会话清空后】按星标/规则默认进入系统
   * 【同会话内】之后一律跟用户当前所在系统（portal_last_system），后台预热不得抢壳
   * 【首屏必载】不论默认主/子系统：快捷导航 + 工作台（首页组件拉取）
   * 【默认主系统·后台】懒加载主系统用户角色/菜单/权限（Redis 优先，首次/变更才打库）
   * 【默认子系统·后台】懒加载子系统 + 主系统 的角色/菜单/权限（同上 Redis 优先）
   * 【OAuth】后台预热（不挡首屏）；进入业务页时若未完成再由 ensureSubSystemReady 补踢
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
      return dispatch('applyPortalBootstrapSystem', { list, defaultConfig, applyGen })
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
   * 后台：子系统 my-menus/权限包 + OAuth，以及主系统角色菜单权限。
   * 只预热缓存，不抢当前壳；用户已切走时作废本次预热。
   */
  warmSubSystemDefaultInBackground({ dispatch, state }, clientId) {
    const key = normalizeSystemKey(clientId)
    const warmGen = portalWarmGeneration
    const subWarm = new Promise(resolve => {
      setTimeout(() => {
        if (warmGen !== portalWarmGeneration) {
          resolve()
          return
        }
        dispatch('ensureSubSystemLoaded', { clientId: key, activate: false })
          .then(() => {
            if (warmGen !== portalWarmGeneration) {
              return
            }
            // SSO 仅在用户仍停留在该子系统时后台预热
            if (state.currentSystem === key && !state.ssoDone[key]) {
              dispatch('runSilentSso', key).catch(err => {
                console.warn('[portal] background SSO warm failed:', err)
              })
            }
          })
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
   */
  activateSubSystemShell({ commit, dispatch, state }, { clientId, skipPersist }) {
    const key = normalizeSystemKey(clientId)
    return dispatch('cacheMainSidebar').then(() => {
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
      return key
    })
  },

  /**
   * 兼容旧调用：后台预热子系统菜单/SSO（不阻塞首屏）
   */
  preloadSubSystemInBackground({ dispatch }, clientId) {
    return dispatch('warmSubSystemDefaultInBackground', clientId)
  },

  /**
   * 进入子系统业务页：
   * 1) my-menus / 权限包（Redis 优先）
   * 2) OAuth：后台预热未完成时这里补踢（仍不阻塞菜单就绪返回）
   */
  ensureSubSystemReady({ dispatch, state }, clientId) {
    const key = normalizeSystemKey(clientId)
    const startGen = portalWarmGeneration
    return dispatch('ensureSubSystemLoaded', { clientId: key, activate: true, startGen }).then(() => {
      if (!state.ssoDone[key]) {
        dispatch('runSilentSso', key).catch(err => {
          console.warn('[portal] SSO lazy start failed:', err)
        })
      }
      return key
    })
  },

  /**
   * 文档加载失败或会话失效：清 SSO 标记、等待静默登录完成，并强制重挂业务 iframe。
   * 只刷新认证与缓存，不抢当前壳。
   */
  reauthSubSystem({ commit, dispatch, state }, clientId) {
    const key = normalizeSystemKey(clientId)
    if (state.reauthInFlight[key]) {
      return state.reauthInFlight[key]
    }
    commit('CLEAR_SSO_DONE', key)
    commit('CLEAR_SSO_ERROR', key)
    const task = dispatch('ensureSubSystemLoaded', { clientId: key, activate: false })
      .then(() => dispatch('runSilentSso', key))
      .then(() => {
        // 用户仍停留在该系统时，同步活动侧栏/链路；否则只 bump 隐藏 iframe
        if (state.currentSystem === key) {
          const pathLinkMap = state.subSystemPathLinkCache[key]
          if (pathLinkMap) {
            commit('SET_PORTAL_PATH_LINKS', sanitizePathLinkMap(pathLinkMap))
          }
          const sidebar = state.subSystemSidebarCache[key]
          if (sidebar && sidebar.length) {
            commit('SET_SIDEBAR_ROUTERS', sidebar, { root: true })
          }
        }
        commit('BUMP_IFRAME_RELOAD', key)
        return key
      })
      .catch(err => {
        commit('SET_SSO_ERROR', {
          clientId: key,
          message: (err && err.message) || '子系统重新认证失败'
        })
        return Promise.reject(err)
      })
      .finally(() => {
        const next = { ...state.reauthInFlight }
        delete next[key]
        state.reauthInFlight = next
      })
    state.reauthInFlight = {
      ...state.reauthInFlight,
      [key]: task
    }
    return task
  },

  /** 子系统 iframe 内 401 → 自动重认证（带去重与冷却，避免死循环） */
  handleSubsystemSessionExpired({ dispatch }, clientId) {
    const key = normalizeSystemKey(clientId)
    const now = Date.now()
    if (sessionExpiredCooldownUntil[key] && now < sessionExpiredCooldownUntil[key]) {
      return Promise.resolve(key)
    }
    sessionExpiredCooldownUntil[key] = now + 15000
    return dispatch('reauthSubSystem', key)
  },

  /** 当前可见子系统时启动健康探测；离开时停止 */
  syncHealthProbe({ dispatch }, clientId) {
    stopAllHealthProbes()
    const key = clientId ? normalizeSystemKey(clientId) : null
    if (!key || key === 'main') {
      return Promise.resolve()
    }
    return dispatch('startHealthProbe', key)
  },

  startHealthProbe({ commit, state }, clientId) {
    const key = normalizeSystemKey(clientId)
    clearHealthProbeTimer(key)
    const runtime = { timer: null, fails: 0 }
    healthProbeRuntime[key] = runtime

    const tick = () => {
      return pingSubsystemUi(key).then(ok => {
        if (!healthProbeRuntime[key]) {
          return
        }
        if (ok) {
          const wasDown = state.subsystemHealthy[key] === false
          runtime.fails = 0
          commit('SET_SUBSYSTEM_HEALTHY', { clientId: key, healthy: true })
          if (wasDown) {
            commit('BUMP_IFRAME_RELOAD', key)
          }
        } else {
          runtime.fails += 1
          if (runtime.fails >= HEALTH_FAIL_THRESHOLD) {
            commit('SET_SUBSYSTEM_HEALTHY', { clientId: key, healthy: false })
          }
        }
      })
    }

    return tick().then(() => {
      if (!healthProbeRuntime[key]) {
        return
      }
      runtime.timer = setInterval(tick, HEALTH_POLL_MS)
    })
  },

  stopHealthProbe(_, clientId) {
    clearHealthProbeTimer(normalizeSystemKey(clientId))
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

  switchSystem({ dispatch, state }, payload) {
    const system = typeof payload === 'object' && payload !== null ? payload.system : payload
    const stayOnPortalHome = typeof payload === 'object' && payload !== null && payload.stayOnPortalHome === true
    const skipNavigate = typeof payload === 'object' && payload !== null && payload.skipNavigate === true
    const skipPersist = typeof payload === 'object' && payload !== null && payload.skipPersist === true

    bumpPortalWarmGeneration()

    if (system === 'main') {
      if (!skipPersist) {
        persistPortalSystemChoice('main')
      }
      return dispatch('cacheMainSidebar').then(() => {
        const enter = state.currentSystem !== 'main'
          ? dispatch('tagsView/clearDockBusinessTabs', null, { root: true }).then(() => {
            return dispatch('enterMainSystem', { stayOnPortalHome, skipNavigate })
          })
          : dispatch('enterMainSystem', { stayOnPortalHome, skipNavigate })
        return enter.then(() => {
          if (stayOnPortalHome) {
            schedulePortalBootstrap(() => dispatch('warmMainSystemInBackground'))
          }
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
      if (!skipPersist) {
        persistPortalSystemChoice(targetSystem)
      }
      return dispatch('cacheMainSidebar').then(() => {
        if (targetSystem === currentSystem) {
          if (stayOnPortalHome) {
            // 门户首页：只保持壳，不预拉 my-menus / OAuth
            return Promise.resolve()
          }
          return dispatch('ensureSubSystemReady', targetSystem).then(() => {
            return dispatch('navigateToPortalHome')
          })
        }
        return dispatch('tagsView/clearDockBusinessTabs', null, { root: true }).then(() => {
          // 停在门户首页：先切壳出子快捷导航；后台懒加载子权限 + 主系统资源
          if (stayOnPortalHome) {
            return dispatch('activateSubSystemShell', {
              clientId: targetSystem,
              skipPersist: true
            }).then(() => {
              schedulePortalBootstrap(() => dispatch('warmSubSystemDefaultInBackground', targetSystem))
              if (skipNavigate) {
                return Promise.resolve()
              }
              return dispatch('navigateToPortalHome')
            })
          }
          return dispatch('enterSubSystem', {
            clientId: targetSystem,
            navigate: false,
            stayOnPortalHome: false
          }).then(() => {
            if (isMainBusinessPath(router.currentRoute.path)) {
              return dispatch('navigateToPortalHome')
            }
            if (skipNavigate) {
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
    const skipNavigate = payload && payload.skipNavigate
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
        return dispatch('navigateToPortalHome')
      }
      return dispatch('tagsView/keepMainViews', null, { root: true }).then(() => {
        return dispatch('goMain')
      })
    })
  },

  goMain() {
    return goPortalIndex()
  },

  /** 回到门户 /index，保留 dock 已打开页签（仅隐藏当前页）；不改变当前系统 */
  navigateToPortalHome({ commit, dispatch, state }) {
    if (state.currentSystem) {
      persistPortalSystemChoice(state.currentSystem)
    }
    commit('SET_PRESERVE_DOCK_TABS', true)
    commit('SET_PORTAL_PATH_LINKS', {})
    return dispatch('tagsView/prunePortalHomeViews', null, { root: true }).then(() => {
      return goPortalIndex().finally(() => {
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

  ensureSubSystemLoaded({ dispatch, state }, payload) {
    const key = normalizeSystemKey(
      payload && typeof payload === 'object' ? payload.clientId : payload
    )
    const activate = !(payload && typeof payload === 'object' && payload.activate === false)
    const startGen = payload && typeof payload === 'object' && payload.startGen != null
      ? payload.startGen
      : portalWarmGeneration

    const ensureCached = () => {
      if (state.loadedSubSystems[key]) {
        return Promise.resolve(state.subSystemEntryPaths[key])
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
        return dispatch('GenerateSubSystemRoutes', {
          subSystemId,
          clientId: target.clientId,
          menus,
          portalHome,
          applyToLive: false
        }, { root: true }).then(({ sidebarRoutes }) => {
          const sidebarRouters = sidebarRoutes || []
          const pathLinkMap = sanitizePathLinkMap(buildPortalPathLinkMap(sidebarRouters))
          if (Object.keys(pathLinkMap).length === 0) {
            return Promise.reject(new Error('外部系统菜单未配置有效链接'))
          }
          const entryPath = '/index'
          commit('SET_SUB_SYSTEM_PATH_LINK_CACHE', { clientId: target.clientId, pathLinkMap })
          commit('MARK_SUB_SYSTEM_LOADED', { clientId: target.clientId, signature, entryPath })
          commit('SET_SUB_SYSTEM_SIDEBAR_CACHE', {
            clientId: target.clientId,
            sidebarRouters
          })
          // 若用户当前仍在该子系统，把缓存同步到活动侧栏/链路；绝不抢改 currentSystem
          if (state.currentSystem === target.clientId) {
            commit('SET_PORTAL_PATH_LINKS', pathLinkMap)
            commit('SET_SIDEBAR_ROUTERS', sidebarRouters, { root: true })
          }
          return entryPath
        })
      })
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
  },

  runSilentSso({ commit, state }, clientId) {
    const key = normalizeSystemKey(clientId)
    if (state.ssoDone[key]) {
      commit('CLEAR_SSO_ERROR', key)
      return Promise.resolve()
    }
    if (state.loadingSso[key]) {
      return state.loadingSso[key]
    }
    const ref = findSystemByClientId(state, key)
    if (!ref) {
      const err = new Error('无效的外部系统')
      commit('SET_SSO_ERROR', { clientId: key, message: err.message })
      return Promise.reject(err)
    }
    commit('CLEAR_SSO_ERROR', key)
    const oauthClientId = ref.clientId
    const redirectUri = buildSubsystemSsoCallbackUri(oauthClientId)
    const scopes = ['user.read']
    const task = authorize('code', oauthClientId, redirectUri, oauthClientId, true, scopes, []).then(res => {
      const href = res && res.data
      if (!href) {
        return Promise.reject(new Error(
          `SSO 自动授权未通过，请检查 OAuth2 应用 ${oauthClientId} 的 redirect_uris 与 auto_approve_scopes`
        ))
      }
      return loadHiddenSsoIframe(href, oauthClientId)
    }).then(() => {
      commit('MARK_SSO_DONE', key)
      commit('CLEAR_SSO_ERROR', key)
    }).catch(err => {
      commit('CLEAR_SSO_DONE', key)
      commit('SET_SSO_ERROR', {
        clientId: key,
        message: (err && err.message) || `${key} SSO 登录失败`
      })
      return Promise.reject(err)
    }).finally(() => {
      const nextMap = { ...state.loadingSso }
      delete nextMap[key]
      state.loadingSso = nextMap
    })
    state.loadingSso = {
      ...state.loadingSso,
      [key]: task
    }
    return task
  },

  /** 已废弃：首页禁止预热 OAuth，保留空实现以免旧调用报错 */
  preAuthSso() {
    return Promise.resolve()
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

function resolvePortalBootstrapSystem(defaultConfig, systemList) {
  const list = systemList || []
  const ruleDefault = resolveRuleBasedPortalDefault(list)
  const preferred = (defaultConfig && defaultConfig.defaultSystem) || ruleDefault
  if (preferred === 'main') {
    return 'main'
  }
  return list.some(item => item.clientId === preferred) ? preferred : ruleDefault
}

function loadHiddenSsoIframe(src, clientId) {
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
        reject(err || new Error(`${clientId} SSO 登录失败`))
      }
    }
    const onMessage = (event) => {
      const data = event && event.data
      if (!isSubsystemSsoDoneMessage(data, clientId)) {
        return
      }
      finish(
        !!data.success,
        data.success ? null : new Error(`${clientId} SSO 登录失败，请确认门户账号在子系统中存在`)
      )
    }
    window.addEventListener('message', onMessage)
    // 子系统回调慢或门户鉴权高峰时，5s 极易误报超时；与守卫超时对齐放宽
    const timer = setTimeout(() => {
      finish(false, new Error(`${clientId} SSO 登录超时`))
    }, 10000)
    iframe.onerror = () => finish(false, new Error(`${clientId} SSO iframe 加载失败`))
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
      const entry = {
        link: route.meta.link,
        title: route.meta.title || route.name || '外部系统',
        icon: route.meta.icon
      }
      map[currentPath] = entry
      if (currentPath.endsWith('/index')) {
        const parentAlias = currentPath.replace(/\/index$/, '')
        if (parentAlias && !map[parentAlias]) {
          map[parentAlias] = entry
        }
      }
    }
    if (route.children && route.children.length) {
      buildPortalPathLinkMap(route.children, currentPath, map)
    }
  })
  return map
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

function clearHealthProbeTimer(clientId) {
  const runtime = healthProbeRuntime[clientId]
  if (!runtime) {
    return
  }
  if (runtime.timer) {
    clearInterval(runtime.timer)
  }
  delete healthProbeRuntime[clientId]
}

function stopAllHealthProbes() {
  Object.keys(healthProbeRuntime).forEach(clearHealthProbeTimer)
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
