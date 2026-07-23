import router from './router'
import store from './store'
import { Message } from 'element-ui'
import { parsePortalClientId, parseLegacyPortalSubSystemId, resolvePortalFrameRoute, isPortalSubSystemHomePath, isSubSystemAllowedPath, isMainBusinessPath, resolveCanonicalPortalPath, shouldNormalizePortalPath } from '@/utils/portalRoute'
import { syncPortalIframeView } from '@/utils/portalIframe'
import { loadMenuStyleDefault } from '@/utils/menuIconStyle'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getAccessToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'

NProgress.configure({ showSpinner: false })

// 增加三方登陆 update by 芋艿
const whiteList = ['/login', '/social-login',  '/auth-redirect', '/bind', '/register', '/oauthLogin/gitee']

function isLoginPath(path) {
  return path === '/login' || path === '/login/' || path.indexOf('/login/') === 0
}

function redirectToStaticLogin(fullPath) {
  const redirect = encodeURIComponent(fullPath)
  window.location.href = `/login/?redirect=${redirect}`
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
  next()
  return true
}

function ensurePortalAccess(to, next) {
  const clientId = parsePortalClientId(to.path)
  if (!clientId) {
    return Promise.resolve(false)
  }
  const portalState = store.state.portal
  const menusReady = !!portalState.loadedSubSystems[clientId]
  const enter = () => {
    return store.dispatch('portal/ensureSubSystemReady', clientId).then(() => {
      // 菜单加载期间若用户已切到其他系统，取消进入，避免抢壳
      if (store.state.portal.currentSystem !== clientId) {
        next({ path: '/index', replace: true })
        return true
      }
      return finishPortalNavigation(to, next)
    })
  }
  if (menusReady) {
    return enter().catch(err => {
      Message.error(typeof err === 'string' ? err : (err.message || '无法进入外部系统'))
      next('/index')
      return true
    })
  }
  const loading = Message({
    message: '加载子系统菜单，请稍候…',
    type: 'info',
    duration: 0,
    showClose: false
  })
  return enter().then(() => {
    loading.close()
    return true
  }).catch(err => {
    loading.close()
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
  NProgress.start()
  if (getAccessToken()) {
    const portalRoute = resolvePortalFrameRoute(to, store.state.portal.pathLinkMap)
    portalRoute.meta.title && store.dispatch('settings/setTitle', portalRoute.meta.title)
    /* has token*/
    if (isLoginPath(to.path)) {
      window.location.href = to.query.redirect ? decodeURIComponent(to.query.redirect) : '/'
      NProgress.done()
    } else {
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完 user_info 信息
        // 轻量 GetInfo（无主菜单树）→ 先进门户；主菜单仅进主系统或后台 Redis 预热
        store.dispatch('GetInfo', { includeMenus: false }).then(() => {
          loadMenuStyleDefault()
          isRelogin.show = false

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
            redirectToStaticLogin(to.fullPath)
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
      redirectToStaticLogin(to.fullPath)
    }
  }
})

router.afterEach((to) => {
  if (store.state.portal.iframeSyncSuspended) {
    NProgress.done()
    return
  }
  const clientId = parsePortalClientId(to.path)
  if (clientId && (to.name === 'PortalFrame' || to.name === 'PortalFrameLegacy')) {
    const pathLinkMap = store.state.portal.pathLinkMap
    if (shouldNormalizePortalPath(to.path, pathLinkMap)) {
      router.replace({
        path: resolveCanonicalPortalPath(to.path, pathLinkMap),
        query: to.query,
        hash: to.hash
      }).catch(() => {})
    }
  }
  const route = resolvePortalFrameRoute(to, store.state.portal.pathLinkMap)
  syncPortalIframeView(store, to)
  if (route.meta && route.meta.link && route.name) {
    store.dispatch('tagsView/addIframeView', route)
  }
  NProgress.done()
})
