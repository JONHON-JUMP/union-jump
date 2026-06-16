<template>
  <div class="jump-portal">
    <header class="portal-header">
      <button class="brand" type="button" aria-label="返回首页" @click="goHome">
        <span class="brand-mark">J</span>
        <span class="brand-copy">
          <strong>JUMP 中航统一制造管理平台</strong>
          <small>JONHON UNIFORM MANUFACTURE PLATFORM</small>
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
              <span>应用搜索</span><small>{{ filteredApps.length }} 个结果</small>
            </div>
            <button
              v-for="app in filteredApps.slice(0, 8)"
              :key="'search-' + app.name + app.path"
              type="button"
              class="search-result"
              @mousedown.prevent="openApp(app)"
            >
              <span class="mini-icon" :style="{ background: app.color }">
                <svg-icon v-if="app.svgIcon" :icon-class="app.svgIcon" />
                <i v-else-if="app.icon" :class="app.icon" />
                <svg-icon v-else icon-class="component" />
              </span>
              <span><strong>{{ app.name }}</strong><small>{{ app.group || app.subtitle || '授权应用' }}</small></span>
              <i class="el-icon-arrow-right" />
            </button>
            <div v-if="!filteredApps.length" class="search-empty">未找到匹配应用，请尝试其他关键词</div>
          </div>
        </div>

        <div class="system-chip">
          <span class="status-dot" />
          <span>当前系统：</span>
          <strong>{{ currentSubsystem }}</strong>
          <el-dropdown trigger="click" placement="bottom-end" @command="handleSubsystemChange">
            <button class="system-switch" type="button">
              切换
              <i class="el-icon-arrow-down" />
            </button>
            <el-dropdown-menu slot="dropdown" class="system-dropdown">
              <el-dropdown-item
                v-for="system in subsystemOptions"
                :key="system.value"
                :command="system.value"
                :class="{ 'is-current': currentSystem === system.value }"
              >
                <span class="system-option">
                  <i :class="system.icon" />
                  <span>
                    <strong>{{ system.label }}</strong>
                    <small>{{ system.description }}</small>
                  </span>
                  <i v-if="currentSystem === system.value" class="el-icon-check" />
                </span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <el-badge :value="unreadCount || ''" class="notice-badge">
          <button class="round-action" type="button" aria-label="通知" @click="activeWorkbench = 'notice'"><i class="el-icon-bell" /></button>
        </el-badge>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <button class="user-entry" type="button">
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

    <main class="portal-main" @click="searchFocused = false">
      <section class="desktop-area">
        <div class="welcome-row">
          <div>
            <p class="date-label">{{ dateLabel }}</p>
            <h1>{{ greeting }}，{{ displayName }}</h1>
            <p>从常用应用快速进入业务现场，也可通过底部“全部应用”查看当前账号的授权入口。</p>
          </div>
          <div class="welcome-actions">
            <button class="quick-nav-config" type="button" @click="quickNavSettingsVisible = true">
              <i class="el-icon-setting" />
              配置快捷导航
            </button>
            <div class="shift-chip"><span class="status-dot" />白班 · 08:00 至 20:00</div>
          </div>
        </div>

        <div
          ref="appViewport"
          class="app-viewport"
          @wheel="handleAppWheel"
          @touchstart="handleTouchStart"
          @touchend="handleTouchEnd"
        >
          <transition :name="pageTransition" mode="out-in">
            <div
              :key="currentAppPage"
              class="app-grid"
              :style="{ '--app-columns': appColumns }"
              role="list"
              aria-label="常用应用"
            >
              <button
                v-for="app in pagedApps"
                :key="app.path || app.name + '|' + (app.subtitle || '')"
                class="app-tile"
                type="button"
                role="listitem"
                @click="openApp(app)"
              >
                <span class="app-icon" :style="{ background: app.color, boxShadow: app.shadow }">
                  <span class="icon-highlight" />
                  <svg-icon v-if="app.svgIcon" :icon-class="app.svgIcon" />
                  <i v-else-if="app.icon" :class="app.icon" />
                  <svg-icon v-else icon-class="component" />
                  <em v-if="app.badge">{{ app.badge }}</em>
                </span>
                <strong>{{ app.name }}</strong>
                <small>{{ app.subtitle }}</small>
              </button>
            </div>
          </transition>
        </div>
        <div v-if="appPageCount > 1" class="page-indicator" aria-label="应用分页">
          <button
            v-for="page in appPageCount"
            :key="page"
            type="button"
            :class="{ active: currentAppPage === page - 1 }"
            :aria-label="'切换到第 ' + page + ' 页'"
            @click="goToAppPage(page - 1)"
          />
        </div>
      </section>

      <aside class="info-rail">
        <section class="info-panel workbench-panel">
          <div class="panel-heading">
            <div>
              <h2>工作台</h2>
              <p>{{ currentWorkbenchSummary }}</p>
            </div>
            <time>{{ currentTime }}</time>
          </div>
          <div class="workbench-tabs" role="tablist">
            <button
              v-for="tab in workbenchTabs"
              :key="tab.key"
              type="button"
              :class="{ active: activeWorkbench === tab.key }"
              @click="activeWorkbench = tab.key"
            >{{ tab.label }}<span v-if="tab.count"> {{ tab.count }}</span></button>
          </div>
          <div class="task-list">
            <button v-for="task in visibleTasks" :key="task.title" type="button" class="task-item" @click="showTask(task)">
              <span><strong>{{ task.title }}</strong><small>{{ task.description }}</small></span>
              <em :class="task.level">{{ task.tag }}</em>
            </button>
          </div>
          <button
            v-if="hasMoreTasks"
            class="workbench-more"
            type="button"
            @click="openWorkbenchMore"
          >
            查看更多
            <span>还有 {{ currentTasks.length - workbenchDisplayLimit }} 条</span>
            <i class="el-icon-arrow-right" />
          </button>
        </section>
      </aside>
    </main>

    <portal-dock
      @all-apps="drawerVisible = true"
    />

    <all-apps-drawer
      ref="allAppsDrawer"
      :visible.sync="drawerVisible"
      :routes="sidebarRouters || []"
      @open="openApp"
      @pinned-change="handlePinnedAppsChange"
    />

    <portal-quick-nav-settings
      v-model="quickNavSettingsVisible"
      :portal-system-list="portalSystemList"
      :initial-tab="quickNavInitialTab"
      @saved="handleQuickNavSaved"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getPath } from '@/utils/ruoyi'
