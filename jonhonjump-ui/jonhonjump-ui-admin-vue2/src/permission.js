import router from './router'
import store from './store'
import { Message } from 'element-ui'
import {
  parsePortalClientId,
  parseLegacyPortalSubSystemId,
  resolvePortalFrameRoute,
  isPortalSubSystemHomePath,
  isSubSystemAllowedPath,
  isMainBusinessPath,
  resolveCanonicalPortalPath,
  shouldNormalizePortalPath,
  resolvePortalShortPathAlias,
  resolvePortalLegacyMenuIdAlias,
  slashIpPortRestToHttp,
  unwrapDirectHttpIframeLink,
  lookupPathLinkEntry,
  resolvePortalMenuTitle
} from '@/utils/portalRoute'
import { isCamstarLikeUrl, isPureHttpUrl } from '@/utils/portalMenuKind'
import { syncPortalIframeView } from '@/utils/portalIframe'
import { loadMenuStyleDefault } from '@/utils/menuIconStyle'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getAccessToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'
import { redirectToLogin, getTopWindow } from '@/utils/switchUser'
import { startPortalPermWatch } from '@/utils/portalPermWatch'

NProgress.configure({ showSpinner: false })

// 增加三方登陆 update by 芋艿
const whiteList = ['/login', '/social-login',  '/auth-redirect', '/bind', '/register', '/oauthLogin/gitee']

function isLoginPath(path) {
  return path === '/login' || path === '/login/' || path.indexOf('/login/') === 0
}

/** 门户首页 / 跨界快捷导航：不启顶栏进度条（bootstrap 期间顶条碍眼） */
function isPortalHomePath(path) {
  return path === '/index' || path === '/'
}

function redirectToStaticLogin() {
  // 会话失效 / 未登录：固定回门户默认首页，避免落到上一业务页
  redirectToLogin('/index')
  NProgress.done()
}


function tryRedirectLegacyPortalPath(to, next) {
  const legacyId = parseLegacyPortalSubSystemId(to.path)
  if (!legacyId) {
    return Promise.resolve(false)
  }
  const redirect = () => {
    const sys = store.state.portal.systemList.find(s => Number(s.subSystemId) === legacyId)
    if (!sys || !sys.clientId) {
      return false
    }
    next({
      path: to.path.replace(`/portal/${legacyId}`, `/portal/${sys.clientId}`),
      query: to.query,
      hash: to.hash,
      replace: true
    })
    return true
  }
  if (store.state.portal.systemList.length > 0) {
    return Promise.resolve(redirect())
  }
  return store.dispatch('portal/loadSystemList').then(() => redirect())
}

function finishPortalNavigation(to, next) {
  if (isPortalSubSystemHomePath(to.path)) {
    next({ path: '/index', replace: true })
    return true
  }
  const pathLinkMap = store.state.portal.pathLinkMap
  if (shouldNormalizePortalPath(to.path, pathLinkMap)) {
    next({
      path: resolveCanonicalPortalPath(to.path, pathLinkMap),
      query: to.query,
      hash: to.hash,
      replace: true
    })
    return true
  }
  // 旧包短码 /portal/x/15/m221256 → MES 对齐 path
  const legacyMesPath = resolvePortalLegacyMenuIdAlias(to.path, pathLinkMap)
  if (legacyMesPath && legacyMesPath !== to.path) {
    next({
      path: legacyMesPath,
      query: to.query,
      hash: to.hash,
      replace: true
    })
    return true
  }
  // 仅「真非法」深链才改写。点分 IP+/端口/（如 192.168.240.129/4200/...）是合法 Camstar 壳，必须放行。
  if (isUnsafePortalShellPath(to.path) && !isCamstarShellPath(to.path)) {
    const shortPath = resolvePortalShortPathAlias(to.path, pathLinkMap)
    if (shortPath && shortPath !== to.path) {
      next({
        path: shortPath,
        query: to.query,
        hash: to.hash,
        replace: true
      })
      return true
    }
    const clientId = parsePortalClientId(to.path)
    if (clientId) {
      next({ path: `/portal/${clientId}`, replace: true })
      return true
    }
  }
  // 禁止写 to.meta（vue-router 路由对象常不可扩展 → TypeError 后 next 断掉 → 一直转圈）
  const entry = lookupPathLinkEntry(to.path, pathLinkMap)
  const menuTitle = resolvePortalMenuTitle(entry && entry.title, entry && entry.menuTitle)
  if (menuTitle) {
    store.dispatch('settings/setTitle', menuTitle).catch(() => {})
  }
  next()
  return true
}

