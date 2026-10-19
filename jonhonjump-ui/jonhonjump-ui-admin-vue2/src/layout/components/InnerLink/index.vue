<template>
  <div
    class="inner-link-root"
    :class="{
      'is-front': active,
      'is-frame-hidden': !active && !releaseOnHide,
      'is-parked-hidden': !active && releaseOnHide
    }"
  >
    <!-- Camstar 直链：简单 src + loading；切走不 reload；超时可重试 -->
    <div v-if="isDirectHttp" class="inner-link is-direct">
      <iframe
        :id="iframeId"
        class="inner-link__frame"
        :class="{ 'is-hidden-doc': hideDirectDoc }"
        :src="directSrc"
        frameborder="no"
        scrolling="auto"
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
        scrolling="auto"
        @load="onFrameLoad"
      />
      <transition name="inner-link-fade">
        <div v-if="showStageBOverlay" class="inner-link__overlay" :class="'is-' + overlayPhase">
          <template v-if="overlayPhase === 'loading'">
            <i class="el-icon-loading" />
            <p>业务系统加载中：正在打开业务页面…</p>
            <small>仅等待页面文档到达；业务内容由业务系统自行渲染</small>
          </template>
          <template v-else-if="overlayPhase === 'slow'">
            <i class="el-icon-warning-outline" />
            <p>业务系统响应较慢：页面文档尚未打开</p>
            <small>可继续等待，或重试打开</small>
            <div class="inner-link__actions">
              <el-button size="small" type="primary" @click="reloadIframe">重试</el-button>
              <el-button size="small" @click="continueWaiting">继续等待</el-button>
            </div>
          </template>
          <template v-else>
            <i class="el-icon-circle-close" />
            <p>业务系统不可用：未能加载页面文档</p>
            <small>请检查业务系统服务是否在线，或点击重试</small>
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
import {
  ensureLocalCamstarCookie,
  seedCamstarCookieForUrlInBackground,
  refreshCamstarCookieForSystem
} from '@/utils/camstarCookie'
import { applyIframeHrefToJump } from '@/utils/portalIframeNav'
import { parsePortalClientId } from '@/utils/portalRoute'

const SOFT_TIMEOUT_MS = 15000
const HARD_TIMEOUT_MS = 30000
const DIRECT_SOFT_TIMEOUT_MS = 15000
const DIRECT_HARD_TIMEOUT_MS = 60000
const OVERLAY_HOLD_MS = 80

function isPureHttpSrc(url) {
  const s = String(url || '')
  return /^https?:\/\//i.test(s) && s.indexOf('/#/') < 0 && s.indexOf('#') < 0
}

const DIRECT_ACQUIRE_EVENT = 'jump-direct-frame-acquire'

function frameOrigin(url) {
  try {
    return new URL(String(url || ''), window.location.href).origin
  } catch (e) {
    return ''
  }
}