import { isExternal } from '@/utils/validate'
import { getTodoTaskPage } from '@/api/bpm/task'
import { getMyNotifyMessagePage, getUnreadNotifyMessageCount } from '@/api/system/notify/message'
import { getUserQuickNavList } from '@/api/system/user/quickNav'
import { getSubSystemUserQuickNavList } from '@/api/system/user/subSystemQuickNav'
import { buildQuickNavItems, buildSidebarQuickApps } from '@/views/home/quickNavFromRoutes'
import PortalQuickNavSettings from '@/views/home/PortalQuickNavSettings'
import { faqList } from '@/views/home/quickNavData'
import { buildSubsystemOptions, resolveCurrentSubsystemLabel } from '@/utils/portalSubsystem'
import { parsePortalClientId, isMainBusinessPath } from '@/utils/portalRoute'
import AllAppsDrawer from './components/AllAppsDrawer.vue'
import PortalDock from '@/layout/components/PortalDock.vue'
import { getCachedPinnedApps, loadPinnedApps } from '@/utils/portalPinnedApps'

const colors = {
  blue: 'linear-gradient(145deg, #2597f4, #086fd8)',
  cyan: 'linear-gradient(145deg, #12a9c4, #087d9f)',
  teal: 'linear-gradient(145deg, #12aeb5, #08768c)',
  green: 'linear-gradient(145deg, #13ad80, #087a59)',
  orange: 'linear-gradient(145deg, #f39a13, #d76700)',
  slate: 'linear-gradient(145deg, #6289b2, #315d91)',
  violet: 'linear-gradient(145deg, #7e78c7, #51489b)'
}

import { resolvePortalMenuIcon } from '@/utils/portalMenuIcon'

/** 跨 /index 挂载保留各系统快捷导航，避免返回首页时闪一下全量菜单 */
const quickNavScopeCache = Object.create(null)

function buildQuickNavScopeKey(currentSystem, subSystemId) {
  if (currentSystem === 'main' || !subSystemId) {
    return 'main'
  }
  return `sub:${subSystemId}`
}

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

import { applyQuickNavDuplicateLabels } from '@/utils/quickNavLabel'

function mapQuickNavApp(item, options) {
  return {
    name: item.name,
    subtitle: item.subtitle || options.subtitle,
    path: item.path,
    svgIcon: item.svgIcon || null,
    icon: item.icon || null,
    color: options.color,
    shadow: options.shadow,
    keywords: item.keywords || item.name,
    external: item.external
  }
}

function mapQuickNavApps(items, options) {
  return applyQuickNavDuplicateLabels(items, options.subtitle).map(item => mapQuickNavApp(item, options))
}

