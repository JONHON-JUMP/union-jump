<template>
  <div
    :class="{ 'dock-expanded': dockExpanded, 'in-business': isPortalBusinessRoute }"
    class="jump-portal-shell"
    @click="handleShellClick"
  >
    <header class="portal-header" :class="{ 'is-collapsed': headerCollapsed }" @click.stop>
      <!-- 折叠态：细条顶栏，点击展开回完整顶栏 -->
      <button v-if="headerCollapsed" class="header-collapsed-bar" type="button"
              aria-label="展开顶栏" @click="headerCollapsed = false">
        <img class="collapsed-mark" :src="avicBrandLogo" alt="">
        <span class="collapsed-title">JUMP 中航光电统一制造管理平台</span>
        <i class="el-icon-arrow-down" />
      </button>
      <template v-else>
      <button class="brand" type="button" aria-label="返回门户首页" @click="goHome">
        <img class="brand-mark" :src="avicBrandLogo" :alt="avicBrandAlt">
        <span class="brand-copy">
          <strong>JUMP 中航光电统一制造管理平台</strong>
          <small>JONHON UNIFORM MANUFACTURING PLATFORM</small>
        </span>
      </button>

      <div class="header-actions">
        <div class="app-search">
          <i class="el-icon-search" />
          <input
            v-model.trim="searchKeyword"
            type="search"
            aria-label="搜索应用"
            placeholder="搜索应用"
            @focus="searchFocused = true"
            @keydown.esc="closeSearch"
          >
          <button v-if="searchKeyword" type="button" aria-label="清空搜索" @click="searchKeyword = ''">
            <i class="el-icon-circle-close" />
          </button>
          <div v-if="showSearchPanel" class="search-panel">
            <div class="search-panel__title">
              <span>应用搜索</span>
              <small>{{ filteredApps.length }} 个结果</small>
            </div>
            <button
              v-for="app in filteredApps.slice(0, 8)"
              :key="app.path"
              type="button"
              class="search-result"
              @mousedown.prevent="openApp(app)"
            >
              <span class="mini-icon" :style="iconStyle(app)">
                <svg-icon :icon-class="app.icon" />
              </span>
              <span>
                <strong>{{ app.name }}</strong>
                <small>{{ app.group }}</small>
              </span>
              <i class="el-icon-arrow-right" />
            </button>
            <div v-if="!filteredApps.length" class="search-empty">未找到匹配应用</div>
          </div>
        </div>

        <portal-system-switch @switch="handleSubsystemChange" />

        <button class="switch-user-btn" type="button" @click="handleSwitchUser">
          切换用户
        </button>

        <el-badge :value="todoBadgeValue" class="notice-badge">
          <button class="round-action" type="button" aria-label="待办任务" @click="goTodo">
            <i class="el-icon-bell" />
          </button>
        </el-badge>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <button class="user-entry" type="button" aria-label="用户菜单">
            <img v-if="avatar" :src="avatar" alt="">
            <span v-else>{{ userInitial }}</span>
          </button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="settings">平台设置</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <button class="round-action" type="button" aria-label="折叠顶栏" title="折叠顶栏，给页面更多空间"
                @click="headerCollapsed = true">
          <i class="el-icon-arrow-up" />
        </button>
      </div>
      </template>
    </header>

    <main class="portal-workspace" :class="{ 'is-iframe-host': isPortalIframeRoute }">
      <div class="workspace-content">
        <slot />
      </div>
    </main>

    <portal-dock
      :expanded="dockExpanded"
      collapsible
      @expand="expandDock"
      @collapse="dockExpanded = false"
      @all-apps="openAllAppsFromDock"
    />

    <all-apps-drawer
      ref="allAppsDrawer"
      :visible.sync="drawerVisible"
      :routes="currentSystemSidebarRouters || []"
      :system-key="currentSystem"
      :system-label="currentSystemLabel"
      :quick-nav-menu-ids="quickNavMenuIds"
      :quick-nav-locked-menu-ids="quickNavLockedMenuIds"
      :quick-nav-configured="quickNavConfigured"
      :sub-system-id="currentSubSystemId"
      :menus-loading="allAppsMenusLoading"
      @open="openAppFromDrawer"
      @quick-nav-change="handleQuickNavChangeFromDrawer"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { confirmSwitchUser, confirmLogout } from '@/utils/switchUser'
