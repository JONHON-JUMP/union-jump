<template>
  <div class="iframe-toggle">
    <!-- Stage A：主系统 SSO -->
    <div v-if="waitingSso" class="iframe-sso-wait">
      <i class="el-icon-loading" />
      <p>主系统处理中：正在完成子系统登录…</p>
      <small>身份认证由主系统发起，完成后自动打开业务页</small>
    </div>

    <!-- SSO 失败（发版后假登录 / 认证失败） -->
    <div v-else-if="ssoFailed" class="iframe-sso-wait is-error">
      <i class="el-icon-warning-outline" />
      <p>主系统处理中：子系统登录失败</p>
      <small>{{ ssoErrorMessage }}</small>
      <div class="iframe-sso-wait__actions">
        <el-button size="small" type="primary" @click="retrySso">重新认证</el-button>
        <el-button size="small" @click="goPortalHome">返回门户</el-button>
      </div>
    </div>

    <!-- 健康探测失败：子系统暂不可用 -->
    <div v-else-if="healthDown" class="iframe-sso-wait is-error">
      <i class="el-icon-circle-close" />
      <p>子系统暂不可用，请稍后重试</p>
      <small>主系统已检测到子系统入口无响应，可稍后重试或返回门户</small>
      <div class="iframe-sso-wait__actions">
        <el-button size="small" type="primary" :loading="healthChecking" @click="retryHealth">重试</el-button>
        <el-button size="small" @click="goPortalHome">返回门户</el-button>
      </div>
    </div>

    <transition-group class="iframe-toggle__frames" name="fade-transform" mode="out-in" tag="div">
      <inner-link
        v-for="(item, index) in iframeViews"
        v-show="isIframeVisible(item)"
        :key="item.path + '|' + (item.meta.link || '')"
        :iframeId="'iframe' + index"
        :src="item.meta.link"
        :active="isIframeVisible(item)"
        :clientId="parsePortalClientId(item.path)"
        :reloadNonce="reloadNonceFor(item)"
      />
    </transition-group>
  </div>
</template>

<script>
import InnerLink from "../InnerLink/index.vue"
import { parsePortalClientId } from '@/utils/portalRoute'

export default {
  components: { InnerLink },
  data() {
    return {
      healthChecking: false
    }
  },
  computed: {
    iframeViews() {
      return this.$store.state.tagsView.iframeViews
    },
    routeClientId() {
      return parsePortalClientId(this.$route.path)
    },
    waitingSso() {
      const clientId = this.routeClientId
      if (!clientId || this.ssoFailed) {
        return false
      }
      return !this.$store.state.portal.ssoDone[clientId]
    },
    ssoFailed() {
      const clientId = this.routeClientId
      if (!clientId) {
        return false
      }
      return Boolean(this.$store.state.portal.ssoError[clientId])
    },
    ssoErrorMessage() {
      const clientId = this.routeClientId
      return (clientId && this.$store.state.portal.ssoError[clientId]) || '请重新认证或联系管理员'
    },
    healthDown() {
      const clientId = this.routeClientId
      if (!clientId) {
        return false
      }
      // 仅在 SSO 已完成后遮罩，避免与 Stage A 重叠
      if (!this.$store.state.portal.ssoDone[clientId]) {
        return false
      }
      return this.$store.state.portal.subsystemHealthy[clientId] === false
    }
  },
  watch: {
    routeClientId: {
      immediate: true,
      handler(clientId) {
        this.$store.dispatch('portal/syncHealthProbe', clientId || null).catch(() => {})
      }
    },
    waitingSso(waiting) {
      if (!waiting || !this.routeClientId) {
        return
      }
      this.$store.dispatch('portal/ensureSubSystemReady', this.routeClientId).catch(() => {})
    }
  },
  beforeDestroy() {
    this.$store.dispatch('portal/syncHealthProbe', null).catch(() => {})
  },
  methods: {
    parsePortalClientId,
    reloadNonceFor(item) {
      const clientId = parsePortalClientId(item.path)
      if (!clientId) {
        return 0
      }
      return this.$store.state.portal.iframeReloadNonce[clientId] || 0
    },
    isIframeVisible(item) {
      if (this.$route.path !== item.path) {
        return false
      }
      const clientId = parsePortalClientId(item.path)
      if (!clientId) {
        return true
      }
      if (!this.$store.state.portal.ssoDone[clientId]) {
        return false
      }
      // 健康探测失败时仍保留 iframe，但上层遮罩；避免反复销毁
      return true
    },
    retrySso() {
      if (!this.routeClientId) {
        return
      }
      this.$store.dispatch('portal/reauthSubSystem', this.routeClientId).catch(() => {})
    },
    retryHealth() {
      if (!this.routeClientId || this.healthChecking) {
        return
      }
      this.healthChecking = true
      this.$store.dispatch('portal/startHealthProbe', this.routeClientId)
        .finally(() => {
          this.healthChecking = false
        })
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
}
.iframe-toggle__frames {
  flex: 1;
  height: 100%;
  min-height: 0;
}
.iframe-sso-wait {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #f5f7fb;
  color: #303133;
  text-align: center;
  padding: 24px;
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
    font-weight: 600;
  }
  small {
    color: #909399;
    max-width: 420px;
    line-height: 1.5;
  }
}
.iframe-sso-wait__actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}
</style>
