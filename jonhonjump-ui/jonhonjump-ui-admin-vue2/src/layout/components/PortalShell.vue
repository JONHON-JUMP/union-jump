<template>
  <div
    :class="{ 'dock-expanded': dockExpanded, 'in-business': isPortalBusinessRoute, 'no-anim': shellAnimSuppressed, 'dock-animating': dockAnimating }"
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
      :animate="dockAnimating"
      collapsible
      @expand="expandDock"
      @collapse="collapseDock(false)"
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
      :workshop-code="currentWorkshopCode"
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
import { parsePortalClientId, alignPortalPathToCurrentLeaf, resolvePortalFrameRoute, isMainBusinessPath, lookupPathLinkEntry, slashIpPortRestToHttp, encodeHttpToMesPath, httpUrlToPortalLocation } from '@/utils/portalRoute'
import { ensureLocalCamstarCookie, seedCamstarCookieForUrlInBackground } from '@/utils/camstarCookie'
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
import { rememberQuickNavSignature } from '@/utils/portalQuickNavWatch'
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
      dockAnimating: false,
      headerCollapsed: false,
      /** 路由驱动的收起瞬时完成，勿做 padding 过渡（否则进业务页页面闪一下） */
      shellAnimSuppressed: false,
      quickNavMenuIds: [],
      quickNavLockedMenuIds: [],
      quickNavConfigured: false,
      todoCount: 0
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
    currentWorkshopCode() {
      if (this.currentSystem === 'main') {
        return ''
      }
      const sys = (this.portalSystemList || []).find(item => item.clientId === this.currentSystem)
      return sys && sys.workshopId ? String(sys.workshopId).trim() : ''
    },
    quickNavScopeKey() {
      if (this.currentSystem === 'main') {
        return 'main'
      }
      const id = this.currentSubSystemId
      if (id == null || id <= 0) {
        return `pending:${this.currentSystem}`
      }
      return buildQuickNavScopeKey(this.currentSystem, id, this.currentWorkshopCode)
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
      if (route.meta && route.meta.link) {
        return true
      }
      // 和 AppMain 一样做兜底：切页瞬间 link 可能还没解析出来，
      // 这里若短暂变 false，.portal-workspace 会从 overflow:hidden 切回 auto，
      // 父级滚动条一出现/消失会改 iframe 宽度，Camstar 跟着重排（滚动条长度变化、页面上下抖）。
      const clientId = parsePortalClientId(this.$route.path)
      return !!(clientId && (this.$route.name === 'PortalFrame' || this.$route.name === 'PortalFrameLegacy'))
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
      // switchSystem 已预拉快捷导航；有 apps 缓存则不再打接口，避免切换下拉「卡第二下」
      const cached = getQuickNavCache(this.quickNavScopeKey)
      if (cached && Array.isArray(cached.apps)) {
        return
      }
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
    // 车间号晚到时 scope 会从「整系统」变成「某一车间」。不重拉的话，界面仍画着系统级列表，
    // 下一次加星却写进空的车间分区，回写结果就只剩新加的那一条。
    currentWorkshopCode(newCode, oldCode) {
      if (this.currentSystem === 'main' || newCode === oldCode) {
        return
      }
      this.restoreQuickNavFromCache()
      this.loadQuickNav()
    },
    drawerVisible(visible) {
      if (visible) {
        this.restoreQuickNavFromCache()
        // 未就绪时立刻走 loading；并探测菜单/角色变更后的 rbac 版本后重拉
        this.$store.dispatch('portal/ensureAllAppsMenusReady').catch(() => {})
      }
    },
    '$route.path'(newPath) {
      // 不再在每次切页时切换 .no-anim（transition:none !important）：
      // 这个类会让整个 shell 强制重算样式，iframe 多时就是切页抖一下的放大器。
      // dock 收起本身走 collapseDock(false)，且业务页 --dock-space 恒定，不需要抑制过渡。
      this.clearDockSpaceInline()
      this.collapseDock(false)
      this.resetWorkspaceScroll()
      this.loadTodoCount()
    },
    // 进入业务菜单（主系统或子系统）自动收起顶栏给页面让空间，回门户首页自动展开；
    // 折叠条可点击展开、展开态右上角箭头可再收起（停留当前路由时保持用户选择）
    // immediate：直接在业务页刷新时初始即处于业务路由，也要收起
    isPortalBusinessRoute: {
      immediate: true,
      handler(inBusiness) {
        this.headerCollapsed = inBusiness
        if (inBusiness) {
          this.suppressShellTransition()
          this.scheduleInitialIframeNudge()
        }
      }
    },
    // 折叠/展开、dock 展开收起都会改变 iframe 可视高度；跨域子系统不监听 resize，
    // 不会重算内部布局，导致内容按旧高度渲染、底部被裁且无滚动条。
    // 变化后对可见 iframe 做 1px 宽度抖动，强制其触发内部 resize 重算。
    headerCollapsed() {
      this.$nextTick(() => this.nudgeVisibleIframes())
      // 顶栏折叠/展开会改变 iframe 可用高度，通知 iframe 层重新量一次并重新锁定
      this.notifyLayoutChanged()
    }
    // dockExpanded 不在这里抖 iframe：手动展开/收起在 expandDock/collapseDock 里补一次，
    // 路由驱动的收起是瞬时布局变化，浏览器本身会触发子页 resize，不再额外抖动
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
    // 业务页按钮打开跨 host 新窗口时，业务页脚本会先通知门户给目标 host 种 Cookie
    this._onSeedUrlMessage = (event) => {
      const data = event && event.data
      if (!data || data.type !== 'JUMP_PORTAL_SEED_URL' || !data.url) {
        return
      }
      const url = String(data.url)
      if (!/^https?:/i.test(url)) {
        return
      }
      // 不传当前系统 id：新窗口可能是另一个系统的 host，按目标 URL 的 origin 匹配身份更准确
      seedCamstarCookieForUrlInBackground(url)
    }
    window.addEventListener('message', this._onSeedUrlMessage)
    this._onPortalExplicitHome = () => {
      // 任意路径回门户首页：关掉「全部应用」，避免盖住快捷导航
      this.drawerVisible = false
    }
    this.$root.$on('portal-explicit-home', this._onPortalExplicitHome)
    this.restoreQuickNavFromCache()
    this.loadQuickNav()
    // 在线变更探测已移除（quickNavWatch/permWatch）：后端 cache-aside（改数据删 Redis），
    // 刷新页面/重新登录自然从库重建；在线探测的版本比对请求是低配机上的无谓负担
    this.loadTodoCount(true)
    // 待办不再定时轮询（无实时性要求）：切回标签页时刷新，路由切换由 $route watch 刷新（带 30s 缓存），
    // 其余场景靠用户手动刷新页面——低配 Chrome 82 上定时器是纯负担
    this._onTodoVisibility = () => {
      if (!document.hidden) {
        this.loadTodoCount(true)
      }
    }
    document.addEventListener('visibilitychange', this._onTodoVisibility)
    // 不再用 ResizeObserver 把实测值写回 --dock-space：内联变量会盖住 CSS 的
    // 展开/收起值，切页时 CSS 先变、内联值后变，内容会二次跳动（闪一下再伸展）。
    // dock 尺寸与 --dock-space 已按同一公式对齐，交给 CSS 即可；
    // 这里再清一次历史内联变量，避免热更新/旧代码残留覆盖 CSS。
    this.clearDockSpaceInline()
  },
  beforeDestroy() {
    if (this._dockAnimTimer) {
      clearTimeout(this._dockAnimTimer)
      this._dockAnimTimer = null
    }
    // dock 占位改由 CSS 维护，清理补抖定时器即可
    if (this._dockSpaceNudgeTimer) {
      clearTimeout(this._dockSpaceNudgeTimer)
      this._dockSpaceNudgeTimer = null
    }
    if (this._onPortalOpenAllApps) {
      this.$root.$off('portal-open-all-apps', this._onPortalOpenAllApps)
      this._onPortalOpenAllApps = null
    }
    if (this._onPortalQuickNavChanged) {
      this.$root.$off('portal-quick-nav-changed', this._onPortalQuickNavChanged)
      this._onPortalQuickNavChanged = null
    }
    if (this._onSeedUrlMessage) {
      window.removeEventListener('message', this._onSeedUrlMessage)
      this._onSeedUrlMessage = null
    }
    if (this._onPortalExplicitHome) {
      this.$root.$off('portal-explicit-home', this._onPortalExplicitHome)
      this._onPortalExplicitHome = null
    }
    if (this._onTodoVisibility) {
      document.removeEventListener('visibilitychange', this._onTodoVisibility)
      this._onTodoVisibility = null
    }
  },
  methods: {
    /** 路由切换引起的 dock/占位收起瞬时到位：过渡动画只服务用户手动展开/收起，否则页面闪一下 */
    suppressShellTransition() {
      this.shellAnimSuppressed = true
      window.requestAnimationFrame(() => {
        window.requestAnimationFrame(() => {
          this.shellAnimSuppressed = false
        })
      })
    },
    /** 新页面从顶部开始：避免沿用上一页的滚动位置，看起来像“页面一打开就往下窜” */
    resetWorkspaceScroll() {
      // iframe 业务页的滚动在子页内部，父级容器 overflow:hidden，不需要动，
      // 避免每次切业务页都去碰 DOM/触发父级布局。
      if (this.isPortalIframeRoute) {
        return
      }
      const body = this.$el && this.$el.querySelector('.app-main__body')
      if (body) {
        body.scrollTop = 0
        body.scrollLeft = 0
      }
      const workspace = this.$el && this.$el.querySelector('.workspace-content')
      if (workspace) {
        workspace.scrollTop = 0
        workspace.scrollLeft = 0
      }
    },
    iconStyle(app) {
      return buildIconStyle(app)
    },
    handleShellClick() {
      this.searchFocused = false
      if (this.dockExpanded) this.collapseDock(false)
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
          // GET 失败保留上次/缓存内容，避免界面看起来被清空
          this.restoreQuickNavFromCache()
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
    /** 只有用户亲手展开/收起底栏才播放伸缩。切页引起的收起必须瞬时到位 */
    playDockAnim() {
      this.dockAnimating = true
      if (this._dockAnimTimer) {
        clearTimeout(this._dockAnimTimer)
      }
      this._dockAnimTimer = window.setTimeout(() => {
        this._dockAnimTimer = null
        this.dockAnimating = false
      }, 280)
    },
    /** 清掉历史版本 ResizeObserver 可能留下的内联 --dock-space（会盖住 CSS） */
    clearDockSpaceInline() {
      if (this.$el && this.$el.style && this.$el.style.getPropertyValue('--dock-space')) {
        this.$el.style.removeProperty('--dock-space')
      }
    },
    /** 明确发生布局变化时通知 iframe 层重新量高度；路由切换不会触发 */
    notifyLayoutChanged() {
      this.$nextTick(() => {
        window.setTimeout(() => this.$root.$emit('portal-layout-changed'), 60)
      })
    },
    expandDock() {
      this.clearDockSpaceInline()
      this.playDockAnim()
      this.dockExpanded = true
      this.notifyLayoutChanged()
    },
    collapseDock(animate) {
      if (!this.dockExpanded) {
        return
      }
      this.clearDockSpaceInline()
      if (animate) {
        this.playDockAnim()
      } else {
        this.dockAnimating = false
      }
      this.dockExpanded = false
      if (animate) {
        // 路由驱动的收起不改高度，不要触发 iframe 重新量高
        this.notifyLayoutChanged()
      }
    },
    /** dock 上的「全部应用」：打开抽屉同时收起 dock，避免挡住页面 */
    openAllAppsFromDock() {
      this.collapseDock(false)
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
    /** 从「全部应用」抽屉打开菜单 */
    openAppFromDrawer(app) {
      this.openApp(app)
    },
    openApp(app) {
      this.searchFocused = false
      const legacy = document.documentElement.classList.contains('legacy-anim')
      // 82：先关抽屉再导航，避免同帧「卸抽屉 + 收顶栏 + 挂 iframe」主线程堵死、闪黑
      const runNavigate = () => this.navigateOpenApp(app)
      if (this.drawerVisible && legacy) {
        this.drawerVisible = false
        this.$nextTick(() => {
          window.requestAnimationFrame(() => runNavigate())
        })
        return
      }
      this.drawerVisible = false
      runNavigate()
    },
    navigateOpenApp(app) {
      if (!app || !app.path) return
      // 导航前先把 dock/顶栏收到目标状态：新页面第一帧就按最终可视高度渲染，
      // 不再出现“先按旧高度画一帧，再收起变高”的闪动。
      this.collapseDock(false)
      if (app.path !== '/index' && app.path !== '/') {
        this.headerCollapsed = true
      }
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
        targetPath = alignPortalPathToCurrentLeaf(targetPath, this.portalSystemList, this.currentSystem)
        const openClientId = parsePortalClientId(targetPath) || clientId
        if (this.$route.path === targetPath) {
          return
        }
        const menusReady = !!(this.$store.state.portal.loadedSubSystems || {})[openClientId]
        const activeMap = this.$store.state.portal.pathLinkMap || {}
        const cachedMap = (this.$store.state.portal.subSystemPathLinkCache || {})[openClientId] || {}
        const entry = activeMap[targetPath] || lookupPathLinkEntry(targetPath, activeMap)
          || cachedMap[targetPath] || lookupPathLinkEntry(targetPath, cachedMap)
          || activeMap[app.path] || lookupPathLinkEntry(app.path, activeMap)
          || cachedMap[app.path] || lookupPathLinkEntry(app.path, cachedMap)
        const link = (entry && entry.link) || (app && app.link) || ''
        // 旧快捷导航可能只有 .../d3/needle；有完整 http(含 #) 时重写成带 __hash__ 的壳 path
        if (link && /^https?:\/\//i.test(link) && String(link).indexOf('#') >= 0) {
          const loc = httpUrlToPortalLocation(openClientId, link)
          if (loc && loc.path) {
            targetPath = alignPortalPathToCurrentLeaf(loc.path, this.portalSystemList, this.currentSystem)
          }
        }
        const rest = String(targetPath).replace(new RegExp('^/portal/' + openClientId + '/'), '').replace(/\/index$/, '')
        const restHttp = /^menu\d+/i.test(rest) ? '' : (slashIpPortRestToHttp(rest.replace(/:/g, '/')) || '')
        // 任意完整 http(s)（含 /#/xxx）都直开；禁止用壳 path 回退成丢 hash 的 host 根
        const isDirect = /^https?:\/\//i.test(link) || !!restHttp
        const resolvedLink = link || restHttp || ''

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
          ensureLocalCamstarCookie(resolvedLink, openClientId)
          if (resolvedLink) {
            seedCamstarCookieForUrlInBackground(resolvedLink, openClientId)
          }
          const afterPush = () => {
            if (!menusReady) {
              this.$store.dispatch('portal/ensureSubSystemLoaded', {
                clientId: openClientId,
                activate: false,
                force: false
              }).catch(() => {})
            }
          }
          const needShell = this.$store.state.portal.currentSystem !== openClientId
            || !(this.$store.state.portal.pathLinkMap && Object.keys(this.$store.state.portal.pathLinkMap).length)
          if (needShell) {
            return this.$store.dispatch('portal/activateSubSystemShell', { clientId: openClientId })
              .then(() => pushWithTitle())
              .then(afterPush)
          }
          return Promise.resolve(pushWithTitle()).then(afterPush)
        }

        // Camstar：一律直开（有 path 编码或 link 即可）
        if (isDirect) {
          return openDirect().catch(err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入业务系统失败'))
          })
        }

        // 菜单已在内存：立即跳转。原逻辑无论菜单是否就绪都先等 ensureSubSystemReady
        // （含版本比对接口）才 push——JUMP 后端与各子系统共机部署，任何一个子系统慢
        // 都会把版本接口拖到超时，表现为"打开任何菜单都一直转圈、过一会儿又好了"。
        // 版本比对转后台：有变更再刷新缓存，不影响本次打开（对齐 Camstar 直开节奏）。
        if (menusReady) {
          const showError = err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入业务系统失败'))
          }
          const afterPush = () => {
            this.$store.dispatch('portal/refreshSubSystemIfChanged', openClientId).catch(() => {})
          }
          const needShell = this.$store.state.portal.currentSystem !== openClientId
            || !(this.$store.state.portal.pathLinkMap && Object.keys(this.$store.state.portal.pathLinkMap).length)
          if (needShell) {
            return this.$store.dispatch('portal/activateSubSystemShell', { clientId: openClientId })
              .then(() => pushWithTitle())
              .then(afterPush)
              .catch(showError)
          }
          return Promise.resolve(pushWithTitle()).then(afterPush).catch(showError)
        }

        // 若依：菜单未就绪时后台拉，不锁全屏；业务区由 InnerLink 自己提示
        this.$message({ message: '正在准备业务系统菜单…', type: 'info', duration: 1500 })
        this.$store.dispatch('portal/ensureSubSystemReady', {
          clientId: openClientId,
          skipSso: false
        })
          .then(() => {
            if (this.$store.state.portal.currentSystem !== openClientId) {
              return
            }
            return pushWithTitle()
          })
          .catch(err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入业务系统失败'))
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
      // 旧 Chromium（legacy-anim）：1px 抖动=强制可见 iframe 全量重排，低配 82 上单次数百毫秒，
      // 是折叠 dock/顶栏、进业务页卡顿的最大单一来源。iframe 容器尺寸变化时浏览器本就会
      // 自动重排其内容，nudge 只服务少数自算固定高度的子页面——体验优先，82 上全部跳过；
      // 若个别子系统页面出现底部留白，再针对该页面单独处理
      if (document.documentElement.classList.contains('legacy-anim')) {
        return
      }
      window.requestAnimationFrame(() => {
        document.querySelectorAll('iframe.inner-link__frame').forEach(frame => {
          const root = frame.closest('.inner-link-root')
          // 只抖当前页，且只抖宽度。隐藏页一律不动：
          // 1px 高度变化会让跨域子页异步重排，切到前台时正好表现为“抖一下”；
          // 已打开页切回抖动就是这里造成的。
          if (!root || !root.classList.contains('is-front')) {
            return
          }
          if (frame.offsetWidth <= 0 || frame.offsetHeight <= 0) {
            return
          }
          const originalW = frame.style.width
          frame.style.width = 'calc(100% - 1px)'
          window.requestAnimationFrame(() => {
            frame.style.width = originalW || ''
          })
        })
      })
    },
    /**
     * 首次进入业务页：顶栏初始折叠/子系统 iframe 加载都会引起容器高度变化，
     * 页面稳定后补一次抖动，确保子系统按最终高度重算
     */
    scheduleInitialIframeNudge() {
      // 只补一次：1500ms 的二次抖动会落在页面已稳定之后，表现为“过一会儿又闪一下”
      window.setTimeout(() => this.nudgeVisibleIframes(), 600)
    },
    goHome() {
      // 已在首页：仍要关抽屉、清 workbench query，避免「点了首页还停在全部应用/待办态」
      this.drawerVisible = false
      this.$store.dispatch('portal/navigateToPortalHome').catch(() => {})
    },
    loadTodoCount(force = false) {
      if (!checkPermi(['bpm:task:query'])) {
        this.todoCount = 0
        return Promise.resolve()
      }
      // 路由切换频繁，30s 内不重复打接口；切回标签页/首次进入用 force 刷新
      if (!force && this._todoLoadedAt && Date.now() - this._todoLoadedAt < 30000) {
        return Promise.resolve(this.todoCount)
      }
      return getTodoTaskPage({ pageNo: 1, pageSize: 1 }, true).then(response => {
        this.todoCount = (response.data && response.data.total) || 0
        this._todoLoadedAt = Date.now()
      }).catch(() => {
        this.todoCount = 0
      })
    },
    goTodo() {
      this.drawerVisible = false
      const openTodo = () => {
        this.$root.$emit('portal-open-workbench', 'todo')
        if (this.$route.path === '/index' || this.$route.path === '/') {
          if (this.$route.query.workbench !== 'todo') {
            this.$router.replace({
              path: '/index',
              query: { ...this.$route.query, workbench: 'todo' }
            }).catch(() => {})
          }
        }
      }
      if (this.$route.path === '/index' || this.$route.path === '/') {
        openTodo()
        return
      }
      const app = this.authorizedApps.find(item => item.path === '/bpm/task/todo')
      if (app) {
        this.openApp(app)
        return
      }
      // 走正式回首页（保留 dock），再切待办；禁止裸 push('/index') 清掉页签
      this.$store.dispatch('portal/navigateToPortalHome', { keepWorkbench: true })
        .then(() => {
          this.$root.$emit('portal-open-workbench', 'todo')
          return this.$router.replace({
            path: '/index',
            query: { workbench: 'todo' }
          }).catch(() => {})
        })
        .catch(() => {})
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
  /* dock 为 fixed 悬浮：高 82px + 底边距 18px + 10px 安全缝 = 实占 110px；
     收起态 24px + 18px + 10px = 52px。
     这里必须和 ResizeObserver 的测量公式一致：以前写 100/44，观察器随后写回
     110/52，会在切页后把内容再往下撑 8~10px（表现为闪一下再向下伸展）。 */
  --dock-space: 110px;
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
  transition: none;
}

.jump-portal-shell.dock-animating {
  transition: padding-bottom .26s cubic-bezier(.16, 1, .3, 1);
}

.jump-portal-shell.dock-expanded {
  --dock-space: 110px;
}

/* 路由驱动收起时瞬时到位，勿做 padding 过渡（进业务页页面闪一下） */
.jump-portal-shell.no-anim {
  transition: none !important;
}

/* 业务页：dock 收起/展开都不改变内容高度（52px），底栏浮在页面之上。
   用 !important 是为了盖住历史版本可能残留的内联 --dock-space；
   否则展开底栏时 iframe 视口会缩 58px，Camstar 的表格/滚动条会重排。 */
.jump-portal-shell.in-business {
  --dock-space: 52px !important;
  padding-bottom: 52px !important;
}

.jump-portal-shell.in-business.dock-expanded {
  --dock-space: 52px !important;
  padding-bottom: 52px !important;
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
  border: 0;
  padding: 2px 6px;
  background: transparent;
  cursor: pointer;
}

/* Chrome 82 不支持 flex gap，用 margin 实现等价间距 */
.header-collapsed-bar > :not(:last-child) {
  margin-right: 10px;
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
  /* 页面内容异步撑高时不要触发浏览器滚动锚定，否则会整体往上/下跳 */
  overflow-anchor: none;
  -webkit-overflow-scrolling: touch;
  scroll-padding-bottom: var(--dock-space, 110px);
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
  .jump-portal-shell { --dock-space: 102px; padding: 12px 12px var(--dock-space); }
  .jump-portal-shell.dock-expanded { --dock-space: 102px; }
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

/* 旧 Chromium（<90）：dock 展开时 padding-bottom 布局动画每帧触发全页 layout（含 iframe 重排），
   82 上卡顿明显（90 同机流畅）；降级为瞬时到位 */
:root.legacy-anim .jump-portal-shell {
  transition: none !important;
  /* 三层 radial-gradient 大背景在 82 上任何重绘都贵，降级为纯色（同底色） */
  background: $canvas;
}
</style>
