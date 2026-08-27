<template>
  <el-drawer
    :visible="visible"
    direction="btt"
    size="100%"
    :append-to-body="true"
    :with-header="false"
    :close-on-press-escape="!activeFolder"
    custom-class="all-apps-drawer"
    @update:visible="handleDrawerVisibleUpdate"
    @close="handleDrawerClose"
  >
    <section
      class="all-apps-shell"
      aria-label="全部应用"
    >
      <header class="drawer-header">
        <div class="drawer-heading">
          <span class="drawer-heading__mark"><i class="el-icon-menu" /></span>
          <div>
            <h2>全部应用</h2>
          </div>
        </div>

        <div class="drawer-actions">
          <label class="menu-search">
            <i class="el-icon-search" />
            <input
              ref="searchInput"
              v-model.trim="keyword"
              type="search"
              aria-label="搜索应用"
              placeholder="搜索应用"
              @keydown.esc.stop="handleEscape"
            >
            <button
              v-if="keyword"
              type="button"
              aria-label="清空搜索"
              @click="keyword = ''"
            >
              <i class="el-icon-circle-close" />
            </button>
          </label>
          <button
            class="drawer-close"
            type="button"
            aria-label="关闭全部应用"
            @click="requestClose"
          >
            <i class="el-icon-close" />
          </button>
        </div>
      </header>

      <div
        v-if="menuGroups.length"
        class="drawer-layout"
      >
        <nav
          class="group-nav"
          aria-label="应用分类"
        >
          <button
            v-for="group in menuGroups"
            :key="group.key"
            type="button"
            :class="{ active: activeGroupKey === group.key && !keyword }"
            :aria-pressed="activeGroupKey === group.key && !keyword"
            @click="selectGroup(group)"
          >
            <span
              class="group-icon"
              :style="iconStyle(group)"
            >
              <svg-icon :icon-class="group.icon" />
            </span>
            <span>{{ group.name }}</span>
            <i class="el-icon-arrow-right group-arrow" />
          </button>
        </nav>

        <main class="apps-content">
          <div class="content-heading">
            <div>
              <h3>{{ contentTitle }}</h3>
            </div>
            <span>{{ displayedItems.length }} 个入口</span>
          </div>

          <div
            v-if="displayedItems.length"
            :class="keyword ? 'search-results' : 'apps-grid'"
          >
            <template v-if="keyword">
              <button
                v-for="item in displayedItems"
                :key="item.key"
                type="button"
                class="search-result"
                @click="openLeaf(item)"
                @mousedown="handleItemPressStart($event, item)"
                @touchstart.passive="handleItemPressStart($event, item)"
                @touchmove.passive="handleItemPressMove"
                @touchend="handleItemPressEnd"
                @touchcancel="handleItemPressEnd"
                @contextmenu.prevent
              >
                <span
                  class="search-result__icon"
                  :style="iconStyle(item)"
                >
                  <svg-icon :icon-class="item.icon" />
                </span>
                <span class="search-result__copy">
                  <strong>{{ item.name }}</strong>
                  <small>{{ item.subtitle || item.groupName || '授权应用' }}</small>
                </span>
                <i class="el-icon-arrow-right" />
              </button>
            </template>
            <template v-else>
              <div
                v-for="item in displayedItems"
                :key="item.key"
                class="app-tile-wrap"
                :class="{ 'is-folder': item.type === 'folder' }"
              >
              <button
                type="button"
                class="app-tile"
                @click="activateItem(item, $event)"
                @mousedown="handleLeafPressStart($event, item)"
                @touchstart.passive="handleLeafPressStart($event, item)"
                @touchmove.passive="handleItemPressMove"
                @touchend="handleItemPressEnd"
                @touchcancel="handleItemPressEnd"
                @contextmenu.prevent
              >
                <span
                  v-if="item.type === 'folder'"
                  class="app-icon folder-icon"
                  :style="[iconStyle(item), folderStyle(item)]"
                >
                  <span class="icon-highlight" />
                  <span class="folder-preview">
                    <span
                      v-for="child in item.children"
                      :key="'preview-' + child.key"
                      class="folder-preview__item"
                      :style="iconStyle(child)"
                    >
                      <svg-icon :icon-class="child.icon" />
                    </span>
                  </span>
                  <em>{{ item.children.length }}</em>
                </span>
                <span
                  v-else
                  class="app-icon"
                  :style="iconStyle(item)"
                >
                  <span class="icon-highlight" />
                  <svg-icon :icon-class="item.icon" />
                </span>
                <strong :title="item.name">{{ item.name }}</strong>
              </button>
              <button
                v-if="item.type !== 'folder' && item.menuId != null && !(isInQuickNav(item) && isQuickNavLocked(item))"
                class="pin-app"
                :class="{ pinned: isInQuickNav(item) }"
                type="button"
                :aria-label="isInQuickNav(item) ? '取消快捷导航：' + item.name : '加入快捷导航：' + item.name"
                :title="isInQuickNav(item) ? '取消快捷导航' : '加入快捷导航'"
                @click.stop="toggleQuickNav(item)"
              >
                <i :class="isInQuickNav(item) ? 'el-icon-star-on' : 'el-icon-star-off'" />
              </button>
            </div>
            </template>
          </div>

          <div
            v-else
            class="drawer-empty"
          >
            <span><i class="el-icon-search" /></span>
            <h3>没有找到匹配的应用</h3>
            <p>请尝试其他名称，搜索支持一级、二级和三级菜单。</p>
          </div>
        </main>
      </div>

      <div
        v-else-if="menusLoading"
        class="drawer-empty drawer-empty--menu drawer-empty--loading"
      >
        <span><i class="el-icon-loading" /></span>
        <h3>菜单加载中</h3>
        <p>全量菜单正在加载，请稍候…</p>
      </div>

      <div
        v-else
        class="drawer-empty drawer-empty--menu"
      >
        <span><i class="el-icon-menu" /></span>
        <h3>暂无可用应用</h3>
        <p>当前账号没有可展示的菜单，请联系管理员配置角色菜单权限。</p>
      </div>

      <transition name="folder-panel">
        <div
          v-if="activeFolder"
          class="folder-overlay"
          role="presentation"
          @mousedown.self="closeFolder"
        >
          <section
            ref="folderPanel"
            class="folder-panel"
            role="dialog"
            aria-modal="true"
            :aria-label="activeFolder.name"
            tabindex="-1"
          >
            <header class="folder-panel__header">
              <div>
                <span
                  class="folder-panel__icon"
                  :style="iconStyle(activeFolder)"
                >
                  <svg-icon :icon-class="activeFolder.icon" />
                </span>
                <div>
                  <h3>{{ activeFolder.name }}</h3>
                  <p>共 {{ activeFolder.children.length }} 个应用入口</p>
                </div>
              </div>
              <button
                ref="folderCloseButton"
                type="button"
                aria-label="关闭文件夹"
                @click="closeFolder"
              >
                <i class="el-icon-close" />
              </button>
            </header>

            <div class="folder-panel__grid">
              <div
                v-for="leaf in activeFolder.children"
                :key="'folder-' + leaf.key"
                class="folder-leaf-wrap"
              >
                <button
                  type="button"
                  class="folder-leaf"
                  @click="openLeaf(leaf)"
                  @mousedown="handleItemPressStart($event, leaf)"
                  @touchstart.passive="handleItemPressStart($event, leaf)"
                  @touchmove.passive="handleItemPressMove"
                  @touchend="handleItemPressEnd"
                  @touchcancel="handleItemPressEnd"
                  @contextmenu.prevent
                >
                  <span
                    class="folder-leaf__icon"
                    :style="iconStyle(leaf)"
                  >
                    <span class="icon-highlight" />
                    <svg-icon :icon-class="leaf.icon" />
                  </span>
                  <strong :title="leaf.name">{{ leaf.name }}</strong>
                </button>
                <button
                  v-if="leaf.menuId != null && !(isInQuickNav(leaf) && isQuickNavLocked(leaf))"
                  class="pin-app pin-app--folder"
                  :class="{ pinned: isInQuickNav(leaf) }"
                  type="button"
                  :aria-label="isInQuickNav(leaf) ? '取消快捷导航：' + leaf.name : '加入快捷导航：' + leaf.name"
                  :title="isInQuickNav(leaf) ? '取消快捷导航' : '加入快捷导航'"
                  @click.stop="toggleQuickNav(leaf)"
                >
                  <i :class="isInQuickNav(leaf) ? 'el-icon-star-on' : 'el-icon-star-off'" />
                </button>
              </div>
            </div>
          </section>
        </div>
      </transition>

      <quick-nav-context-menu
        :visible="contextMenu.visible"
        :menu-style="contextMenuStyle"
        :show-subscribe="contextMenuShowSubscribe"
        :show-unsubscribe="contextMenuShowUnsubscribe"
        :unsubscribe-disabled="contextMenu.item && isQuickNavLocked(contextMenu.item)"
        :manual-visible.sync="manualDialogVisible"
        :manual-title="manualDialogTitle"
        :manual-content="manualDialogContent"
        @close="closeContextMenu"
        @subscribe="handleContextSubscribe"
        @unsubscribe="handleContextUnsubscribe"
        @view-manual="handleContextViewManual"
        @manual-closed="manualDialogItem = null"
      />
    </section>
  </el-drawer>