/**
 * 门户壳「真非法」：夹了完整 http(s) 或冒号端口（路由 : 易出问题）。
 * 注意：192.168.x.x/4200/... 点分 IP 是现场合法 Camstar path，不要当非法。
 */
function isUnsafePortalShellPath(path) {
  const p = String(path || '')
  if (!p.startsWith('/portal/')) {
    return false
  }
  return /:\d+/.test(p) || /https?:\/\//i.test(p)
}

/** 壳 path 能否还原成 Camstar http 直链（含 192.168.x.x/4200 与 192/168/.../4200） */
function isCamstarShellPath(path) {
  const clientId = parsePortalClientId(path)
  if (!clientId) {
    return false
  }
  const rest = String(path).replace(new RegExp('^/portal/' + clientId + '/'), '')
  return !!slashIpPortRestToHttp(rest.replace(/:/g, '/'))
}

function isPureHttpLink(link) {
  return isPureHttpUrl(link)
}

/**
 * 是否 Camstar/外链直开（跳过 OAuth）：
 * Camstar 是主系统 iframe 直开能力，菜单只挂在子系统树下，与子系统 SSO 无关。
 * 只认 pathLinkMap.kind / 纯 http link / 壳 path 含 Camstar 特征。
 * 若依（kind=ruoyi 或 link 带 #）一律 false。
 */
function isDirectExternalPortalTarget(to) {
  if (!to || !to.path) {
    return false
  }
  const clientId = parsePortalClientId(to.path)
  if (!clientId) {
    return false
  }
  const entry = lookupPathLinkEntry(to.path, store.state.portal.pathLinkMap)
  if (entry) {
    if (entry.kind === 'ruoyi') {
      return false
    }
    if (entry.kind === 'camstar') {
      return true
    }
    const link = unwrapDirectHttpIframeLink(entry.link || '')
    if (link && String(link).indexOf('#') >= 0) {
      return false
    }
    return isPureHttpLink(link)
  }
  const rest = String(to.path).replace(new RegExp('^/portal/' + clientId + '/'), '').replace(/\/index$/, '')
  const asHttp = slashIpPortRestToHttp(rest.replace(/:/g, '/'))
  // 无 map 时：仅 Camstar 特征壳 path 才直开，避免误伤
  return !!(asHttp && isCamstarLikeUrl(asHttp))
}

function ensurePortalAccess(to, next) {
  const clientId = parsePortalClientId(to.path)
  if (!clientId) {
    return Promise.resolve(false)
  }
  const portalState = store.state.portal
  const menusReady = !!portalState.loadedSubSystems[clientId]

  // Camstar/外链：对齐 4200 —— 立刻进页，绝不在关键口等 my-menus / version（那是 10s+ 主因）
  if (isDirectExternalPortalTarget(to)) {
    const go = () => {
      finishPortalNavigation(to, next)
      // 菜单后台补，不挡 iframe
      if (!menusReady) {
        store.dispatch('portal/ensureSubSystemLoaded', {
          clientId,
          activate: false,
          force: false
        }).catch(() => {})
      }
      return true
    }
    if (portalState.currentSystem !== clientId) {
      return store.dispatch('portal/activateSubSystemShell', { clientId }).then(go).catch(err => {
        Message.error(typeof err === 'string' ? err : (err.message || '无法进入外部系统'))
        next('/index')
        return true
      })
    }
    return Promise.resolve(go())
  }

  const enter = () => {
    return store.dispatch('portal/ensureSubSystemReady', {
      clientId,
      skipSso: false
    }).then(() => {
      if (store.state.portal.currentSystem !== clientId) {
        next({ path: '/index', replace: true })
        return true
      }
      return finishPortalNavigation(to, next)
    })
  }
  // 若依：不弹常驻 Message / 不锁全屏，菜单与 dock 必须始终可点
  return enter().catch(err => {
    if (err && err.message === 'RBAC_CHANGED_REQUIRE_RELOGIN') {
      return true
    }
    Message.error(typeof err === 'string' ? err : (err.message || '无法进入外部系统'))
    next('/index')
    return true
  })
}

