<template>
  <div class="inner-link-root">
    <!-- Camstar 直链：简单 src + loading；切走不 reload；超时可重试 -->
    <div v-if="isDirectHttp" class="inner-link is-direct">
      <iframe
        :id="iframeId"
        class="inner-link__frame"
        :src="directSrc"
        frameborder="no"
        @load="onDirectLoad"
      />
      <div
        v-if="showDirectOverlay"
        class="inner-link__overlay"
        :class="'is-' + directOverlayPhase"
      >
        <template v-if="directOverlayPhase === 'loading'">
          <i class="el-icon-loading" />
          <p>正在打开业务页面…</p>
          <small>请稍候，打开后由业务页自行加载数据</small>
        </template>
        <template v-else-if="directOverlayPhase === 'slow'">
          <i class="el-icon-warning-outline" />
          <p>业务页面打开较慢，仍在等待…</p>
          <small>可继续等待，或点重试</small>
          <div class="inner-link__actions">
            <el-button size="small" type="primary" @click="reloadIframe">重试</el-button>
            <el-button size="small" @click="continueDirectWaiting">继续等待</el-button>
          </div>
        </template>
        <template v-else>
          <i class="el-icon-circle-close" />
          <p>业务页面未能打开</p>
          <small>请检查业务系统是否在线，或点击重试</small>
          <div class="inner-link__actions">
            <el-button size="small" type="primary" @click="reloadIframe">重试</el-button>
            <el-button size="small" @click="goPortalHome">返回门户</el-button>
          </div>
        </template>
      </div>
    </div>

    <!-- 若依子系统 -->
    <div
      v-else
      class="inner-link"
      :class="{ 'is-pending': showStageBOverlay }"
    >
      <iframe
        ref="frame"
        :id="iframeId"
        class="inner-link__frame"
        :class="{ 'is-hidden-doc': hideFrameDoc }"
        :src="frameSrc"
        frameborder="no"
        @load="onFrameLoad"
      />
      <transition name="inner-link-fade">
        <div v-if="showStageBOverlay" class="inner-link__overlay" :class="'is-' + overlayPhase">
          <template v-if="overlayPhase === 'loading'">
            <i class="el-icon-loading" />
            <p>子系统加载中：正在打开业务页面…</p>
            <small>仅等待页面文档到达；业务内容由子系统自行渲染</small>
          </template>
          <template v-else-if="overlayPhase === 'slow'">
            <i class="el-icon-warning-outline" />
            <p>子系统响应较慢：页面文档尚未打开</p>
            <small>可继续等待，或重试打开</small>
            <div class="inner-link__actions">
              <el-button size="small" type="primary" @click="reloadIframe">重试</el-button>
              <el-button size="small" @click="continueWaiting">继续等待</el-button>
            </div>
          </template>
          <template v-else>
            <i class="el-icon-circle-close" />
            <p>子系统不可用：未能加载页面文档</p>
            <small>请检查子系统服务是否在线，或点击重试</small>
            <div class="inner-link__actions">
              <el-button size="small" type="primary" @click="reloadIframe">重试</el-button>
              <el-button size="small" @click="goPortalHome">返回门户</el-button>
            </div>
          </template>
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
import { ensureLocalCamstarCookie, seedCamstarCookieForUrlInBackground } from '@/utils/camstarCookie'
import { markCamstarOpen } from '@/utils/camstarOpenDiag'

const SOFT_TIMEOUT_MS = 15000
const HARD_TIMEOUT_MS = 30000
const DIRECT_SOFT_TIMEOUT_MS = 15000
const DIRECT_HARD_TIMEOUT_MS = 60000
const OVERLAY_HOLD_MS = 80

function isPureHttpSrc(url) {
  const s = String(url || '')
  return /^https?:\/\//i.test(s) && s.indexOf('/#/') < 0 && s.indexOf('#') < 0
}

function sameCamstarDocument(a, b) {
  const norm = (u) => {
    try {
      const parsed = new URL(String(u), window.location.href)
      parsed.searchParams.delete('_portal_t')
      parsed.searchParams.delete('_jump_camstar_warm')
      let path = parsed.pathname || '/'
      if (path.length > 1 && path.endsWith('/')) {
        path = path.slice(0, -1)
      }
      return `${parsed.origin}${path}`.toLowerCase()
    } catch (e) {
      return String(u || '').replace(/\/+$/, '').toLowerCase()
    }
  }
  return norm(a) === norm(b)
}

