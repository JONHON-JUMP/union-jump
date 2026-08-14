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

    <div class="iframe-toggle__frames">
      <!-- 活跃 + Camstar 保温：保温帧 v-show=false，再开不重建 -->
      <inner-link
        v-for="item in allIframeFrames"
        v-show="isIframeVisible(item)"
        :key="item.path"
        :iframeId="'iframe-' + stableFrameId(item.path)"
        :src="normalizeLink(item)"
        :active="isIframeVisible(item)"
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
  lookupPathLinkEntry
} from '@/utils/portalRoute'
import { syncPortalIframeView } from '@/utils/portalIframe'

export default {
  components: { InnerLink },
  data() {
    return {
      placeholderFailed: false,
      placeholderTimer: null
    }
  },
  computed: {
    iframeViews() {
      return this.$store.state.tagsView.iframeViews
    },
    allIframeFrames() {
      const map = {}
      ;(this.$store.state.tagsView.warmIframeViews || []).forEach(item => {
        if (item && item.path) {
          map[item.path] = item
        }
      })
      ;(this.iframeViews || []).forEach(item => {
        if (item && item.path) {
          map[item.path] = item
        }
      })
      return Object.keys(map).map(k => map[k])
    },
    routeClientId() {
      return parsePortalClientId(this.$route.path)
    },
    resolvedFrameLink() {
      const fromView = (this.allIframeFrames || []).find(item =>
        this.menuPathsMatch(this.$route.path, item && item.path)
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
      const clientId = this.routeClientId
      if (!clientId) {
        return false
      }
      const rest = String(this.$route.path || '')
        .replace(new RegExp('^/portal/' + clientId + '/'), '')
        .replace(/\/index$/, '')
      return !!slashIpPortRestToHttp(rest.replace(/:/g, '/'))
    },
    showFramePlaceholder() {
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
    '$route.path': {
      immediate: true,
      handler() {
        this.placeholderFailed = false
        this.armPlaceholderTimeout()
      }
    },
    showFramePlaceholder(show) {
      if (show) {
        this.armPlaceholderTimeout()
      } else {
        this.clearPlaceholderTimeout()
        this.placeholderFailed = false
      }
    }
  },
  beforeDestroy() {
    this.clearPlaceholderTimeout()
  },
  methods: {
    parsePortalClientId,
    stableFrameId(path) {
      return String(path || '').replace(/[^\w]+/g, '_').slice(0, 120)
    },
    retryRegister() {
      this.placeholderFailed = false
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
    isIframeVisible(item) {
      if (!this.menuPathsMatch(this.$route.path, item && item.path)) {
        return false
      }
      return true
    },
    pathsMatchPortalAlias(routePath, itemPath) {
      if (!routePath || !itemPath) {
        return false
      }
      if (routePath === itemPath) {
        return true
      }
      const strip = p => String(p).replace(/\/index\/?$/, '').replace(/\/$/, '')
      return strip(routePath) === strip(itemPath)
    },
    menuPathsMatch(routePath, itemPath) {
      if (this.pathsMatchPortalAlias(routePath, itemPath)) {
        return true
      }
      const map = this.$store.state.portal.pathLinkMap
      if (!map) {
        return false
      }
      const a = lookupPathLinkEntry(routePath, map)
      const b = lookupPathLinkEntry(itemPath, map)
      if (!a || !b) {
        return false
      }
      if (a === b) {
        return true
      }
      return !!(a.link && b.link && a.link === b.link && a.title && b.title && a.title === b.title)
    },
    goPortalHome() {
      this.$router.push({ path: '/index' }).catch(() => {})
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
  background: #f5f7fb;
}
.iframe-toggle__frames {
  flex: 1;
  height: 100%;
  min-height: 0;
  background: #f5f7fb;
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
