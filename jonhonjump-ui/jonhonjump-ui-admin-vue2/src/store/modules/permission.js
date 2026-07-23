import {constantRoutes} from '@/router'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView';
import {toCamelCase} from "@/utils";
import { buildPortalHomeMenu } from '@/utils/portalRoute'

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    sidebarRouters: [], // 左侧边菜单的路由，被 Sidebar/index.vue 使用
    topbarRouters: [] // 顶部菜单的路由，被 TopNav/index.vue 使用
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
    /**
     * 生成路由
     *
     * @param commit commit 函数
     * @param menus  路由参数
     */
    GenerateRoutes({commit, rootState}, menus) {
      return new Promise(resolve => {
        // 将 menus 菜单，转换为 route 路由数组
        const sdata = JSON.parse(JSON.stringify(menus || [])) // 【重要】用于菜单中的数据
        const rdata = JSON.parse(JSON.stringify(menus || [])) // 用于最后添加到 Router 中的数据
        const sidebarRoutes = filterAsyncRouter(sdata)
        const rewriteRoutes = filterAsyncRouter(rdata, false, true)
        rewriteRoutes.push({path: '*', redirect: '/404', hidden: true})
        commit('SET_ROUTES', rewriteRoutes)
        commit('SET_DEFAULT_ROUTES', sidebarRoutes)
        commit('SET_TOPBAR_ROUTES', sidebarRoutes)
        const mainSidebar = constantRoutes.concat(sidebarRoutes)
        // 主系统侧栏进门户缓存；若当前默认是子系统，不覆盖其侧栏
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
        // 后台预热只缓存，不覆盖当前壳的侧栏（避免切回主系统后又被预热抢壳）
        if (applyToLive !== false) {
          commit('SET_SIDEBAR_ROUTERS', sidebarRoutes)
          commit('SET_TOPBAR_ROUTES', sidebarRoutes)
        }
        resolve({ rewriteRoutes, nestedRewrite, sidebarRoutes })
      })
    }
  }
}

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    // 将 ruoyi 后端原有耦合前端的逻辑，迁移到此处
    // 处理 meta 属性
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
    // 处理 name 属性
    if (route.componentName && route.componentName.length > 0) {
      route.name = route.componentName
    } else {
      // 路由地址转首字母大写驼峰，作为路由名称，适配 keepAlive
      route.name = toCamelCase(route.path, true)
      // 处理三级及以上菜单路由缓存问题，将 path 名字赋值给 name
      if (route.path.indexOf("/") !== -1) {
        const pathArr = route.path.split("/");
        route.name = toCamelCase(pathArr[pathArr.length - 1], true)
      }
    }
    // 处理 component 属性
    if (route.children) { // 父节点
      if (route.parentId === 0) {
        route.component = Layout
      } else {
        route.component = ParentView
      }
    } else { // 根节点
      route.component = loadView(route.component)
    }

    // filterChildren
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
      route.alwaysShow = route.alwaysShow !== undefined ? route.alwaysShow  : true
    } else {
      delete route['children']
      delete route['alwaysShow'] // 如果没有子菜单，就不需要考虑 alwaysShow 字段
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  let children = [];
  childrenMap.forEach((el, index) => {
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

export const loadView = (view) => { // 路由懒加载
  return (resolve) => require([`@/views/${view}`], resolve)
}

function filterSubSystemRouter(asyncRouterMap, subSystemId, clientId, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    ensureTopLevelIframeRouteLayout(route)
    route.meta = {
      title: route.name,
      icon: route.icon,
      color: route.color,
      shape: route.shape,
      noCache: !route.keepAlive,
      menuId: route.id,
      subSystemId,
      clientId,
      manualUrl: route.manualUrl
    }
    if (route.link) {
      route.meta.link = route.link
    }
    if (route.portalHome) {
      route.meta.portalHome = true
    }
    route.hidden = !route.visible
    route.name = route.componentName && route.componentName.length > 0
      ? `${route.componentName}Sub${subSystemId}_${route.id}`
      : `SubMenu${subSystemId}_${route.id}`
    if (route.parentId === 0) {
      route.path = `/portal/${clientId}/${route.path}`
    }
    if (route.children) {
      if (route.parentId === 0) {
        route.component = Layout
      } else {
        route.component = ParentView
      }
    } else if (route.link) {
      route.component = loadView(route.component || 'system/subSystem/portal/Empty')
    } else {
      route.component = loadView(route.component)
    }
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterSubSystemRouter(route.children, subSystemId, clientId, route, type)
      route.alwaysShow = route.alwaysShow !== undefined ? route.alwaysShow : true
    } else {
      delete route['children']
      delete route['alwaysShow']
    }
    return true
  })
}

// 顶级 iframe 菜单必须挂在 Layout 下，否则 AppMain/IframeToggle 不会渲染
function ensureTopLevelIframeRouteLayout(route) {
  if (route.parentId !== 0 || !route.link || route.children) {
    return
  }
  route.children = [{
    id: route.id,
    parentId: route.id,
    name: route.name,
    path: 'index',
    icon: route.icon,
    visible: route.visible,
    keepAlive: route.keepAlive,
    link: route.link,
    portalHome: route.portalHome,
    manualUrl: route.manualUrl,
    component: route.component || 'system/subSystem/portal/Empty',
    componentName: route.componentName,
    alwaysShow: false
  }]
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
  if (segment.startsWith('/')) {
    return segment
  }
  const normalizedBase = (base || '').replace(/\/+$/, '')
  return `${normalizedBase}/${segment}`.replace(/\/+/g, '/')
}

// 子系统 iframe 菜单展平为顶层路由，避免动态 addRoutes 后 /portal/x/... 匹配失败
function buildPortalRewriteRoutes(routes) {
  const flat = []
  function walk(items, parentPath) {
    (items || []).forEach(route => {
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
          component: route.component
        }]
      })
    })
  }
  walk(routes, '')
  return flat
}

export default permission