function handlePortalHomeQuery(to, next) {
  if (to.path !== '/index') {
    return Promise.resolve(false)
  }
  const clientId = to.query.portal
  if (clientId) {
    return store.dispatch('portal/switchSystem', {
      system: clientId,
      stayOnPortalHome: true,
      skipNavigate: true
    }).then(() => {
      next({ path: '/index', replace: true })
      return true
    }).catch(err => {
      Message.error(typeof err === 'string' ? err : (err.message || '无法进入外部系统'))
      next({ path: '/index', replace: true })
      return true
    })
  }
  const legacyId = to.query.portalLegacy
  if (legacyId) {
    const activate = () => {
      const sys = store.state.portal.systemList.find(s => Number(s.subSystemId) === Number(legacyId))
      if (!sys || !sys.clientId) {
        next({ path: '/index', replace: true })
        return true
      }
      return store.dispatch('portal/switchSystem', {
        system: sys.clientId,
        stayOnPortalHome: true,
        skipNavigate: true
      }).then(() => {
        next({ path: '/index', replace: true })
        return true
      })
    }
    if (store.state.portal.systemList.length > 0) {
      return Promise.resolve(activate())
    }
    return store.dispatch('portal/loadSystemList').then(() => activate())
  }
  return Promise.resolve(false)
}

function portalNavigation(to, next) {
  return tryRedirectLegacyPortalPath(to, next).then(legacyHandled => {
    if (legacyHandled) {
      return true
    }
    return ensurePortalAccess(to, next)
  })
}

function clearDockOnPortalHome(to, from) {
  if (to.path !== '/index' && to.path !== '/') {
    return Promise.resolve()
  }
  if (!from || !from.path || from.path === '/index' || from.path === '/') {
    return Promise.resolve()
  }
  if (store.state.portal.preserveDockTabs) {
    return Promise.resolve()
  }
  return store.dispatch('tagsView/clearDockBusinessTabs')
}

function enforceSystemScope(to, next) {
  const currentSystem = store.state.portal.currentSystem
  if (currentSystem === 'main') {
    return Promise.resolve(false)
  }
  if (isSubSystemAllowedPath(to.path, currentSystem)) {
    return Promise.resolve(false)
  }
  if (isMainBusinessPath(to.path)) {
    return store.dispatch('portal/switchSystem', {
      system: 'main',
      skipNavigate: true
    }).then(() => {
      next({ ...to, replace: true })
      NProgress.done()
      return Promise.resolve(true)
    }).catch(err => {
      Message.error(typeof err === 'string' ? err : (err.message || '切换主系统失败'))
      next({ path: '/index', replace: true })
      NProgress.done()
      return Promise.resolve(true)
    })
  }
      Message.warning('当前为子系统模式，请从门户首页选择应用进入')
  next({ path: '/index', replace: true })
  NProgress.done()
  return Promise.resolve(true)
}

function ensurePortalBootstrap() {
  return store.dispatch('portal/bootstrapAfterAuth')
}

