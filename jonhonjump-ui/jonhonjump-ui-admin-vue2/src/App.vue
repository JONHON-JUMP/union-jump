<template>
  <div id="app">
    <router-view />
    <session-lock
      :visible="sessionLocked"
      :idle-minutes="sessionIdleMinutes"
      @relogin="handleRelogin"
    />
    <theme-picker />
  </div>
</template>

<script>
import ThemePicker from "@/components/ThemePicker";
import SessionLock from "@/components/SessionLock";
import { getAccessToken } from "@/utils/auth";
import { initSessionGuard, destroySessionGuard, pauseSessionGuard } from "@/utils/sessionGuard";
import { loadSessionIdleTimeoutConfig, getSessionIdleTimeoutMinutes } from "@/utils/sessionIdleConfig";
import { redirectToLogin } from "@/utils/switchUser";
import { broadcastForceLoginHome } from "@/utils/portalLogoutBroadcast";

export default {
  name: "App",
  components: { ThemePicker, SessionLock },
  data() {
    return {
      sessionLocked: false,
      sessionIdleMinutes: 60
    }
  },
  watch: {
    '$route'() {
      this.syncSessionGuard()
    },
    sessionLocked(locked) {
      if (locked) {
        pauseSessionGuard()
      } else {
        this.syncSessionGuard()
      }
    }
  },
  mounted() {
    this.syncSessionGuard()
  },
  beforeDestroy() {
    destroySessionGuard()
  },
  methods: {
    syncSessionGuard() {
      destroySessionGuard()
      this.sessionLocked = false
      if (!getAccessToken()) {
        return
      }
      // 并行加载空闲锁屏时长（来自参数配置，非写死）
      loadSessionIdleTimeoutConfig(true).then(timeoutMs => {
        this.sessionIdleMinutes = getSessionIdleTimeoutMinutes()
        if (timeoutMs <= 0) {
          return
        }
        initSessionGuard(() => {
          pauseSessionGuard()
          broadcastForceLoginHome()
          this.$store.dispatch('LogOut').finally(() => {
            this.sessionLocked = true
          })
        }, timeoutMs)
      })
    },
    handleRelogin() {
      destroySessionGuard()
      redirectToLogin('/index')
    }
  },
  metaInfo() {
    return {
      title: this.$store.state.settings.dynamicTitle && this.$store.state.settings.title,
      titleTemplate: title => {
        return title ? `${title} - ${process.env.VUE_APP_TITLE}` : process.env.VUE_APP_TITLE
      }
    }
  }
};
</script>
<style scoped>
#app .theme-picker {
  display: none;
}
</style>