export default {
  name: 'JumpPortalHome',
  components: { AllAppsDrawer, PortalDock, PortalQuickNavSettings },
  data() {
    return {
      searchKeyword: '',
      searchFocused: false,
      drawerVisible: false,
      quickNavSettingsVisible: false,
      activeWorkbench: 'notice',
      currentAppPage: 0,
      appColumns: 6,
      appRows: 2,
      pageDirection: 'next',
      touchStartX: 0,
      touchStartY: 0,
      lastWheelAt: 0,
      appResizeObserver: null,
      now: new Date(),
      timer: null,
      pinnedApps: getCachedPinnedApps(),
      quickNavMenuIds: [],
      quickNavConfigured: false,
      quickNavLoadedScope: null,
      quickNavLoading: false,
      workbenchLoading: false,
      unreadCount: 0,
      desktopApps: [],
      workbenchDisplayLimit: 3,
      workbenchTabs: [
        { key: 'notice', label: '系统公告' },
        { key: 'todo', label: '待办', count: 0 },
        { key: 'qa', label: '常见 QA' }
      ],
      taskMap: {
        todo: [],
        notice: [],
        qa: faqList.map(item => ({
          title: item.title,
          description: item.author,
          tag: item.category.replace(/[【】]/g, ''),
          level: 'info'
        }))
      }
    }
  },
  computed: {
    ...mapGetters([
      'avatar', 'nickname', 'name', 'sidebarRouters',
      'currentSystem', 'portalSystemList'
    ]),
    subsystemOptions() {
      return buildSubsystemOptions(this.portalSystemList)
    },
    currentSubsystem() {
      return resolveCurrentSubsystemLabel(this.currentSystem, this.portalSystemList)
    },
    currentSubSystemId() {
      if (this.currentSystem === 'main') {
        return 0
      }
      const sys = (this.portalSystemList || []).find(item => item.clientId === this.currentSystem)
      return sys ? Number(sys.subSystemId) : 0
    },
    quickNavInitialTab() {
      if (this.currentSystem === 'main' || !this.currentSubSystemId) {
        return 'main'
      }
      return `sub-${this.currentSubSystemId}`
    },
    quickNavScopeKey() {
      return buildQuickNavScopeKey(this.currentSystem, this.currentSubSystemId)
    },
    displayName() {
      return this.nickname || this.name || '制造同事'
    },
    userInitial() {
      return this.displayName.slice(0, 1)
    },
    greeting() {
      const hour = this.now.getHours()
      if (hour < 6) return '夜深了'
      if (hour < 12) return '上午好'
      if (hour < 14) return '中午好'
      if (hour < 18) return '下午好'
      return '晚上好'
    },
    dateLabel() {
      return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(this.now)
    },
    currentTime() {
      return this.now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
    },
    appPageSize() {
      return Math.max(1, this.appColumns * this.appRows)
    },
    appPageCount() {
      return Math.max(1, Math.ceil(this.homeApps.length / this.appPageSize))
    },
    pagedApps() {
      const start = this.currentAppPage * this.appPageSize
      return this.homeApps.slice(start, start + this.appPageSize)
    },
    pageTransition() {
      return this.pageDirection === 'next' ? 'app-page-next' : 'app-page-prev'
    },
    authorizedApps() {
      return this.flattenRoutes(this.sidebarRouters || [])
    },
    homeApps() {
      return this.buildHomeApps(this.quickNavMenuIds, this.quickNavConfigured)
    },
    drawerApps() {
      const seen = new Set()
      return [...this.homeApps, ...this.authorizedApps].filter(app => {
        const key = app.name + (app.path || '')
        if (seen.has(key)) return false
        seen.add(key)
        return true
      })
    },
    filteredApps() {
      const keyword = this.searchKeyword.toLowerCase()
      if (!keyword) return this.drawerApps.slice(0, 6)
      return this.drawerApps.filter(app => `${app.name} ${app.subtitle || ''} ${app.keywords || ''} ${app.group || ''}`.toLowerCase().includes(keyword))
    },
    showSearchPanel() {
      return this.searchFocused && Boolean(this.searchKeyword)
    },
    currentTasks() {
      return this.taskMap[this.activeWorkbench] || []
    },
    visibleTasks() {
      return this.currentTasks.slice(0, this.workbenchDisplayLimit)
    },
    hasMoreTasks() {
      return this.currentTasks.length > this.workbenchDisplayLimit
    },
    currentWorkbenchSummary() {
      const summaries = {
        notice: '平台通知与版本发布信息',
        todo: `${this.taskMap.todo.length} 项任务等待处理`,
        qa: '制造业务常见问题指引'
      }
      return summaries[this.activeWorkbench]
    }
  },
  watch: {
    currentSystem() {
      this.restoreQuickNavFromCache()
      this.currentAppPage = 0
      this.loadQuickNav().finally(() => {
        this.$nextTick(this.updateAppPagination)
      })
    },
    '$route.path'(path) {
      if (path === '/index' || path === '/') {
        this.restoreQuickNavFromCache()
        this.loadQuickNav()
      }
    }
  },
  mounted() {
    if (this.$route.query.workbench && this.taskMap[this.$route.query.workbench]) {
      this.activeWorkbench = this.$route.query.workbench
    }
    this.restoreQuickNavFromCache()
    this.timer = window.setInterval(() => { this.now = new Date() }, 30000)
    this.loadPinnedApps()
    this.$store.dispatch('portal/bootstrapAfterAuth').then(() => {
      this.loadSubsystems().then(() => this.loadQuickNav())
    })
    this.loadWorkbenchData()
    this.loadUnreadCount()
    this.$nextTick(this.initAppPagination)
  },
  beforeDestroy() {
    window.clearInterval(this.timer)
    if (this.appResizeObserver) this.appResizeObserver.disconnect()
    window.removeEventListener('resize', this.updateAppPagination)
  },
  methods: {
    buildHomeApps(menuIds, configured) {
      const scopeKey = this.quickNavScopeKey
      if (this.quickNavLoadedScope !== scopeKey) {
        const cached = quickNavScopeCache[scopeKey]
        if (cached) {
          menuIds = cached.menuIds
          configured = cached.configured
        } else if (this.quickNavLoading) {
          return []
        }
      }

      if (this.currentSystem !== 'main') {
        const quickNavApps = buildQuickNavItems(this.sidebarRouters, menuIds)
        if (quickNavApps.length) {
          return mapQuickNavApps(quickNavApps, {
            subtitle: '快捷导航',
            color: colors.teal,
            shadow: '0 12px 22px rgba(8,118,140,.24)'
          })
        }
        if (!configured) {
          return mapQuickNavApps(buildSidebarQuickApps(this.sidebarRouters), {
            subtitle: '快捷入口',
            color: colors.teal,
            shadow: '0 12px 22px rgba(8,118,140,.24)'
          })
        }
        return []
      }

      const quickNavApps = mapQuickNavApps(buildQuickNavItems(this.sidebarRouters, menuIds), {
        subtitle: '快捷导航',
        color: colors.blue,
        shadow: '0 12px 22px rgba(8,111,216,.24)'
      })
      const pinned = this.pinnedApps.map(app => ({
        ...app,
        ...resolveMenuIconFields(app.icon, { title: app.name, path: app.path }),
        subtitle: app.group || '固定应用',
        color: colors.blue,
        shadow: '0 12px 22px rgba(8,111,216,.24)',
        pinned: true
      }))
      const seen = new Set()
      return [...quickNavApps, ...pinned].filter(app => {
        const key = app.path || `${app.name}|${app.subtitle}`
        if (seen.has(key)) return false
        seen.add(key)
        return true
      })
    },
    restoreQuickNavFromCache() {
      const scopeKey = this.quickNavScopeKey
      const cached = quickNavScopeCache[scopeKey]
      if (cached) {
        this.quickNavMenuIds = [...cached.menuIds]
        this.quickNavConfigured = cached.configured
        this.quickNavLoadedScope = scopeKey
        return
      }
      this.quickNavMenuIds = []
      this.quickNavConfigured = false
      this.quickNavLoadedScope = null
    },
    cacheQuickNavState(scopeKey, menuIds, configured) {
      quickNavScopeCache[scopeKey] = {
        menuIds: [...menuIds],
        configured: !!configured
      }
    },
    preloadQuickNavForSystem(systemValue) {
      if (systemValue === 'main') {
        const cached = quickNavScopeCache.main
        if (cached) {
          this.quickNavMenuIds = [...cached.menuIds]
          this.quickNavConfigured = cached.configured
          this.quickNavLoadedScope = 'main'
        } else {
          this.quickNavMenuIds = []
          this.quickNavConfigured = false
          this.quickNavLoadedScope = null
        }
        return
      }
      const sys = (this.portalSystemList || []).find(item => item.clientId === systemValue)
      const scopeKey = buildQuickNavScopeKey(systemValue, sys ? Number(sys.subSystemId) : 0)
      const cached = quickNavScopeCache[scopeKey]
      if (cached) {
        this.quickNavMenuIds = [...cached.menuIds]
        this.quickNavConfigured = cached.configured
        this.quickNavLoadedScope = scopeKey
      } else {
        this.quickNavMenuIds = []
        this.quickNavConfigured = false
        this.quickNavLoadedScope = null
      }
    },
    loadSubsystems() {
      return this.$store.dispatch('portal/loadSystemList')
    },
    loadQuickNav() {
      const scopeKey = this.quickNavScopeKey
      const request = this.currentSubSystemId > 0
        ? getSubSystemUserQuickNavList(this.currentSubSystemId)
        : getUserQuickNavList()
      this.quickNavLoading = true
      return request.then(res => {
        const config = res.data || {}
        const menuIds = config.menuIds || []
        const configured = !!config.configured
        if (scopeKey !== this.quickNavScopeKey) {
          return
        }
        this.quickNavMenuIds = menuIds
        this.quickNavConfigured = configured
        this.quickNavLoadedScope = scopeKey
        this.cacheQuickNavState(scopeKey, menuIds, configured)
        this.$nextTick(this.updateAppPagination)
      }).catch(() => {
        if (scopeKey === this.quickNavScopeKey) {
          this.quickNavMenuIds = []
          this.quickNavConfigured = false
          this.quickNavLoadedScope = scopeKey
        }
      }).finally(() => {
        if (scopeKey === this.quickNavScopeKey) {
          this.quickNavLoading = false
        }
      })
    },
    handleQuickNavSaved(payload) {
      if (!payload || payload.scope === 'main') {
        if (this.currentSystem === 'main') {
          const menuIds = (payload && payload.menuIds) || []
          this.quickNavMenuIds = menuIds
          this.quickNavConfigured = true
          this.quickNavLoadedScope = 'main'
          this.cacheQuickNavState('main', menuIds, true)
        }
      } else if (payload.scope === 'sub' && payload.subSystemId === this.currentSubSystemId) {
        const scopeKey = this.quickNavScopeKey
        const menuIds = payload.menuIds || []
        this.quickNavMenuIds = menuIds
        this.quickNavConfigured = true
        this.quickNavLoadedScope = scopeKey
        this.cacheQuickNavState(scopeKey, menuIds, true)
      }
      this.currentAppPage = 0
      this.$nextTick(this.updateAppPagination)
    },
    loadUnreadCount() {
      return getUnreadNotifyMessageCount().then(res => {
        this.unreadCount = res.data || 0
      }).catch(() => {
        this.unreadCount = 0
      })
    },
    loadWorkbenchData() {
      this.workbenchLoading = true
      return Promise.all([this.loadTodoTasks(), this.loadNoticeTasks()]).finally(() => {
        this.workbenchLoading = false
      })
    },
    loadTodoTasks() {
      return getTodoTaskPage({ pageNo: 1, pageSize: 8 }).then(response => {
        const list = response.data.list || []
        this.workbenchTabs = this.workbenchTabs.map(tab => tab.key === 'todo'
          ? { ...tab, count: response.data.total || 0 }
          : tab)
        this.taskMap.todo = list.map(row => ({
          title: this.buildTodoTitle(row),
          description: row.processInstance && row.processInstance.name ? row.processInstance.name : '流程审批',
          tag: row.name || '待办',
          level: 'warning',
          raw: row
        }))
      }).catch(() => {
        this.taskMap.todo = []
      })
    },
    loadNoticeTasks() {
      return getMyNotifyMessagePage({ pageNo: 1, pageSize: 8 }).then(response => {
        const list = response.data.list || []
        this.taskMap.notice = list.map(row => ({
          title: row.templateContent || '-',
          description: row.templateNickname || '系统通知',
          tag: '通知',
          level: 'info',
          raw: row
        }))
      }).catch(() => {
        this.taskMap.notice = []
      })
    },
    buildTodoTitle(row) {
      const user = row.processInstance && row.processInstance.startUserNickname
      const taskName = row.name || ''
      if (user && taskName) return `${user} - ${taskName}`
      return taskName || user || '-'
    },
    async loadPinnedApps() {
      this.pinnedApps = await loadPinnedApps()
      this.$nextTick(this.updateAppPagination)
    },
    handlePinnedAppsChange(apps) {
      this.pinnedApps = apps
      if (this.currentAppPage >= this.appPageCount) {
        this.currentAppPage = this.appPageCount - 1
      }
      this.$nextTick(this.updateAppPagination)
    },
    handleSubsystemChange(value) {
      if (value === this.currentSystem) return
      this.preloadQuickNavForSystem(value)
      this.currentAppPage = 0
      const loading = this.$loading({
        lock: true,
        text: '正在切换系统...',
        spinner: 'el-icon-loading'
      })
      this.$store.dispatch('portal/switchSystem', { system: value, stayOnPortalHome: true }).catch(err => {
        this.restoreQuickNavFromCache()
        this.$message.error(typeof err === 'string' ? err : (err.message || '切换系统失败'))
      }).finally(() => {
        loading.close()
      })
    },
    initAppPagination() {
      this.updateAppPagination()
      if (typeof ResizeObserver !== 'undefined') {
        this.appResizeObserver = new ResizeObserver(this.updateAppPagination)
        this.appResizeObserver.observe(this.$refs.appViewport)
      } else {
        window.addEventListener('resize', this.updateAppPagination)
      }
    },
    updateAppPagination() {
      const viewport = this.$refs.appViewport
      if (!viewport) return
      const width = viewport.clientWidth
      const height = viewport.clientHeight
      let columns = 3
      if (width >= 1020) columns = 6
      else if (width >= 820) columns = 5
      else if (width >= 620) columns = 4
      const rows = Math.max(1, Math.min(3, Math.floor((height + 28) / 155)))
      this.appColumns = columns
      this.appRows = rows
      if (this.currentAppPage >= this.appPageCount) {
        this.currentAppPage = this.appPageCount - 1
      }
    },
    handleAppWheel(event) {
      if (this.appPageCount <= 1) return
      const delta = Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY
      if (Math.abs(delta) < 18) return
      event.preventDefault()
      const now = Date.now()
      if (now - this.lastWheelAt < 420) return
      this.lastWheelAt = now
      this.changeAppPage(delta > 0 ? 1 : -1)
    },
    handleTouchStart(event) {
      const touch = event.touches && event.touches[0]
      if (!touch) return
      this.touchStartX = touch.clientX
      this.touchStartY = touch.clientY
    },
    handleTouchEnd(event) {
      const touch = event.changedTouches && event.changedTouches[0]
      if (!touch || this.appPageCount <= 1) return
      const deltaX = touch.clientX - this.touchStartX
      const deltaY = touch.clientY - this.touchStartY
      if (Math.abs(deltaX) < 45 || Math.abs(deltaX) <= Math.abs(deltaY)) return
      this.changeAppPage(deltaX < 0 ? 1 : -1)
    },
    changeAppPage(step) {
      const nextPage = Math.min(Math.max(this.currentAppPage + step, 0), this.appPageCount - 1)
      if (nextPage === this.currentAppPage) return
      this.pageDirection = step > 0 ? 'next' : 'prev'
      this.currentAppPage = nextPage
    },
    goToAppPage(page) {
      if (page === this.currentAppPage) return
      this.pageDirection = page > this.currentAppPage ? 'next' : 'prev'
      this.currentAppPage = page
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
            ...resolveMenuIconFields((route.meta && route.meta.icon) || '', {
              title,
              path
            }),
            color: colors.blue,
            keywords: `${title} ${group}`
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
      if (!app) return
      if (!app.path) {
        const keywords = [app.name]
          .concat((app.keywords || '').split(/\s+/))
          .filter(keyword => keyword && keyword.length > 1)
        const authorizedApp = this.authorizedApps.find(item => keywords.some(keyword => {
          return item.name.includes(keyword) || (item.group || '').includes(keyword)
        }))
        if (authorizedApp) {
          this.openApp(authorizedApp)
        } else {
          this.drawerVisible = true
          this.$nextTick(() => {
            if (this.$refs.allAppsDrawer) this.$refs.allAppsDrawer.openWithKeyword(app.name)
          })
        }
        return
      }
      if (this.currentSystem !== 'main' && isMainBusinessPath(app.path)) {
        this.$message.warning('当前为子系统模式，请从门户首页选择应用进入')
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
        const loading = this.$loading({ lock: true, text: '正在进入子系统...', spinner: 'el-icon-loading' })
        this.$store.dispatch('portal/enterSubSystem', { clientId, navigate: false })
          .then(() => this.$router.push(app.path))
          .catch(err => {
            this.$message.error(typeof err === 'string' ? err : (err.message || '进入子系统失败'))
          })
          .finally(() => loading.close())
        return
      }
      if (this.$route.path !== app.path) {
        this.$router.push(app.path)
      }
    },
    openByKeyword(keyword) {
      const app = this.drawerApps.find(item => item.name.includes(keyword) || (item.group || '').includes(keyword))
      if (app) this.openApp(app)
      else {
        this.drawerVisible = true
        this.$nextTick(() => {
          if (this.$refs.allAppsDrawer) this.$refs.allAppsDrawer.openWithKeyword(keyword)
        })
      }
    },
    closeSearch() {
      this.searchKeyword = ''
      this.searchFocused = false
    },
    goHome() {
      if (this.$route.path === '/index' || this.$route.path === '/') return
      this.$store.dispatch('portal/navigateToPortalHome').catch(() => {})
    },
    showTask(task) {
      if (this.activeWorkbench === 'todo' && task.raw && task.raw.processInstance) {
        this.$router.push({ name: 'BpmProcessInstanceDetail', query: { id: task.raw.processInstance.id } }).catch(() => {})
        return
      }
      if (this.activeWorkbench === 'notice') {
        this.$router.push('/user/notify-message').catch(() => {})
        return
      }
      this.$message.info(task.title)
    },
    openWorkbenchMore() {
      const targets = {
        notice: {
          paths: ['/system/notice', '/user/notify-message'],
          keyword: '公告'
        },
        todo: {
          paths: ['/bpm/task/todo'],
          keyword: '待办'
        },
        qa: {
          paths: [],
          keyword: '帮助'
        }
      }
      const target = targets[this.activeWorkbench]
      if (!target) return

      const app = this.authorizedApps.find(item => target.paths.includes(item.path))
      if (app) {
        this.openApp(app)
        return
      }

      this.drawerVisible = true
      this.$nextTick(() => {
        if (this.$refs.allAppsDrawer) this.$refs.allAppsDrawer.openWithKeyword(target.keyword)
      })
    },
    handleUserCommand(command) {
      if (command === 'profile') this.$router.push('/user/profile')
      else if (command === 'settings') this.$message.info('平台设置入口已打开，可在此接入个人偏好配置')
      else if (command === 'logout') {
        this.$confirm('确定退出 JUMP 统一制造管理平台吗？', '退出登录', {
          confirmButtonText: '退出',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$store.dispatch('LogOut').then(() => { location.href = getPath('/index') })
        }).catch(() => {})
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

.jump-portal {
  display: flex;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  padding: 22px 26px 20px;
  box-sizing: border-box;
  flex-direction: column;
  color: $ink;
  background:
    radial-gradient(circle at 2% 2%, rgba(67, 183, 239, .18), transparent 30%),
    radial-gradient(circle at 98% 18%, rgba(42, 195, 172, .14), transparent 28%),
    radial-gradient(circle at 58% 100%, rgba(255, 192, 75, .11), transparent 24%),
    $canvas;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
}

button, input { font: inherit; }
button { color: inherit; }

.portal-header {
  position: relative;
  z-index: 20;
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  min-height: 86px;
  padding: 12px 16px;
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
  display: grid;
  flex: 0 0 60px;
  width: 60px;
  height: 60px;
  place-items: center;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(145deg, #0e88e9, #08a7bd);
  box-shadow: 0 8px 16px rgba(8, 124, 229, .22);
  font-size: 27px;
  font-weight: 700;
}

.brand-copy {
  display: flex;
  min-width: 0;
  margin-left: 15px;
  flex-direction: column;
}

.brand-copy strong {
  overflow: hidden;
  font-size: 22px;
  line-height: 1.35;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.brand-copy small { margin-top: 3px; color: $muted; font-size: 13px; }
.header-actions { display: flex; align-items: center; gap: 10px; }

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
.search-panel__title small, .search-result small { color: $muted; }

.search-result {
  display: flex;
  width: 100%;
  padding: 9px 8px;
  align-items: center;
  border: 0;
  border-radius: 10px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.search-result:hover, .search-result:focus-visible { outline: none; background: #edf6fd; }
.search-result > span:nth-child(2) { display: flex; min-width: 0; margin-left: 10px; flex: 1; flex-direction: column; }
.search-result small { margin-top: 2px; font-size: 12px; }
.search-result > i { color: #7790aa; }
.search-empty { padding: 24px 12px; color: $muted; text-align: center; }

.system-chip, .shift-chip {
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
.system-switch {
  height: 30px;
  margin-left: 9px;
  padding: 0 10px;
  border: 0;
  border-radius: 15px;
  color: #075eb5;
  background: rgba(255, 255, 255, .72);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background .18s ease, transform .18s ease;
}
.system-switch:hover, .system-switch:focus-visible { outline: none; background: #fff; }
.system-switch:active { transform: scale(.97); }
.system-switch i { margin-left: 2px; font-size: 10px; }
.status-dot { display: inline-block; width: 9px; height: 9px; margin-right: 8px; border-radius: 50%; background: #11a574; box-shadow: 0 0 0 4px rgba(17, 165, 116, .12); }

.round-action, .user-entry {
  display: grid;
  width: 50px;
  height: 50px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
}

.round-action { color: #29435f; background: #eaf3fb; font-size: 20px; }
.round-action:hover, .round-action:focus-visible { outline: 3px solid rgba(8, 124, 229, .16); background: #dcecf9; }
.user-entry { overflow: hidden; color: #fff; background: $primary; font-size: 19px; font-weight: 700; }
.user-entry img { width: 100%; height: 100%; object-fit: cover; }
.notice-badge ::v-deep .el-badge__content { top: 8px; right: 10px; }

.portal-main { display: grid; min-height: 0; margin-top: 22px; grid-template-columns: minmax(0, 1fr) 460px; gap: 24px; flex: 1 1 auto; }
.desktop-area { position: relative; display: flex; min-height: 0; padding: 26px 22px; flex-direction: column; }
.welcome-row { display: flex; align-items: flex-start; justify-content: space-between; }
.welcome-actions { display: flex; flex-direction: column; align-items: flex-end; gap: 12px; }
.quick-nav-config {
  display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; border: 1px solid rgba(8,111,216,.18);
  border-radius: 999px; background: rgba(255,255,255,.92); color: #086fd8; font-size: 13px; cursor: pointer;
  transition: background .2s ease, box-shadow .2s ease;
  &:hover { background: #fff; box-shadow: 0 8px 18px rgba(8,111,216,.12); }
}
.date-label { margin: 0 0 7px; color: #3275aa; font-size: 14px; font-weight: 600; }
.welcome-row h1 { margin: 0; color: #0e223c; font-size: 36px; line-height: 1.25; letter-spacing: -.02em; }
.welcome-row > div:first-child > p:last-child { max-width: 620px; margin: 10px 0 0; color: $muted; font-size: 16px; line-height: 1.7; }
.shift-chip { height: 42px; background: rgba(255, 255, 255, .66); }

.app-viewport {
  position: relative;
  height: auto;
  min-height: 260px;
  max-height: 430px;
  margin-top: 44px;
  padding: 12px 6px 0;
  overflow: hidden;
  box-sizing: border-box;
  flex: 1 1 auto;
  touch-action: pan-y;
}

.app-grid {
  display: grid;
  height: calc(100% - 12px);
  align-content: start;
  grid-template-columns: repeat(var(--app-columns), minmax(108px, 1fr));
  column-gap: clamp(14px, 2vw, 30px);
  row-gap: 30px;
}

.app-tile { display: flex; min-width: 0; padding: 0; align-items: center; border: 0; outline: 0; background: transparent; flex-direction: column; cursor: pointer; }
.app-icon { position: relative; display: grid; width: 92px; height: 92px; place-items: center; border-radius: 22px; color: #fff; font-size: 43px; transition: transform .2s cubic-bezier(.16, 1, .3, 1), filter .2s ease; }
.app-tile:hover .app-icon, .app-tile:focus-visible .app-icon { transform: translateY(-5px); filter: saturate(1.08); }
.app-tile:focus-visible .app-icon { outline: 3px solid rgba(8, 124, 229, .24); outline-offset: 5px; }
.app-tile:active .app-icon { transform: translateY(-1px) scale(.97); }
.app-icon .svg-icon { position: relative; z-index: 1; width: 42px; height: 42px; }
.app-icon > i { position: relative; z-index: 1; font-size: 42px; line-height: 1; }
.icon-highlight { position: absolute; top: 0; right: 8px; left: 8px; height: 27px; border-radius: 18px; background: linear-gradient(180deg, rgba(255, 255, 255, .28), transparent); pointer-events: none; }
.app-icon em { position: absolute; top: -7px; right: -7px; z-index: 2; min-width: 24px; height: 24px; padding: 0 6px; border: 3px solid $canvas; border-radius: 12px; color: #fff; background: #ed3d45; font-size: 12px; font-style: normal; font-weight: 700; line-height: 18px; }
.app-tile > strong { margin-top: 13px; font-size: 16px; line-height: 1.4; }
.app-tile > small { margin-top: 3px; color: $muted; font-size: 12px; }
.app-page-next-enter-active,
.app-page-next-leave-active,
.app-page-prev-enter-active,
.app-page-prev-leave-active {
  transition: transform .24s cubic-bezier(.16, 1, .3, 1), opacity .18s ease;
}
.app-page-next-enter { opacity: 0; transform: translateX(34px); }
.app-page-next-leave-to { opacity: 0; transform: translateX(-34px); }
.app-page-prev-enter { opacity: 0; transform: translateX(-34px); }
.app-page-prev-leave-to { opacity: 0; transform: translateX(34px); }
.page-indicator { display: flex; min-height: 20px; margin-top: 16px; align-items: center; justify-content: center; gap: 7px; }
.page-indicator button { width: 9px; height: 9px; padding: 0; border: 0; border-radius: 5px; background: #9eb1c5; cursor: pointer; transition: width .2s ease, background .2s ease; }
.page-indicator button:hover, .page-indicator button:focus-visible { outline: 3px solid rgba(8, 124, 229, .15); outline-offset: 2px; }
.page-indicator button.active { width: 34px; background: $primary; }
.info-rail {
  min-width: 0;
  min-height: 0;
  height: 100%;
  margin: 0;
  padding: 0;
  border-radius: 0;
  color: $ink;
  background: transparent;
  font-family: inherit;
  font-size: inherit;
  line-height: normal;
}
.info-panel {
  padding: 22px;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, .72);
  border-radius: 16px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, .76), rgba(224, 242, 255, .64)),
    rgba(234, 247, 255, .68);
  box-shadow: 0 8px 22px rgba(34, 112, 166, .08);
  backdrop-filter: blur(18px) saturate(130%);
  -webkit-backdrop-filter: blur(18px) saturate(130%);
}
.panel-heading { display: flex; align-items: center; justify-content: space-between; }
.panel-heading h2 { margin: 0; font-size: 20px; }
.panel-heading p { margin: 5px 0 0; color: #607895; font-size: 12px; }
.panel-heading time, .panel-heading span { color: $muted; font-size: 13px; }
.panel-heading button { padding: 5px; border: 0; color: $muted; background: transparent; cursor: pointer; }

.workbench-panel { display: flex; height: 100%; min-height: 0; flex-direction: column; }
.workbench-tabs { display: flex; margin-top: 20px; gap: 7px; }
.workbench-tabs button { height: 38px; padding: 0 14px; border: 1px solid rgba(255, 255, 255, .76); border-radius: 19px; color: #4f6680; background: rgba(231, 242, 251, .72); font-size: 14px; font-weight: 600; white-space: nowrap; cursor: pointer; transition: background .18s ease, color .18s ease, transform .18s ease; }
.workbench-tabs button:hover { background: rgba(255, 255, 255, .9); }
.workbench-tabs button.active { color: #fff; background: $primary; }
.workbench-tabs button:active { transform: scale(.98); }
.workbench-tabs button:focus-visible { outline: 3px solid rgba(8, 124, 229, .2); outline-offset: 2px; }
.task-list { display: flex; min-height: 0; margin-top: 16px; overflow: auto; gap: 10px; flex: 1 1 auto; flex-direction: column; scrollbar-color: #aac0d3 transparent; scrollbar-width: thin; }
.task-item { display: flex; width: 100%; padding: 16px 14px; align-items: flex-start; justify-content: space-between; border: 1px solid rgba(255, 255, 255, .78); border-radius: 12px; background: linear-gradient(135deg, rgba(255, 255, 255, .76), rgba(222, 240, 253, .66)); text-align: left; cursor: pointer; transition: background .18s ease, transform .18s ease; }
.task-item:hover, .task-item:focus-visible { outline: none; background: rgba(255, 255, 255, .94); transform: translateY(-1px); }
.task-item:active { transform: translateY(0) scale(.995); }
.task-item > span { display: flex; min-width: 0; padding-right: 8px; flex-direction: column; }
.task-item strong { font-size: 14px; }
.task-item small { margin-top: 7px; color: #5a718e; font-size: 12px; line-height: 1.5; }
.task-item em { flex: 0 0 auto; padding: 4px 8px; border-radius: 12px; color: #116bb6; background: #cce7fb; font-size: 11px; font-style: normal; font-weight: 700; }
.task-item em.urgent { color: #d72835; background: #ffd8db; }
.task-item em.warning { color: #9b5a00; background: #ffe5b8; }
.task-item em.success { color: #087a59; background: #cceee3; }
.workbench-more {
  display: flex;
  width: 100%;
  min-height: 42px;
  margin-top: 12px;
  padding: 0 12px;
  align-items: center;
  border: 0;
  border-radius: 11px;
  color: #075eb5;
  background: rgba(222, 239, 252, .82);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background .18s ease, transform .18s ease;
}
.workbench-more:hover,
.workbench-more:focus-visible { outline: 3px solid rgba(8, 124, 229, .14); background: #d5eafb; }
.workbench-more:active { transform: scale(.995); }
.workbench-more span { margin-left: 7px; color: #637b95; font-size: 11px; font-weight: 400; }
.workbench-more i { margin-left: auto; }

.mini-icon { display: grid; flex: 0 0 42px; width: 42px; height: 42px; place-items: center; border-radius: 12px; color: #fff; }
.mini-icon .svg-icon { width: 22px; height: 22px; }
.mini-icon > i { font-size: 22px; line-height: 1; }
@media (max-width: 1280px) {
  .portal-header { min-height: 76px; }
  .brand-mark { flex-basis: 52px; width: 52px; height: 52px; }
  .brand-copy strong { font-size: 19px; }
  .brand-copy small { display: none; }
  .app-search { width: 270px; }
  .system-chip span:not(.status-dot) { display: none; }
  .portal-main { grid-template-columns: minmax(0, 1fr) 410px; gap: 18px; }
}

@media (max-width: 1080px) {
  .jump-portal { height: auto; min-height: 100vh; overflow-x: hidden; overflow-y: auto; padding-right: 18px; padding-bottom: 98px; padding-left: 18px; }
  .portal-header { align-items: flex-start; gap: 12px; flex-direction: column; }
  .header-actions { width: 100%; }
  .app-search { width: auto; flex: 1; }
  .portal-main { min-height: auto; grid-template-columns: 1fr; flex: none; }
  .desktop-area { min-height: auto; padding-bottom: 58px; }
  .info-rail { height: auto; }
  .workbench-panel { height: auto; min-height: 560px; }
  .task-list { max-height: 430px; min-height: 360px; }
}

@media (max-width: 820px) {
  .jump-portal { padding: 12px 12px 98px; }
  .brand-copy strong { font-size: 17px; }
  .welcome-row h1 { font-size: 30px; }
  .shift-chip { display: none; }
  .app-viewport { height: 300px; margin-top: 36px; }
  .app-grid { grid-template-columns: repeat(var(--app-columns), minmax(84px, 1fr)); }
  .app-icon { width: 76px; height: 76px; border-radius: 18px; }
  .app-icon .svg-icon { width: 34px; height: 34px; }
  .workbench-tabs { overflow-x: auto; padding-bottom: 4px; }
}

@media (max-width: 560px) {
  .notice-badge, .user-entry { display: none; }
  .header-actions { flex-wrap: wrap; }
  .app-search { flex-basis: 100%; }
  .system-chip { max-width: 100%; }
  .brand-mark { flex-basis: 46px; width: 46px; height: 46px; border-radius: 13px; }
  .desktop-area { padding-right: 4px; padding-left: 4px; }
  .app-viewport { height: 300px; }
  .app-grid { grid-template-columns: repeat(var(--app-columns), minmax(80px, 1fr)); column-gap: 8px; }
  .app-tile > small { display: none; }
  .workbench-panel { min-height: 520px; padding: 18px 14px; }
  .task-list { min-height: 330px; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; animation-iteration-count: 1 !important; }
  .app-tile:hover .app-icon, .app-tile:focus-visible .app-icon, .app-tile:active .app-icon { transform: none; }
}
</style>

<style lang="scss">
.system-dropdown { min-width: 260px; padding: 8px; border: 0; border-radius: 14px; box-shadow: 0 12px 28px rgba(41, 81, 117, .16); }
.system-dropdown .el-dropdown-menu__item { height: auto; padding: 9px 10px; border-radius: 10px; line-height: 1.4; }
.system-dropdown .el-dropdown-menu__item:hover { color: #075eb5; background: #edf6fd; }
.system-dropdown .el-dropdown-menu__item.is-current { color: #075eb5; background: #e5f2fd; }
.system-option { display: flex; align-items: center; gap: 10px; }
.system-option > i:first-child { width: 28px; color: #2785d1; font-size: 18px; text-align: center; }
.system-option > span { display: flex; min-width: 0; flex: 1; flex-direction: column; }
.system-option strong { color: #183653; font-size: 13px; }
.system-option small { margin-top: 2px; color: #71859b; font-size: 11px; }
.system-option > .el-icon-check { color: #087ce5; font-weight: 700; }
</style>