import { isExternal } from '@/utils/validate'
import { parsePortalClientId, resolvePortalFrameRoute, isMainBusinessPath, lookupPathLinkEntry, slashIpPortRestToHttp, encodeHttpToMesPath } from '@/utils/portalRoute'
import { ensureLocalCamstarCookie, seedCamstarCookieForUrlInBackground } from '@/utils/camstarCookie'
import { startCamstarOpenTrace, markCamstarOpen } from '@/utils/camstarOpenDiag'
import AllAppsDrawer from '@/views/components/AllAppsDrawer.vue'
import PortalDock from './PortalDock.vue'
import PortalSystemSwitch from './PortalSystemSwitch.vue'
import { AVIC_BRAND_LOGO, AVIC_BRAND_ALT } from '@/constants/brand'
import { buildIconStyle, resolveMenuColors } from '@/utils/menuIconStyle'
import { resolvePortalMenuIcon } from '@/utils/portalMenuIcon'
import {
  buildQuickNavScopeKey,
  getQuickNavCache,
  setQuickNavCache
} from '@/utils/portalQuickNavCache'
import { startQuickNavWatch, stopQuickNavWatch, rememberQuickNavSignature } from '@/utils/portalQuickNavWatch'
import { startPortalPermWatch, stopPortalPermWatch } from '@/utils/portalPermWatch'
import { getTodoTaskPage } from '@/api/bpm/task'
import { checkPermi } from '@/utils/permission'

function resolveMenuIconFields(icon, meta = {}) {
  const resolved = resolvePortalMenuIcon(icon, {
    name: meta.title || meta.name,
    path: meta.path
  })
  return {
    svgIcon: resolved.svgIcon,
    icon: resolved.icon,
    hasIcon: resolved.hasIcon
  }
}

