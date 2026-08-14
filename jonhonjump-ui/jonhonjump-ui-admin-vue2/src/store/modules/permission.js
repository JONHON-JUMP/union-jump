import {constantRoutes} from '@/router'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView';
import {toCamelCase} from "@/utils";
import { buildPortalHomeMenu, normalizeSubsystemIframeLink, unwrapDirectHttpIframeLink, slashIpPortRestToHttp, encodeHttpToMesPath } from '@/utils/portalRoute'
import { isExternal } from '@/utils/validate'
import { classifyPortalMenu, isPureHttpUrl } from '@/utils/portalMenuKind'

/**
 * 菜单二分（见 portalMenuKind.js，勿在别处另写一套）：
 * - Camstar：路由地址 http → iframe 直开
 * - 若依：组件路径有值 / 相对路由 → OAuth + systemUrl/#/
 */
const PORTAL_IFRAME_EMPTY = 'system/subSystem/portal/Empty'

function resolveSubSystemBaseUrl(clientId) {
  try {
    const store = require('@/store').default
    const systems = (store && store.state && store.state.portal && store.state.portal.systemList) || []
    const sys = systems.find(s => s && s.clientId === clientId)
    return String((sys && (sys.systemUrl || sys.homePageUrl)) || '').replace(/\/+$/, '')
  } catch (e) {
    return ''
  }
}

/**
 * 门户壳 path（仅地址栏）：
 * - Camstar：编码业务 http
 * - 若依：MES #/ 后段
 */
function toPortalShellPath(route, clientId) {
  const kind = classifyPortalMenu(route)
  if (route && route.link && kind === 'camstar') {
    const fixed = unwrapDirectHttpIframeLink(route.link)
    if (fixed) {
      route.link = fixed
    }
  }
  const link = (route && route.link) || ''
  const base = resolveSubSystemBaseUrl(clientId)

  if (kind === 'camstar' && link && isPureHttpUrl(link)) {
    const shell = encodeHttpToMesPath(link)
    if (base && shell === encodeHttpToMesPath(base)) {
      return 'menu' + (route && route.id != null ? route.id : '0')
    }
    return shell
  }

  const hashIdx = link.indexOf('/#/')
  if (hashIdx >= 0) {
    return link.substring(hashIdx + 3).replace(/:/g, '/').replace(/^\/+/, '').split('#')[0]
  }

  const raw = String((route && route.path) || '').replace(/^\/+/, '')
  if (!raw) {
    return 'menu' + (route && route.id != null ? route.id : '0')
  }
  if (kind === 'camstar') {
    const asHttp = slashIpPortRestToHttp(raw.replace(/:/g, '/')) || (isExternal(raw) ? raw : '')
    if (asHttp) {
      return encodeHttpToMesPath(asHttp)
    }
    return encodeHttpToMesPath(raw)
  }
  return raw.replace(/:/g, '/').replace(/\./g, '_').split('#')[0]
}

/**
 * 补全 iframe link：严格按 Camstar / 若依二分，禁止互转
 */
function ensureMenuIframeLink(route, clientId, parentRestPath) {
  if (route.children && route.children.length) {
    return
  }
  const base = resolveSubSystemBaseUrl(clientId)
  const kind = classifyPortalMenu(route)

  // 若依：永远 systemUrl/#/路由；有脏 link 也纠正回来
  if (kind === 'ruoyi') {
    if (route.link && String(route.link).indexOf('/#/') >= 0) {
      return
    }
    if (!base) {
      return
    }
    const raw = String(route.path || '').replace(/^\/+/, '')
    if (!raw || isExternal(raw)) {
      return
    }
    let mesRoute = raw.replace(/:/g, '/')
    if (parentRestPath && mesRoute !== parentRestPath && mesRoute.indexOf(parentRestPath + '/') !== 0) {
      mesRoute = `${parentRestPath}/${mesRoute}`.replace(/\/+/g, '/')
    }
    route.link = `${base}/#/${mesRoute}`
    return
  }

  // Camstar：纯 http 直链
  if (route.link) {
    route.link = unwrapDirectHttpIframeLink(route.link) || route.link
    return
  }
  const raw = String(route.path || '').replace(/^\/+/, '')
  if (!raw) {
    return
  }
  if (isPureHttpUrl(raw) || isExternal(raw)) {
    route.link = raw
    return
  }
  const asHttp = slashIpPortRestToHttp(raw.replace(/:/g, '/'))
  if (asHttp) {
    route.link = asHttp
  }
}

