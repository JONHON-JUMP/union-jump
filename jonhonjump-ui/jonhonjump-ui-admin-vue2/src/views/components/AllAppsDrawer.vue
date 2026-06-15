<template>
  <el-drawer
    :visible="visible"
    direction="btt"
    size="82%"
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
            <p>按业务分类查看当前账号的授权入口</p>
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
              placeholder="搜索一级、二级或三级菜单"
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
              <p>{{ contentDescription }}</p>
            </div>
            <span>{{ displayedItems.length }} 个入口</span>
          </div>

          <div
            v-if="displayedItems.length"
            class="apps-grid"
          >
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
                <strong>{{ item.name }}</strong>
                <small>{{ item.type === 'folder' ? '文件夹 · ' + item.children.length + ' 项' : '授权应用' }}</small>
              </button>
              <button
                v-if="item.type !== 'folder'"
                class="pin-app"
                :class="{ pinned: isPinned(item) }"
                type="button"
                :aria-label="isPinned(item) ? '取消固定' + item.name : '固定' + item.name + '到首页'"
                :title="isPinned(item) ? '取消固定到首页' : '固定到首页'"
                @click.stop="togglePin(item)"
              >
                <i :class="isPinned(item) ? 'el-icon-star-on' : 'el-icon-star-off'" />
              </button>
            </div>
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
        v-else
        class="drawer-empty drawer-empty--menu"
      >
        <span><i class="el-icon-menu" /></span>
        <h3>暂无可用应用</h3>
        <p>当前账号还没有授权菜单，请联系管理员配置权限。</p>
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
                >
                  <span
                    class="folder-leaf__icon"
                    :style="iconStyle(leaf)"
                  >
                    <span class="icon-highlight" />
                    <svg-icon :icon-class="leaf.icon" />
                  </span>
                  <strong>{{ leaf.name }}</strong>
                </button>
                <button
                  class="pin-app pin-app--folder"
                  :class="{ pinned: isPinned(leaf) }"
                  type="button"
                  :aria-label="isPinned(leaf) ? '取消固定' + leaf.name : '固定' + leaf.name + '到首页'"
                  @click.stop="togglePin(leaf)"
                >
                  <i :class="isPinned(leaf) ? 'el-icon-star-on' : 'el-icon-star-off'" />
                </button>
              </div>
            </div>
          </section>
        </div>
      </transition>
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
  getCachedPinnedApps,
  loadPinnedApps,
  togglePinnedApp
} from '@/utils/portalPinnedApps'

const APP_COLORS = [
  ['linear-gradient(145deg, #2597f4, #086fd8)', 'rgba(8, 111, 216, .26)'],
  ['linear-gradient(145deg, #12a9c4, #087d9f)', 'rgba(8, 125, 159, .24)'],
  ['linear-gradient(145deg, #12aeb5, #08768c)', 'rgba(8, 118, 140, .24)'],
  ['linear-gradient(145deg, #13ad80, #087a59)', 'rgba(8, 122, 89, .24)'],
  ['linear-gradient(145deg, #f39a13, #d76700)', 'rgba(215, 103, 0, .24)'],
  ['linear-gradient(145deg, #6289b2, #315d91)', 'rgba(49, 93, 145, .22)'],
  ['linear-gradient(145deg, #7e78c7, #51489b)', 'rgba(81, 72, 155, .22)']
]