function sameCamstarDocument(a, b) {
  const norm = (u) => {
    try {
      const parsed = new URL(String(u), window.location.href)
      parsed.searchParams.delete('_portal_t')
      parsed.searchParams.delete('_jump_camstar_warm')
      parsed.searchParams.sort()
      let path = parsed.pathname || '/'
      if (path.length > 1 && path.endsWith('/')) {
        path = path.slice(0, -1)
      }
      return `${parsed.origin}${path}${parsed.search}${parsed.hash}`.toLowerCase()
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
    releaseOnHide: { type: Boolean, default: false },
    clientId: { type: String, default: null },
    directExternal: { type: Boolean, default: false }
  },
  data() {
    return {
      directSrc: '',
      directLoaded: false,
      directPhase: 'idle',
      directSlowDismissed: false,
      directHardDismissed: false,
      directSoftTimer: null,
      directHardTimer: null,
      frameSrc: this.src || '/',
      phase: 'idle',
      hasLoaded: false,
      softTimer: null,
      hardTimer: null,
      holdTimer: null,
      slowDismissed: false,
      hardDismissed: false,
      _reloadSeq: 0,
      _navPollTimer: null,
      _lastReportedHref: '',
      _lastReportedTitle: '',
      _heldSrc: '',
      _bindSeq: 0,
      _acquireToken: ''
    }
  },
  created() {
    this._acquireToken = `direct-${Date.now()}-${Math.random()}`
    this._onDirectAcquire = (event) => {
      const detail = event && event.detail
      if (!detail || detail.token === this._acquireToken || this.active) {
        return
      }
      // 已经打开成功的页面留在内存里，切走再切回不重新加载。
      // 只卸掉还在转圈的帧，避免慢请求占住同一主机的连接。
      if (this.directLoaded) {
        return
      }
      const mine = frameOrigin(this._heldSrc || this.directSrc)
      if (!mine || mine !== detail.origin) {
        return
      }
      this.suspendDirectFrame()
    }
    window.addEventListener(DIRECT_ACQUIRE_EVENT, this._onDirectAcquire)
  },
  computed: {
    isDirectHttp() {
      return this.directExternal || isPureHttpSrc(this.src)
    },
    hideDirectDoc() {
      return this.active && !this.directLoaded
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
        this.resetLoadState()
        if (this.active) {
          this.frameSrc = n
          this.beginDocumentLoad()
        } else {
          // 不可见时不发起加载（加载中帧切走即停）；切回由兜底按 src 重挂
          this.frameSrc = 'about:blank'
        }
      }
    },
    releaseOnHide(parked) {
      if (parked && !this.active && this.isDirectHttp) {
        this.suspendDirectFrame()
      }
    },
    active(visible) {
      if (this.isDirectHttp) {
        // 隐藏帧被停掉后，再打开按原地址重载。同一帧切走再切回且未停，仍不 reload
        if (visible && this._heldSrc && (this.directSrc === 'about:blank' || !this.directSrc)) {
          const held = this._heldSrc
          this._heldSrc = ''
          this.bindDirectSrc(held)
        } else if (visible && !this.directLoaded && this.directSrc && this.directSrc !== '/' && this.directSrc !== 'about:blank') {
          this.armDirectTimeouts()
        } else if (visible && !this.directLoaded && (!this.directSrc || this.directSrc === 'about:blank') && !this._heldSrc && this.src && this.src !== '/') {
          // 挂起时从未加载成功（_heldSrc 已清空）：重开按 src 全新加载，
          // 否则两个分支都不走，帧停在 about:blank 上白屏
          this.bindDirectSrc(this.src)
        }
        if (visible) {
          this.startNavSync()
        } else {
          this.stopNavSync()
          // 保温帧、或加载中切走的帧：一律挂起取消请求。已加载完成的帧保持保活（切回不重载）
          if (this.releaseOnHide || !this.directLoaded) {
            this.suspendDirectFrame()
          }
        }
        return
      }
      if (visible) {
        if (this.hasLoaded) {
          this.phase = 'idle'
          return
        }
        // 失败停帧（about:blank）后切回：重新挂载原地址，否则停在空白
        // bump _reloadSeq 使 reloadIframe 残留的 nextTick 回调失效，避免双重挂载
        if (this.frameSrc === 'about:blank' && this.src && this.src !== '/') {
          this._reloadSeq += 1
          const base = this.src
          const sep = base.indexOf('?') >= 0 ? '&' : '?'
          this.frameSrc = `${base}${sep}_portal_t=${Date.now()}`
        }
        this.beginDocumentLoad()
      } else {
        this.clearTimers()
        if (!this.hasLoaded && this.phase !== 'failed') {
          this.phase = 'idle'
        }
        // 加载中切走：停帧取消请求，切回按 src 重挂；已加载完成的帧保持保活
        if (!this.hasLoaded && this.frameSrc && this.frameSrc !== 'about:blank') {
          this.frameSrc = 'about:blank'
        }
      }
    }
  },
  mounted() {
    // 兜底：保温帧重建时若已带上真实 src，补挂起，确保隐藏帧不发起加载
    if (this.releaseOnHide && !this.active && this.isDirectHttp) {
      this.suspendDirectFrame()
    }
    if (this.active) {
      this.startNavSync()
    }
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
    this._bindSeq += 1
    if (this._onDirectAcquire) {
      window.removeEventListener(DIRECT_ACQUIRE_EVENT, this._onDirectAcquire)
    }
    this.clearTimers()
    this.clearDirectTimers()
    this.stopNavSync()
  },
  methods: {
    bindDirectSrc(url) {
      const n = url || '/'
      if (!n || n === '/') {
        this.directSrc = ''
        this.directPhase = 'idle'
        return
      }
      // 保温帧、或加载中且不可见的帧一律不真实加载：入口直接挂起（about:blank 释放
      // 连接/CPU），不依赖 watch 时序。加载未完成的帧只有当前页签可见时才允许占资源——
      // 切走/关闭即取消请求，杜绝坏页面、慢页面拖住同主机其它菜单
      if (!this.active && (this.releaseOnHide || !this.directLoaded)) {
        this.suspendDirectFrame()
        return
      }
      if (this.directLoaded && sameCamstarDocument(this.directSrc, n)) {
        return
      }
      if (this.directSrc && sameCamstarDocument(this.directSrc, n) && this.directPhase === 'loading') {
        return
      }
      // 按"当前选择的系统"取该用户的对接身份；优先用 my-list 花名册整项 refresh，避免只 ensure 时 map 未齐
      const portalClientId = parsePortalClientId(this.$route.path)
      const portalSys = (this.$store.state.portal.systemList || []).find(s => s && s.clientId === portalClientId)
      if (portalSys) {
        refreshCamstarCookieForSystem(portalSys)
      } else {
        ensureLocalCamstarCookie(n, portalClientId)
      }
      seedCamstarCookieForUrlInBackground(n, portalClientId)
      const origin = frameOrigin(n)
      if (origin) {
        window.dispatchEvent(new CustomEvent(DIRECT_ACQUIRE_EVENT, {
          detail: { origin, token: this._acquireToken }
        }))
      }
      this._heldSrc = ''
      this.directLoaded = false
      this.directSlowDismissed = false
      this.directHardDismissed = false
      this.directPhase = 'loading'
      // 地址立刻挂上。延迟赋值会赶不上 load，遮罩就一直停在「正在打开业务页面」。
      this.directSrc = n
      this.armDirectTimeouts()
    },
    suspendDirectFrame() {
      const held = this._heldSrc || (this.directSrc && this.directSrc !== 'about:blank' ? this.directSrc : '')
      if (!held) {
        return
      }
      this._bindSeq += 1
      // 打开成功过的帧保留地址，重开秒开；从未加载成功（连不通/一直在转圈）的不保留，
      // 重开时按 src 全新加载——关掉的坏页面不得再把挂死状态带回来
      this._heldSrc = this.directLoaded ? held : ''
      this.clearDirectTimers()
      this.directLoaded = false
      this.directPhase = 'idle'
      if (this.directSrc !== 'about:blank') {
        this.directSrc = 'about:blank'
      }
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
        if (this.directLoaded || this.directHardDismissed) {
          return
        }
        this.directPhase = 'failed'
        this.clearDirectTimers()
        // 判失败同时停帧：挂着的文档请求会继续占该主机连接配额，把同源新菜单挤进
        // 队列（表现为别的页面也一直转圈）。停到 about:blank 立即释放，重试按钮会重新挂载
        if (this.directSrc && this.directSrc !== 'about:blank') {
          this.directSrc = 'about:blank'
        }
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
      this.captureIframeHref()
      this.hookIframeHistory()
      this.tryInjectJumpSync()
    },
    continueDirectWaiting() {
      this.directSlowDismissed = true
      // 用户明确接受慢（页面确实能开、只是要几分钟）：豁免硬超时，不得再判死/停帧
      this.directHardDismissed = true
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
      this.softTimer = setTimeout(() => {
        if (!this.active || this.hasLoaded || this.phase === 'failed') {
          return
        }
        if (!this.slowDismissed) {
          this.phase = 'slow'
        }
      }, SOFT_TIMEOUT_MS)
      this.hardTimer = setTimeout(() => {
        if (!this.active || this.hasLoaded || this.hardDismissed) {
          return
        }
        this.phase = 'failed'
        this.clearTimers()
        // 同 direct：判失败即停帧释放连接，勿让挂死请求占坑拖慢同源新页面
        if (this.frameSrc && this.frameSrc !== 'about:blank') {
          this.frameSrc = 'about:blank'
        }
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
        this.captureIframeHref()
        this.hookIframeHistory()
        this.tryInjectJumpSync()
      }, OVERLAY_HOLD_MS)
    },
    continueWaiting() {
      this.slowDismissed = true
      // 同 direct：继续等待 = 用户接受慢，豁免硬超时
      this.hardDismissed = true
      if (!this.hasLoaded && this.phase === 'slow') {
        this.phase = 'loading'
      }
    },
    reloadIframe() {
      if (this.isDirectHttp) {
        const base = this.src || '/'
        const portalClientId = parsePortalClientId(this.$route.path)
        const portalSys = (this.$store.state.portal.systemList || []).find(s => s && s.clientId === portalClientId)
        if (portalSys) {
          refreshCamstarCookieForSystem(portalSys)
        } else {
          ensureLocalCamstarCookie(base, portalClientId)
        }
        seedCamstarCookieForUrlInBackground(base, portalClientId)
        this.directLoaded = false
        this.directSlowDismissed = false
        this.directHardDismissed = false
        this.directPhase = 'loading'
        const sep = base.indexOf('?') >= 0 ? '&' : '?'
        this.directSrc = `${base}${sep}_portal_t=${Date.now()}`
        this.armDirectTimeouts()
        return
      }
      const base = this.src || '/'
      const seq = ++this._reloadSeq
      this.hasLoaded = false
      this.slowDismissed = false
      this.hardDismissed = false
      this.clearTimers()
      this.phase = 'loading'
      this.frameSrc = 'about:blank'
      this.$nextTick(() => {
        // 重试后已切走：不再挂载，切回时由兜底重挂，避免不可见帧又发请求
        if (seq !== this._reloadSeq || !this.active) {
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
      this.$store.dispatch('portal/navigateToPortalHome').catch(() => {})
    },
    getFrameEl() {
      if (this.iframeId) {
        return document.getElementById(this.iframeId)
      }
      return this.$refs.frame || this.$el.querySelector('iframe')
    },
    reportHref(href, title) {
      if (!this.active || !href) {
        return
      }
      if (href === this._lastReportedHref && title === this._lastReportedTitle) {
        return
      }
      this._lastReportedHref = href
      this._lastReportedTitle = title || ''
      applyIframeHrefToJump(this.$router, href, this.clientId, title)
    },
    captureIframeHref() {
      const el = this.getFrameEl()
      if (!el) {
        return
      }
      try {
        const href = el.contentWindow && el.contentWindow.location && el.contentWindow.location.href
        if (href && href.indexOf('about:blank') < 0) {
          let title = ''
          try {
            title = (el.contentDocument && el.contentDocument.title) || ''
          } catch (e2) { /* ignore */ }
          this.reportHref(href, title)
        }
      } catch (e) { /* 跨域读不到，靠 postMessage */ }
    },
    hookIframeHistory() {
      const el = this.getFrameEl()
      if (!el) {
        return
      }
      try {
        const w = el.contentWindow
        if (!w || w.__jumpPortalNavHook) {
          return
        }
        w.__jumpPortalNavHook = true
        const self = this
        const report = function () {
          try {
            let title = ''
            try {
              title = w.document && w.document.title
            } catch (e2) { /* ignore */ }
            self.reportHref(w.location.href, title)
          } catch (e) { /* ignore */ }
        }
        w.addEventListener('hashchange', report)
        w.addEventListener('popstate', report)
        const push = w.history.pushState
        const replace = w.history.replaceState
        w.history.pushState = function () {
          const ret = push.apply(this, arguments)
          report()
          return ret
        }
        w.history.replaceState = function () {
          const ret = replace.apply(this, arguments)
          report()
          return ret
        }
        report()
      } catch (e) { /* 跨域 */ }
    },
    tryInjectJumpSync() {
      const el = this.getFrameEl()
      if (!el) {
        return
      }
      try {
        const doc = el.contentDocument
        if (!doc || doc.getElementById('jump-portal-sync')) {
          return
        }
        const s = doc.createElement('script')
        s.id = 'jump-portal-sync'
        s.src = `${window.location.origin}/jump-portal-sync.js`
        ;(doc.head || doc.documentElement).appendChild(s)
      } catch (e) { /* 跨域：靠 9100 nginx 注入 */ }
    },
    startNavSync() {
      this.stopNavSync()
      this.captureIframeHref()
      this.hookIframeHistory()
      this._navPollTimer = setInterval(() => {
        if (!this.active) {
          return
        }
        this.captureIframeHref()
      }, 400)
    },
    stopNavSync() {
      if (this._navPollTimer) {
        clearInterval(this._navPollTimer)
        this._navPollTimer = null
      }
    },
    resetLoadState() {
      this.hasLoaded = false
      this.slowDismissed = false
      this.hardDismissed = false
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
.inner-link-root.is-front {
  z-index: 1;
}
/* 已打开、但不在前台的帧：不切显隐，只压到下层，保留其滚动/焦点/布局现场。
   显隐切换会让 Camstar 在重新可见时恢复焦点/滚动，造成“数据在但抖一下”。 */
.inner-link-root.is-frame-hidden {
  pointer-events: none;
  z-index: 0;
}
/* 已关闭/保温帧透明隐藏，避免打开新页面时闪出旧页面残影 */
.inner-link-root.is-parked-hidden {
  opacity: 0;
  pointer-events: none;
  z-index: 0;
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
    /* 勿用 visibility:hidden：82 上会露出 iframe 空白文档（黑/白闪一下）；
       用透明叠在浅底上，等 onload 再显示，底下始终是 #f5f7fb */
    opacity: 0;
    pointer-events: none;
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

<style lang="scss">
/* Chrome <90：iframe 占位淡入淡出会在切换瞬间露出底层空白，感知成闪一下 */
html.legacy-anim {
  .inner-link-fade-enter-active,
  .inner-link-fade-leave-active,
  .inner-link-fade-enter,
  .inner-link-fade-leave-to {
    transition: none !important;
    opacity: 1 !important;
  }
}
</style>