function parentRestPath(lastRouter, clientId) {
  if (!lastRouter || !lastRouter.path || !clientId) {
    return ''
  }
  const p = String(lastRouter.path)
  const prefix = `/portal/${clientId}/`
  if (p.startsWith(prefix)) {
    return p.substring(prefix.length).replace(/\/index$/, '').replace(/^\/+/, '')
  }
  if (p.startsWith('/portal/')) {
    return ''
  }
  return p.replace(/^\/+/, '')
}

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    sidebarRouters: [],
    topbarRouters: []
  },
  mutations: {
    SET_ROUTES: (state, routes) => {
      state.addRoutes = routes
      state.routes = constantRoutes.concat(routes)
    },
    SET_DEFAULT_ROUTES: (state, routes) => {
      state.defaultRoutes = constantRoutes.concat(routes)
    },
    SET_TOPBAR_ROUTES: (state, routes) => {
      state.topbarRouters = routes
    },
    SET_SIDEBAR_ROUTERS: (state, routes) => {
      state.sidebarRouters = routes
    }
  },
  actions: {
    GenerateRoutes({commit, rootState}, menus) {
      return new Promise(resolve => {
        const sdata = JSON.parse(JSON.stringify(menus || []))
        const rdata = JSON.parse(JSON.stringify(menus || []))
        const sidebarRoutes = filterAsyncRouter(sdata)
        const rewriteRoutes = filterAsyncRouter(rdata, false, true)
        rewriteRoutes.push({path: '*', redirect: '/404', hidden: true})
        commit('SET_ROUTES', rewriteRoutes)
        commit('SET_DEFAULT_ROUTES', sidebarRoutes)
        commit('SET_TOPBAR_ROUTES', sidebarRoutes)
        const mainSidebar = constantRoutes.concat(sidebarRoutes)
        commit('portal/SET_MAIN_SIDEBAR_ROUTERS', mainSidebar, { root: true })
        const currentSystem = rootState.portal && rootState.portal.currentSystem
        if (!currentSystem || currentSystem === 'main') {
          commit('SET_SIDEBAR_ROUTERS', mainSidebar)
        }
        resolve(rewriteRoutes)
      })
    },
    GenerateSubSystemRoutes({ commit }, { subSystemId, clientId, menus, portalHome, applyToLive }) {
      return new Promise(resolve => {
        let menuList = JSON.parse(JSON.stringify(menus))
        if (portalHome && portalHome.link) {
          menuList = [buildPortalHomeMenu(subSystemId, portalHome), ...menuList]
        }
        const sdata = JSON.parse(JSON.stringify(menuList))
        const rdata = JSON.parse(JSON.stringify(menuList))
        const sidebarRoutes = filterSubSystemRouter(sdata, subSystemId, clientId)
        const nestedRewrite = filterSubSystemRouter(rdata, subSystemId, clientId, false, true)
        const rewriteRoutes = buildPortalRewriteRoutes(nestedRewrite)
        if (applyToLive !== false) {
          commit('SET_SIDEBAR_ROUTERS', sidebarRoutes)
          commit('SET_TOPBAR_ROUTES', sidebarRoutes)
        }
        resolve({ rewriteRoutes, nestedRewrite, sidebarRoutes })
      })
    }
  }
}

function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    route.meta = {
      title: route.name,
      icon: route.icon,
      color: route.color,
      shape: route.shape,
      noCache: !route.keepAlive,
      menuId: route.id,
      manualUrl: route.manualUrl
    }
    route.hidden = !route.visible
    if (route.componentName && route.componentName.length > 0) {
      route.name = route.componentName
    } else {
      route.name = toCamelCase(route.path, true)
      if (route.path && route.path.indexOf('/') !== -1) {
        const pathArr = route.path.split('/')
        route.name = toCamelCase(pathArr[pathArr.length - 1], true)
      }
    }
    if (route.children) {
      route.component = route.parentId === 0 ? Layout : ParentView
    } else {
      route.component = loadView(route.component)
    }
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
      route.alwaysShow = route.alwaysShow !== undefined ? route.alwaysShow : true
    } else {
      delete route['children']
      delete route['alwaysShow']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  let children = []
  childrenMap.forEach(el => {
    if (el.children && el.children.length) {
      if (!el.component && !lastRouter) {
        el.children.forEach(c => {
          c.path = el.path + '/' + c.path
          if (c.children && c.children.length) {
            children = children.concat(filterChildren(c.children, c))
            return
          }
          children.push(c)
        })
        return
      }
    }
    if (lastRouter) {
      el.path = lastRouter.path + '/' + el.path
    }
    children = children.concat(el)
  })
  return children
}

export const loadView = (view) => {
  return (resolve) => require([`@/views/${view}`], resolve)
}

/**
 * 子系统路由：
 * - 叶子（若依组件页 / Camstar 路由地址）→ 绝对门户 path + Empty iframe
 * - 子 path 用绝对地址时 Sidebar path.resolve 不会再拼成 /15/xxx
 * - 目录树保留，不拆平
 */