export default {
  props: {
    src: { type: String, default: '/' },
    iframeId: { type: String, default: '' },
    active: { type: Boolean, default: false },
    clientId: { type: String, default: null },
    directExternal: { type: Boolean, default: false }
  },
  data() {
    return {
      directSrc: '',
      directLoaded: false,
      directPhase: 'idle',
      directSlowDismissed: false,
      directSoftTimer: null,
      directHardTimer: null,
      frameSrc: this.src || '/',
      phase: 'idle',
      hasLoaded: false,
      softTimer: null,
      hardTimer: null,
      holdTimer: null,
      slowDismissed: false,
      _diagLoadStart: 0,
      _reloadSeq: 0
    }
  },
  computed: {
    isDirectHttp() {
      return this.directExternal || isPureHttpSrc(this.src)
    },
    showDirectOverlay() {
      if (!this.active || this.directLoaded) {
        return false
      }
      return this.directPhase === 'loading' || this.directPhase === 'slow' || this.directPhase === 'failed'
    },
    directOverlayPhase() {
      if (this.directPhase === 'slow' || this.directPhase === 'failed') {
        return this.directPhase
      }
      return 'loading'
    },
    hideFrameDoc() {
      return this.active && !this.hasLoaded
    },
    showStageBOverlay() {
      if (!this.active || this.hasLoaded) {
        return false
      }
      return this.phase === 'loading' || this.phase === 'slow' || this.phase === 'failed' || this.phase === 'idle'
    },
    overlayPhase() {
      if (this.phase === 'slow' || this.phase === 'failed') {
        return this.phase
      }
      return 'loading'
    }
  },
  watch: {
    src: {
      immediate: true,
      handler(next) {
        const n = next || '/'
        if (this.isDirectHttp || isPureHttpSrc(n)) {
          this.bindDirectSrc(n)
          return
        }
        if (n === this.frameSrc) {
          return
        }
        this.frameSrc = n
        this.resetLoadState()
        if (this.active) {
          this.beginDocumentLoad()
        }
      }
    },
    active(visible) {
      if (this.isDirectHttp) {
        // 切走/再开：不改 src、不 reload；已加载则直接显示
        if (visible && !this.directLoaded && this.directSrc && this.directSrc !== '/') {
          this.armDirectTimeouts()
        }
        return
      }
      if (visible) {
        if (this.hasLoaded) {
          this.phase = 'idle'
          return
        }
        this.beginDocumentLoad()
      } else {
        this.clearTimers()
        if (!this.hasLoaded && this.phase !== 'failed') {
          this.phase = 'idle'
        }
      }
    }
  },
  mounted() {
    if (this.isDirectHttp) {
      return
    }
    if (!this.src || this.src === '/') {
      if (this.active) {
        this.phase = 'failed'
      }
      return
    }
    if (!this.hasLoaded && this.active) {
      this.beginDocumentLoad()
    }
  },
  beforeDestroy() {
    this.clearTimers()
    this.clearDirectTimers()
  },
  methods: {
    bindDirectSrc(url) {
      const n = url || '/'
      if (!n || n === '/') {
        this.directSrc = ''
        this.directPhase = 'idle'
        return
      }
      if (this.directLoaded && sameCamstarDocument(this.directSrc, n)) {
        return
      }
      if (this.directSrc && sameCamstarDocument(this.directSrc, n) && this.directPhase === 'loading') {
        return
      }
      ensureLocalCamstarCookie()
      seedCamstarCookieForUrlInBackground(n)
      this.directLoaded = false
      this.directSlowDismissed = false
      this.directPhase = 'loading'
      this.directSrc = n
      this._diagLoadStart = Date.now()
      this.armDirectTimeouts()
      let tid = 0
      try {
        tid = Number(sessionStorage.getItem('JUMP_CAMSTAR_TRACE') || 0)
      } catch (e) { /* ignore */ }
      markCamstarOpen(tid, 'iframe-mount', { src: n, mode: 'camstar-stable' })
    },
    armDirectTimeouts() {
      this.clearDirectTimers()
      if (this.directLoaded) {
        return
      }
      this.directSoftTimer = setTimeout(() => {
        if (this.directLoaded || this.directPhase === 'failed') {
          return
        }
        if (!this.directSlowDismissed) {
          this.directPhase = 'slow'
        }
      }, DIRECT_SOFT_TIMEOUT_MS)
      this.directHardTimer = setTimeout(() => {
        if (this.directLoaded) {
          return
        }
        this.directPhase = 'failed'
        this.clearDirectTimers()
      }, DIRECT_HARD_TIMEOUT_MS)
    },
    clearDirectTimers() {
      if (this.directSoftTimer) {
        clearTimeout(this.directSoftTimer)
        this.directSoftTimer = null
      }
      if (this.directHardTimer) {
        clearTimeout(this.directHardTimer)
        this.directHardTimer = null
      }
    },
    onDirectLoad() {
      if (!this.directSrc || this.directSrc === 'about:blank') {
        return
      }
      this.clearDirectTimers()
      this.directLoaded = true
      this.directPhase = 'idle'
      const cost = this._diagLoadStart ? (Date.now() - this._diagLoadStart) : -1
      let tid = 0
      try {
        tid = Number(sessionStorage.getItem('JUMP_CAMSTAR_TRACE') || 0)
      } catch (e) { /* ignore */ }
      markCamstarOpen(tid, 'iframe-load', {
        src: this.directSrc,
        docCostMs: cost,
        note: 'onload 收遮罩；之后变慢多半是 Camstar 页内接口'
      })
    },
    continueDirectWaiting() {
      this.directSlowDismissed = true
      if (!this.directLoaded && this.directPhase === 'slow') {
        this.directPhase = 'loading'
      }
    },
    beginDocumentLoad() {
      if (this.hasLoaded) {
        return
      }
      this.clearTimers()
      this.phase = 'loading'
      this.slowDismissed = false
      this._diagLoadStart = Date.now()
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
      const current = this.frameSrc || ''
      if (!current || current === 'about:blank') {
        return
      }
      this.clearTimers()
      this.holdTimer = setTimeout(() => {
        this.hasLoaded = true
        this.phase = 'idle'
        this.holdTimer = null
      }, OVERLAY_HOLD_MS)
    },
    continueWaiting() {
      this.slowDismissed = true
      if (!this.hasLoaded && this.phase === 'slow') {
        this.phase = 'loading'
      }
    },
    reloadIframe() {
      if (this.isDirectHttp) {
        const base = this.src || '/'
        ensureLocalCamstarCookie()
        seedCamstarCookieForUrlInBackground(base)
        this.directLoaded = false
        this.directSlowDismissed = false
        this.directPhase = 'loading'
        const sep = base.indexOf('?') >= 0 ? '&' : '?'
        this.directSrc = `${base}${sep}_portal_t=${Date.now()}`
        this._diagLoadStart = Date.now()
        this.armDirectTimeouts()
        return
      }
      const base = this.src || '/'
      const seq = ++this._reloadSeq
      this.hasLoaded = false
      this.slowDismissed = false
      this.clearTimers()
      this.phase = 'loading'
      this.frameSrc = 'about:blank'
      this.$nextTick(() => {
        if (seq !== this._reloadSeq) {
          return
        }
        const sep = base.indexOf('?') >= 0 ? '&' : '?'
        this.frameSrc = `${base}${sep}_portal_t=${Date.now()}`
        if (this.active) {
          this.beginDocumentLoad()
        }
      })
    },
    goPortalHome() {
      this.$router.push({ path: '/index' }).catch(() => {})
    },
    resetLoadState() {
      this.hasLoaded = false
      this.slowDismissed = false
      this.clearTimers()
      this.phase = this.active ? 'loading' : 'idle'
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
      if (this.holdTimer) {
        clearTimeout(this.holdTimer)
        this.holdTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.inner-link-root,
.inner-link {
  position: relative;
  height: 100%;
  min-height: 0;
  background: #f5f7fb;
}
.inner-link-root > .inner-link {
  height: 100%;
}
.inner-link__frame {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
  background: #f5f7fb;
  &.is-hidden-doc {
    visibility: hidden;
  }
}
.inner-link__overlay {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;
  background: #f5f7fb;
  color: #303133;
  i {
    font-size: 28px;
    margin-bottom: 12px;
    color: #409eff;
  }
  p {
    margin: 0 0 6px;
    font-size: 15px;
    font-weight: 600;
  }
  small {
    color: #909399;
    max-width: 360px;
    line-height: 1.5;
  }
  &.is-slow i {
    color: #e6a23c;
  }
  &.is-failed i {
    color: #f56c6c;
  }
}
.inner-link__actions {
  margin-top: 14px;
}
.inner-link-fade-enter-active,
.inner-link-fade-leave-active {
  transition: opacity 0.12s ease;
}
.inner-link-fade-enter,
.inner-link-fade-leave-to {
  opacity: 0;
}
</style>