export default {
  name: 'AllAppsDrawer',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    routes: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      keyword: '',
      activeGroupKey: '',
      activeFolder: null,
      folderTrigger: null,
      pinnedApps: getCachedPinnedApps(),
      pinSaving: false
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
      return this.keyword ? `“${this.keyword}”的搜索结果` : (this.activeGroup ? this.activeGroup.name : '全部应用')
    },
    contentDescription() {
      return this.keyword
        ? '搜索结果覆盖所有业务分类和菜单层级'
        : '选择应用直接进入，选择文件夹查看其中全部入口'
    }
  },
  watch: {
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
        document.addEventListener('keydown', this.handleDocumentKeydown)
      } else {
        this.resetTransientState(false)
        document.removeEventListener('keydown', this.handleDocumentKeydown)
      }
    }
  },
  mounted() {
    if (this.visible) document.addEventListener('keydown', this.handleDocumentKeydown)
    this.loadPinnedApps()
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
    isPinned(item) {
      return this.pinnedApps.some(app => app.path === item.path)
    },
    async loadPinnedApps() {
      this.pinnedApps = await loadPinnedApps()
      this.$emit('pinned-change', this.pinnedApps)
    },
    async togglePin(item) {
      if (this.pinSaving) return
      this.pinSaving = true
      try {
        this.pinnedApps = await togglePinnedApp(item, this.pinnedApps)
        const pinned = this.isPinned(item)
        this.$message.success(pinned ? `已将“${item.name}”固定到首页` : `已取消固定“${item.name}”`)
        this.$emit('pinned-change', this.pinnedApps)
      } catch (error) {
        this.$message.error('首页快捷应用保存失败')
      } finally {
        this.pinSaving = false
      }
    },
    selectGroup(group) {
      this.keyword = ''
      this.activeGroupKey = group.key
      this.closeFolder()
    },
    activateItem(item, event) {
      if (item.type === 'folder') {
        this.folderTrigger = event && event.currentTarget
        this.activeFolder = item
        this.$nextTick(this.focusFolderPanel)
        return
      }
      this.openLeaf(item)
    },
    openLeaf(leaf) {
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
      this.closeFolder(restoreFocus)
    },
    handleEscape() {
      if (this.activeFolder) {
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
      const key = String(item && (item.key || item.name) || '')
      let hash = 0
      for (let index = 0; index < key.length; index += 1) {
        hash = ((hash << 5) - hash) + key.charCodeAt(index)
        hash |= 0
      }
      const palette = APP_COLORS[Math.abs(hash) % APP_COLORS.length]
      return {
        background: palette[0],
        boxShadow: `0 12px 22px ${palette[1]}`
      }
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
  gap: 10px;
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
.drawer-close:focus-visible,
.folder-panel__header button:hover,
.folder-panel__header button:focus-visible {
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

.group-nav button:hover,
.group-nav button:focus-visible {
  outline: none;
  color: #075eb5;
  background: #edf6fd;
}

.group-nav button:focus-visible {
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
  grid-template-columns: repeat(auto-fill, minmax(128px, 1fr));
  column-gap: clamp(12px, 2vw, 28px);
  row-gap: 30px;
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

.pin-app:hover,
.pin-app:focus-visible {
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
  color: #fff;
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

.app-tile:hover .app-icon,
.app-tile:focus-visible .app-icon {
  transform: translateY(-5px);
  filter: saturate(1.08);
}

.app-tile:focus-visible .app-icon {
  outline: 3px solid rgba(8, 124, 229, .24);
  outline-offset: 5px;
}

.app-tile:active .app-icon {
  transform: translateY(-1px) scale(.97);
}

.app-tile > strong {
  max-width: 100%;
  margin-top: 13px;
  overflow: hidden;
  font-size: 16px;
  line-height: 1.4;
  white-space: nowrap;
  text-overflow: ellipsis;
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
  inset: 0;
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

.folder-leaf:hover,
.folder-leaf:focus-visible {
  outline: none;
  background: #e5f2fc;
  transform: translateY(-2px);
}

.folder-leaf:focus-visible {
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
  max-width: 100%;
  margin-top: 10px;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.4;
  white-space: nowrap;
  text-overflow: ellipsis;
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
    gap: 12px;
    flex-direction: column;
  }

  .group-nav {
    display: flex;
    flex: 0 0 auto;
    padding: 6px;
    overflow-x: auto;
    overflow-y: hidden;
    gap: 6px;
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
    grid-template-columns: repeat(auto-fill, minmax(108px, 1fr));
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
    gap: 10px;
    flex-direction: column;
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
  .app-tile:focus-visible .app-icon,
  .app-tile:active .app-icon,
  .folder-leaf:hover,
  .folder-leaf:focus-visible,
  .folder-leaf:active {
    transform: none;
  }
}
</style>

<style lang="scss">
.all-apps-drawer {
  overflow: hidden;
  border-radius: 16px 16px 0 0;
  background: #eaf4fc;
  box-shadow: 0 -12px 34px rgba(31, 70, 105, .2);
}

.all-apps-drawer .el-drawer__body {
  height: 100%;
  overflow: hidden;
}

@media (max-width: 820px) {
  .all-apps-drawer {
    height: 88% !important;
  }
}

@media (max-width: 560px) {
  .all-apps-drawer {
    height: 94% !important;
    border-radius: 14px 14px 0 0;
  }
}
</style>
