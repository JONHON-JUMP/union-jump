<template>
  <div
    :class="{ 'dock-expanded': dockExpanded }"
    class="jump-portal-shell"
    @click="handleShellClick"
  >
    <header class="portal-header" @click.stop>
      <button class="brand" type="button" aria-label="返回门户首页" @click="goHome">
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
              <span class="mini-icon" :style="{ background: app.color }">
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

        <div class="system-chip">
          <span class="status-dot" />
          <span>当前子系统：</span>
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
                :class="{ 'is-current': currentSubsystem === system.value }"
              >
                <span class="system-option">
                  <i :class="system.icon" />
                  <span>
                    <strong>{{ system.label }}</strong>
                    <small>{{ system.description }}</small>
                  </span>
                  <i v-if="currentSubsystem === system.value" class="el-icon-check" />
                </span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>

        <el-badge :value="3" class="notice-badge">
          <button class="round-action" type="button" aria-label="通知" @click="goTodo">
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

    <main class="portal-workspace">
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
      :routes="permission_routes || []"
      @open="openApp"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getPath } from '@/utils/ruoyi'
import { isExternal } from '@/utils/validate'
import AllAppsDrawer from '@/views/components/AllAppsDrawer.vue'
import PortalDock from './PortalDock.vue'

const colors = {
  blue: 'linear-gradient(145deg, #2597f4, #086fd8)'
}

export default {
  name: 'PortalShell',
  components: { AllAppsDrawer, PortalDock },
  data() {
    return {
      searchKeyword: '',
      searchFocused: false,
      drawerVisible: false,
      dockExpanded: false,
      currentSubsystem: 'C5 立库 MES',
      subsystemOptions: [
        { value: 'C5 立库 MES', label: 'C5 立库 MES', description: '立体库制造执行', icon: 'el-icon-box' },
        { value: 'C6 新能源 MES', label: 'C6 新能源 MES', description: '新能源产线执行', icon: 'el-icon-cpu' },
        { value: 'A7 库房 WMS', label: 'A7 库房 WMS', description: '仓储与配送管理', icon: 'el-icon-house' },
        { value: '质量管理平台', label: '质量管理平台', description: '检验与质量处置', icon: 'el-icon-circle-check' },
        { value: '设备管理系统', label: '设备管理系统', description: '点检、维保与台账', icon: 'el-icon-setting' }
      ]
    }
  },
  computed: {
    ...mapGetters(['avatar', 'nickname', 'name', 'permission_routes']),
    displayName() {
      return this.nickname || this.name || '制造同仁'
    },
    userInitial() {
      return this.displayName.slice(0, 1)
    },
    authorizedApps() {
      return this.flattenRoutes(this.permission_routes || [])
    },
    filteredApps() {
      const keyword = this.searchKeyword.toLowerCase()
      if (!keyword) return this.authorizedApps.slice(0, 6)
      return this.authorizedApps.filter(app => `${app.name} ${app.group}`.toLowerCase().includes(keyword))
    },
    showSearchPanel() {
      return this.searchFocused && Boolean(this.searchKeyword)
    }
  },
  watch: {
    '$route.path'() {
      this.dockExpanded = false
    }
  },
  methods: {
    handleShellClick() {
      this.searchFocused = false
      if (this.dockExpanded) this.dockExpanded = false
    },
    expandDock() {
      this.dockExpanded = true
    },
    handleSubsystemChange(value) {
      if (value === this.currentSubsystem) return
      this.currentSubsystem = value
      this.$message.success(`已切换至 ${value}`)
    },
    flattenRoutes(routes, basePath = '', group = '') {
      const result = []
      routes.forEach(route => {
        if (!route || route.hidden || route.path === '*' || route.path === '/404') return
        const title = route.meta && route.meta.title
        const path = this.resolveRoutePath(basePath, route.path)
        if (title && path !== '/index' && route.redirect !== 'noRedirect') {
          result.push({
            name: title,
            group: group || '授权菜单',
            path,
            icon: (route.meta && route.meta.icon) || 'component',
            color: colors.blue
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
      if (isExternal(app.path)) {
        if (/^https?:/.test(app.path)) {
          const openedWindow = window.open(app.path, '_blank', 'noopener,noreferrer')
          if (openedWindow) openedWindow.opener = null
        } else {
          window.location.href = app.path
        }
      } else if (this.$route.path !== app.path) {
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
      if (this.$route.path !== '/index') this.$router.push('/index')
    },
    goTodo() {
      this.$router.push({ path: '/index', query: { workbench: 'todo' } })
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
          this.$store.dispatch('LogOut').then(() => {
            location.href = getPath('/index')
          })
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
.brand-copy strong { overflow: hidden; font-size: 22px; line-height: 1.35; white-space: nowrap; text-overflow: ellipsis; }
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
.search-panel__title small,
.search-result small { color: $muted; }
.search-result { display: flex; width: 100%; padding: 9px 8px; align-items: center; border: 0; border-radius: 10px; background: transparent; text-align: left; cursor: pointer; }
.search-result:hover,
.search-result:focus-visible { outline: none; background: #edf6fd; }
.search-result > span:nth-child(2) { display: flex; min-width: 0; margin-left: 10px; flex: 1; flex-direction: column; }
.search-result small { margin-top: 2px; font-size: 12px; }
.search-result > i { color: #7790aa; }
.search-empty { padding: 24px 12px; color: $muted; text-align: center; }
.mini-icon { display: grid; flex: 0 0 42px; width: 42px; height: 42px; place-items: center; border-radius: 12px; color: #fff; }
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
.workspace-content { min-height: 0; overflow: auto; flex: 1 1 auto; }

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

<style lang="scss">
.system-dropdown {
  min-width: 260px;
  padding: 8px;
  border: 0;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(41, 81, 117, .16);
}
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