function continueNavigation(to, from, next) {
  // 必须等 bootstrap 切壳完成后再放行，避免先按错误系统渲染再跳
  ensurePortalBootstrap().then(() => {
    clearDockOnPortalHome(to, from).then(() => {
      return enforceSystemScope(to, next)
    }).then(scopeHandled => {
      if (scopeHandled) {
        return
      }
      handlePortalHomeQuery(to, next).then(handled => {
        if (handled) {
          return
        }
        portalNavigation(to, next).then(portalHandled => {
          if (!portalHandled) {
            next()
          }
        })
      })
    })
  })
}

router.beforeEach((to, from, next) => {
  if (isPortalHomePath(to.path)) {
    NProgress.done()
  } else {
    NProgress.start()
  }
  if (getAccessToken()) {
    const portalRoute = resolvePortalFrameRoute(to, store.state.portal.pathLinkMap, store.state.portal.systemList)
    portalRoute.meta.title && store.dispatch('settings/setTitle', portalRoute.meta.title)
    /* has token*/
    if (isLoginPath(to.path)) {
      // 已登录访问登录页：顶层回到业务页，避免壳内残留登录页
      getTopWindow().location.href = to.query.redirect ? decodeURIComponent(to.query.redirect) : '/'
      NProgress.done()
    } else {
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完 user_info 信息
        // 轻量 GetInfo（无主菜单树）→ 先进门户；主菜单仅进主系统或后台 Redis 预热
        store.dispatch('GetInfo', { includeMenus: false }).then(() => {
          loadMenuStyleDefault()
          isRelogin.show = false
          startPortalPermWatch(router)

          const finishNavigation = () => {
            enforceSystemScope(to, next).then(scopeHandled => {
              if (scopeHandled) {
                return
              }
              portalNavigation(to, next).then(handled => {
                if (!handled) {
                  next({ ...to, replace: true })
                }
              })
            })
          }

          // 先等壳切换完成再放行，避免快捷导航先按错误系统渲染再跳
          ensurePortalBootstrap().finally(() => {
            const needMainMenusNow = isMainBusinessPath(to.path) && !parsePortalClientId(to.path)
            if (needMainMenusNow) {
              // 刷新落在主业务页：必须现在拉主菜单（可打库）
              store.dispatch('LoadMainMenus', { redisOnly: false }).then(() => finishNavigation()).catch(() => finishNavigation())
            } else {
              finishNavigation()
            }
          })
        }).catch(err => {
          isRelogin.show = false
          store.dispatch('LogOut').then(() => {
            Message.error(typeof err === 'string' ? err : '登录状态异常，请重新登录')
            redirectToStaticLogin()
          })
        })
      } else {
        continueNavigation(to, from, next)
      }
    }
  } else {
    // 没有token
    if (isLoginPath(to.path) || whiteList.indexOf(to.path) !== -1) {
      // 进登录页清门户会话，避免未走 LogOut 时残留 last_system，登录后跳过星标默认
      if (isLoginPath(to.path)) {
        store.commit('portal/RESET_PORTAL')
      }
      next()
    } else {
      redirectToStaticLogin()
    }
  }
})

router.afterEach((to) => {
  if (store.state.portal.iframeSyncSuspended) {
    NProgress.done()
    return
  }
  const clientId = parsePortalClientId(to.path)
  // Camstar 直链：禁止 alias→/index 二次 replace（会换 path key，再冷开一套 iframe）
  if (clientId && (to.name === 'PortalFrame' || to.name === 'PortalFrameLegacy')
    && !isDirectExternalPortalTarget(to)) {
    const pathLinkMap = store.state.portal.pathLinkMap
    if (shouldNormalizePortalPath(to.path, pathLinkMap)) {
      router.replace({
        path: resolveCanonicalPortalPath(to.path, pathLinkMap),
        query: to.query,
        hash: to.hash
      }).catch(() => {})
    }
  }
  try {
    syncPortalIframeView(store, to)
  } catch (err) {
    if (typeof console !== 'undefined' && console.error) {
      console.error('[portal] afterEach iframe sync failed', err)
    }
  }
  NProgress.done()
})