function filterSubSystemRouter(asyncRouterMap, subSystemId, clientId, lastRouter = false, type = false) {
  return (asyncRouterMap || []).filter(route => {
    const restParent = parentRestPath(lastRouter, clientId)
    // 改写 path/component 前先分类并算 link
    const rawPath = route.path
    const rawComponent = route.component
    ensureMenuIframeLink(route, clientId, restParent)
    const portalKind = classifyPortalMenu({
      path: rawPath,
      component: rawComponent,
      link: route.link
    })

    const isIndexChild = !!lastRouter && route.path === 'index' && !!route.link
    const shellPath = toPortalShellPath(route, clientId)
    // 必须在改写 route.name 之前钉死菜单显示名（Camstar 走静态 PortalFrame 时靠这个还原 dock 标题）
    const menuTitle = String(route.name || '').trim()

    route.meta = {
      title: menuTitle,
      menuTitle,
      icon: route.icon,
      color: route.color,
      shape: route.shape,
      noCache: !route.keepAlive,
      menuId: route.id,
      subSystemId,
      clientId,
      manualUrl: route.manualUrl,
      portalKind
    }
    if (route.link) {
      route.meta.link = normalizeSubsystemIframeLink(route.link, clientId)
      if (portalKind === 'camstar') {
        route.meta.noCache = false
      }
    }
    if (route.portalHome) {
      route.meta.portalHome = true
    }
    route.hidden = !route.visible
    route.name = route.componentName && route.componentName.length > 0
      ? `${route.componentName}Sub${subSystemId}_${route.id}`
      : `SubMenu${subSystemId}_${route.id}`

    if (isIndexChild) {
      // Layout 下的 index，保持相对 path
    } else if (route.link && shellPath) {
      // 绝对 path：避免挂在目录下时被拼成 /portal/x/15/相对段
      route.parentId = 0
      route.path = `/portal/${clientId}/${shellPath}`
      wrapIframeLayout(route)
    } else if (!lastRouter) {
      route.path = `/portal/${clientId}/${shellPath || 'menu'}`
    } else {
      route.path = shellPath || 'menu'
    }

    const isIframeLayout = route.children && route.children.some(c => c && c.path === 'index' && (c.link || (c.meta && c.meta.link)))
    if (route.children && route.children.length && !isIframeLayout) {
      route.component = String(route.path).startsWith('/portal/') ? Layout : ParentView
    } else if (route.children && route.children.length) {
      route.component = Layout
    } else {
      // 绝不能 loadView(若依业务组件路径)
      route.component = loadView(PORTAL_IFRAME_EMPTY)
    }

    if (type && route.children) {
      route.children = filterChildren(route.children)
    }

    if (route.children != null && route.children && route.children.length) {
      route.children = filterSubSystemRouter(route.children, subSystemId, clientId, route, type)
      route.alwaysShow = route.alwaysShow !== undefined ? route.alwaysShow : true
    } else {
      delete route.children
      delete route.alwaysShow
    }
    return true
  })
}

function wrapIframeLayout(route) {
  if (!route.link || route.children) {
    return
  }
  const link = route.link
  const metaLink = (route.meta && route.meta.link) || normalizeSubsystemIframeLink(link, route.meta && route.meta.clientId)
  route.children = [{
    id: route.id,
    parentId: route.id,
    name: route.name,
    path: 'index',
    icon: route.icon,
    visible: route.visible,
    keepAlive: route.keepAlive,
    link,
    portalHome: route.portalHome,
    manualUrl: route.manualUrl,
    component: PORTAL_IFRAME_EMPTY,
    componentName: route.componentName,
    alwaysShow: false,
    meta: {
      title: route.meta && (route.meta.menuTitle || route.meta.title),
      menuTitle: route.meta && (route.meta.menuTitle || route.meta.title),
      icon: route.meta && route.meta.icon,
      link: metaLink,
      menuId: route.id,
      subSystemId: route.meta && route.meta.subSystemId,
      clientId: route.meta && route.meta.clientId,
      noCache: route.meta && route.meta.noCache,
      manualUrl: route.manualUrl,
      portalKind: route.meta && route.meta.portalKind
    }
  }]
  // 父级保留 meta.link，供 pathLinkMap / iframe 命中（去 /index 别名）
  route.link = undefined
  route.portalHome = undefined
  route.component = undefined
  route.componentName = undefined
  route.redirect = 'index'
}

function joinPortalRoutePath(base, segment) {
  if (!segment) {
    return base || ''
  }
  if (isExternal(segment) || /https?:\/\//i.test(segment)) {
    return base || ''
  }
  if (segment.startsWith('/')) {
    return segment
  }
  const normalizedBase = (base || '').replace(/\/+$/, '')
  return `${normalizedBase}/${segment}`.replace(/\/+/g, '/')
}

function buildPortalRewriteRoutes(routes) {
  const flat = []
  function walk(items, parentPath) {
    ;(items || []).forEach(route => {
      const currentPath = joinPortalRoutePath(parentPath, route.path)
      if (route.children && route.children.length) {
        walk(route.children, currentPath)
        return
      }
      if (!route.meta || !route.meta.link) {
        return
      }
      flat.push({
        path: currentPath,
        component: Layout,
        hidden: route.hidden,
        children: [{
          path: '',
          name: route.name,
          meta: { ...route.meta },
          component: loadView(PORTAL_IFRAME_EMPTY)
        }]
      })
    })
  }
  walk(routes, '')
  return flat
}

export default permission
