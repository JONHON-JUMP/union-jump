<template>
  <nav
    :class="{
      collapsed: collapsible && !expanded,
      'has-tabs': businessTabs.length
    }"
    class="portal-taskbar"
    :aria-label="expanded ? '门户应用任务栏' : '展开门户应用任务栏'"
    @click.stop
  >
    <button
      v-if="collapsible && !expanded"
      class="taskbar-indicator"
      type="button"
      aria-label="展开底部导航栏"
      @click="$emit('expand')"
    >
      <span />
    </button>

    <div class="taskbar-content" :aria-hidden="collapsible && !expanded">
      <button
        class="fixed-entry home-entry"
        :class="{ active: isHome }"
        type="button"
        :tabindex="tabIndex"
        @click="goHome"
      >
        <i class="el-icon-house" />
        <span>首页</span>
      </button>

      <div v-if="businessTabs.length" ref="tabScroller" class="business-tabs" aria-label="已打开应用">
        <button
          v-for="tab in businessTabs"
          :key="tab.path"
          :class="{ active: isActive(tab) }"
          class="business-tab"
          type="button"
          :tabindex="tabIndex"
          :title="tab.title"
          @click="activateTab(tab)"
        >
          <svg-icon :icon-class="tabIcon(tab)" />
          <span>{{ tab.title }}</span>
          <i
            class="el-icon-close close-tab"
            role="button"
            :aria-label="'关闭' + tab.title"
            @click.stop="closeTab(tab)"
          />
        </button>
      </div>

      <div class="fixed-actions">
        <span v-if="businessTabs.length" class="taskbar-divider" />
        <button class="all-apps" type="button" :tabindex="tabIndex" @click="$emit('all-apps')">
          <i class="el-icon-menu" />
          <span>全部应用</span>
        </button>
      </div>
    </div>
  </nav>
</template>

<script>
import {
  resolvePortalFrameRoute,
  isPortalSubSystemHomePath,
  resolvePortalMenuTitle,
  lookupPathLinkEntry
} from '@/utils/portalRoute'
import { syncPortalIframeView } from '@/utils/portalIframe'

