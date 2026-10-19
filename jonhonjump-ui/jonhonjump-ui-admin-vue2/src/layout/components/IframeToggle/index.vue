<template>
  <div class="iframe-toggle">
    <!-- 当前 path 尚无 iframe 记录时短暂占位 -->
    <div
      v-if="showFramePlaceholder"
      class="iframe-sso-wait"
      :class="{ 'is-error': placeholderFailed }"
    >
      <i :class="placeholderFailed ? 'el-icon-warning-outline' : 'el-icon-loading'" />
      <p v-if="!placeholderFailed">{{ isDirectExternalRoute ? '正在打开业务页面…' : '正在准备业务页面…' }}</p>
      <p v-else>业务页面未能挂载</p>
      <small v-if="!placeholderFailed">请稍候</small>
      <small v-else>请重试，或从快捷导航重新打开</small>
      <div v-if="placeholderFailed" class="iframe-sso-wait__actions">
        <el-button size="small" type="primary" @click="retryRegister">重试</el-button>
        <el-button size="small" @click="goPortalHome">返回门户</el-button>
      </div>
    </div>

    <div class="iframe-toggle__frames" :style="frameHeight ? { height: frameHeight + 'px' } : null">
      <!-- 活跃 + Camstar 保温：保温帧 v-show=false，再开不重建 -->
      <inner-link
        v-for="item in allIframeFrames"
        :class="{
          'is-frame-hidden': !isIframeVisible(item) && !isParkedFrame(item),
          'is-parked-hidden': !isIframeVisible(item) && isParkedFrame(item)
        }"
        :key="iframeKey(item)"
        :iframeId="'iframe-' + stableFrameId(iframeKey(item))"
        :src="normalizeLink(item)"
        :active="isIframeVisible(item)"
        :releaseOnHide="isParkedFrame(item)"
        :clientId="parsePortalClientId(item.path)"
        :directExternal="isDirectExternalLink(normalizeLink(item))"
      />
    </div>
  </div>
</template>

<script>
import InnerLink from "../InnerLink/index.vue"
import {
  parsePortalClientId,
  normalizeSubsystemIframeLink,
  resolvePortalFrameRoute,
  slashIpPortRestToHttp,
  unwrapDirectHttpIframeLink,
  portalTabKey,
  portalTabsMatch,
  portalPathAliasKey
} from '@/utils/portalRoute'
import { syncPortalIframeView } from '@/utils/portalIframe'

