<template>
  <div
    class="portal-quick-nav-root"
    v-loading="showQuickNavLoading"
    element-loading-text="快捷导航加载中"
    element-loading-spinner="el-icon-loading"
    element-loading-background="rgba(255, 255, 255, 0.55)"
  >
  <div
    v-if="homeApps.length || quickNavEditMode || showQuickNavLoading"
    ref="appViewport"
    class="portal-quick-nav"
    :class="[
      `portal-quick-nav--${variant}`,
      { 'is-quick-nav-edit': quickNavEditMode }
    ]"
    @wheel="handleAppWheel"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
  >
    <div v-if="quickNavEditMode" class="quick-nav-edit-bar">
      <span>按住图标拖动排序；点右上角减号可移除；带锁的为角色默认不可删</span>
      <div class="quick-nav-edit-actions">
        <button type="button" class="quick-nav-reset" @click="resetQuickNavToRoleDefault">恢复角色默认</button>
        <button type="button" @click="exitQuickNavEditMode">完成</button>
      </div>
    </div>

    <draggable
      v-if="quickNavEditMode"
      v-model="quickNavEditApps"
      class="app-grid app-grid--edit"
      :style="gridStyle"
      :animation="220"
      :delay="80"
      :delay-on-touch-only="true"
      :touch-start-threshold="5"
      ghost-class="app-tile-ghost"
      chosen-class="app-tile-chosen"
      @end="handleQuickNavDragEnd"
    >
      <div
        v-for="app in quickNavEditApps"
        :key="'edit-' + app.menuId"
        class="app-tile app-tile--edit"
        :class="{ 'is-locked': isQuickNavLocked(app) }"
        role="listitem"
      >
        <button
          v-if="!isQuickNavLocked(app)"
          type="button"
          class="quick-nav-remove"
          aria-label="移除快捷导航"
          @click.stop="removeQuickNavApp(app)"
        >
          <i class="el-icon-minus" />
        </button>
        <span
          v-else
          class="quick-nav-lock"
          title="角色默认快捷导航，不可移除"
          aria-label="角色默认快捷导航，不可移除"
        >
          <i class="el-icon-lock" />
        </span>
        <span class="app-icon" :style="iconStyle(app)">
          <span class="icon-highlight" />
          <svg-icon v-if="app.svgIcon" :icon-class="app.svgIcon" />
          <i v-else-if="app.icon" :class="app.icon" />
          <svg-icon v-else icon-class="component" />
        </span>
        <strong :title="app.name">{{ app.name }}</strong>
      </div>
    </draggable>

    <transition v-else :name="pageTransition" mode="out-in">
      <div
        :key="currentAppPage"
        class="app-grid"
        :style="gridStyle"
        role="list"
        aria-label="快捷导航"
      >
        <button
          v-for="app in pagedApps"
          :key="appKey(app)"
          class="app-tile"
          type="button"
          role="listitem"
          @click="handleAppClick(app)"
          @mousedown="handleItemPressStart($event, app)"
          @touchstart.passive="handleItemPressStart($event, app)"
          @touchmove.passive="handleItemPressMove"
          @touchend="handleItemPressEnd"
          @touchcancel="handleItemPressEnd"
          @contextmenu.prevent
        >
          <span class="app-icon" :style="iconStyle(app)">
            <span class="icon-highlight" />
            <svg-icon v-if="app.svgIcon" :icon-class="app.svgIcon" />
            <i v-else-if="app.icon" :class="app.icon" />
            <svg-icon v-else icon-class="component" />
            <em v-if="app.badge">{{ app.badge }}</em>
          </span>
          <strong :title="app.name">{{ app.name }}</strong>
        </button>
      </div>
    </transition>

    <div
      v-if="variant === 'home' && appPageCount > 1 && !quickNavEditMode && !contextMenu.visible"
      class="page-indicator"
      aria-label="应用分页"
    >
      <button
        v-for="page in appPageCount"
        :key="page"
        type="button"
        :class="{ active: currentAppPage === page - 1 }"
        :aria-label="'切换到第 ' + page + ' 页'"
        @click="goToAppPage(page - 1)"
      />
    </div>

  </div>

  <quick-nav-context-menu
    :visible="contextMenu.visible"
    :menu-style="contextMenuStyle"
    :show-reorder="true"
    :show-unsubscribe="true"
    :unsubscribe-disabled="contextMenu.item && isQuickNavLocked(contextMenu.item)"
    :manual-visible.sync="manualDialogVisible"
    :manual-title="manualDialogTitle"
    :manual-content="manualDialogContent"
    @close="closeContextMenu"
    @reorder="handleContextReorder"
    @unsubscribe="handleContextUnsubscribe"
    @view-manual="handleContextViewManual"
    @manual-closed="manualDialogItem = null"
  />
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import draggable from 'vuedraggable'
import { syncUserQuickNavFromRole } from '@/api/system/user/quickNav'
import { syncSubSystemUserQuickNavFromRole } from '@/api/system/user/subSystemQuickNav'
import { saveQuickNavMenuIds, isQuickNavMenuLocked, removeQuickNavMenuId, resolveQuickNavMenuIds } from '@/utils/portalQuickNavToggle'
import {
  buildQuickNavScopeKey,
  clearQuickNavCache,
  getQuickNavCache,
  setQuickNavCache
} from '@/utils/portalQuickNavCache'
import { checkQuickNavUpdateIfNeeded, rememberQuickNavSignature } from '@/utils/portalQuickNavWatch'
import { buildPortalHomeApps } from '@/utils/portalQuickNavApps'
import { buildIconStyle } from '@/utils/menuIconStyle'
import QuickNavContextMenu from '@/components/QuickNavContextMenu.vue'
import quickNavLongPressMixin from '@/mixins/quickNavLongPressMixin'