export default {
  name: 'PortalDock',
  props: {
    expanded: {
      type: Boolean,
      default: true
    },
    collapsible: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    visitedViews() {
      return this.$store.state.tagsView.visitedViews
    },
    recentViewPaths() {
      return this.$store.state.tagsView.recentViewPaths
    },
    businessTabs() {
      const map = this.$store.state.portal.pathLinkMap
      const systems = this.$store.state.portal.systemList
      const currentSystem = this.$store.state.portal.currentSystem || 'main'
      return this.visitedViews
        .filter(view => {
          if (view.path === '/index' || view.path === '/') return false
          if (!view.name && !(view.meta && view.meta.link)) return false
          if (view.meta && view.meta.portalHome) return false
          if (isPortalSubSystemHomePath(view.path)) return false
          // 只展示当前系统页签：切走系统后不应再看到上一系统菜单
          // 例外：主系统菜单（工作台下的页面）在子系统模式下也能打开，应显示在 Dock
          const isPortal = /^\/portal\//.test(view.path || '')
          if (currentSystem === 'main') {
            if (isPortal) return false
          } else if (isPortal && !(
            view.path === `/portal/${currentSystem}`
            || String(view.path).startsWith(`/portal/${currentSystem}/`)
          )) {
            // 其它子系统的 portal 页签过滤掉；主系统路由（非 portal）放行
            return false
          }
          return true
        })
        .map(view => {
          // 只有门户壳才按 PortalFrame 还原标题，勿把主系统页签强行改写
          const isPortal = /^\/portal\//.test(view.path || '')
          const resolved = isPortal
            ? resolvePortalFrameRoute(
              { ...view, name: view.name || 'PortalFrame' },
              map,
              systems
            )
            : view
          const mapEntry = isPortal ? lookupPathLinkEntry(view.path, map) : null
          const title = resolvePortalMenuTitle(
            resolved.meta && resolved.meta.menuTitle,
            resolved.meta && resolved.meta.title,
            mapEntry && mapEntry.title,
            mapEntry && mapEntry.menuTitle,
            view.title,
            view.meta && view.meta.menuTitle,
            view.meta && view.meta.title
          ) || '业务页'
          if (!title || title === 'no-name') {
            return null
          }
          return {
            ...view,
            title,
            meta: {
              ...(view.meta || {}),
              ...((isPortal && resolved.meta) || {}),
              title,
              menuTitle: title
            }
          }
        })
        .filter(Boolean)
    },
    isHome() {
      return this.$route.path === '/index' || this.$route.path === '/'
    },
    tabIndex() {
      return this.collapsible && !this.expanded ? -1 : 0
    }
  },
  watch: {
    '$route.fullPath': {
      immediate: true,
      handler() {
        this.registerCurrentRoute()
        this.$nextTick(this.scrollActiveTabIntoView)
      }
    },
    expanded(isExpanded) {
      if (isExpanded) this.$nextTick(this.scrollActiveTabIntoView)
    }
  },
  methods: {
    registerCurrentRoute() {
      if (this.$store.state.portal.iframeSyncSuspended) return
      const route = this.$route
      if (!route.name || this.isHome) return
      if (isPortalSubSystemHomePath(route.path)) return
      syncPortalIframeView(this.$store, route)
      this.$store.dispatch('tagsView/touchVisitedView', route)
    },
    isActive(tab) {
      return tab.path === this.$route.path
    },
    tabIcon(tab) {
      return (tab.meta && tab.meta.icon) || 'component'
    },
    activateTab(tab) {
      if (!this.isActive(tab)) {
        this.$router.push(tab.fullPath || tab.path)
      }
      // 用完即收：iframe 页内点击不会冒泡回父页，展开的 dock 会一直挡住页面，
      // 点击标签切换后主动收起
      this.$emit('collapse')
    },
    closeTab(tab) {
      this.$store.dispatch('portal/closePortalTab', {
        tab,
        active: this.isActive(tab)
      }).catch(() => {})
    },
    goHome() {
      if (this.isHome) return
      this.$store.dispatch('portal/navigateToPortalHome').catch(err => {
        this.$message.error(typeof err === 'string' ? err : (err.message || '返回首页失败'))
      })
    },
    scrollActiveTabIntoView() {
      const scroller = this.$refs.tabScroller
      if (!scroller) return
      const activeTab = scroller.querySelector('.business-tab.active')
      if (activeTab && typeof activeTab.scrollIntoView === 'function') {
        activeTab.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #087ce5;

.portal-taskbar {
  position: fixed;
  bottom: 18px;
  left: 50%;
  z-index: 30;
  display: grid;
  width: max-content;
  max-width: calc(100% - 40px);
  height: 82px;
  padding: 8px 10px;
  box-sizing: border-box;
  place-items: center;
  border-radius: 18px;
  background: rgba(255, 255, 255, .97);
  box-shadow: 0 12px 28px rgba(36, 76, 111, .18);
  transform: translateX(-50%);
  transition:
    width .26s cubic-bezier(.16, 1, .3, 1),
    height .26s cubic-bezier(.16, 1, .3, 1),
    padding .26s cubic-bezier(.16, 1, .3, 1),
    border-radius .26s cubic-bezier(.16, 1, .3, 1),
    box-shadow .22s ease;
}

.portal-taskbar.collapsed {
  width: 152px;
  height: 24px;
  padding: 0;
  border-radius: 12px 12px 0 0;
  box-shadow: 0 5px 14px rgba(36, 76, 111, .16);
}

.portal-taskbar.collapsed.has-tabs {
  width: 152px;
}

.taskbar-content {
  display: flex;
  width: max-content;
  max-width: 100%;
  min-width: 0;
  height: 66px;
  align-items: center;
  opacity: 1;
  transition: opacity .14s ease .08s;
}
.taskbar-content > * + * {
  margin-left: 7px;
}

.portal-taskbar.collapsed .taskbar-content {
  position: absolute;
  opacity: 0;
  pointer-events: none;
  transition-delay: 0s;
}

.portal-taskbar button {
  border: 0;
  color: #536a83;
  background: transparent;
  font: inherit;
  cursor: pointer;
}

.fixed-entry,
.fixed-actions > button {
  display: flex;
  flex: 0 0 64px;
  width: 64px;
  height: 66px;
  padding: 0;
  align-items: center;
  justify-content: center;
  border-radius: 13px;
  flex-direction: column;
}

.fixed-entry:hover,
.fixed-actions > button:hover {
  outline: none;
  background: #edf5fc;
}
.fixed-entry:focus,
.fixed-actions > button:focus {
  outline: none;
  background: #edf5fc;
}

.fixed-entry.active {
  color: #fff;
  background: $primary;
}

.fixed-entry i,
.fixed-actions i {
  font-size: 23px;
}

.fixed-entry span,
.fixed-actions span {
  margin-top: 4px;
  font-size: 11px;
}

.business-tabs {
  display: flex;
  width: max-content;
  max-width: min(720px, calc(100vw - 220px));
  min-width: 0;
  height: 66px;
  padding: 0;
  overflow-x: auto;
  overflow-y: hidden;
  align-items: center;
  flex: 0 1 auto;
  scrollbar-color: #abc0d3 transparent;
  scrollbar-width: thin;
}
.business-tabs > * + * {
  margin-left: 7px;
}

.business-tabs::-webkit-scrollbar { height: 4px; }
.business-tabs::-webkit-scrollbar-thumb { border-radius: 2px; background: #abc0d3; }

.business-tab {
  position: relative;
  display: flex;
  flex: 0 0 72px;
  width: 72px;
  height: 66px;
  padding: 0 4px;
  align-items: center;
  justify-content: center;
  border-radius: 13px;
  background: transparent !important;
  flex-direction: column;
  transition: color .18s ease, background .18s ease, transform .18s ease;
}

.business-tab:hover {
  outline: none;
  color: #075eb5;
  background: #edf5fc !important;
}
.business-tab:focus {
  outline: none;
  color: #075eb5;
  background: #edf5fc !important;
}

.business-tab:active { transform: scale(.98); }

.business-tab.active {
  color: #fff;
  background: $primary !important;
}

.business-tab .svg-icon {
  flex: 0 0 23px;
  width: 23px;
  height: 23px;
}

.business-tab > span {
  width: 100%;
  margin: 5px 0 0;
  overflow: hidden;
  font-size: 11px;
  font-weight: 500;
  line-height: 15px;
  text-align: center;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.close-tab {
  position: absolute;
  top: 3px;
  right: 3px;
  display: grid;
  width: 18px;
  height: 18px;
  place-items: center;
  border-radius: 50%;
  color: #61778e;
  background: #dce8f2;
  font-size: 10px;
  opacity: 0;
  transition: opacity .16s ease, color .16s ease, background .16s ease;
}

.business-tab:hover .close-tab,
.business-tab.active .close-tab { opacity: 1; }
.business-tab:focus .close-tab { opacity: 1; }
.business-tab.active .close-tab { color: #075eb5; background: rgba(255, 255, 255, .88); }
.close-tab:hover {
  color: #fff;
  background: #e5484d;
}

.fixed-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
}
.fixed-actions > * + * {
  margin-left: 7px;
}

.fixed-actions .all-apps {
  color: #fff;
  background: #17263a;
}

.taskbar-divider {
  width: 1px;
  height: 42px;
  margin: 0 3px !important;
  background: #d9e4ed;
}

.taskbar-indicator {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  width: 100%;
  height: 100%;
  padding: 0;
  place-items: center;
  border-radius: inherit;
}

.taskbar-indicator:hover {
  outline: none;
  background: rgba(255, 255, 255, .72);
}
.taskbar-indicator:focus {
  outline: none;
  background: rgba(255, 255, 255, .72);
}

.taskbar-indicator span {
  width: 92px;
  height: 5px;
  border-radius: 3px;
  background: #61758b;
  transition: width .18s ease, background .18s ease;
}

.taskbar-indicator:hover span {
  width: 104px;
  background: $primary;
}
.taskbar-indicator:focus span {
  width: 104px;
  background: $primary;
}

@media (max-width: 720px) {
  .portal-taskbar:not(.collapsed) {
    bottom: 10px;
    width: calc(100% - 20px);
  }

  .portal-taskbar.collapsed,
  .portal-taskbar.collapsed.has-tabs {
    bottom: 10px;
    width: 136px;
  }

  .fixed-entry,
  .fixed-actions > button {
    flex-basis: 56px;
    width: 56px;
  }

  .business-tabs {
    max-width: calc(100vw - 160px);
  }

  .business-tab {
    flex-basis: 64px;
    width: 64px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    transition-duration: .01ms !important;
  }
}
</style>