export default {
  components: { InnerLink },
  data() {
    return {
      placeholderFailed: false,
      placeholderTimer: null,
      /** 切页后先给已登记帧一帧时间挂上来，避免“旧的刚隐藏、新的还没显示”时闪一下占位 */
      placeholderArmed: false,
      placeholderArmTimer: null,
      /** 锁住 iframe 视口高度：路由切换过程中父级哪怕有 1px 抖动，也不让 Camstar 重排 */
      frameHeight: 0,
      frameHeightTimer: null
    }
  },
  computed: {
    iframeViews() {
      return this.$store.state.tagsView.iframeViews
    },
    visitedViews() {
      return this.$store.state.tagsView.visitedViews
    },
    allIframeFrames() {
      const map = {}
      ;(this.$store.state.tagsView.warmIframeViews || []).forEach(item => {
        if (item && item.path) {
          map[portalTabKey(item) || item.path] = item
        }
      })
      ;(this.iframeViews || []).forEach(item => {
        if (item && item.path) {
          map[portalTabKey(item) || item.path] = item
        }
      })
      return Object.keys(map).map(k => map[k])
    },
    routeClientId() {
      return parsePortalClientId(this.$route.path)
    },
    resolvedFrameLink() {
      const fromView = (this.allIframeFrames || []).find(item =>
        portalTabsMatch(this.$route, item)
      )
      if (fromView && fromView.meta && fromView.meta.link) {
        return unwrapDirectHttpIframeLink(fromView.meta.link) || fromView.meta.link
      }
      const resolved = resolvePortalFrameRoute(
        this.$route,
        this.$store.state.portal.pathLinkMap,
        this.$store.state.portal.systemList
      )
      const link = resolved && resolved.meta && resolved.meta.link
      if (link) {
        return unwrapDirectHttpIframeLink(link) || link
      }
      const clientId = this.routeClientId
      if (clientId) {
        const rest = String(this.$route.path || '')
          .replace(new RegExp('^/portal/' + clientId + '/'), '')
          .replace(/\/index$/, '')
        const asHttp = slashIpPortRestToHttp(rest.replace(/:/g, '/'))
        if (asHttp) {
          return asHttp
        }
      }
      return ''
    },
    isDirectExternalRoute() {
      if (this.isDirectExternalLink(this.resolvedFrameLink)) {
        return true
      }
      const link = this.resolvedFrameLink
      // 带 /#/ 的 SPA 也算已解析到业务地址
      return !!(link && /^https?:\/\//i.test(link))
    },
    showFramePlaceholder() {
      if (!this.placeholderArmed) {
        return false
      }
      if (!this.routeClientId) {
        return false
      }
      if (!this.resolvedFrameLink && !this.isDirectExternalRoute) {
        return false
      }
      const hasVisible = (this.allIframeFrames || []).some(item => this.isIframeVisible(item))
      return !hasVisible
    }
  },
  watch: {
    '$route.fullPath': {
      immediate: true,
      handler() {
        this.placeholderFailed = false
        const isIframeRoute = !!this.routeClientId
        // 从首页/主系统页进入业务页：
        // - 第一次进入业务页：还没有业务态高度，先按容器自适应，再锁定；
        // - 之后（首页 ↔ 已打开业务页）：保持业务态高度锁，绝不在首页高度和业务高度之间来回 resize，
        //   否则 Camstar 视口一变就会闪/抖。
        if (isIframeRoute && !this._lastWasIframeRoute) {
          if (!this._hasBusinessHeight) {
            this.frameHeight = 0
            this.scheduleFrameHeightSync(false)
          }
        }
        this._lastWasIframeRoute = isIframeRoute
        this.$nextTick(() => this.ensureFrameRegistered())
        this.armPlaceholderDelayed()
      }
    },
    showFramePlaceholder(show) {
      if (show) {
        this.ensureFrameRegistered()
        this.armPlaceholderTimeout()
      } else {
        this.clearPlaceholderTimeout()
        this.placeholderFailed = false
      }
    },
    resolvedFrameLink(link) {
      if (link) {
        this.ensureFrameRegistered()
      }
    }
  },
  mounted() {
    this.ensureFrameRegistered()
    this.armPlaceholderDelayed()
    this._hasBusinessHeight = false
    this._onPortalLayoutChanged = () => {
      // 只有业务页里的布局变化（收起/展开顶栏、底栏）才需要重测；
      // 首页的布局变化不影响已锁定的业务态高度，保持不动，避免回业务页时 resize。
      if (this.routeClientId) {
        this.scheduleFrameHeightSync(true)
      }
    }
    this._onWindowResize = () => {
      // 窗口尺寸变了，业务态高度需要重测；首页里先标记失效
      this._hasBusinessHeight = false
      if (this.routeClientId) {
        this.scheduleFrameHeightSync(true)
      }
    }
    this.$root.$on('portal-layout-changed', this._onPortalLayoutChanged)
    window.addEventListener('resize', this._onWindowResize)
    this._lastWasIframeRoute = !!this.routeClientId
    if (this.routeClientId) {
      this.scheduleFrameHeightSync(false)
    }
  },
  beforeDestroy() {
    this.clearPlaceholderTimeout()
    if (this.placeholderArmTimer) {
      clearTimeout(this.placeholderArmTimer)
      this.placeholderArmTimer = null
    }
    if (this.frameHeightTimer) {
      clearTimeout(this.frameHeightTimer)
      this.frameHeightTimer = null
    }
    if (this._onPortalLayoutChanged) {
      this.$root.$off('portal-layout-changed', this._onPortalLayoutChanged)
      this._onPortalLayoutChanged = null
    }
    if (this._onWindowResize) {
      window.removeEventListener('resize', this._onWindowResize)
      this._onWindowResize = null
    }
  },
  methods: {
    parsePortalClientId,
    /**
     * 把 iframe 视口高度锁成像素值。
     * 路由切换时不再重算，父级哪怕有 1px 抖动也不会传给 Camstar；
     * 只有窗口 resize、顶栏折叠、底栏展开收起这类“明确的布局变化”才解锁重算。
     */
    scheduleFrameHeightSync(unlock) {
      if (this.frameHeightTimer) {
        clearTimeout(this.frameHeightTimer)
        this.frameHeightTimer = null
      }
      if (unlock) {
        this.frameHeight = 0
      }
      this.$nextTick(() => {
        this.frameHeightTimer = window.setTimeout(() => {
          this.frameHeightTimer = null
          const h = this.$el ? this.$el.clientHeight : 0
          this.frameHeight = h > 0 ? h : 0
          if (this.routeClientId) {
            this._hasBusinessHeight = true
          }
        }, unlock ? 380 : 80)
      })
    },
    stableFrameId(path) {
      return String(path || '').replace(/[^\w]+/g, '_').slice(0, 120)
    },
    /** 有业务 link 但尚未登记 iframe 时补登记，避免「未能挂载」空转 */
    ensureFrameRegistered() {
      const link = this.resolvedFrameLink
      if (!link || !this.routeClientId) {
        return
      }
      if ((this.allIframeFrames || []).some(item => this.isIframeVisible(item))) {
        return
      }
      if (this.$store.state.portal.iframeSyncSuspended) {
        return
      }
      syncPortalIframeView(this.$store, this.$route)
    },
    retryRegister() {
      this.placeholderFailed = false
      this.ensureFrameRegistered()
      syncPortalIframeView(this.$store, this.$route)
      this.armPlaceholderTimeout()
    },
    armPlaceholderTimeout() {
      this.clearPlaceholderTimeout()
      if (!this.showFramePlaceholder) {
        return
      }
      this.placeholderTimer = setTimeout(() => {
        if (this.showFramePlaceholder) {
          this.placeholderFailed = true
        }
      }, 12000)
    },
    /**
     * 切页后决定是否延迟占位层：
     * - 目标页已经有 iframe（已打开过）→ 延迟 80ms，让新帧直接顶上来，不闪 loading；
     * - 目标页没有 iframe（新页面）→ 立即显示 loading，并用不透明占位盖住旧帧，
     *   避免“打开新页面时闪一下上一个页面/已关闭页面”的残影。
     */
    armPlaceholderDelayed() {
      const hasTargetFrame = (this.allIframeFrames || []).some(item => this.isIframeVisible(item))
      if (this.placeholderArmTimer) {
        clearTimeout(this.placeholderArmTimer)
        this.placeholderArmTimer = null
      }
      if (!hasTargetFrame) {
        this.placeholderArmed = true
        this.armPlaceholderTimeout()
        return
      }
      this.placeholderArmed = false
      this.placeholderArmTimer = window.setTimeout(() => {
        this.placeholderArmTimer = null
        this.placeholderArmed = true
        this.armPlaceholderTimeout()
      }, 80)
    },
    clearPlaceholderTimeout() {
      if (this.placeholderTimer) {
        clearTimeout(this.placeholderTimer)
        this.placeholderTimer = null
      }
    },
    isDirectExternalLink(link) {
      const s = String(link || '')
      return /^https?:\/\//i.test(s) && s.indexOf('/#/') < 0 && s.indexOf('#') < 0
    },
    normalizeLink(item) {
      const raw = (item && item.meta && item.meta.link) || ''
      const unwrapped = unwrapDirectHttpIframeLink(raw) || raw
      if (this.isDirectExternalLink(unwrapped)) {
        return unwrapped
      }
      const clientId = parsePortalClientId(item && item.path) || this.routeClientId
      return normalizeSubsystemIframeLink(unwrapped, clientId)
    },
    iframeKey(item) {
      const base = portalTabKey(item) || (item && item.path) || ''
      const nonce = item && item.meta && item.meta.portalSrcNonce
      return nonce ? `${base}__${nonce}` : base
    },
    isIframeVisible(item) {
      return portalTabsMatch(this.$route, item)
    },
    /** 已关页签、只在保温池里的帧。关掉后要释放业务会话，不能继续占着请求 */
    isParkedFrame(item) {
      const key = portalTabKey(item)
      if (!key) {
        return false
      }
      // 已打开页签（visitedViews 里还在）一律不算保温：iframeViews 的 key 可能因
      // Camstar 内部路由/标题同步而漂移，误判成保温会在切走时挂起、切回时重载，
      // 表现就是 Camstar 页面切回时闪/抖。
      if ((this.visitedViews || []).some(view => portalTabsMatch(view, item))) {
        return false
      }
      return !(this.iframeViews || []).some(view =>
        portalTabKey(view) === key
        || portalPathAliasKey(view.path) === portalPathAliasKey(item.path)
      )
    },
    goPortalHome() {
      this.$store.dispatch('portal/navigateToPortalHome').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.iframe-toggle {
  flex: 1;
  height: 100%;
  min-height: 0;
  position: relative;
  overflow: hidden;
  background: #f5f7fb;
}
.iframe-toggle__frames {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  min-height: 0;
  overflow: hidden;
  background: #f5f7fb;
}
.iframe-toggle__frames > .inner-link-root {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  height: auto;
}
/* 已打开、但不在前台的页：不做任何显隐，只压到下层。
   显隐（visibility/opacity/display）都会让 Camstar 这类跨域页面在重新可见时
   恢复焦点/滚动，表现为“数据没刷新但页面抖一下”。前台帧根节点是不透明底，
   会完整盖住下面的帧，所以保持可见不会露馅。已关闭/保温帧仍透明隐藏。 */
.iframe-toggle__frames > .inner-link-root.is-frame-hidden {
  pointer-events: none;
  z-index: 0;
}
/* 已关闭/保温帧：透明隐藏，避免残留旧页面画面（切回会重建/重载，属于预期） */
.iframe-toggle__frames > .inner-link-root.is-parked-hidden {
  opacity: 0;
  pointer-events: none;
  z-index: 0;
}
.iframe-sso-wait {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fb;
  color: #303133;
  text-align: center;
  padding: 24px;
  & > * + * {
    margin-top: 8px;
  }
  i {
    font-size: 28px;
    color: #409eff;
  }
  &.is-error i {
    color: #f56c6c;
  }
  p {
    margin: 0;
    font-size: 15px;
    font-weight: 500;
  }
  small {
    color: #909399;
    max-width: 360px;
  }
  &__actions {
    margin-top: 12px;
  }
}
</style>
