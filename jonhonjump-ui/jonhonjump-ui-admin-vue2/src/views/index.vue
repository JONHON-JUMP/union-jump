<template>
  <div class="jump-portal jump-portal--embedded">
    <main class="portal-main" @click="searchFocused = false">
      <section class="desktop-area">
        <div class="welcome-row">
          <div>
            <p class="date-label">{{ dateLabel }}</p>
            <h1>{{ greeting }}，{{ displayName }}</h1>
          </div>
        </div>

        <portal-quick-nav-panel
          ref="quickNavPanel"
          variant="home"
          @open="openApp"
          @quick-nav-change="handleQuickNavChange"
        />
      </section>

      <aside class="info-rail">
        <section ref="workbenchPanel" class="info-panel workbench-panel">
          <div class="panel-heading">
            <div>
              <h2>工作台</h2>
            </div>
            <time>{{ currentTime }}</time>
          </div>
          <div class="workbench-tabs-row">
            <div class="workbench-tabs" role="tablist">
              <button
                v-for="tab in workbenchTabs"
                :key="tab.key"
                type="button"
                :class="{ active: activeWorkbench === tab.key }"
                @click="switchWorkbench(tab.key)"
              >{{ tab.label }}<span v-if="tab.count"> {{ tab.count }}</span></button>
            </div>
            <div class="workbench-tabs-actions">
              <button
                type="button"
                class="workbench-action"
                aria-label="刷新"
                :disabled="workbenchLoading"
                @click="refreshWorkbench"
              >
                <i class="el-icon-refresh" :class="{ 'is-spinning': workbenchLoading }" />
              </button>
              <button type="button" class="workbench-action workbench-action--more" @click="openWorkbenchMore">
                更多
              </button>
            </div>
          </div>
          <div v-loading="workbenchLoading" class="task-list">
            <button
              v-for="task in visibleTasks"
              :key="getTaskKey(task)"
              type="button"
              class="task-item"
              @click="showTask(task)"
            >
              <span><strong>{{ task.title }}</strong><small>{{ task.description }}</small></span>
              <em :class="task.level">{{ task.tag }}</em>
            </button>
            <div v-if="!workbenchLoading && !visibleTasks.length" class="task-empty">
              {{ workbenchEmptyText }}
            </div>
          </div>
          <button
            v-if="hasMoreTasks"
            class="workbench-more"
            type="button"
            @click="openWorkbenchMore"
          >
            查看更多
            <span>还有 {{ remainingTaskCount }} 条</span>
            <i class="el-icon-arrow-right" />
          </button>
        </section>
      </aside>
    </main>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getPath, parseTime } from '@/utils/ruoyi'
