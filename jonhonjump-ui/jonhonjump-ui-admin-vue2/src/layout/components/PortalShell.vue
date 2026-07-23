<template>
  <div
    :class="{ 'dock-expanded': dockExpanded }"
    class="jump-portal-shell"
    @click="handleShellClick"
  >
    <header class="portal-header" @click.stop>
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
      </div>
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
      @all-apps="drawerVisible = true"
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
      @open="openApp"
      @quick-nav-change="handleQuickNavChangeFromDrawer"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { confirmSwitchUser, confirmLogout } from '@/utils/switchUser'
import { isExternal } from '@/utils/validate'
import { parsePortalClientId, resolvePortalFrameRoute, isMainBusinessPath } from '@/utils/portalRoute'
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
      quickNavMenuIds: [],
      quickNavLockedMenuIds: [],
      quickNavConfigured: false,
      todoCount: 0,
      todoRefreshTimer: null
    }
  },
  computed: {
    ...mapGetters(['avatar', 'nickname', 'name', 'sidebarRouters', 'currentSystemSidebarRouters', 'currentSystemLabel', 'currentSystem', 'portalSystemList']),
    todoBadgeValue() {
      return this.todoCount > 0 ? this.todoCount : ''
    },
    currentSubSystemId() {
      if (this.currentSystem === 'main') {
        return 0
      }
      const sys = (this.portalSystemList || []).find(item => item.clientId === this.currentSystem)
      return sys ? Number(sys.subSystemId) : 0
    },
    quickNavScopeKey() {
      return buildQuickNavScopeKey(this.currentSystem, this.currentSubSystemId)
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
      const route = resolvePortalFrameRoute(this.$route, this.$store.state.portal.pathLinkMap)
      return Boolean(route.meta && route.meta.link)
    }
  },
  watch: {
    currentSystem() {
      this.searchKeyword = ''
      this.searchFocused = false
      this.restoreQuickNavFromCache()
      this.loadQuickNav()
    },
    drawerVisible(visible) {
      if (visible) {
        // 首页 Panel 取消收藏后 Shell 可能仍持旧列表；打开抽屉前先对齐缓存，避免再收藏时把已取消项写回
        this.restoreQuickNavFromCache()
      }
      if (!visible || this.currentSystem === 'main') {
        return
      }
      const loaded = !!(this.$store.state.portal.loadedSubSystems || {})[this.currentSystem]
      if (!loaded) {
        this.$store.dispatch('portal/ensureSubSystemLoaded', {
          clientId: this.currentSystem,
          activate: false
        }).catch(() => {})
      }
    },
    '$route.path'() {
      this.dockExpanded = false
      this.loadTodoCount()
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
      if (!payload || payload.scopeKey !== this.quickNavScopeKey || payload.source === 'shell') {
        return
      }
      this.handleQuickNavChange(payload)
    }
    this.$root.$on('portal-open-all-apps', this._onPortalOpenAllApps)
    this.$root.$on('portal-quick-nav-changed', this._onPortalQuickNavChanged)
    this.restoreQuickNavFromCache()
    this.loadQuickNav()
    this.loadTodoCount()
    this.todoRefreshTimer = window.setInterval(() => {
      this.loadTodoCount()
    }, 60000)
  },
  beforeDestroy() {
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
      return this.$store.dispatch('portal/loadQuickNavConfig', {
        subSystemId: this.currentSubSystemId
      }).then(config => {
        if (scopeKey !== this.quickNavScopeKey) {
          return
        }
        const menuIds = (config && config.menuIds) || []
        const lockedMenuIds = (config && config.lockedMenuIds) || []
        const configured = !!(config && config.configured)
        this.quickNavMenuIds = menuIds
        this.quickNavLockedMenuIds = lockedMenuIds
        this.quickNavConfigured = configured
        setQuickNavCache(scopeKey, menuIds, configured, lockedMenuIds)
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
      setQuickNavCache(this.quickNavScopeKey, menuIds, configured, lockedMenuIds)
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
    handleSubsystemChange(value) {
      if (value === this.currentSystem) return
      const loading = this.$loading({
        lock: true,
        text: '正在切换系统...',
        spinner: 'el-icon-loading'
      })
      this.$store.dispatch('portal/switchSystem', { system: value, stayOnPortalHome: false }).catch(err => {
        this.$message.error(typeof err === 'string' ? err : (err.message || '切换系统失败'))
      }).finally(() => {
        loading.close()
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
    openApp(app) {
      this.searchFocused = false
      this.drawerVisible = false
      if (!app || !app.path) return
      if (this.currentSystem !== 'main' && isMainBusinessPath(app.path)) {
        const loading = this.$loading({ lock: true, text: '正在进入主系统...', spinner: 'el-icon-loading' })
        this.$store.dispatch('portal/switchSystem', { system: 'main', skipNavigate: true })
          .then(() => this.$router.push(app.path))
          .catch(err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入主系统失败'))
          })
          .finally(() => loading.close())
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
        if (this.$route.path === app.path) {
          return
        }
        const menusReady = !!(this.$store.state.portal.loadedSubSystems || {})[clientId]
        const loading = menusReady
          ? null
          : this.$loading({ lock: true, text: '加载子系统菜单...', spinner: 'el-icon-loading' })
        this.$store.dispatch('portal/ensureSubSystemReady', clientId)
          .then(() => {
            // ensureSubSystemReady 已按需激活；若加载期间用户已切走，不再跳转
            if (this.$store.state.portal.currentSystem !== clientId) {
              return
            }
            return this.$router.push(app.path)
          })
          .catch(err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入子系统失败'))
          })
          .finally(() => loading && loading.close())
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
    goHome() {
      if (this.$route.path === '/index' || this.$route.path === '/') return
      this.$store.dispatch('portal/navigateToPortalHome').catch(() => {})
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
      confirmSwitchUser(this.$store, this.$route.fullPath)
    },
    handleLogout() {
      confirmLogout(this.$store, this.$route.fullPath)
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
  --dock-space: 50px;
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
.header-actions { display: flex; align-items: center; gap: 10px; }
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
.switch-user-btn:hover,
.switch-user-btn:focus-visible {
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
.search-result:hover,
.search-result:focus-visible { outline: none; background: #edf6fd; }
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
.system-switch:hover,
.system-switch:focus-visible { outline: none; background: #fff; }
.status-dot { display: inline-block; width: 9px; height: 9px; margin-right: 8px; border-radius: 50%; background: #11a574; box-shadow: 0 0 0 4px rgba(17, 165, 116, .12); }

.round-action,
.user-entry { display: grid; width: 50px; height: 50px; padding: 0; place-items: center; border: 0; border-radius: 50%; cursor: pointer; }
.round-action { color: #29435f; background: #eaf3fb; font-size: 20px; }
.round-action:hover,
.round-action:focus-visible { outline: 3px solid rgba(8, 124, 229, .16); background: #dcecf9; }
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
  scroll-padding-bottom: var(--dock-space, 50px);
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
  .portal-header { align-items: flex-start; gap: 12px; flex-direction: column; }
  .header-actions { width: 100%; }
  .app-search { width: auto; flex: 1; }
}

@media (max-width: 820px) {
  .jump-portal-shell { --dock-space: 44px; padding: 12px 12px var(--dock-space); }
  .jump-portal-shell.dock-expanded { --dock-space: 106px; }
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