export default {
  name: 'PortalShell',
  components: { AllAppsDrawer, PortalDock, PortalSystemSwitch },
  data() {
    return {
      avicBrandLogo: AVIC_BRAND_LOGO,
      avicBrandAlt: AVIC_BRAND_ALT,
      searchKeyword: '',
      searchFocused: false,
      drawerVisible: false,
      dockExpanded: false,
      headerCollapsed: false,
      quickNavMenuIds: [],
      quickNavLockedMenuIds: [],
      quickNavConfigured: false,
      todoCount: 0,
      todoRefreshTimer: null
    }
  },
  computed: {
    ...mapGetters(['avatar', 'nickname', 'name', 'sidebarRouters', 'currentSystemSidebarRouters', 'currentSystemLabel', 'currentSystem', 'portalSystemList', 'allAppsMenusLoading']),
    todoBadgeValue() {
      return this.todoCount > 0 ? this.todoCount : ''
    },
    currentSubSystemId() {
      if (this.currentSystem === 'main') {
        return 0
      }
      const sys = (this.portalSystemList || []).find(item => item.clientId === this.currentSystem)
      if (!sys || sys.subSystemId == null) {
        return null
      }
      return Number(sys.subSystemId) || null
    },
    quickNavScopeKey() {
      if (this.currentSystem === 'main') {
        return 'main'
      }
      const id = this.currentSubSystemId
      if (id == null || id <= 0) {
        return `pending:${this.currentSystem}`
      }
      return buildQuickNavScopeKey(this.currentSystem, id)
    },
    displayName() {
      return this.nickname || this.name || '制造同仁'
    },
    userInitial() {
      return this.displayName.slice(0, 1)
    },
    authorizedApps() {
      return this.flattenRoutes(this.currentSystemSidebarRouters || [])
    },
    filteredApps() {
      const keyword = this.searchKeyword.toLowerCase()
      if (!keyword) return this.authorizedApps.slice(0, 6)
      return this.authorizedApps.filter(app => `${app.name} ${app.group}`.toLowerCase().includes(keyword))
    },
    showSearchPanel() {
      return this.searchFocused && Boolean(this.searchKeyword)
    },
    isPortalIframeRoute() {
      const route = resolvePortalFrameRoute(this.$route, this.$store.state.portal.pathLinkMap, this.$store.state.portal.systemList)
      return Boolean(route.meta && route.meta.link)
    },
    /**
     * 顶栏折叠条件：离开门户首页进入任何业务菜单页（主系统组件页 + 子系统 iframe 页）都收起，
     * 回门户首页展开；停留当前页时以用户手动操作为准（折叠条展开 / 折叠按钮收起）
     */
    isPortalBusinessRoute() {
      const path = this.$route.path || '/'
      return path !== '/index' && path !== '/'
    }
  },
  watch: {
    currentSystem() {
      this.searchKeyword = ''
      this.searchFocused = false
      this.restoreQuickNavFromCache()
      this.loadQuickNav()
    },
    currentSubSystemId(newId, oldId) {
      if (this.currentSystem === 'main') {
        return
      }
      if (newId != null && newId > 0 && newId !== oldId) {
        this.restoreQuickNavFromCache()
        this.loadQuickNav()
      }
    },
    drawerVisible(visible) {
      if (visible) {
        this.restoreQuickNavFromCache()
        // 未就绪时立刻走 loading；并探测菜单/角色变更后的 rbac 版本后重拉
        this.$store.dispatch('portal/ensureAllAppsMenusReady').catch(() => {})
      }
    },
    '$route.path'(newPath) {
      this.dockExpanded = false
      this.loadTodoCount()
      this.restoreAllAppsDrawerOnHome(newPath)
    },
    // 进入业务菜单（主系统或子系统）自动收起顶栏给页面让空间，回门户首页自动展开；
    // 折叠条可点击展开、展开态右上角箭头可再收起（停留当前路由时保持用户选择）
    // immediate：直接在业务页刷新时初始即处于业务路由，也要收起
    isPortalBusinessRoute: {
      immediate: true,
      handler(inBusiness) {
        this.headerCollapsed = inBusiness
        this.scheduleInitialIframeNudge()
      }
    },
    // 折叠/展开、dock 展开收起都会改变 iframe 可视高度；跨域子系统不监听 resize，
    // 不会重算内部布局，导致内容按旧高度渲染、底部被裁且无滚动条。
    // 变化后对可见 iframe 做 1px 宽度抖动，强制其触发内部 resize 重算。
    headerCollapsed() {
      this.$nextTick(() => this.nudgeVisibleIframes())
    },
    dockExpanded() {
      this.$nextTick(() => this.nudgeVisibleIframes())
    }
  },
  mounted() {
    this._onPortalOpenAllApps = (keyword) => {
      this.drawerVisible = true
      this.$nextTick(() => {
        if (this.$refs.allAppsDrawer) {
          this.$refs.allAppsDrawer.openWithKeyword(keyword || '')
        }
      })
    }
    this._onPortalQuickNavChanged = (payload) => {
      if (!payload || payload.scopeKey !== this.quickNavScopeKey) {
        return
      }
      // shell 自己发出的不回写；watch / panel / drawer 都要同步到 Dock
      if (payload.source === 'shell') {
        return
      }
      this.handleQuickNavChange(payload)
    }
    this.$root.$on('portal-open-all-apps', this._onPortalOpenAllApps)
    this.$root.$on('portal-quick-nav-changed', this._onPortalQuickNavChanged)
    this.restoreQuickNavFromCache()
    this.loadQuickNav()
    startQuickNavWatch(this.$router)
    startPortalPermWatch(this.$router)
    this.loadTodoCount()
    this.todoRefreshTimer = window.setInterval(() => {
      this.loadTodoCount()
    }, 60000)
    this.setupDockSpaceSync()
  },
  beforeDestroy() {
    this.teardownDockSpaceSync()
    stopQuickNavWatch()
    stopPortalPermWatch()
    if (this._onPortalOpenAllApps) {
      this.$root.$off('portal-open-all-apps', this._onPortalOpenAllApps)
      this._onPortalOpenAllApps = null
    }
    if (this._onPortalQuickNavChanged) {
      this.$root.$off('portal-quick-nav-changed', this._onPortalQuickNavChanged)
      this._onPortalQuickNavChanged = null
    }
    if (this.todoRefreshTimer) {
      window.clearInterval(this.todoRefreshTimer)
      this.todoRefreshTimer = null
    }
  },
  methods: {
    iconStyle(app) {
      return buildIconStyle(app)
    },
    handleShellClick() {
      this.searchFocused = false
      if (this.dockExpanded) this.dockExpanded = false
    },
    handleQuickNavChange(payload) {
      this.quickNavMenuIds = (payload && payload.menuIds) || []
      this.quickNavLockedMenuIds = (payload && payload.lockedMenuIds) || []
      this.quickNavConfigured = !!(payload && payload.configured)
    },
    restoreQuickNavFromCache() {
      const cached = getQuickNavCache(this.quickNavScopeKey)
      if (cached) {
        this.quickNavMenuIds = [...cached.menuIds]
        this.quickNavLockedMenuIds = [...(cached.lockedMenuIds || [])]
        this.quickNavConfigured = cached.configured
        return
      }
      this.quickNavMenuIds = []
      this.quickNavLockedMenuIds = []
      this.quickNavConfigured = false
    },
    loadQuickNav() {
      const scopeKey = this.quickNavScopeKey
      if (this.currentSystem !== 'main' && (this.currentSubSystemId == null || this.currentSubSystemId <= 0)) {
        return Promise.resolve()
      }
      return this.$store.dispatch('portal/loadQuickNavConfig', {
        subSystemId: this.currentSubSystemId,
        force: false
      }).then(config => {
        if (scopeKey !== this.quickNavScopeKey) {
          return
        }
        const menuIds = (config && config.menuIds) || []
        const lockedMenuIds = (config && config.lockedMenuIds) || []
        const configured = !!(config && config.configured)
        const apps = config && Object.prototype.hasOwnProperty.call(config, 'apps')
          ? (Array.isArray(config.apps) ? config.apps : [])
          : null
        this.quickNavMenuIds = menuIds
        this.quickNavLockedMenuIds = lockedMenuIds
        this.quickNavConfigured = configured
        setQuickNavCache(scopeKey, menuIds, configured, lockedMenuIds, apps)
        rememberQuickNavSignature(scopeKey, menuIds, lockedMenuIds, apps)
      }).catch(() => {
        if (scopeKey === this.quickNavScopeKey) {
          this.quickNavMenuIds = []
          this.quickNavLockedMenuIds = []
          this.quickNavConfigured = false
        }
      })
    },
    handleQuickNavChangeFromDrawer(payload) {
      this.handleQuickNavChange(payload)
      const menuIds = (payload && payload.menuIds) || []
      const configured = !!(payload && payload.configured)
      const lockedMenuIds = (payload && payload.lockedMenuIds) || this.quickNavLockedMenuIds
      const apps = payload && Object.prototype.hasOwnProperty.call(payload, 'apps')
        ? (payload.apps || [])
        : undefined
      setQuickNavCache(this.quickNavScopeKey, menuIds, configured, lockedMenuIds, apps)
      this.$root.$emit('portal-quick-nav-changed', {
        menuIds,
        lockedMenuIds,
        configured,
        apps,
        scopeKey: this.quickNavScopeKey,
        source: 'shell'
      })
    },
    expandDock() {
      this.dockExpanded = true
    },
    /** dock 上的「全部应用」：打开抽屉同时收起 dock，避免挡住页面 */
    openAllAppsFromDock() {
      this.dockExpanded = false
      this.drawerVisible = true
    },
    handleSubsystemChange(value) {
      if (value === this.currentSystem) return
      // 切换系统停在门户首页：先快捷导航（watch currentSystem → loadQuickNav），
      // 全量 my-menus / 主系统菜单树后台 warm，与登录约定一致
      this.$store.dispatch('portal/switchSystem', { system: value, stayOnPortalHome: true }).catch(err => {
        this.$message.error(typeof err === 'string' ? err : (err.message || '切换系统失败'))
      })
    },
    flattenRoutes(routes, basePath = '', group = '') {
      const result = []
      routes.forEach(route => {
        if (!route || route.hidden || route.path === '*' || route.path === '/404') return
        const title = route.meta && route.meta.title
        const path = this.resolveRoutePath(basePath, route.path)
        const hasChildren = route.children && route.children.length > 0
        if (title && path !== '/index' && route.redirect !== 'noRedirect' && !hasChildren) {
          result.push({
            name: title,
            group: group || '授权菜单',
            path,
            ...resolveMenuIconFields((route.meta && route.meta.icon) || '', { title, path }),
            ...resolveMenuColors(route.meta || {})
          })
        }
        if (route.children) result.push(...this.flattenRoutes(route.children, path, title || group))
      })
      return result
    },
    resolveRoutePath(basePath, routePath) {
      if (!routePath) return basePath || '/'
      if (isExternal(routePath) || routePath.charAt(0) === '/') return routePath
      return `${basePath}/${routePath}`.replace(/\/+/g, '/')
    },
    /** 从「全部应用」抽屉打开菜单：记住来源，关闭菜单回到首页时自动重开抽屉 */
    openAppFromDrawer(app) {
      try { sessionStorage.setItem('JUMP_ALLAPPS_RETURN', '1') } catch (e) { /* ignore */ }
      this.openApp(app)
    },
    openApp(app) {
      this.searchFocused = false
      this.drawerVisible = false
      if (!app || !app.path) return
      if (this.currentSystem !== 'main' && isMainBusinessPath(app.path)) {
        // 禁止 lock 全屏：会挡住 dock/菜单，用户以为「卡死不能点」
        this.$store.dispatch('portal/switchSystem', { system: 'main', skipNavigate: true })
          .then(() => this.$router.push(app.path))
          .catch(err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入主系统失败'))
          })
        return
      }
      if (isExternal(app.path)) {
        if (/^https?:/.test(app.path)) {
          const openedWindow = window.open(app.path, '_blank', 'noopener,noreferrer')
          if (openedWindow) openedWindow.opener = null
        } else {
          window.location.href = app.path
        }
        return
      }
      const clientId = parsePortalClientId(app.path)
      if (clientId) {
        // 快捷导航 apps 可能仍是旧斜杠编码；对齐侧栏 IP9port，否则 pathLinkMap 对不上、页打不开
        let targetPath = app.path
        const rest0 = String(app.path).replace(new RegExp('^/portal/' + clientId + '/'), '')
        const asHttp0 = slashIpPortRestToHttp(String(rest0).replace(/:/g, '/'))
        if (asHttp0) {
          targetPath = `/portal/${clientId}/` + encodeHttpToMesPath(asHttp0)
        }
        if (this.$route.path === targetPath) {
          return
        }
        const menusReady = !!(this.$store.state.portal.loadedSubSystems || {})[clientId]
        const activeMap = this.$store.state.portal.pathLinkMap || {}
        const cachedMap = (this.$store.state.portal.subSystemPathLinkCache || {})[clientId] || {}
        const entry = activeMap[targetPath] || lookupPathLinkEntry(targetPath, activeMap)
          || cachedMap[targetPath] || lookupPathLinkEntry(targetPath, cachedMap)
          || activeMap[app.path] || lookupPathLinkEntry(app.path, activeMap)
          || cachedMap[app.path] || lookupPathLinkEntry(app.path, cachedMap)
        const link = (entry && entry.link) || ''
        const rest = String(targetPath).replace(new RegExp('^/portal/' + clientId + '/'), '')
        const isDirect = (/^https?:\/\//i.test(link) && link.indexOf('#') < 0)
          || !!slashIpPortRestToHttp(rest.replace(/:/g, '/'))
        const resolvedLink = link || slashIpPortRestToHttp(rest.replace(/:/g, '/')) || ''

        const pushWithTitle = () => {
          return this.$router.push(targetPath).then(() => {
            if (app.name) {
              this.$store.dispatch('tagsView/updateVisitedView', {
                path: targetPath,
                name: 'PortalFrame',
                title: app.name,
                meta: {
                  title: app.name,
                  link: resolvedLink || undefined,
                  icon: app.svgIcon || app.icon
                }
              })
            }
          }).catch(() => {})
        }

        const openDirect = () => {
          // 对齐 4200：只切壳 + 立刻 push，绝不 await 全量菜单（那会到 10s+）
          const traceId = startCamstarOpenTrace({
            path: targetPath,
            link: resolvedLink,
            clientId,
            title: app.name
          })
          const tCookie0 = Date.now()
          ensureLocalCamstarCookie()
          markCamstarOpen(traceId, 'cookie', { ms: Date.now() - tCookie0 })
          if (resolvedLink) {
            seedCamstarCookieForUrlInBackground(resolvedLink)
          }
          const afterPush = () => {
            markCamstarOpen(traceId, 'navigate', { path: targetPath })
            try {
              sessionStorage.setItem('JUMP_CAMSTAR_TRACE', String(traceId))
            } catch (e) { /* ignore */ }
            if (!menusReady) {
              this.$store.dispatch('portal/ensureSubSystemLoaded', {
                clientId,
                activate: false,
                force: false
              }).catch(() => {})
            }
          }
          const needShell = this.$store.state.portal.currentSystem !== clientId
            || !(this.$store.state.portal.pathLinkMap && Object.keys(this.$store.state.portal.pathLinkMap).length)
          if (needShell) {
            const tShell = Date.now()
            return this.$store.dispatch('portal/activateSubSystemShell', { clientId })
              .then(() => {
                markCamstarOpen(traceId, 'shell', { ms: Date.now() - tShell })
                return pushWithTitle()
              })
              .then(afterPush)
          }
          markCamstarOpen(traceId, 'shell', { ms: 0, skipped: true })
          return Promise.resolve(pushWithTitle()).then(afterPush)
        }

        // Camstar：一律直开（有 path 编码或 link 即可）
        if (isDirect) {
          return openDirect().catch(err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入子系统失败'))
          })
        }

        // 若依：菜单未就绪时后台拉，不锁全屏；业务区由 InnerLink 自己提示
        if (!menusReady) {
          this.$message({ message: '正在准备子系统菜单…', type: 'info', duration: 1500 })
        }
        this.$store.dispatch('portal/ensureSubSystemReady', {
          clientId,
          skipSso: false
        })
          .then(() => {
            if (this.$store.state.portal.currentSystem !== clientId) {
              return
            }
            return pushWithTitle()
          })
          .catch(err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入子系统失败'))
          })
        return
      }
      if (this.$route.path !== app.path) {
        this.$router.push(app.path)
      }
    },
    openByKeyword(keyword) {
      const app = this.authorizedApps.find(item => item.name.includes(keyword) || item.group.includes(keyword))
      if (app) {
        this.openApp(app)
        return
      }
      this.drawerVisible = true
      this.$nextTick(() => {
        if (this.$refs.allAppsDrawer) this.$refs.allAppsDrawer.openWithKeyword(keyword)
      })
    },
    closeSearch() {
      this.searchKeyword = ''
      this.searchFocused = false
    },
    /**
     * 1px 宽度抖动可见 iframe：跨域子系统收不到父页 resize 事件，
     * 靠抖动触发其内部 window.resize 重算布局，修复底部内容滞留旧视口
     */
    nudgeVisibleIframes() {
      window.requestAnimationFrame(() => {
        document.querySelectorAll('iframe.inner-link__frame').forEach(frame => {
          // 隐藏的保温帧跳过，等它再次可见时高度自然按新容器渲染
          if (frame.offsetWidth <= 0 || frame.offsetHeight <= 0) {
            return
          }
          const originalW = frame.style.width
          const originalH = frame.style.height
          frame.style.width = 'calc(100% - 1px)'
          frame.style.height = 'calc(100% - 1px)'
          window.requestAnimationFrame(() => {
            frame.style.width = originalW || ''
            frame.style.height = originalH || ''
          })
        })
      })
    },
    /**
     * 首次进入业务页：顶栏初始折叠/子系统 iframe 加载都会引起容器高度变化，
     * 页面稳定后补一次抖动，确保子系统按最终高度重算
     */
    scheduleInitialIframeNudge() {
      window.setTimeout(() => this.nudgeVisibleIframes(), 600)
      window.setTimeout(() => this.nudgeVisibleIframes(), 1500)
    },
    /** dock 占位变化后防抖补抖（与 setupDockSpaceSync / padding transition 联动） */
    scheduleDockSpaceIframeNudge() {
      if (this._dockSpaceNudgeTimer) {
        clearTimeout(this._dockSpaceNudgeTimer)
      }
      this._dockSpaceNudgeTimer = window.setTimeout(() => {
        this._dockSpaceNudgeTimer = null
        this.nudgeVisibleIframes()
      }, 320)
    },
    /**
     * 实测 dock 占位，替代 CSS 里 100/116/44 的估算值：
     * --dock-space 由 dock 真实渲染位置（顶边到视口底的距离）逐帧写回，
     * 展开/收起动画的中间态、未来改 dock 样式都不会再出现"预留不足 → 内容被盖"。
     * ResizeObserver 不支持时静默退回 CSS 估算值。
     */
    setupDockSpaceSync() {
      if (typeof ResizeObserver === 'undefined') {
        return
      }
      const shellEl = this.$el
      const dockEl = shellEl.querySelector('.portal-taskbar')
      if (!shellEl || !dockEl) {
        return
      }
      const sync = () => {
        const rect = dockEl.getBoundingClientRect()
        if (!rect || rect.height <= 0) {
          return
        }
        // dock 顶边到视口底 + 安全缝（含阴影/亚像素）；不低于 24px 兜底
        const space = Math.max(24, Math.ceil(window.innerHeight - rect.top) + 10)
        shellEl.style.setProperty('--dock-space', space + 'px')
        // 跨域 iframe（Camstar 等）收不到 resize：占位变化后必须补抖，否则仍按旧高度渲染、底栏文字被裁
        this.scheduleDockSpaceIframeNudge()
      }
      this._dockSpaceObserver = new ResizeObserver(sync)
      this._dockSpaceObserver.observe(dockEl)
      this._dockSpaceSync = sync
      window.addEventListener('resize', this._dockSpaceSync)
      // padding-bottom 有 transition：动画结束后再抖一次，避免中间态高度错误被业务页锁死
      this._dockSpaceTransitionEnd = (ev) => {
        if (ev.target === shellEl && ev.propertyName === 'padding-bottom') {
          this.scheduleDockSpaceIframeNudge()
        }
      }
      shellEl.addEventListener('transitionend', this._dockSpaceTransitionEnd)
      // 首帧 + 动画期间 ResizeObserver 会连续回调，padding 的 transition 让跟随保持平滑
      sync()
    },
    teardownDockSpaceSync() {
      if (this._dockSpaceObserver) {
        this._dockSpaceObserver.disconnect()
        this._dockSpaceObserver = null
      }
      if (this._dockSpaceSync) {
        window.removeEventListener('resize', this._dockSpaceSync)
        this._dockSpaceSync = null
      }
      if (this._dockSpaceTransitionEnd && this.$el) {
        this.$el.removeEventListener('transitionend', this._dockSpaceTransitionEnd)
        this._dockSpaceTransitionEnd = null
      }
      if (this._dockSpaceNudgeTimer) {
        clearTimeout(this._dockSpaceNudgeTimer)
        this._dockSpaceNudgeTimer = null
      }
      if (this.$el) {
        this.$el.style.removeProperty('--dock-space')
      }
    },
    goHome() {
      if (this.$route.path === '/index' || this.$route.path === '/') return
      // 主动回首页：不自动重开「全部应用」抽屉
      try { sessionStorage.removeItem('JUMP_ALLAPPS_RETURN') } catch (e) { /* ignore */ }
      this.$store.dispatch('portal/navigateToPortalHome').catch(() => {})
    },
    /**
     * 关闭从「全部应用」打开的菜单回到门户首页时，自动重新打开抽屉，
     * 用户可继续浏览/选择下一个应用（主系统与子系统同逻辑）
     */
    restoreAllAppsDrawerOnHome(newPath) {
      if (newPath !== '/index' && newPath !== '/') {
        return
      }
      let reopen = false
      try { reopen = sessionStorage.getItem('JUMP_ALLAPPS_RETURN') === '1' } catch (e) { /* ignore */ }
      if (reopen) {
        try { sessionStorage.removeItem('JUMP_ALLAPPS_RETURN') } catch (e) { /* ignore */ }
        this.drawerVisible = true
      }
    },
    loadTodoCount() {
      if (!checkPermi(['bpm:task:query'])) {
        this.todoCount = 0
        return Promise.resolve()
      }
      return getTodoTaskPage({ pageNo: 1, pageSize: 1 }, true).then(response => {
        this.todoCount = (response.data && response.data.total) || 0
      }).catch(() => {
        this.todoCount = 0
      })
    },
    goTodo() {
      if (this.$route.path === '/index' || this.$route.path === '/') {
        // 首页已打开时：直接通知切到待办（仅改 query 可能因重复导航不触发 watch）
        this.$root.$emit('portal-open-workbench', 'todo')
        if (this.$route.query.workbench !== 'todo') {
          this.$router.replace({
            path: '/index',
            query: { ...this.$route.query, workbench: 'todo' }
          }).catch(() => {})
        }
        return
      }
      const app = this.authorizedApps.find(item => item.path === '/bpm/task/todo')
      if (app) {
        this.openApp(app)
        return
      }
      this.$router.push({ path: '/index', query: { workbench: 'todo' } }).catch(() => {})
    },
    handleSwitchUser() {
      // 换人后固定 /index，由 bootstrap 按新用户星标默认系统进入
      confirmSwitchUser(this.$store)
    },
    handleLogout() {
      confirmLogout(this.$store)
    },
    handleUserCommand(command) {
      if (command === 'profile') this.$router.push('/user/profile')
      else if (command === 'settings') this.$message.info('平台设置入口已打开，可在此接入个人偏好配置')
      else if (command === 'logout') {
        this.handleLogout()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #087ce5;
$ink: #10233e;
$muted: #5d718c;
$canvas: #eaf4fc;

.jump-portal-shell {
  /* dock 为 fixed 悬浮：高 82px + 底边距 18px = 实占 100px，内容须预留完整空间，
     否则 iframe 页底部会被 dock 盖住 */
  --dock-space: 100px;
  display: flex;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  padding: 22px 26px var(--dock-space);
  box-sizing: border-box;
  flex-direction: column;
  color: $ink;
  background:
    radial-gradient(circle at 2% 2%, rgba(67, 183, 239, .18), transparent 30%),
    radial-gradient(circle at 98% 18%, rgba(42, 195, 172, .14), transparent 28%),
    radial-gradient(circle at 58% 100%, rgba(255, 192, 75, .11), transparent 24%),
    $canvas;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  transition: padding-bottom .26s cubic-bezier(.16, 1, .3, 1);
}

.jump-portal-shell.dock-expanded {
  --dock-space: 116px;
}

/* 业务页：dock 默认收成小把手（24px+边距14px），内容只预留把手空间，给页面让高度；
   点把手临时展开时仍按完整高度预留 */
.jump-portal-shell.in-business {
  --dock-space: 44px;
}

.jump-portal-shell.in-business.dock-expanded {
  --dock-space: 116px;
}

button,
input { font: inherit; }
button { color: inherit; }

.portal-header {
  position: relative;
  z-index: 20;
  display: flex;
  flex: 0 0 auto;
  min-height: 86px;
  padding: 12px 16px;
  align-items: center;
  justify-content: space-between;
  border-radius: 16px;
  background: rgba(255, 255, 255, .95);
  box-shadow: 0 6px 16px rgba(45, 91, 130, .08);
}

/* 折叠态：细条顶栏，为业务页面让出空间 */
.portal-header.is-collapsed {
  min-height: 0;
  padding: 4px 10px;
  border-radius: 10px;
}

.header-collapsed-bar {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 10px;
  border: 0;
  padding: 2px 6px;
  background: transparent;
  cursor: pointer;
}

.header-collapsed-bar .collapsed-mark {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.header-collapsed-bar .collapsed-title {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: #25435f;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-collapsed-bar .el-icon-arrow-down {
  margin-left: auto;
  color: #6b8aa9;
}

.brand {
  display: flex;
  min-width: 0;
  padding: 0;
  align-items: center;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.brand-mark {
  display: block;
  flex: 0 0 60px;
  width: 60px;
  height: 60px;
  padding: 4px;
  border-radius: 16px;
  object-fit: contain;
  background: #fff;
  box-shadow: 0 8px 16px rgba(8, 124, 229, .22);
}

.brand-copy {
  display: flex;
  min-width: 0;
  margin-left: 15px;
  flex-direction: column;
}
.brand-copy strong { overflow: hidden; font-size: 22px; line-height: 1.35; white-space: nowrap; text-overflow: ellipsis; }
.brand-copy small { margin-top: 3px; color: $muted; font-size: 13px; }
.header-actions { display: flex; align-items: center; }
.header-actions > * + * { margin-left: 10px; }
.switch-user-btn {
  height: 38px;
  padding: 0 14px;
  border: 1px solid #f5c06a;
  border-radius: 999px;
  background: #fff7e6;
  color: #b88230;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background .2s ease, box-shadow .2s ease;
}
.switch-user-btn:hover {
  outline: none;
  background: #ffefd2;
  box-shadow: 0 0 0 3px rgba(230, 162, 60, .16);
}
.switch-user-btn:focus {
  outline: none;
  background: #ffefd2;
  box-shadow: 0 0 0 3px rgba(230, 162, 60, .16);
}

.app-search {
  position: relative;
  display: flex;
  width: clamp(280px, 27vw, 400px);
  height: 52px;
  padding: 0 16px;
  align-items: center;
  border-radius: 14px;
  background: #e8f2fc;
  transition: box-shadow .2s ease, background .2s ease;
}
.app-search:focus-within { background: #fff; box-shadow: 0 0 0 3px rgba(8, 124, 229, .16); }
.app-search > i { color: #54708f; font-size: 18px; }
.app-search input { width: 100%; height: 100%; padding: 0 10px; border: 0; outline: 0; color: $ink; background: transparent; font-size: 16px; }
.app-search input::placeholder { color: #607590; }
.app-search > button { padding: 6px; border: 0; background: transparent; cursor: pointer; }

.search-panel {
  position: absolute;
  top: 60px;
  right: 0;
  left: 0;
  overflow: hidden;
  padding: 10px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 16px 34px rgba(34, 75, 112, .18);
}
.search-panel__title { display: flex; padding: 6px 8px 10px; justify-content: space-between; }
.search-panel__title span { font-weight: 700; }
.search-panel__title small,
.search-result small { color: $muted; }
.search-result { display: flex; width: 100%; padding: 9px 8px; align-items: center; border: 0; border-radius: 10px; background: transparent; text-align: left; cursor: pointer; }
.search-result:hover { outline: none; background: #edf6fd; }
.search-result:focus { outline: none; background: #edf6fd; }
.search-result > span:nth-child(2) { display: flex; min-width: 0; margin-left: 10px; flex: 1; flex-direction: column; }
.search-result small { margin-top: 2px; font-size: 12px; }
.search-result > i { color: #7790aa; }
.search-empty { padding: 24px 12px; color: $muted; text-align: center; }
.mini-icon { display: grid; flex: 0 0 42px; width: 42px; height: 42px; place-items: center; border-radius: 12px; }
.mini-icon .svg-icon { width: 22px; height: 22px; }

.system-chip {
  display: flex;
  height: 48px;
  padding: 0 8px 0 15px;
  align-items: center;
  border-radius: 24px;
  color: #44607f;
  background: #e3f1fd;
  font-size: 14px;
  white-space: nowrap;
}
.system-chip strong { color: #075eb5; }
.system-switch { height: 30px; margin-left: 9px; padding: 0 10px; border: 0; border-radius: 15px; color: #075eb5; background: rgba(255, 255, 255, .72); font-size: 12px; font-weight: 600; cursor: pointer; }
.system-switch:hover { outline: none; background: #fff; }
.system-switch:focus { outline: none; background: #fff; }
.status-dot { display: inline-block; width: 9px; height: 9px; margin-right: 8px; border-radius: 50%; background: #11a574; box-shadow: 0 0 0 4px rgba(17, 165, 116, .12); }

.round-action,
.user-entry { display: grid; width: 50px; height: 50px; padding: 0; place-items: center; border: 0; border-radius: 50%; cursor: pointer; }
.round-action { color: #29435f; background: #eaf3fb; font-size: 20px; }
.round-action:hover { outline: 3px solid rgba(8, 124, 229, .16); background: #dcecf9; }
.round-action:focus { outline: 3px solid rgba(8, 124, 229, .16); background: #dcecf9; }
.user-entry { overflow: hidden; color: #fff; background: $primary; font-size: 19px; font-weight: 700; }
.user-entry img { width: 100%; height: 100%; object-fit: cover; }
.notice-badge ::v-deep .el-badge__content { top: 8px; right: 10px; }

.portal-workspace {
  display: flex;
  min-height: 0;
  margin-top: 18px;
  overflow: hidden;
  border-radius: 16px;
  background: rgba(255, 255, 255, .96);
  box-shadow: 0 8px 22px rgba(34, 112, 166, .08);
  flex: 1 1 auto;
  flex-direction: column;
}

.portal-workspace.is-iframe-host {
  min-height: 0;
}
.workspace-content {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  scroll-padding-bottom: var(--dock-space, 100px);
}
.workspace-content > .app-main {
  flex: 1 1 auto;
  min-height: 0;
}
.portal-workspace.is-iframe-host .workspace-content {
  overflow: hidden;
}

@media (max-width: 1280px) {
  .portal-header { min-height: 76px; }
  .brand-mark { flex-basis: 52px; width: 52px; height: 52px; }
  .brand-copy strong { font-size: 19px; }
  .brand-copy small { display: none; }
  .app-search { width: 270px; }
  .system-chip span:not(.status-dot) { display: none; }
}

@media (max-width: 1080px) {
  .jump-portal-shell { padding-right: 18px; padding-left: 18px; }
  .portal-header { align-items: flex-start; flex-direction: column; }
  .portal-header > * + * { margin-top: 12px; }
  .header-actions { width: 100%; }
  .app-search { width: auto; flex: 1; }
}

@media (max-width: 820px) {
  .jump-portal-shell { --dock-space: 100px; padding: 12px 12px var(--dock-space); }
  .jump-portal-shell.dock-expanded { --dock-space: 116px; }
  .brand-copy strong { font-size: 17px; }
}

@media (max-width: 560px) {
  .notice-badge,
  .user-entry { display: none; }
  .header-actions { flex-wrap: wrap; }
  .app-search { flex-basis: 100%; }
  .system-chip { max-width: 100%; }
  .brand-mark { flex-basis: 46px; width: 46px; height: 46px; border-radius: 13px; }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; animation-iteration-count: 1 !important; }
}
</style>