import { isExternal } from '@/utils/validate'
import { getTodoTaskPage } from '@/api/bpm/task'
import { listNoticeWorkbench } from '@/api/system/notice'
import { listFaqWorkbench } from '@/api/system/faq'
import { checkPermi } from '@/utils/permission'
import { parsePortalClientId, isMainBusinessPath } from '@/utils/portalRoute'
import PortalQuickNavPanel from './components/PortalQuickNavPanel.vue'
import { buildPortalHomeApps } from '@/utils/portalQuickNavApps'
import { buildIconStyle, resolveMenuColors } from '@/utils/menuIconStyle'
import { ensureDictDatas } from '@/utils/dict'
import { resolvePortalMenuIcon } from '@/utils/portalMenuIcon'

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
  name: 'JumpPortalHome',
  components: { PortalQuickNavPanel },
  data() {
    return {
      searchKeyword: '',
      searchFocused: false,
      activeWorkbench: 'notice',
      now: new Date(),
      timer: null,
      quickNavMenuIds: [],
      quickNavLockedMenuIds: [],
      quickNavConfigured: false,
      workbenchLoading: false,
      workbenchVisible: false,
      workbenchLoaded: {
        notice: false,
        todo: false,
        qa: false
      },
      workbenchInFlight: {
        notice: null,
        todo: null,
        qa: null
      },
      workbenchObserver: null,
      workbenchDisplayLimit: 5,
      noticeRecentTotal: 0,
      qaRecentTotal: 0,
      noticeLoadFailed: false,
      todoCount: 0,
      todoRefreshTimer: null,
      workbenchTabs: [
        { key: 'notice', label: '通知' },
        { key: 'todo', label: '待办', count: 0 },
        { key: 'qa', label: '常见 QA' }
      ],
      taskMap: {
        todo: [],
        notice: [],
        qa: []
      }
    }
  },
  computed: {
    ...mapGetters([
      'avatar', 'nickname', 'name', 'sidebarRouters', 'currentSystemSidebarRouters', 'currentSystemLabel',
      'currentSystem', 'portalSystemList'
    ]),
    currentSubSystemId() {
      if (this.currentSystem === 'main') {
        return 0
      }
      const sys = (this.portalSystemList || []).find(item => item.clientId === this.currentSystem)
      return sys ? Number(sys.subSystemId) : 0
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
    authorizedApps() {
      return this.flattenRoutes(this.currentSystemSidebarRouters || [])
    },
    homeApps() {
      return buildPortalHomeApps(
        this.currentSystemSidebarRouters,
        this.currentSystem,
        this.quickNavMenuIds,
        this.quickNavConfigured
      )
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
    remainingTaskCount() {
      if (this.activeWorkbench === 'notice') {
        return Math.max(0, this.noticeRecentTotal - this.workbenchDisplayLimit)
      }
      if (this.activeWorkbench === 'qa') {
        return Math.max(0, this.qaRecentTotal - this.workbenchDisplayLimit)
      }
      return Math.max(0, this.currentTasks.length - this.workbenchDisplayLimit)
    },
    hasMoreTasks() {
      return this.remainingTaskCount > 0
    },
    todoBadgeValue() {
      return this.todoCount > 0 ? this.todoCount : ''
    },
    workbenchEmptyText() {
      if (this.activeWorkbench === 'notice' && this.noticeLoadFailed) {
        return '通知加载失败，请点击刷新重试'
      }
      const labels = {
        notice: '暂无通知公告',
        todo: '暂无待办任务',
        qa: '暂无常见问题'
      }
      return labels[this.activeWorkbench] || '暂无数据'
    }
  },
  watch: {
    currentSystem() {
      this.searchKeyword = ''
      this.searchFocused = false
    },
    activeWorkbench(tabKey) {
      if (this.workbenchVisible) {
        this.ensureWorkbenchTabLoaded(tabKey)
      }
    },
    '$route'(to) {
      if (to.path !== '/index' && to.path !== '/') {
        return
      }
      this.applyWorkbenchFromQuery(to.query && to.query.workbench)
      this.loadTodoCount()
      if (this.workbenchVisible) {
        this.refreshWorkbench()
      }
    }
  },
  mounted() {
    this.applyWorkbenchFromQuery(this.$route.query.workbench)
    this._onOpenWorkbench = (tabKey) => {
      this.openWorkbenchTab(tabKey)
    }
    this.$root.$on('portal-open-workbench', this._onOpenWorkbench)
    this.timer = window.setInterval(() => { this.now = new Date() }, 30000)
    this.setupWorkbenchObserver()
    this.loadTodoCount()
    this.todoRefreshTimer = window.setInterval(() => {
      this.loadTodoCount()
    }, 60000)
  },
  activated() {
    this.applyWorkbenchFromQuery(this.$route.query.workbench)
  },
  beforeDestroy() {
    if (this._onOpenWorkbench) {
      this.$root.$off('portal-open-workbench', this._onOpenWorkbench)
      this._onOpenWorkbench = null
    }
    window.clearInterval(this.timer)
    if (this.todoRefreshTimer) {
      window.clearInterval(this.todoRefreshTimer)
      this.todoRefreshTimer = null
    }
    if (this.workbenchObserver) {
      this.workbenchObserver.disconnect()
      this.workbenchObserver = null
    }
  },
  methods: {
    iconStyle(app) {
      return buildIconStyle(app)
    },
    applyWorkbenchFromQuery(tabKey) {
      if (tabKey && Object.prototype.hasOwnProperty.call(this.taskMap, tabKey)) {
        this.openWorkbenchTab(tabKey)
      }
    },
    openWorkbenchTab(tabKey) {
      if (!tabKey || !Object.prototype.hasOwnProperty.call(this.taskMap, tabKey)) {
        return
      }
      this.switchWorkbench(tabKey)
    },
    setupWorkbenchObserver() {
      if (typeof IntersectionObserver === 'undefined') {
        this.workbenchVisible = true
        this.ensureWorkbenchTabLoaded(this.activeWorkbench)
        return
      }
      this.$nextTick(() => {
        const panel = this.$refs.workbenchPanel
        if (!panel) {
          return
        }
        this.workbenchObserver = new IntersectionObserver(entries => {
          const visible = entries.some(entry => entry.isIntersecting)
          if (!visible || this.workbenchVisible) {
            return
          }
          this.workbenchVisible = true
          this.ensureWorkbenchTabLoaded(this.activeWorkbench)
        }, { threshold: 0.1 })
        this.workbenchObserver.observe(panel)
      })
    },
    switchWorkbench(tabKey) {
      this.activeWorkbench = tabKey
      if (!this.workbenchVisible) {
        this.workbenchVisible = true
      }
      this.ensureWorkbenchTabLoaded(tabKey)
    },
    ensureWorkbenchTabLoaded(tabKey) {
      if (!tabKey || this.workbenchLoaded[tabKey]) {
        return Promise.resolve()
      }
      if (this.workbenchInFlight[tabKey]) {
        return this.workbenchInFlight[tabKey]
      }
      const task = this.loadWorkbenchTab(tabKey).finally(() => {
        this.workbenchInFlight[tabKey] = null
      })
      this.workbenchInFlight[tabKey] = task
      return task
    },
    loadWorkbenchTab(tabKey) {
      if (tabKey === 'todo') {
        return this.loadTodoTasks().then(() => {
          this.workbenchLoaded.todo = true
        })
      }
      if (tabKey === 'notice') {
        return ensureDictDatas(this.DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE).then(() => this.loadNoticeTasks()).then(() => {
          this.workbenchLoaded.notice = true
        }).catch(() => {
          // 失败也标记，避免超时后无限重试把控制台刷爆
          this.workbenchLoaded.notice = true
        })
      }
      if (tabKey === 'qa') {
        return ensureDictDatas(this.DICT_TYPE.SYSTEM_FAQ_CATEGORY).then(() => this.loadFaqTasks()).then(() => {
          this.workbenchLoaded.qa = true
        }).catch(() => {
          this.workbenchLoaded.qa = true
        })
      }
      return Promise.resolve()
    },
    refreshWorkbench() {
      this.workbenchLoaded[this.activeWorkbench] = false
      this.workbenchInFlight[this.activeWorkbench] = null
      return this.loadWorkbenchTab(this.activeWorkbench)
    },
    loadTodoCount() {
      if (!checkPermi(['bpm:task:query'])) {
        this.todoCount = 0
        this.workbenchTabs = this.workbenchTabs.map(tab => tab.key === 'todo'
          ? { ...tab, count: 0 }
          : tab)
        return Promise.resolve()
      }
      return getTodoTaskPage({ pageNo: 1, pageSize: 1 }, true).then(response => {
        this.todoCount = (response.data && response.data.total) || 0
        this.workbenchTabs = this.workbenchTabs.map(tab => tab.key === 'todo'
          ? { ...tab, count: this.todoCount }
          : tab)
      }).catch(() => {
        this.todoCount = 0
      })
    },
    loadTodoTasks() {
      if (!checkPermi(['bpm:task:query'])) {
        this.todoCount = 0
        this.workbenchTabs = this.workbenchTabs.map(tab => tab.key === 'todo'
          ? { ...tab, count: undefined }
          : tab)
        this.taskMap.todo = []
        return Promise.resolve()
      }
      this.workbenchLoading = true
      return getTodoTaskPage({ pageNo: 1, pageSize: 8 }, true).then(response => {
        const list = response.data.list || []
        this.todoCount = (response.data && response.data.total) || 0
        this.workbenchTabs = this.workbenchTabs.map(tab => tab.key === 'todo'
          ? { ...tab, count: this.todoCount }
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
      }).finally(() => {
        this.workbenchLoading = false
      })
    },
    loadNoticeTasks() {
      this.workbenchLoading = true
      return listNoticeWorkbench({
        pageNo: 1,
        pageSize: this.workbenchDisplayLimit
      }).then(response => {
        const page = (response && response.data) || {}
        const list = page.list || []
        const total = Number(page.total) || 0
        this.noticeRecentTotal = total
        this.noticeLoadFailed = false
        this.workbenchTabs = this.workbenchTabs.map(tab => tab.key === 'notice'
          ? { ...tab, count: total || undefined }
          : tab)
        this.taskMap.notice = list.map(row => ({
          title: row.title || '-',
          description: this.getDictDataLabel(this.DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE, row.type) || '-',
          tag: parseTime(row.createTime, '{y}-{m}-{d} {h}:{i}') || '-',
          level: 'info',
          raw: row
        }))
      }).catch(err => {
        console.error('[workbench] load notice failed', err)
        this.noticeLoadFailed = true
        this.noticeRecentTotal = 0
        this.taskMap.notice = []
        this.workbenchTabs = this.workbenchTabs.map(tab => tab.key === 'notice'
          ? { ...tab, count: undefined }
          : tab)
      }).finally(() => {
        this.workbenchLoading = false
      })
    },
    loadFaqTasks() {
      this.workbenchLoading = true
      return listFaqWorkbench({
        pageNo: 1,
        pageSize: this.workbenchDisplayLimit
      }).then(response => {
        const list = response.data.list || []
        this.qaRecentTotal = response.data.total || 0
        this.taskMap.qa = list.map(row => ({
          title: row.title || '-',
          description: this.getDictDataLabel(this.DICT_TYPE.SYSTEM_FAQ_CATEGORY, row.category) || '-',
          tag: parseTime(row.createTime, '{y}-{m}-{d} {h}:{i}') || '-',
          level: 'info',
          raw: row
        }))
      }).catch(() => {
        this.qaRecentTotal = 0
        this.taskMap.qa = []
      }).finally(() => {
        this.workbenchLoading = false
      })
    },
    buildTodoTitle(row) {
      const user = row.processInstance && row.processInstance.startUserNickname
      const taskName = row.name || ''
      if (user && taskName) return `${user} - ${taskName}`
      return taskName || user || '-'
    },
    handleQuickNavChange(payload) {
      this.quickNavMenuIds = (payload && payload.menuIds) || []
      this.quickNavLockedMenuIds = (payload && payload.lockedMenuIds) || []
      this.quickNavConfigured = !!(payload && payload.configured)
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
            ...resolveMenuColors(route.meta || {}),
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
          this.$root.$emit('portal-open-all-apps', app.name)
        }
        return
      }
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
      const app = this.drawerApps.find(item => item.name.includes(keyword) || (item.group || '').includes(keyword))
      if (app) this.openApp(app)
      else {
        this.$root.$emit('portal-open-all-apps', keyword)
      }
    },
    closeSearch() {
      this.searchKeyword = ''
      this.searchFocused = false
    },
    showTask(task) {
      if (this.activeWorkbench === 'todo' && task.raw && task.raw.processInstance) {
        this.$router.push({ name: 'BpmProcessInstanceDetail', query: { id: task.raw.processInstance.id } }).catch(() => {})
        return
      }
      if (this.activeWorkbench === 'notice' && task.raw && task.raw.id) {
        this.$router.push({ name: 'MyNotifyMessageDetail', query: { noticeId: task.raw.id } }).catch(() => {})
        return
      }
      if (this.activeWorkbench === 'qa' && task.raw && task.raw.id) {
        this.$router.push({ name: 'MyFaqDetail', query: { faqId: task.raw.id } }).catch(() => {})
        return
      }
      this.$message.info(task.title)
    },
    getTaskKey(task) {
      if (task.raw && task.raw.id) {
        return `${this.activeWorkbench}-${task.raw.id}`
      }
      return `${this.activeWorkbench}-${task.title}`
    },
    openWorkbenchMore() {
      if (this.activeWorkbench === 'notice') {
        this.$router.push({ name: 'MyNotifyMessage' }).catch(() => {})
        return
      }
      if (this.activeWorkbench === 'qa') {
        this.$router.push({ name: 'MyFaq' }).catch(() => {})
        return
      }
      const targets = {
        todo: {
          paths: ['/bpm/task/todo'],
          keyword: '待办'
        }
      }
      const target = targets[this.activeWorkbench]
      if (!target) return

      const app = this.authorizedApps.find(item => target.paths.includes(item.path))
      if (app) {
        this.openApp(app)
        return
      }

      this.$root.$emit('portal-open-all-apps', target.keyword)
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
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 0;
  box-sizing: border-box;
  flex-direction: column;
  color: $ink;
  background: transparent;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
}

.jump-portal--embedded {
  flex: 1 1 auto;
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

.brand-copy strong {
  overflow: hidden;
  font-size: 22px;
  line-height: 1.35;
  white-space: nowrap;
  text-overflow: ellipsis;
}

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

.portal-main { display: grid; min-height: 0; margin-top: 0; grid-template-columns: minmax(0, 1fr) 460px; gap: 24px; flex: 1 1 auto; }
.desktop-area { position: relative; display: flex; min-height: 0; padding: 26px 22px; flex-direction: column; }
.welcome-row { display: flex; align-items: flex-start; justify-content: space-between; }
.date-label { margin: 0 0 7px; color: #3275aa; font-size: 14px; font-weight: 600; }
.welcome-row h1 { margin: 0; color: #0e223c; font-size: 36px; line-height: 1.25; letter-spacing: -.02em; }

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
.workbench-tabs-row {
  display: flex;
  margin-top: 20px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.workbench-tabs { display: flex; min-width: 0; gap: 7px; flex: 1 1 auto; }
.workbench-tabs-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}
.workbench-action {
  display: grid;
  width: 32px;
  height: 32px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 8px;
  color: #5a718e;
  background: rgba(255, 255, 255, .55);
  font-size: 16px;
  cursor: pointer;
  transition: background .18s ease, color .18s ease;
}
.workbench-action:hover,
.workbench-action:focus-visible {
  outline: none;
  color: #087ce5;
  background: rgba(255, 255, 255, .92);
}
.workbench-action:disabled {
  opacity: .6;
  cursor: not-allowed;
}
.workbench-action--more {
  width: auto;
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}
.workbench-action .is-spinning {
  animation: workbench-spin .8s linear infinite;
}
@keyframes workbench-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.workbench-tabs button { height: 38px; padding: 0 14px; border: 1px solid rgba(255, 255, 255, .76); border-radius: 19px; color: #4f6680; background: rgba(231, 242, 251, .72); font-size: 14px; font-weight: 600; white-space: nowrap; cursor: pointer; transition: background .18s ease, color .18s ease, transform .18s ease; }
.workbench-tabs button:hover { background: rgba(255, 255, 255, .9); }
.workbench-tabs button.active { color: #fff; background: $primary; }
.workbench-tabs button:active { transform: scale(.98); }
.workbench-tabs button:focus-visible { outline: 3px solid rgba(8, 124, 229, .2); outline-offset: 2px; }
.task-list { display: flex; min-height: 0; margin-top: 16px; overflow: auto; gap: 10px; flex: 1 1 auto; flex-direction: column; scrollbar-color: #aac0d3 transparent; scrollbar-width: thin; }
.task-empty {
  display: flex;
  min-height: 120px;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  border: 1px dashed rgba(8, 124, 229, .18);
  border-radius: 12px;
  color: #6a8199;
  background: rgba(255, 255, 255, .42);
  font-size: 13px;
}
.task-item { display: flex; width: 100%; padding: 16px 14px; align-items: flex-start; justify-content: space-between; border: 1px solid rgba(255, 255, 255, .78); border-radius: 12px; background: linear-gradient(135deg, rgba(255, 255, 255, .76), rgba(222, 240, 253, .66)); text-align: left; cursor: pointer; transition: background .18s ease, transform .18s ease; }
.task-item:hover, .task-item:focus-visible { outline: none; background: rgba(255, 255, 255, .94); transform: translateY(-1px); }
.task-item:active { transform: translateY(0) scale(.995); }
.task-item > span { display: flex; min-width: 0; padding-right: 8px; flex-direction: column; }
.task-item strong { font-size: 14px; }
.task-item small { margin-top: 7px; color: #5a718e; font-size: 12px; line-height: 1.5; }
.task-item em { flex: 0 0 auto; padding: 4px 8px; border-radius: 12px; color: #116bb6; background: #cce7fb; font-size: 11px; font-style: normal; font-weight: 700; white-space: nowrap; }
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

.mini-icon { display: grid; flex: 0 0 42px; width: 42px; height: 42px; place-items: center; border-radius: 12px; }
.mini-icon .svg-icon { width: 22px; height: 22px; }
.mini-icon > i { font-size: 22px; line-height: 1; }
@media (max-width: 1280px) {
  .portal-header { min-height: 76px; }
  .brand-mark { flex-basis: 52px; width: 52px; height: 52px; }
  .brand-copy strong { font-size: 19px; }
  .brand-copy small { display: none; }
  .app-search { width: 270px; }
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
  .workbench-tabs-row { align-items: flex-start; flex-wrap: wrap; }
  .workbench-tabs { overflow-x: auto; padding-bottom: 4px; }
}

@media (max-width: 560px) {
  .notice-badge, .user-entry { display: none; }
  .header-actions { flex-wrap: wrap; }
  .app-search { flex-basis: 100%; }
  .brand-mark { flex-basis: 46px; width: 46px; height: 46px; border-radius: 13px; }
  .desktop-area { padding-right: 4px; padding-left: 4px; }
  .workbench-panel { min-height: 520px; padding: 18px 14px; }
  .task-list { min-height: 330px; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { transition-duration: .01ms !important; animation-duration: .01ms !important; animation-iteration-count: 1 !important; }
}
</style>
