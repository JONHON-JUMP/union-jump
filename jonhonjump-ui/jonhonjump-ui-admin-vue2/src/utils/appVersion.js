/**
 * 主系统发版检测：轮询 /version.json；资源 Chunk 加载失败时提示刷新。
 */
import { MessageBox } from 'element-ui'

const POLL_INTERVAL_MS = 5 * 60 * 1000
const VERSION_URL = `${process.env.BASE_URL || '/'}version.json`

let pollTimer = null
let promptShown = false
let localVersion = null
let chunkHandlerInstalled = false

function readBuildId() {
  return process.env.VUE_APP_BUILD_ID || ''
}

function isChunkLoadError(err) {
  if (!err) {
    return false
  }
  const msg = err.message || String(err)
  return /Loading chunk [\w-]+ failed|ChunkLoadError|Loading CSS chunk [\w-]+ failed/i.test(msg)
}

function promptRefresh(reason) {
  if (promptShown) {
    return
  }
  promptShown = true
  const tip = reason === 'chunk'
    ? '页面资源已更新或加载失败，请刷新后继续使用'
    : '系统已更新，请刷新页面以使用新版本'
  MessageBox.confirm(tip, '系统提示', {
    confirmButtonText: '立即刷新',
    cancelButtonText: '稍后',
    type: 'warning',
    closeOnClickModal: false,
    distinguishCancelAndClose: true
  }).then(() => {
    window.location.reload(true)
  }).catch(() => {
    // 允许稍后；下次轮询或再次 chunk 错误可再提示
    setTimeout(() => {
      promptShown = false
    }, 60 * 1000)
  })
}

function fetchRemoteVersion() {
  const url = `${VERSION_URL}${VERSION_URL.indexOf('?') >= 0 ? '&' : '?'}_t=${Date.now()}`
  return fetch(url, { cache: 'no-store', credentials: 'same-origin' })
    .then(res => {
      if (!res.ok) {
        return null
      }
      return res.json()
    })
    .then(data => (data && data.version) ? String(data.version) : null)
    .catch(() => null)
}

function checkVersionOnce() {
  if (!localVersion) {
    return Promise.resolve()
  }
  return fetchRemoteVersion().then(remote => {
    if (remote && remote !== localVersion) {
      promptRefresh('version')
    }
  })
}

/** 安装 ChunkLoadError / 路由懒加载失败监听 */
export function installChunkLoadGuard(router) {
  if (chunkHandlerInstalled) {
    return
  }
  chunkHandlerInstalled = true

  window.addEventListener('unhandledrejection', event => {
    if (isChunkLoadError(event && event.reason)) {
      promptRefresh('chunk')
    }
  })

  window.addEventListener('error', event => {
    if (isChunkLoadError(event && (event.error || event.message))) {
      promptRefresh('chunk')
    }
  }, true)

  if (router && typeof router.onError === 'function') {
    router.onError(err => {
      if (isChunkLoadError(err)) {
        promptRefresh('chunk')
      }
    })
  }
}

/**
 * 启动版本轮询（登录后调用更合适；未登录也可轮询静态 version.json）
 */
export function startAppVersionPoll() {
  localVersion = readBuildId()
  if (!localVersion) {
    // 开发环境可能未注入；首次拉取作为基准
    return fetchRemoteVersion().then(v => {
      localVersion = v
      if (!pollTimer) {
        pollTimer = setInterval(checkVersionOnce, POLL_INTERVAL_MS)
      }
    })
  }
  if (!pollTimer) {
    // 稍延迟首检，避免与首屏抢带宽
    setTimeout(checkVersionOnce, 15000)
    pollTimer = setInterval(checkVersionOnce, POLL_INTERVAL_MS)
  }
  return Promise.resolve()
}

export function stopAppVersionPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}
