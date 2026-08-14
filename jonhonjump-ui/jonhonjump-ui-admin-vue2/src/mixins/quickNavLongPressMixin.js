export const QUICK_NAV_LONG_PRESS_MS = 1000

import { fetchMenuManualUrl, downloadMenuManual } from '@/utils/menuManualUrl'

export default {
  data() {
    return {
      suppressNextItemClick: false,
      longPressTimer: null,
      longPressMoved: false,
      longPressStartX: 0,
      longPressStartY: 0,
      longPressTarget: null,
      longPressItem: null,
      longPressMoveHandler: null,
      longPressEndHandler: null,
      contextMenu: {
        visible: false,
        item: null,
        anchorRect: null
      },
      manualDialogVisible: false,
      manualDialogItem: null
    }
  },
  computed: {
    contextMenuShowSubscribe() {
      return false
    },
    contextMenuShowUnsubscribe() {
      return false
    },
    contextMenuShowReorder() {
      return false
    },
    contextMenuStyle() {
      const rect = this.contextMenu.anchorRect
      if (!rect) {
        return {}
      }
      const menuWidth = 220
      let itemCount = 1
      if (this.contextMenuShowReorder) itemCount += 1
      if (this.contextMenuShowSubscribe) itemCount += 1
      if (this.contextMenuShowUnsubscribe) itemCount += 1
      const menuHeight = itemCount * 56
      const gap = 14
      let left = rect.left + rect.width / 2 - menuWidth / 2
      let top = rect.top - menuHeight - gap
      if (top < 12) {
        top = rect.bottom + gap
      }
      left = Math.max(12, Math.min(left, window.innerWidth - menuWidth - 12))
      top = Math.max(12, Math.min(top, window.innerHeight - menuHeight - 12))
      return {
        left: `${left}px`,
        top: `${top}px`,
        width: `${menuWidth}px`
      }
    },
    manualDialogTitle() {
      const item = this.manualDialogItem
      return item && item.name ? `${item.name} 说明书` : '说明书'
    },
    manualDialogContent() {
      if (!this.manualDialogItem) {
        return ''
      }
      if (this.manualDialogItem.manualUrl) {
        return ''
      }
      return '暂无说明书内容，请联系管理员配置。'
    }
  },
  methods: {
    getPressPoint(event) {
      const touch = (event.touches && event.touches[0]) || (event.changedTouches && event.changedTouches[0])
      if (touch) {
        return { x: touch.clientX, y: touch.clientY }
      }
      return { x: event.clientX, y: event.clientY }
    },
    clearLongPress() {
      if (this.longPressTimer) {
        window.clearTimeout(this.longPressTimer)
        this.longPressTimer = null
      }
      if (this.longPressMoveHandler) {
        document.removeEventListener('mousemove', this.longPressMoveHandler)
        this.longPressMoveHandler = null
      }
      if (this.longPressEndHandler) {
        document.removeEventListener('mouseup', this.longPressEndHandler)
        this.longPressEndHandler = null
      }
    },
    canLongPressItem(item) {
      return Boolean(item && item.menuId != null)
    },
    handleItemPressStart(event, item) {
      if (this.contextMenu.visible || !this.canLongPressItem(item)) {
        return
      }
      if (typeof this.shouldBlockLongPress === 'function' && this.shouldBlockLongPress()) {
        return
      }
      if (event.type === 'mousedown' && event.button !== 0) {
        return
      }
      event.stopPropagation()
      this.clearLongPress()
      this.longPressMoved = false
      this.longPressTarget = event.currentTarget
      this.longPressItem = item
      const point = this.getPressPoint(event)
      this.longPressStartX = point.x
      this.longPressStartY = point.y

      const onMove = (moveEvent) => {
        const nextPoint = this.getPressPoint(moveEvent)
        if (Math.abs(nextPoint.x - this.longPressStartX) > 12 || Math.abs(nextPoint.y - this.longPressStartY) > 12) {
          this.longPressMoved = true
          this.clearLongPress()
        }
      }
      const onEnd = () => {
        this.clearLongPress()
      }

      this.longPressMoveHandler = onMove
      this.longPressEndHandler = onEnd
      if (event.type === 'mousedown') {
        document.addEventListener('mousemove', onMove)
        document.addEventListener('mouseup', onEnd)
      }

      this.longPressTimer = window.setTimeout(() => {
        if (!this.longPressMoved && this.longPressItem && this.longPressTarget) {
          this.openContextMenu(this.longPressItem, this.longPressTarget)
          if (navigator.vibrate) {
            navigator.vibrate(40)
          }
        }
        this.clearLongPress()
      }, QUICK_NAV_LONG_PRESS_MS)
    },
    handleItemPressMove(event) {
      if (!this.longPressTimer || !this.longPressMoveHandler) {
        return
      }
      event.stopPropagation()
      this.longPressMoveHandler(event)
    },
    handleItemPressEnd(event) {
      if (event) {
        event.stopPropagation()
      }
      this.clearLongPress()
    },
    openContextMenu(item, target) {
      if (!item || !target || !target.getBoundingClientRect) {
        return
      }
      this.suppressNextItemClick = true
      window.setTimeout(() => {
        this.suppressNextItemClick = false
      }, 400)
      this.contextMenu = {
        visible: true,
        item,
        anchorRect: target.getBoundingClientRect()
      }
    },
    closeContextMenu() {
      this.contextMenu = {
        visible: false,
        item: null,
        anchorRect: null
      }
      this.longPressItem = null
      this.longPressTarget = null
    },
    handleContextViewManual() {
      const item = this.contextMenu.item
      if (!item) {
        return
      }
      this.closeContextMenu()
      this.openManualDialog(item)
    },
    resolveManualSubSystemId() {
      const currentSystem = this.$store.getters.currentSystem
      if (currentSystem === 'main') {
        return 0
      }
      const list = this.$store.getters.portalSystemList || []
      const sys = list.find(entry => entry.clientId === currentSystem)
      return sys ? Number(sys.subSystemId) : 0
    },
    async openManualDialog(item) {
      let manualUrl = item.manualUrl ? String(item.manualUrl).split(',')[0].trim() : ''
      if (!manualUrl && this.canLongPressItem(item)) {
        try {
          manualUrl = await fetchMenuManualUrl({
            menuId: item.menuId,
            currentSystem: this.$store.getters.currentSystem,
            subSystemId: this.resolveManualSubSystemId()
          }) || ''
        } catch (e) {
          // 拉取失败时仍展示空状态提示
        }
      }
      if (manualUrl) {
        downloadMenuManual(manualUrl, item.name)
        return
      }
      this.manualDialogItem = { ...item }
      this.manualDialogVisible = true
    },
    handleContextMenuKeydown(event) {
      if (event.key !== 'Escape') {
        return
      }
      if (this.contextMenu.visible) {
        this.closeContextMenu()
      }
    }
  },
  beforeDestroy() {
    this.clearLongPress()
    this.closeContextMenu()
  }
}