</template>

<script>
const {
  normalizeMenuTree,
  searchMenus,
  folderPreviewStyle
} = require('./allAppsMenu')
import {
  isMenuInQuickNav,
  isQuickNavMenuLocked,
  toggleQuickNavMenu
} from '@/utils/portalQuickNavToggle'
import { buildIconStyle } from '@/utils/menuIconStyle'
import QuickNavContextMenu from '@/components/QuickNavContextMenu.vue'
import quickNavLongPressMixin from '@/mixins/quickNavLongPressMixin'

export default {
  name: 'AllAppsDrawer',
  components: { QuickNavContextMenu },
  mixins: [quickNavLongPressMixin],
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    routes: {
      type: Array,
      default: () => []
    },
    quickNavMenuIds: {
      type: Array,
      default: () => []
    },
    quickNavLockedMenuIds: {
      type: Array,
      default: () => []
    },
    quickNavConfigured: {
      type: Boolean,
      default: false
    },
    subSystemId: {
      type: Number,
      default: 0
    },
    systemKey: {
      type: String,
      default: 'main'
    },
    systemLabel: {
      type: String,
      default: ''
    },
    menusLoading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      keyword: '',
      activeGroupKey: '',
      activeFolder: null,
      folderTrigger: null,
      quickNavSaving: false,
      // 本地副本：保证星标随 props / 操作即时刷新（勿直接读 session 缓存，无响应式）
      localQuickNavMenuIds: [],
      localQuickNavLockedMenuIds: [],
      localQuickNavConfigured: false
    }
  },
  computed: {
    menuGroups() {
      return normalizeMenuTree(this.routes)
    },
    activeGroup() {
      return this.menuGroups.find(group => group.key === this.activeGroupKey) || this.menuGroups[0] || null
    },
    searchResults() {
      return searchMenus(this.menuGroups, this.keyword)
    },
    displayedItems() {
      return this.keyword ? this.searchResults : (this.activeGroup ? this.activeGroup.children : [])
    },
    contentTitle() {
      if (this.keyword) {
        const scope = this.systemLabel ? ` · ${this.systemLabel}` : ''
        return `“${this.keyword}”的搜索结果${scope}`
      }
      return this.activeGroup ? this.activeGroup.name : '全部应用'
    },
    contextMenuShowSubscribe() {
      const item = this.contextMenu.item
      return Boolean(item && !this.isInQuickNav(item))
    },
    contextMenuShowUnsubscribe() {
      const item = this.contextMenu.item
      return Boolean(item && this.isInQuickNav(item))
    }
  },
  watch: {
    quickNavMenuIds: {
      immediate: true,
      handler(ids) {
        // 保存中勿被过期 props 打断乐观/权威结果
        if (this.quickNavSaving) {
          return
        }
        this.localQuickNavMenuIds = [...(ids || [])]
      }
    },
    quickNavLockedMenuIds: {
      immediate: true,
      handler(ids) {
        if (this.quickNavSaving) {
          return
        }
        this.localQuickNavLockedMenuIds = [...(ids || [])]
      }
    },
    quickNavConfigured: {
      immediate: true,
      handler(val) {
        if (this.quickNavSaving) {
          return
        }
        this.localQuickNavConfigured = !!val
      }
    },
    systemKey() {
      this.keyword = ''
      this.closeFolder(false)
    },
    menuGroups: {
      immediate: true,
      handler(groups) {
        if (!groups.length) {
          this.activeGroupKey = ''
          if (this.activeFolder) this.closeFolder(false)
          return
        }
        if (this.activeFolder) {
          const currentFolder = this.findFolder(groups, this.activeFolder.key)
          if (!currentFolder) {
            this.closeFolder(false)
          } else {
            this.activeFolder = currentFolder
          }
        }
        if (!groups.some(group => group.key === this.activeGroupKey)) {
          this.activeGroupKey = groups[0].key
        }
      }
    },
    visible(isVisible) {
      if (isVisible) {
        this.resetTransientState(false)
        // 打开时对齐最新 props（首页可能已改过快捷导航）
        this.localQuickNavMenuIds = [...(this.quickNavMenuIds || [])]
        this.localQuickNavLockedMenuIds = [...(this.quickNavLockedMenuIds || [])]
        this.localQuickNavConfigured = !!this.quickNavConfigured
        document.addEventListener('keydown', this.handleDocumentKeydown)
      } else {
        this.closeContextMenu()
        this.resetTransientState(false)
        document.removeEventListener('keydown', this.handleDocumentKeydown)
      }
    }
  },
  mounted() {
    if (this.visible) document.addEventListener('keydown', this.handleDocumentKeydown)
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.handleDocumentKeydown)
  },
  methods: {
    openWithKeyword(keyword) {
      this.keyword = keyword || ''
      this.closeFolder(false)
      this.$nextTick(() => {
        const searchInput = this.$refs.searchInput
        if (searchInput && typeof searchInput.focus === 'function') searchInput.focus()
      })
    },
    isInQuickNav(item) {
      return isMenuInQuickNav(item.menuId, this.localQuickNavMenuIds)
    },
    isQuickNavLocked(item) {
      return isQuickNavMenuLocked(item.menuId, this.localQuickNavLockedMenuIds)
    },
    handleLeafPressStart(event, item) {
      if (!item || item.type === 'folder') {
        return
      }
      this.handleItemPressStart(event, item)
    },
    async handleContextSubscribe() {
      const item = this.contextMenu.item
      if (!item) {
        return
      }
      this.closeContextMenu()
      if (this.isInQuickNav(item)) {
        return
      }
      await this.toggleQuickNav(item)
    },
    async handleContextUnsubscribe() {
      const item = this.contextMenu.item
      if (!item) {
        return
      }
      if (this.isQuickNavLocked(item)) {
        this.$message.warning('该入口为角色默认快捷导航，不可取消订阅')
        return
      }
      this.closeContextMenu()
      if (!this.isInQuickNav(item)) {
        return
      }
      await this.toggleQuickNav(item)
    },
    async toggleQuickNav(item) {
      if (this.quickNavSaving || item.menuId == null) return
      const wasInQuickNav = this.isInQuickNav(item)
      if (wasInQuickNav && this.isQuickNavLocked(item)) {
        this.$message.warning('该入口为角色默认快捷导航，不可移除')
        return
      }
      this.quickNavSaving = true
      const prevMenuIds = [...this.localQuickNavMenuIds]
      const prevLocked = [...this.localQuickNavLockedMenuIds]
      const prevConfigured = this.localQuickNavConfigured
      try {
        const baseMenuIds = [...this.localQuickNavMenuIds]
        const configured = this.localQuickNavConfigured
        const lockedMenuIds = [...this.localQuickNavLockedMenuIds]
        // 先乐观更新星标，避免等接口才变
        const optimisticId = Number(item.menuId)
        if (wasInQuickNav) {
          this.localQuickNavMenuIds = baseMenuIds.filter(id => Number(id) !== optimisticId)
        } else {
          this.localQuickNavMenuIds = [...baseMenuIds, optimisticId]
        }
        // 以保存接口回写为准（含锁定合并），不再二次 GET，避免旧 list 盖掉新数据
        const config = await toggleQuickNavMenu(
          item.menuId,
          baseMenuIds,
          this.subSystemId,
          lockedMenuIds,
          configured,
          baseMenuIds
        )
        this.$store.dispatch('portal/applyQuickNavConfig', {
          subSystemId: this.subSystemId,
          config
        })
        const menuIds = (config && config.menuIds) || []
        const nextLocked = (config && config.lockedMenuIds) || lockedMenuIds
        const nextConfigured = !!(config && config.configured)
        const nextApps = (config && config.apps) || []
        this.localQuickNavMenuIds = [...menuIds]
        this.localQuickNavLockedMenuIds = [...nextLocked]
        this.localQuickNavConfigured = nextConfigured
        this.$emit('quick-nav-change', {
          menuIds,
          lockedMenuIds: [...nextLocked],
          configured: nextConfigured,
          apps: nextApps
        })
        const pinned = menuIds.some(id => Number(id) === Number(item.menuId))
        if (!wasInQuickNav && !pinned) {
          this.$message.warning(`“${item.name}”未能加入快捷导航，请刷新后重试`)
          return
        }
        if (wasInQuickNav && pinned) {
          this.$message.warning(`“${item.name}”为角色默认项，无法取消`)
          return
        }
        this.$message.success(wasInQuickNav ? `已取消“${item.name}”的快捷导航` : `已将“${item.name}”加入快捷导航`)
      } catch (error) {
        this.localQuickNavMenuIds = prevMenuIds
        this.localQuickNavLockedMenuIds = prevLocked
        this.localQuickNavConfigured = prevConfigured
        if (error && error.message === 'ROLE_QUICK_NAV_LOCKED') {
          this.$message.warning('该入口为角色默认快捷导航，不可移除')
        } else {
          this.$message.error('快捷导航保存失败')
        }
      } finally {
        this.quickNavSaving = false
      }
    },
    selectGroup(group) {
      this.keyword = ''
      this.activeGroupKey = group.key
      this.closeFolder()
    },
    activateItem(item, event) {
      if (this.suppressNextItemClick || this.contextMenu.visible) {
        this.suppressNextItemClick = false
        return
      }
      if (item.type === 'folder') {
        this.folderTrigger = event && event.currentTarget
        this.activeFolder = item
        this.$nextTick(this.focusFolderPanel)
        return
      }
      this.openLeaf(item)
    },
    openLeaf(leaf) {
      if (this.suppressNextItemClick || this.contextMenu.visible) {
        this.suppressNextItemClick = false
        return
      }
      this.closeFolder(false)
      this.$emit('open', leaf)
      if (this.visible) this.$emit('update:visible', false)
    },
    requestClose() {
      this.closeFolder(false)
      if (this.visible) this.$emit('update:visible', false)
    },
    handleDrawerClose() {
      this.resetTransientState(false)
    },
    handleDrawerVisibleUpdate(isVisible) {
      if (!isVisible && this.visible) this.$emit('update:visible', false)
    },
    closeFolder(restoreFocus = true) {
      const trigger = this.folderTrigger
      this.activeFolder = null
      this.folderTrigger = null
      if (restoreFocus && trigger && document.contains(trigger)) {
        this.$nextTick(() => trigger.focus())
      }
    },
    resetTransientState(restoreFocus = false) {
      this.keyword = ''
      this.closeContextMenu()
      this.closeFolder(restoreFocus)
    },
    handleEscape() {
      if (this.contextMenu.visible) {
        this.closeContextMenu()
      } else if (this.activeFolder) {
        this.closeFolder()
      } else if (this.keyword) {
        this.keyword = ''
      } else {
        this.requestClose()
      }
    },
    handleDocumentKeydown(event) {
      if (event.key === 'Escape' || event.keyCode === 27) {
        event.stopPropagation()
        this.handleEscape()
      } else if ((event.key === 'Tab' || event.keyCode === 9) && this.activeFolder) {
        this.trapFolderFocus(event)
      }
    },
    focusFolderPanel() {
      const target = this.$refs.folderCloseButton || this.getFolderFocusableElements()[0] || this.$refs.folderPanel
      if (target && typeof target.focus === 'function') target.focus()
    },
    getFolderFocusableElements() {
      const panel = this.$refs.folderPanel
      if (!panel) return []
      return Array.from(panel.querySelectorAll(
        'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      ))
    },
    trapFolderFocus(event) {
      const panel = this.$refs.folderPanel
      const focusable = this.getFolderFocusableElements()
      if (!panel || !focusable.length) {
        event.preventDefault()
        if (panel) panel.focus()
        return
      }

      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      const activeElement = document.activeElement
      if (event.shiftKey && (activeElement === first || !panel.contains(activeElement))) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && (activeElement === last || !panel.contains(activeElement))) {
        event.preventDefault()
        first.focus()
      }
    },
    findFolder(groups, key) {
      for (let groupIndex = 0; groupIndex < groups.length; groupIndex += 1) {
        const children = groups[groupIndex].children || []
        const folder = children.find(item => item.type === 'folder' && item.key === key)
        if (folder) return folder
      }
      return null
    },
    iconStyle(item) {
      return buildIconStyle(item)
    },
    folderStyle(folder) {
      const preview = folderPreviewStyle(folder.children.length)
      const available = 66
      const columns = Math.max(1, preview.columns)
      const padding = Math.min(3, available / (columns * 4))
      const contentSize = available - padding * 2
      const gap = Math.min(3, contentSize / (columns * 4))
      const iconSize = Math.max(
        Number.EPSILON,
        (contentSize - gap * (columns - 1)) / columns
      )
      return {
        '--folder-columns': columns,
        '--folder-preview-size': `${available}px`,
        '--folder-preview-padding': `${padding}px`,
        '--folder-item-size': `${iconSize}px`,
        '--folder-gap': `${gap}px`
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

button,
input {
  font: inherit;
}

button {
  color: inherit;
}

.all-apps-shell {
  position: relative;
  display: flex;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  color: $ink;
  background:
    radial-gradient(circle at 3% 2%, rgba(67, 183, 239, .16), transparent 28%),
    radial-gradient(circle at 98% 18%, rgba(42, 195, 172, .11), transparent 25%),
    $canvas;
  flex-direction: column;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
}

.drawer-header {
  position: relative;
  z-index: 4;
  display: flex;
  min-height: 92px;
  padding: 18px 26px;
  box-sizing: border-box;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, .96);
  box-shadow: 0 6px 16px rgba(45, 91, 130, .08);
}

.drawer-heading,
.drawer-heading__mark,
.drawer-actions,
.menu-search {
  display: flex;
  align-items: center;
}

.drawer-heading__mark {
  width: 54px;
  height: 54px;
  justify-content: center;
  border-radius: 15px;
  color: #fff;
  background: linear-gradient(145deg, #0e88e9, #08a7bd);
  box-shadow: 0 8px 16px rgba(8, 124, 229, .22);
  font-size: 25px;
}

.drawer-heading > div {
  margin-left: 14px;
}

.drawer-heading h2,
.content-heading h3,
.folder-panel h3,
.drawer-empty h3 {
  margin: 0;
}

.drawer-heading h2 {
  font-size: 23px;
  line-height: 1.3;
}

.drawer-heading p,
.content-heading p,
.folder-panel p,
.drawer-empty p {
  margin: 4px 0 0;
  color: $muted;
  line-height: 1.5;
}

.drawer-heading p {
  font-size: 13px;
}

.drawer-actions {
  /* gap replaced by margin for Chrome < 80 compat */
}
.drawer-actions > * + * {
  margin-left: 10px;
}

.menu-search {
  width: clamp(280px, 31vw, 440px);
  height: 48px;
  padding: 0 14px;
  box-sizing: border-box;
  border-radius: 14px;
  background: #e8f2fc;
  transition: background .2s ease, box-shadow .2s ease;
}

.menu-search:focus-within {
  background: #fff;
  box-shadow: 0 0 0 3px rgba(8, 124, 229, .16);
}

.menu-search > i {
  color: #54708f;
  font-size: 18px;
}

.menu-search input {
  width: 100%;
  height: 100%;
  padding: 0 10px;
  border: 0;
  outline: 0;
  color: $ink;
  background: transparent;
  font-size: 15px;
}

.menu-search input::placeholder {
  color: #607590;
}

.menu-search button,
.drawer-close,
.folder-panel__header button {
  border: 0;
  cursor: pointer;
}

.menu-search button {
  padding: 6px;
  color: #607590;
  background: transparent;
}

.drawer-close,
.folder-panel__header button {
  display: grid;
  width: 48px;
  height: 48px;
  padding: 0;
  place-items: center;
  border-radius: 50%;
  color: #29435f;
  background: #eaf3fb;
  font-size: 20px;
  transition: background .18s ease, transform .18s ease;
}

.drawer-close:hover,
.folder-panel__header button:hover {
  outline: 3px solid rgba(8, 124, 229, .16);
  background: #dcecf9;
}
.drawer-close:focus,
.folder-panel__header button:focus {
  outline: 3px solid rgba(8, 124, 229, .16);
  background: #dcecf9;
}

.drawer-close:active,
.folder-panel__header button:active {
  transform: scale(.96);
}

.drawer-layout {
  display: grid;
  min-height: 0;
  padding: 20px 24px 24px;
  grid-template-columns: 230px minmax(0, 1fr);
  gap: 22px;
  flex: 1 1 auto;
}

.group-nav {
  min-height: 0;
  padding: 8px;
  overflow-y: auto;
  border-radius: 16px;
  background: rgba(255, 255, 255, .82);
  box-shadow: 0 8px 22px rgba(34, 112, 166, .08);
}

.group-nav button {
  display: flex;
  width: 100%;
  min-height: 58px;
  padding: 8px 10px;
  align-items: center;
  border: 0;
  border-radius: 12px;
  color: #4f6680;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: color .18s ease, background .18s ease, transform .18s ease;
}

.group-nav button + button {
  margin-top: 4px;
}

.group-nav button:hover {
  outline: none;
  color: #075eb5;
  background: #edf6fd;
}
.group-nav button:focus {
  outline: none;
  color: #075eb5;
  background: #edf6fd;
}

.group-nav button:focus {
  box-shadow: inset 0 0 0 3px rgba(8, 124, 229, .15);
}

.group-nav button:active {
  transform: scale(.985);
}

.group-nav button.active {
  color: #075eb5;
  background: #dfeffc;
  font-weight: 700;
}

.group-icon {
  display: grid;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 11px;
  color: #fff;
  box-shadow: none !important;
}

.group-icon .svg-icon {
  width: 20px;
  height: 20px;
}

.group-nav button > span:nth-child(2) {
  min-width: 0;
  margin-left: 10px;
  overflow: hidden;
  flex: 1;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.group-arrow {
  margin-left: 6px;
  color: #89a0b7;
  font-size: 12px;
}

.apps-content {
  min-width: 0;
  min-height: 0;
  padding: 22px;
  overflow-y: auto;
  border-radius: 16px;
  background: rgba(255, 255, 255, .72);
}

.content-heading {
  display: flex;
  min-height: 52px;
  align-items: flex-start;
  justify-content: space-between;
}

.content-heading h3 {
  font-size: 20px;
  line-height: 1.35;
}

.content-heading p {
  font-size: 13px;
}

.content-heading > span {
  flex: 0 0 auto;
  margin-left: 20px;
  padding: 7px 11px;
  border-radius: 14px;
  color: #075eb5;
  background: #dceefd;
  font-size: 12px;
  font-weight: 700;
}

.apps-grid {
  display: grid;
  margin-top: 22px;
  grid-template-columns: repeat(auto-fill, minmax(148px, 1fr));
  column-gap: clamp(12px, 2vw, 28px);
  row-gap: 34px;
}

.search-results {
  display: flex;
  margin-top: 18px;
  flex-direction: column;
}
.search-results > * + * {
  margin-top: 4px;
}

.search-result {
  display: flex;
  width: 100%;
  padding: 10px 12px;
  align-items: center;
  border: 0;
  border-radius: 12px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.search-result:hover {
  outline: none;
  background: #edf6fd;
}
.search-result:focus {
  outline: none;
  background: #edf6fd;
}

.search-result__icon {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font-size: 18px;
}

.search-result__copy {
  display: flex;
  min-width: 0;
  margin-left: 12px;
  flex: 1;
  flex-direction: column;
}

.search-result__copy strong {
  overflow: hidden;
  font-size: 15px;
  line-height: 1.35;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.search-result__copy small {
  margin-top: 2px;
  color: $muted;
  font-size: 12px;
}

.search-result > .el-icon-arrow-right {
  flex: 0 0 auto;
  margin-left: 8px;
  color: #7790aa;
}

.app-tile-wrap {
  position: relative;
  min-width: 0;
}

.app-tile {
  display: flex;
  width: 100%;
  min-width: 0;
  padding: 4px 0;
  align-items: center;
  border: 0;
  outline: 0;
  background: transparent;
  flex-direction: column;
  cursor: pointer;
}

.pin-app {
  position: absolute;
  top: -3px;
  left: calc(50% + 25px);
  z-index: 3;
  display: grid;
  width: 28px;
  height: 28px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 50%;
  color: #54708f;
  background: #fff;
  box-shadow: 0 4px 8px rgba(38, 78, 113, .18);
  cursor: pointer;
  transition: color .18s ease, background .18s ease, transform .18s ease;
}

.pin-app:hover {
  outline: 3px solid rgba(8, 124, 229, .16);
  color: #075eb5;
  transform: scale(1.06);
}
.pin-app:focus {
  outline: 3px solid rgba(8, 124, 229, .16);
  color: #075eb5;
  transform: scale(1.06);
}

.pin-app.pinned {
  color: #fff;
  background: $primary;
}

.app-icon {
  position: relative;
  display: grid;
  width: 92px;
  height: 92px;
  overflow: hidden;
  place-items: center;
  border-radius: 22px;
  font-size: 43px;
  transition: transform .2s cubic-bezier(.16, 1, .3, 1), filter .2s ease;
}

.app-icon > .svg-icon {
  position: relative;
  z-index: 1;
  width: 42px;
  height: 42px;
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

.app-tile:hover .app-icon {
  transform: translateY(-5px);
  filter: saturate(1.08);
}
.app-tile:focus .app-icon {
  transform: translateY(-5px);
  filter: saturate(1.08);
}

.app-tile:focus .app-icon {
  outline: 3px solid rgba(8, 124, 229, .24);
  outline-offset: 5px;
}

.app-tile:active .app-icon {
  transform: translateY(-1px) scale(.97);
}

.app-tile > strong {
  display: -webkit-box;
  max-width: 100%;
  margin-top: 13px;
  overflow: hidden;
  font-size: 15px;
  line-height: 1.35;
  text-align: center;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  white-space: normal;
}

.app-tile > small {
  margin-top: 3px;
  color: $muted;
  font-size: 12px;
}

.folder-icon {
  overflow: visible;
}

.folder-icon::before {
  position: absolute;
  top: -7px;
  left: 10px;
  width: 34px;
  height: 16px;
  border-radius: 8px 8px 0 0;
  background: inherit;
  content: "";
}

.folder-preview {
  position: relative;
  z-index: 1;
  display: grid;
  width: var(--folder-preview-size);
  height: var(--folder-preview-size);
  padding: var(--folder-preview-padding);
  overflow: hidden;
  box-sizing: border-box;
  align-content: center;
  justify-content: center;
  grid-template-columns: repeat(var(--folder-columns), var(--folder-item-size));
  gap: var(--folder-gap);
}

.folder-preview__item {
  display: grid;
  width: var(--folder-item-size);
  height: var(--folder-item-size);
  overflow: hidden;
  place-items: center;
  border-radius: max(3px, calc(var(--folder-item-size) * .28));
  color: #fff;
  box-shadow: none !important;
}

.folder-preview__item .svg-icon {
  width: 64%;
  height: 64%;
}

.folder-icon em {
  position: absolute;
  top: -8px;
  right: -8px;
  z-index: 2;
  min-width: 25px;
  height: 25px;
  padding: 0 6px;
  box-sizing: border-box;
  border: 3px solid $canvas;
  border-radius: 13px;
  color: #fff;
  background: #ed3d45;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  line-height: 19px;
}

.drawer-empty--loading > span {
  color: #3488cc;
  background: #eef6fc;
}

.drawer-empty--loading > span .el-icon-loading {
  animation: rotating 1.2s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.drawer-empty {
  display: flex;
  min-height: 280px;
  padding: 36px 20px;
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  color: $muted;
  flex-direction: column;
  text-align: center;
}

.drawer-empty--menu {
  flex: 1 1 auto;
}

.drawer-empty > span {
  display: grid;
  width: 64px;
  height: 64px;
  margin-bottom: 15px;
  place-items: center;
  border-radius: 18px;
  color: #3488cc;
  background: #dceefa;
  font-size: 28px;
}

.drawer-empty h3 {
  color: $ink;
  font-size: 18px;
}

.drawer-empty p {
  max-width: 460px;
  font-size: 13px;
}

.folder-overlay {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  padding: 28px;
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  background: rgba(21, 49, 76, .28);
  backdrop-filter: blur(12px) saturate(115%);
  -webkit-backdrop-filter: blur(12px) saturate(115%);
}

.folder-panel {
  display: flex;
  width: min(840px, 78vw);
  max-height: min(650px, calc(100% - 28px));
  padding: 22px;
  box-sizing: border-box;
  overflow: hidden;
  border-radius: 16px;
  background: rgba(247, 252, 255, .94);
  box-shadow: 0 20px 44px rgba(22, 56, 87, .24);
  flex-direction: column;
}

.folder-panel__header,
.folder-panel__header > div {
  display: flex;
  align-items: center;
}

.folder-panel__header {
  flex: 0 0 auto;
  justify-content: space-between;
}

.folder-panel__icon {
  display: grid;
  width: 52px;
  height: 52px;
  overflow: hidden;
  place-items: center;
  border-radius: 15px;
  color: #fff;
}

.folder-panel__icon .svg-icon {
  width: 26px;
  height: 26px;
}

.folder-panel__header > div > div {
  margin-left: 12px;
}

.folder-panel h3 {
  font-size: 20px;
}

.folder-panel p {
  font-size: 12px;
}

.folder-panel__header button {
  flex: 0 0 auto;
  margin-left: 18px;
}

.folder-panel__grid {
  display: grid;
  min-height: 0;
  margin-top: 22px;
  padding: 6px;
  overflow-y: auto;
  grid-template-columns: repeat(auto-fill, minmax(118px, 1fr));
  gap: 22px 16px;
}

.folder-leaf-wrap {
  position: relative;
  min-width: 0;
}

.folder-leaf {
  display: flex;
  min-width: 0;
  padding: 8px 4px;
  align-items: center;
  border: 0;
  border-radius: 12px;
  background: transparent;
  flex-direction: column;
  cursor: pointer;
  transition: background .18s ease, transform .18s ease;
}

.pin-app--folder {
  top: 1px;
  left: calc(50% + 12px);
  width: 25px;
  height: 25px;
  font-size: 12px;
}

.folder-leaf:hover {
  outline: none;
  background: #e5f2fc;
  transform: translateY(-2px);
}
.folder-leaf:focus {
  outline: none;
  background: #e5f2fc;
  transform: translateY(-2px);
}

.folder-leaf:focus {
  box-shadow: inset 0 0 0 3px rgba(8, 124, 229, .15);
}

.folder-leaf:active {
  transform: scale(.98);
}

.folder-leaf__icon {
  position: relative;
  display: grid;
  width: 64px;
  height: 64px;
  overflow: hidden;
  place-items: center;
  border-radius: 17px;
  color: #fff;
}

.folder-leaf__icon .svg-icon {
  position: relative;
  z-index: 1;
  width: 31px;
  height: 31px;
}

.folder-leaf strong {
  display: -webkit-box;
  max-width: 100%;
  margin-top: 10px;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.35;
  text-align: center;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  white-space: normal;
}

.folder-panel-enter-active,
.folder-panel-leave-active {
  transition: opacity .18s ease;
}

.folder-panel-enter-active .folder-panel,
.folder-panel-leave-active .folder-panel {
  transition: transform .22s cubic-bezier(.16, 1, .3, 1), opacity .18s ease;
}

.folder-panel-enter,
.folder-panel-leave-to {
  opacity: 0;
}

.folder-panel-enter .folder-panel,
.folder-panel-leave-to .folder-panel {
  opacity: 0;
  transform: translateY(14px) scale(.98);
}

@media (max-width: 820px) {
  .drawer-header {
    min-height: 84px;
    padding: 14px 16px;
  }

  .drawer-heading__mark {
    width: 48px;
    height: 48px;
    border-radius: 13px;
  }

  .drawer-heading p {
    display: none;
  }

  .menu-search {
    width: min(48vw, 360px);
  }

  .drawer-layout {
    display: flex;
    padding: 14px;
    flex-direction: column;
  }
  .drawer-layout > * + * {
    margin-top: 12px;
  }

  .group-nav {
    display: flex;
    flex: 0 0 auto;
    padding: 6px;
    overflow-x: auto;
    overflow-y: hidden;
  }
  .group-nav > * + * {
    margin-left: 6px;
  }

  .group-nav button {
    width: auto;
    min-width: max-content;
    min-height: 46px;
    padding: 6px 12px 6px 7px;
  }

  .group-nav button + button {
    margin-top: 0;
  }

  .group-icon {
    flex-basis: 34px;
    width: 34px;
    height: 34px;
    border-radius: 10px;
  }

  .group-arrow {
    display: none;
  }

  .apps-content {
    padding: 18px 14px;
  }

  .apps-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    column-gap: 10px;
  }

  .app-icon {
    width: 78px;
    height: 78px;
    border-radius: 19px;
  }

  .app-icon > .svg-icon {
    width: 35px;
    height: 35px;
  }

  .folder-icon::before {
    left: 9px;
    width: 30px;
  }

  .folder-preview {
    transform: scale(.88);
  }

  .folder-panel {
    width: min(700px, 88vw);
  }
}

@media (max-width: 560px) {
  .drawer-header {
    min-height: auto;
    padding: 12px;
    align-items: stretch;
    flex-direction: column;
  }
  .drawer-header > * + * {
    margin-top: 10px;
  }

  .drawer-heading__mark {
    width: 42px;
    height: 42px;
    font-size: 20px;
  }

  .drawer-heading h2 {
    font-size: 19px;
  }

  .drawer-actions {
    width: 100%;
  }

  .menu-search {
    width: auto;
    height: 44px;
    flex: 1;
  }

  .drawer-close {
    width: 44px;
    height: 44px;
  }

  .drawer-layout {
    padding: 10px;
  }

  .apps-content {
    padding: 14px 8px;
  }

  .content-heading {
    padding: 0 6px;
  }

  .content-heading p {
    display: none;
  }

  .content-heading > span {
    margin-left: 10px;
  }

  .apps-grid {
    margin-top: 14px;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    row-gap: 24px;
  }

  .app-icon {
    width: 68px;
    height: 68px;
    border-radius: 17px;
  }

  .app-icon > .svg-icon {
    width: 31px;
    height: 31px;
  }

  .app-tile > strong {
    margin-top: 10px;
    font-size: 13px;
  }

  .app-tile > small {
    display: none;
  }

  .folder-preview {
    transform: scale(.76);
  }

  .folder-icon::before {
    left: 8px;
    width: 27px;
    height: 14px;
  }

  .folder-overlay {
    padding: 10px;
  }

  .folder-panel {
    width: calc(100vw - 20px);
    max-height: calc(100% - 12px);
    padding: 16px 10px;
  }

  .folder-panel__header {
    padding: 0 4px;
  }

  .folder-panel__icon {
    width: 46px;
    height: 46px;
    border-radius: 13px;
  }

  .folder-panel h3 {
    max-width: 48vw;
    overflow: hidden;
    font-size: 17px;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .folder-panel__header button {
    width: 42px;
    height: 42px;
  }

  .folder-panel__grid {
    margin-top: 16px;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px 6px;
  }

  .folder-leaf__icon {
    width: 56px;
    height: 56px;
    border-radius: 15px;
  }

  .folder-leaf strong {
    font-size: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    transition-duration: .01ms !important;
    animation-duration: .01ms !important;
    animation-iteration-count: 1 !important;
  }

  .app-tile:hover .app-icon,
  .app-tile:active .app-icon,
  .folder-leaf:hover,
  .folder-leaf:active {
    transform: none;
  }
  .app-tile:focus .app-icon,
  .folder-leaf:focus {
    transform: none;
  }
}
</style>

<style lang="scss">
.all-apps-drawer {
  top: 0 !important;
  height: 100% !important;
  overflow: hidden;
  border-radius: 0;
  background: #eaf4fc;
  box-shadow: none;
}

.all-apps-drawer .el-drawer__body {
  height: 100%;
  overflow: hidden;
}

@media (max-width: 820px) {
  .all-apps-drawer {
    height: 100% !important;
  }
}

@media (max-width: 560px) {
  .all-apps-drawer {
    height: 100% !important;
    border-radius: 0;
  }
}
</style>
