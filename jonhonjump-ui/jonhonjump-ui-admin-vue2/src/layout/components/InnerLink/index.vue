<template>
  <div class="inner-link">
    <iframe
      ref="frame"
      :id="iframeId"
      class="inner-link__frame"
      :src="frameSrc"
      frameborder="no"
      @load="onFrameLoad"
    />

    <!-- Stage B：文档加载中 / 软超时 / 硬失败（仅 active 且未 onload） -->
    <div v-if="showStageBOverlay" class="inner-link__overlay" :class="'is-' + phase">
      <template v-if="phase === 'loading'">
        <i class="el-icon-loading" />
        <p>子系统加载中：正在打开业务页面…</p>
        <small>仅等待页面文档到达；业务内容由子系统自行渲染</small>
      </template>
      <template v-else-if="phase === 'slow'">
        <i class="el-icon-warning-outline" />
        <p>子系统响应较慢：页面文档尚未打开</p>
        <small>可继续等待，或重试打开；业务页本身较慢不会在此阶段误判</small>
        <div class="inner-link__actions">
          <el-button size="small" type="primary" @click="reloadIframe">重试</el-button>
          <el-button size="small" @click="continueWaiting">继续等待</el-button>
          <el-button v-if="clientId" size="small" @click="reauthSubSystem">重新认证</el-button>
        </div>
      </template>
      <template v-else-if="phase === 'failed'">
        <i class="el-icon-circle-close" />
        <p>子系统不可用：未能加载页面文档</p>
        <small>请检查子系统服务是否在线，或点击重试 / 重新认证</small>
        <div class="inner-link__actions">
          <el-button size="small" type="primary" @click="reloadIframe">重试</el-button>
          <el-button v-if="clientId" size="small" @click="reauthSubSystem">重新认证</el-button>
          <el-button size="small" @click="goPortalHome">返回门户</el-button>
        </div>
      </template>
    </div>

    <!-- Stage C：文档已到，仅提供轻量刷新（不超时、不全屏遮罩） -->
    <div v-if="active && hasLoaded" class="inner-link__stage-c" title="业务页由子系统渲染；若空白可刷新该页">
      <el-button type="text" size="mini" icon="el-icon-refresh" @click="reloadIframe">刷新</el-button>
    </div>
  </div>
</template>

<script>
const SOFT_TIMEOUT_MS = 15000
const HARD_TIMEOUT_MS = 30000

export default {
  props: {
    src: {
      type: String,
      default: '/'
    },
    iframeId: {
      type: String,
      default: ''
    },
    /** 当前是否为可见业务 iframe；隐藏时不计时、不展示 Stage B */
    active: {
      type: Boolean,
      default: false
    },
    clientId: {
      type: String,
      default: null
    },
    /** 门户重认证成功后递增，强制重挂文档 */
    reloadNonce: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      frameSrc: this.src || '/',
      phase: 'idle', // idle | loading | slow | failed
      hasLoaded: false,
      softTimer: null,
      hardTimer: null,
      slowDismissed: false,
      /** 重新认证完成后，再次可见时强制重挂文档 */
      pendingReloadAfterAuth: false
    }
  },
  computed: {
    showStageBOverlay() {
      return this.active && !this.hasLoaded && (this.phase === 'loading' || this.phase === 'slow' || this.phase === 'failed')
    }
  },
  watch: {
    src(next) {
      this.frameSrc = next || '/'
      this.resetLoadState()
      if (this.active) {
        this.beginDocumentLoad()
      }
    },
    reloadNonce(nonce, prev) {
      if (!nonce || nonce === prev) {
        return
      }
      if (this.active) {
        this.reloadIframe()
      } else {
        this.pendingReloadAfterAuth = true
      }
    },
    active(visible) {
      if (visible) {
        if (this.pendingReloadAfterAuth) {
          this.pendingReloadAfterAuth = false
          this.reloadIframe()
          return
        }
        if (!this.hasLoaded) {
          this.beginDocumentLoad()
        }
      } else {
        this.clearTimers()
        if (!this.hasLoaded && this.phase !== 'failed') {
          this.phase = 'idle'
        }
      }
    }
  },
  mounted() {
    if (this.active && !this.hasLoaded) {
      this.beginDocumentLoad()
    }
  },
  beforeDestroy() {
    this.clearTimers()
  },
  methods: {
    beginDocumentLoad() {
      if (this.hasLoaded) {
        return
      }
      this.clearTimers()
      this.phase = 'loading'
      this.slowDismissed = false
      this.softTimer = setTimeout(() => {
        if (!this.active || this.hasLoaded || this.phase === 'failed') {
          return
        }
        if (!this.slowDismissed) {
          this.phase = 'slow'
        }
      }, SOFT_TIMEOUT_MS)
      this.hardTimer = setTimeout(() => {
        if (!this.active || this.hasLoaded) {
          return
        }
        this.phase = 'failed'
        this.clearTimers()
      }, HARD_TIMEOUT_MS)
    },
    onFrameLoad() {
      // about:blank 重挂过程中的中间态忽略
      const current = this.frameSrc || ''
      if (!current || current === 'about:blank') {
        return
      }
      this.hasLoaded = true
      this.phase = 'idle'
      this.clearTimers()
    },
    continueWaiting() {
      this.slowDismissed = true
      if (!this.hasLoaded && this.phase === 'slow') {
        this.phase = 'loading'
      }
    },
    reloadIframe() {
      const base = this.src || '/'
      this.hasLoaded = false
      this.slowDismissed = false
      this.clearTimers()
      this.phase = 'loading'
      // 先置空再赋原 URL（带时间戳），强制重新拉取文档
      this.frameSrc = 'about:blank'
      this.$nextTick(() => {
        const sep = base.indexOf('?') >= 0 ? '&' : '?'
        this.frameSrc = `${base}${sep}_portal_t=${Date.now()}`
        if (this.active) {
          this.beginDocumentLoad()
        }
      })
    },
    reauthSubSystem() {
      if (!this.clientId) {
        return
      }
      this.hasLoaded = false
      this.phase = 'idle'
      this.slowDismissed = false
      this.pendingReloadAfterAuth = true
      this.clearTimers()
      this.frameSrc = 'about:blank'
      this.$store.dispatch('portal/reauthSubSystem', this.clientId).catch(() => {})
    },
    goPortalHome() {
      this.$router.push({ path: '/index' }).catch(() => {})
    },
    resetLoadState() {
      this.hasLoaded = false
      this.slowDismissed = false
      this.pendingReloadAfterAuth = false
      this.clearTimers()
      this.phase = 'idle'
    },
    clearTimers() {
      if (this.softTimer) {
        clearTimeout(this.softTimer)
        this.softTimer = null
      }
      if (this.hardTimer) {
        clearTimeout(this.hardTimer)
        this.hardTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.inner-link {
  position: relative;
  height: 100%;
  min-height: 0;
}
.inner-link__frame {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
  background: #fff;
}
.inner-link__overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
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
  &.is-slow i {
    color: #e6a23c;
  }
  &.is-failed i {
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
.inner-link__actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}
.inner-link__stage-c {
  position: absolute;
  right: 8px;
  bottom: 8px;
  z-index: 1;
  opacity: 0.35;
  transition: opacity 0.2s;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 4px;
  padding: 0 4px;
  &:hover {
    opacity: 1;
  }
}
</style>
