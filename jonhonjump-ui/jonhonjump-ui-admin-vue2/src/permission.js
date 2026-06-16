import router from './router'
import store from './store'
import { Message } from 'element-ui'
import { parsePortalClientId, parseLegacyPortalSubSystemId, resolvePortalFrameRoute, isPortalSubSystemHomePath, isSubSystemAllowedPath } from '@/utils/portalRoute'
import { syncPortalIframeView } from '@/utils/portalIframe'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getAccessToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'

NProgress.configure({ showSpinner: false })

// 增加三方登陆 update by 芋艿
const whiteList = ['/login', '/social-login',  '/auth-redirect', '/bind', '/register', '/oauthLogin/gitee']


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

function ensurePortalAccess(to, next) {
  const clientId = parsePortalClientId(to.path)
  if (!clientId) {
    return Promise.resolve(false)
  }
  const portalState = store.state.portal
  const finish = () => {
    if (isPortalSubSystemHomePath(to.path)) {
      next({ path: '/index', replace: true })
      return true
    }
    next()
    return true
  }
  if (portalState.loadedSubSystems[clientId] && portalState.currentSystem === clientId) {
    return store.dispatch('portal/activateSubSystem', clientId).then(() => finish()).catch(err => {
      Message.error(typeof err === 'string' ? err : (err.message || '无法进入外部系统'))
      next('/index')
      return true
    })
  }
  return store.dispatch('portal/enterSubSystem', { clientId, navigate: false }).then(() => {
    if (isPortalSubSystemHomePath(to.path)) {
      next({ path: '/index', replace: true })
      return true
    }
    next({ ...to, replace: true })
    return true
  }).catch(err => {
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
  Message.warning('当前为子系统模式，请从门户首页选择应用进入')
  next({ path: '/index', replace: true })
  NProgress.done()
  return Promise.resolve(true)
}

function ensurePortalBootstrap() {
  return store.dispatch('portal/bootstrapAfterAuth')
}

function continueNavigation(to, from, next) {
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
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else {
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        // 获取字典数据 add by 芋艿
        store.dispatch('dict/loadDictDatas')
        // 判断当前用户是否已拉取完 user_info 信息
        store.dispatch('GetInfo').then(userInfo => {
          isRelogin.show = false
          // 触发 GenerateRoutes 事件时，将 menus 菜单树传递进去
          store.dispatch('GenerateRoutes', userInfo.menus).then(accessRoutes => {
            // 根据 roles 权限生成可访问的路由表
            router.addRoutes(accessRoutes) // 动态添加可访问路由表
            store.dispatch('portal/ensureMainSidebarCached')
            store.dispatch('portal/bootstrapAfterAuth').then(() => {
              portalNavigation(to, next).then(handled => {
                if (!handled) {
                  next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
                }
              })
            })
          })
        }).catch(err => {
          isRelogin.show = false
          store.dispatch('LogOut').then(() => {
            Message.error(typeof err === 'string' ? err : '登录状态异常，请重新登录')
            next({ path: '/login', replace: true })
          })
        })
      } else {
        continueNavigation(to, from, next)
      }
    }
  } else {
    // 没有token
    if (whiteList.indexOf(to.path) !== -1) {
      // 在免登录白名单，直接进入
      next()
    } else {
      const redirect = encodeURIComponent(to.fullPath) // 编码 URI，保证参数跳转回去后，可以继续带上
      next(`/login?redirect=${redirect}`) // 否则全部重定向到登录页
      NProgress.done()
    }
  }
})

router.afterEach((to) => {
  if (store.state.portal.iframeSyncSuspended) {
    NProgress.done()
    return
  }
  const route = resolvePortalFrameRoute(to, store.state.portal.pathLinkMap)
  syncPortalIframeView(store, to)
  if (route.meta && route.meta.link && route.name) {
    store.dispatch('tagsView/addIframeView', route)
  }
  NProgress.done()
})