export default {
  name: 'PortalQuickNavPanel',
  components: { draggable, QuickNavContextMenu },
  mixins: [quickNavLongPressMixin],
  props: {
    variant: {
      type: String,
      default: 'home'
    }
  },
  data() {
    return {
      quickNavMenuIds: [],
      quickNavLockedMenuIds: [],
      quickNavConfigured: false,
      /** null=未就绪；[]/数组=服务端已确认（禁止再等侧栏） */
      quickNavApps: null,
      quickNavLoadedScope: null,
      quickNavLoading: false,
      quickNavEditMode: false,
      quickNavEditApps: [],
      quickNavSaving: false,
      currentAppPage: 0,
      appColumns: 6,
      appRows: 2,
      pageDirection: 'next',
      touchStartX: 0,
      touchStartY: 0,
      lastWheelAt: 0,
      appResizeObserver: null
    }
  },
  computed: {
    ...mapGetters(['sidebarRouters', 'currentSystemSidebarRouters', 'currentSystem', 'portalSystemList']),
    ...mapState('portal', ['quickNavSyncing']),
    showQuickNavLoading() {
      return !!(this.quickNavLoading || this.quickNavSyncing)
    },
    currentSubSystemId() {
      if (this.currentSystem === 'main') {
        return 0
      }
      const sys = (this.portalSystemList || []).find(item => item.clientId === this.currentSystem)
      // 列表未就绪时返回 null，禁止误打主系统 quick-nav
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
    homeApps() {
      return this.buildHomeApps(this.quickNavMenuIds, this.quickNavConfigured)
    },
    appPageSize() {
      if (this.variant === 'compact') {
        return Math.max(1, this.homeApps.length)
      }
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
    gridStyle() {
      if (this.variant === 'compact') {
        return {
          '--app-columns': Math.min(this.homeApps.length || 1, 8)
        }
      }
      return { '--app-columns': this.appColumns }
    },
    contextMenuShowUnsubscribe() {
      return true
    },
    contextMenuShowReorder() {
      return this.homeApps.length > 1
    }
  },
  watch: {
    currentSystem() {
      this.onSystemChange()
    },
    // 子系统列表晚到：从 pending 解析出真实 subSystemId 后再拉正确 scope
    currentSubSystemId(newId, oldId) {
      if (this.currentSystem === 'main') {
        return
      }
      if (newId != null && newId > 0 && newId !== oldId) {
        this.onSystemChange()
      }
    },
    homeApps() {
      this.$nextTick(this.updateAppPagination)
      // 数据就绪后恢复上次浏览页（打开菜单再返回时保持当前分页）
      this.restoreAppPageOnce()
    }
  },
  mounted() {
    this.restoreQuickNavFromCache()
    this.loadQuickNav()
    this.$nextTick(this.initAppPagination)
    document.addEventListener('keydown', this.handleQuickNavEditKeydown)
    document.addEventListener('visibilitychange', this.handleVisibilityChange)
    this._onPortalQuickNavChanged = (payload) => {
      if (!payload || payload.scopeKey !== this.quickNavScopeKey || payload.source === 'panel') {
        return
      }
      // 全部应用抽屉 / 变更探测后同步到首页
      this.applyQuickNavChange(payload)
    }
    this.$root.$on('portal-quick-nav-changed', this._onPortalQuickNavChanged)
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.handleQuickNavEditKeydown)
    document.removeEventListener('visibilitychange', this.handleVisibilityChange)
    if (this._onPortalQuickNavChanged) {
      this.$root.$off('portal-quick-nav-changed', this._onPortalQuickNavChanged)
      this._onPortalQuickNavChanged = null
    }
    if (this.appResizeObserver) {
      this.appResizeObserver.disconnect()
    }
    window.removeEventListener('resize', this.updateAppPagination)
  },
  methods: {
    iconStyle(app) {
      return buildIconStyle(app)
    },
    appKey(app) {
      return app.path || `${app.name}|${app.subtitle || ''}|${app.menuId || ''}`
    },
    buildHomeApps(menuIds, configured) {
      const scopeKey = this.quickNavScopeKey
      if (this.quickNavLoadedScope !== scopeKey) {
        const cached = getQuickNavCache(scopeKey)
        if (cached) {
          menuIds = cached.menuIds
          configured = cached.configured
        }
      }
      return buildPortalHomeApps(
        this.currentSystemSidebarRouters,
        this.currentSystem,
        menuIds,
        configured,
        this.quickNavApps
      )
    },
    isQuickNavLocked(app) {
      return isQuickNavMenuLocked(app && app.menuId, this.quickNavLockedMenuIds)
    },
    getDisplayedQuickNavMenuIds() {
      return this.homeApps.map(app => app.menuId).filter(id => id != null)
    },
    resolvePersistMenuIds(menuIds) {
      if (Array.isArray(menuIds)) {
        return menuIds
      }
      return resolveQuickNavMenuIds(
        this.quickNavMenuIds,
        this.quickNavConfigured,
        this.getDisplayedQuickNavMenuIds()
      )
    },
    restoreQuickNavFromCache() {
      const scopeKey = this.quickNavScopeKey
      const cached = getQuickNavCache(scopeKey)
      if (cached && Array.isArray(cached.apps)) {
        this.quickNavMenuIds = [...cached.menuIds]
        this.quickNavLockedMenuIds = [...(cached.lockedMenuIds || [])]
        this.quickNavConfigured = cached.configured
        this.quickNavApps = [...cached.apps]
        this.quickNavLoadedScope = scopeKey
        this.emitQuickNavChange()
        return
      }
      this.quickNavMenuIds = []
      this.quickNavLockedMenuIds = []
      this.quickNavConfigured = false
      this.quickNavApps = null
      this.quickNavLoadedScope = null
      this.emitQuickNavChange()
    },
    preloadForSystem(systemValue) {
      if (systemValue === 'main') {
        const cached = getQuickNavCache('main')
        if (cached && Array.isArray(cached.apps)) {
          this.quickNavMenuIds = [...cached.menuIds]
          this.quickNavLockedMenuIds = [...(cached.lockedMenuIds || [])]
          this.quickNavConfigured = cached.configured
          this.quickNavApps = [...cached.apps]
          this.quickNavLoadedScope = 'main'
        } else {
          this.quickNavMenuIds = []
          this.quickNavLockedMenuIds = []
          this.quickNavConfigured = false
          this.quickNavApps = null
          this.quickNavLoadedScope = null
        }
        this.emitQuickNavChange()
        return
      }
      const sys = (this.portalSystemList || []).find(item => item.clientId === systemValue)
      const scopeKey = buildQuickNavScopeKey(systemValue, sys ? Number(sys.subSystemId) : 0)
      const cached = getQuickNavCache(scopeKey)
      if (cached && Array.isArray(cached.apps)) {
        this.quickNavMenuIds = [...cached.menuIds]
        this.quickNavLockedMenuIds = [...(cached.lockedMenuIds || [])]
        this.quickNavConfigured = cached.configured
        this.quickNavApps = [...cached.apps]
        this.quickNavLoadedScope = scopeKey
      } else {
        this.quickNavMenuIds = []
        this.quickNavLockedMenuIds = []
        this.quickNavConfigured = false
        this.quickNavApps = null
        this.quickNavLoadedScope = null
      }
      this.emitQuickNavChange()
    },
    onSystemChange() {
      this.exitQuickNavEditMode()
      this.closeContextMenu()
      this.restoreQuickNavFromCache()
      this.currentAppPage = 0
      // 切换系统后允许恢复新 scope 自己的页码
      this._appPageRestoredKey = null
      this.loadQuickNav().finally(() => {
        this.$nextTick(this.updateAppPagination)
      })
    },
    reload() {
      return this.loadQuickNav()
    },
    loadQuickNav(options = {}) {
      const scopeKey = this.quickNavScopeKey
      // 子系统但 id 未知：等 systemList，避免打成主系统接口
      if (this.currentSystem !== 'main' && (this.currentSubSystemId == null || this.currentSubSystemId <= 0)) {
        this.quickNavLoading = false
        return Promise.resolve()
      }
      this.quickNavLoading = true
      // 默认不 force：与 PortalShell / bootstrap 预取单飞；角色变更探测仍可 force
      return this.$store.dispatch('portal/loadQuickNavConfig', {
        subSystemId: this.currentSubSystemId,
        force: !!options.force
      }).then(config => {
        const data = config || {}
        const menuIds = data.menuIds || []
        const lockedMenuIds = data.lockedMenuIds || []
        const configured = !!data.configured
        const apps = Object.prototype.hasOwnProperty.call(data, 'apps')
          ? (Array.isArray(data.apps) ? data.apps : [])
          : null
        if (scopeKey !== this.quickNavScopeKey) {
          return
        }
        this.quickNavMenuIds = menuIds
        this.quickNavLockedMenuIds = lockedMenuIds
        this.quickNavConfigured = configured
        this.quickNavApps = apps
        this.quickNavLoadedScope = scopeKey
        setQuickNavCache(scopeKey, menuIds, configured, lockedMenuIds, apps)
        this.emitQuickNavChange()
        rememberQuickNavSignature(scopeKey, menuIds, lockedMenuIds, apps)
        this.$nextTick(this.updateAppPagination)
      }).catch(() => {
        if (scopeKey === this.quickNavScopeKey) {
          this.quickNavMenuIds = []
          this.quickNavLockedMenuIds = []
          this.quickNavConfigured = false
          this.quickNavApps = []
          this.quickNavLoadedScope = scopeKey
          this.emitQuickNavChange()
        }
      }).finally(() => {
        if (scopeKey === this.quickNavScopeKey) {
          this.quickNavLoading = false
        }
      })
    },
    emitQuickNavChange() {
      const payload = {
        menuIds: [...this.quickNavMenuIds],
        lockedMenuIds: [...this.quickNavLockedMenuIds],
        configured: this.quickNavConfigured,
        apps: Array.isArray(this.quickNavApps) ? [...this.quickNavApps] : this.quickNavApps
      }
      this.$emit('quick-nav-change', payload)
      // 通知 PortalShell / 全部应用抽屉对齐，避免取消后再添加时用旧列表写回
      this.$root.$emit('portal-quick-nav-changed', {
        ...payload,
        scopeKey: this.quickNavScopeKey,
        source: 'panel'
      })
    },
    applyQuickNavChange(payload) {
      const menuIds = (payload && payload.menuIds) || []
      const lockedMenuIds = (payload && payload.lockedMenuIds) || this.quickNavLockedMenuIds
      const scopeKey = this.quickNavScopeKey
      this.quickNavMenuIds = menuIds
      this.quickNavLockedMenuIds = [...lockedMenuIds]
      this.quickNavConfigured = !!(payload && payload.configured)
      this.quickNavLoadedScope = scopeKey
      if (payload && Object.prototype.hasOwnProperty.call(payload, 'apps')) {
        this.quickNavApps = Array.isArray(payload.apps)
          ? payload.apps
          : (payload.apps === null ? null : [])
      }
      setQuickNavCache(
        scopeKey,
        menuIds,
        this.quickNavConfigured,
        lockedMenuIds,
        payload && Object.prototype.hasOwnProperty.call(payload, 'apps')
          ? (Array.isArray(payload.apps) ? payload.apps : (payload.apps === null ? null : []))
          : undefined
      )
      this.currentAppPage = 0
      if (this.quickNavEditMode) {
        this.quickNavEditApps = this.homeApps.map(app => ({ ...app }))
        if (!this.quickNavEditApps.length) {
          this.exitQuickNavEditMode()
        }
      }
      this.emitQuickNavChange()
      this.$nextTick(this.updateAppPagination)
    },
    shouldBlockLongPress() {
      return this.quickNavEditMode
    },
    handleAppClick(app) {
      if (this.quickNavEditMode || this.suppressNextItemClick || this.contextMenu.visible) {
        this.suppressNextItemClick = false
        return
      }
      this.$emit('open', app)
    },
    enterQuickNavEditMode() {
      if (!this.homeApps.length || this.quickNavEditMode) {
        return
      }
      this.quickNavEditMode = true
      this.quickNavEditApps = this.homeApps.map(app => ({ ...app }))
      this.suppressNextItemClick = true
      window.setTimeout(() => {
        this.suppressNextItemClick = false
      }, 400)
    },
    exitQuickNavEditMode() {
      if (!this.quickNavEditMode) {
        return
      }
      this.quickNavEditMode = false
      this.quickNavEditApps = []
      this.$nextTick(this.updateAppPagination)
    },
    handleQuickNavEditKeydown(event) {
      if (event.key !== 'Escape') {
        return
      }
      if (this.contextMenu.visible) {
        this.closeContextMenu()
        return
      }
      if (this.quickNavEditMode) {
        this.exitQuickNavEditMode()
      }
    },
    handleContextReorder() {
      this.closeContextMenu()
      this.enterQuickNavEditMode()
    },
    async handleContextUnsubscribe() {
      const app = this.contextMenu.item
      if (!app) {
        return
      }
      if (this.isQuickNavLocked(app)) {
        this.$message.warning('该入口为角色默认快捷导航，不可取消订阅')
        return
      }
      this.closeContextMenu()
      try {
        await this.persistQuickNavRemove(app.menuId)
        this.$message.success(`已取消订阅「${app.name}」`)
      } catch (error) {
        if (error && error.message === 'ROLE_QUICK_NAV_LOCKED') {
          this.$message.warning('该入口为角色默认快捷导航，不可取消订阅')
        }
      }
    },
    async persistQuickNav(menuIds) {
      if (this.quickNavSaving) {
        return
      }
      this.quickNavSaving = true
      try {
        const config = await saveQuickNavMenuIds(menuIds, this.currentSubSystemId)
        this.applySavedQuickNavConfig(config)
      } catch (error) {
        this.$message.error('快捷导航保存失败')
        if (this.quickNavEditMode) {
          this.quickNavEditApps = this.homeApps.map(app => ({ ...app }))
        }
      } finally {
        this.quickNavSaving = false
      }
    },
    async persistQuickNavRemove(menuId) {
      if (this.quickNavSaving) {
        return
      }
      this.quickNavSaving = true
      try {
        const config = await removeQuickNavMenuId(
          menuId,
          this.quickNavMenuIds,
          this.currentSubSystemId,
          this.quickNavLockedMenuIds,
          this.quickNavConfigured,
          this.getDisplayedQuickNavMenuIds()
        )
        this.applySavedQuickNavConfig(config)
      } catch (error) {
        if (error && error.message === 'ROLE_QUICK_NAV_LOCKED') {
          throw error
        }
        this.$message.error('快捷导航保存失败')
        throw error
      } finally {
        this.quickNavSaving = false
      }
    },
    applySavedQuickNavConfig(config) {
      this.$store.dispatch('portal/applyQuickNavConfig', {
        subSystemId: this.currentSubSystemId,
        config
      })
      this.quickNavMenuIds = [...(config.menuIds || [])]
      this.quickNavLockedMenuIds = [...(config.lockedMenuIds || [])]
      this.quickNavConfigured = config.configured !== false
      this.quickNavApps = Array.isArray(config.apps) ? [...config.apps] : []
      this.quickNavLoadedScope = this.quickNavScopeKey
      this.emitQuickNavChange()
      if (this.quickNavEditMode) {
        this.quickNavEditApps = this.homeApps.map(app => ({ ...app }))
        if (!this.quickNavEditApps.length) {
          this.exitQuickNavEditMode()
        }
      }
      this.$nextTick(this.updateAppPagination)
    },
    handleQuickNavDragEnd() {
      const menuIds = this.quickNavEditApps
        .map(app => app.menuId)
        .filter(menuId => menuId != null)
      this.persistQuickNav(menuIds)
    },
    removeQuickNavApp(app) {
      if (!app || app.menuId == null || this.quickNavSaving || this.isQuickNavLocked(app)) {
        return
      }
      this.persistQuickNavRemove(app.menuId).catch(() => {})
    },
    async resetQuickNavToRoleDefault() {
      if (this.quickNavSaving) {
        return
      }
      try {
        await this.$confirm('将按当前角色默认快捷导航重新同步，是否继续？', '恢复角色默认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (error) {
        return
      }
      this.quickNavSaving = true
      try {
        const request = this.currentSubSystemId > 0
          ? syncSubSystemUserQuickNavFromRole(this.currentSubSystemId)
          : syncUserQuickNavFromRole()
        await request
        clearQuickNavCache(this.quickNavScopeKey)
        this.exitQuickNavEditMode()
        await this.loadQuickNav({ force: true })
      } catch (error) {
        this.$message.error('恢复角色默认失败')
      } finally {
        this.quickNavSaving = false
      }
    },
    handleVisibilityChange() {
      if (document.visibilityState !== 'visible' || this.quickNavEditMode || this.quickNavSaving) {
        return
      }
      checkQuickNavUpdateIfNeeded(this.currentSubSystemId)
    },
    initAppPagination() {
      if (this.variant !== 'home') {
        return
      }
      this.updateAppPagination()
      if (typeof ResizeObserver !== 'undefined' && this.$refs.appViewport) {
        this.appResizeObserver = new ResizeObserver(this.updateAppPagination)
        this.appResizeObserver.observe(this.$refs.appViewport)
      } else {
        window.addEventListener('resize', this.updateAppPagination)
      }
    },
    updateAppPagination() {
      if (this.variant !== 'home') {
        return
      }
      const viewport = this.$refs.appViewport
      if (!viewport) {
        return
      }
      const width = viewport.clientWidth
      const height = viewport.clientHeight
      let columns = 3
      if (width >= 1020) columns = 6
      else if (width >= 820) columns = 5
      else if (width >= 620) columns = 4
      const rows = this.variant === 'home'
        ? 2
        : Math.max(1, Math.min(3, Math.floor((height + 28) / 155)))
      this.appColumns = columns
      this.appRows = rows
      if (this.currentAppPage >= this.appPageCount) {
        this.currentAppPage = this.appPageCount - 1
      }
    },
    handleAppWheel(event) {
      if (this.variant !== 'home' || this.quickNavEditMode || this.contextMenu.visible || this.appPageCount <= 1) {
        return
      }
      const delta = Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY
      if (Math.abs(delta) < 18) {
        return
      }
      event.preventDefault()
      const now = Date.now()
      if (now - this.lastWheelAt < 420) {
        return
      }
      this.lastWheelAt = now
      this.changeAppPage(delta > 0 ? 1 : -1)
    },
    handleTouchStart(event) {
      const touch = event.touches && event.touches[0]
      if (!touch) {
        return
      }
      this.touchStartX = touch.clientX
      this.touchStartY = touch.clientY
    },
    handleTouchEnd(event) {
      if (this.variant !== 'home' || this.quickNavEditMode || this.contextMenu.visible) {
        return
      }
      const touch = event.changedTouches && event.changedTouches[0]
      if (!touch || this.appPageCount <= 1) {
        return
      }
      const deltaX = touch.clientX - this.touchStartX
      const deltaY = touch.clientY - this.touchStartY
      if (Math.abs(deltaX) < 45 || Math.abs(deltaX) <= Math.abs(deltaY)) {
        return
      }
      this.changeAppPage(deltaX < 0 ? 1 : -1)
    },
    changeAppPage(step) {
      const nextPage = Math.min(Math.max(this.currentAppPage + step, 0), this.appPageCount - 1)
      if (nextPage === this.currentAppPage) {
        return
      }
      this.pageDirection = step > 0 ? 'next' : 'prev'
      this.currentAppPage = nextPage
      this.persistAppPage()
    },
    goToAppPage(page) {
      if (page === this.currentAppPage) {
        return
      }
      this.pageDirection = page > this.currentAppPage ? 'next' : 'prev'
      this.currentAppPage = page
      this.persistAppPage()
    },
    /** 快捷导航分页记忆：按 scope 存 sessionStorage，重进首页后不回第一页 */
    appPageStorageKey() {
      return `JUMP_QUICKNAV_PAGE_${this.quickNavScopeKey}`
    },
    persistAppPage() {
      try {
        sessionStorage.setItem(this.appPageStorageKey(), String(this.currentAppPage))
      } catch (e) { /* ignore */ }
    },
    restoreAppPageOnce() {
      if (this._appPageRestoredKey === this.quickNavScopeKey) {
        return
      }
      if (!this.homeApps.length) {
        return
      }
      let saved = null
      try {
        const raw = sessionStorage.getItem(this.appPageStorageKey())
        if (raw != null && raw !== '') {
          saved = parseInt(raw, 10)
        }
      } catch (e) { /* ignore */ }
      if (saved != null && !Number.isNaN(saved)) {
        this.currentAppPage = Math.min(Math.max(saved, 0), this.appPageCount - 1)
      }
      this._appPageRestoredKey = this.quickNavScopeKey
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #087ce5;
$canvas: #eaf4fc;

.portal-quick-nav-root {
  position: relative;
  min-height: 120px;
}

.portal-quick-nav {
  position: relative;
  box-sizing: border-box;
  touch-action: pan-y;
}

.portal-quick-nav--home {
  display: flex;
  height: auto;
  min-height: 260px;
  max-height: none;
  margin-top: 44px;
  padding: 12px 6px 8px;
  overflow: visible;
  flex: 1 1 auto;
  flex-direction: column;
}

.portal-quick-nav--home.is-quick-nav-edit {
  max-height: none;
  overflow: visible;
  touch-action: none;
}

.portal-quick-nav--compact {
  flex: 0 0 auto;
  margin: 0 0 14px;
  padding: 12px 14px 6px;
  border-radius: 14px;
  background: rgba(255, 255, 255, .72);
  box-shadow: 0 4px 14px rgba(34, 112, 166, .06);
  overflow-x: auto;
  overflow-y: hidden;
}

.portal-quick-nav--compact .app-grid {
  display: flex;
  width: max-content;
  min-width: 100%;
  height: auto;
  align-items: flex-start;
}
.portal-quick-nav--compact .app-grid > * + * {
  margin-left: 18px;
}

.portal-quick-nav--compact .app-tile {
  flex: 0 0 92px;
  width: 92px;
}

.portal-quick-nav--compact .app-icon {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  font-size: 34px;
}

.portal-quick-nav--compact .app-icon .svg-icon {
  width: 34px;
  height: 34px;
}

.portal-quick-nav--compact .app-icon > i {
  font-size: 34px;
}

.portal-quick-nav--compact .app-tile > strong {
  margin-top: 10px;
  font-size: 13px;
}

.portal-quick-nav--compact .quick-nav-remove {
  left: calc(50% - 36px);
}

.quick-nav-edit-bar {
  display: flex;
  margin: -8px 0 18px;
  padding: 10px 14px;
  align-items: center;
  justify-content: space-between;
  border-radius: 12px;
  color: #24527a;
  background: rgba(255, 255, 255, .82);
  box-shadow: 0 4px 14px rgba(34, 112, 166, .08);
  font-size: 13px;
}
.quick-nav-edit-bar > * + * {
  margin-left: 12px;
}

.quick-nav-edit-actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
}
.quick-nav-edit-actions > * + * {
  margin-left: 8px;
}

.quick-nav-edit-bar button {
  flex: 0 0 auto;
  height: 34px;
  padding: 0 16px;
  border: 0;
  border-radius: 17px;
  color: #fff;
  background: $primary;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.quick-nav-edit-bar .quick-nav-reset {
  color: #24527a;
  background: rgba(255, 255, 255, .95);
  border: 1px solid rgba(36, 82, 122, .18);
}

.portal-quick-nav--home .app-grid {
  height: auto;
}

.app-grid {
  display: grid;
  height: calc(100% - 12px);
  align-content: start;
  grid-template-columns: repeat(var(--app-columns), minmax(108px, 1fr));
  column-gap: clamp(14px, 2vw, 30px);
  row-gap: 30px;
}

.app-grid--edit {
  height: auto;
  min-height: 220px;
}

.app-tile {
  display: flex;
  min-width: 0;
  padding: 0;
  align-items: center;
  border: 0;
  outline: 0;
  background: transparent;
  flex-direction: column;
  cursor: pointer;
}

.app-tile--edit {
  position: relative;
  cursor: grab;
  user-select: none;
  -webkit-user-select: none;
}

.app-tile--edit:active {
  cursor: grabbing;
}

.app-tile--edit .app-icon {
  animation: quick-nav-wiggle .34s ease-in-out infinite alternate;
}

.app-tile--edit:nth-child(2n) .app-icon {
  animation-delay: -.17s;
}

.app-tile--edit:nth-child(3n) .app-icon {
  animation-delay: -.08s;
}

.app-icon {
  position: relative;
  display: grid;
  width: 92px;
  height: 92px;
  place-items: center;
  border-radius: 22px;
  font-size: 43px;
  transition: transform .2s cubic-bezier(.16, 1, .3, 1), filter .2s ease;
}

.app-tile:hover .app-icon,
.app-tile:focus .app-icon {
  transform: translateY(-5px);
  filter: saturate(1.08);
}

.app-tile:active .app-icon {
  transform: translateY(-1px) scale(.97);
}

.app-icon .svg-icon {
  position: relative;
  z-index: 1;
  width: 42px;
  height: 42px;
}

.app-icon > i {
  position: relative;
  z-index: 1;
  font-size: 42px;
  line-height: 1;
}

.icon-highlight {
  position: absolute;
  top: 0;
  right: 8px;
  left: 8px;
  height: 27px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, .28), transparent);
  pointer-events: none;
}

.app-icon em {
  position: absolute;
  top: -7px;
  right: -7px;
  z-index: 2;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border: 3px solid $canvas;
  border-radius: 12px;
  color: #fff;
  background: #ed3d45;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  line-height: 18px;
}

.app-tile > strong {
  display: -webkit-box;
  margin-top: 13px;
  overflow: hidden;
  font-size: 15px;
  line-height: 1.35;
  text-align: center;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.app-tile > small {
  display: none;
}

.quick-nav-remove {
  position: absolute;
  top: -2px;
  left: calc(50% - 46px);
  z-index: 3;
  display: grid;
  width: 24px;
  height: 24px;
  padding: 0;
  place-items: center;
  border: 2px solid #fff;
  border-radius: 50%;
  color: #fff;
  background: #ed3d45;
  box-shadow: 0 2px 8px rgba(237, 61, 69, .28);
  font-size: 12px;
  cursor: pointer;
}

.quick-nav-lock {
  position: absolute;
  top: -2px;
  left: calc(50% - 46px);
  z-index: 3;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border: 2px solid #fff;
  border-radius: 50%;
  color: #fff;
  background: #7a8ea3;
  box-shadow: 0 2px 8px rgba(84, 112, 143, .24);
  font-size: 12px;
}

.app-tile--edit.is-locked > strong {
  color: #4f6478;
}

.app-tile-ghost {
  opacity: .45;
}

.app-tile-chosen .app-icon {
  transform: scale(1.05);
}

.page-indicator {
  display: flex;
  flex: 0 0 auto;
  min-height: 20px;
  margin-top: 16px;
  align-items: center;
  justify-content: center;
}
.page-indicator > * + * {
  margin-left: 7px;
}

.page-indicator button {
  width: 9px;
  height: 9px;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: #9eb1c5;
  cursor: pointer;
  transition: width .2s ease, background .2s ease;
}

.page-indicator button.active {
  width: 34px;
  background: $primary;
}

.app-page-next-enter-active,
.app-page-next-leave-active,
.app-page-prev-enter-active,
.app-page-prev-leave-active {
  transition: transform .24s cubic-bezier(.16, 1, .3, 1), opacity .18s ease;
}

.app-page-next-enter {
  opacity: 0;
  transform: translateX(34px);
}

.app-page-next-leave-to {
  opacity: 0;
  transform: translateX(-34px);
}

.app-page-prev-enter {
  opacity: 0;
  transform: translateX(-34px);
}

.app-page-prev-leave-to {
  opacity: 0;
  transform: translateX(34px);
}

@keyframes quick-nav-wiggle {
  0% { transform: rotate(-1.4deg); }
  100% { transform: rotate(1.4deg); }
}

</style>
