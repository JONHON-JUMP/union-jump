import { Message } from 'element-ui'
import store from '@/store'
import { buildQuickNavScopeKey, getQuickNavCache, setQuickNavCache } from '@/utils/portalQuickNavCache'

/** 同页签内最短探测间隔，避免连续切路由打爆接口 */
const MIN_INTERVAL_MS = 5000
let lastCheckAt = 0
let checking = false
/** 每个 scope 上次已知签名；首次只记不提示，避免刚进系统误报 */
const LAST_SIG = Object.create(null)

function normalizeIds(ids) {
  return (ids || [])
    .map(id => Number(id))
    .filter(id => !Number.isNaN(id) && id > 0)
    .sort((a, b) => a - b)
}

export function quickNavSignature(menuIds, lockedMenuIds, apps) {
  const ids = normalizeIds(menuIds).join(',')
  const locked = normalizeIds(lockedMenuIds).join(',')
  // 带上名称/路径：菜单改名、改图标路径时，在线用户也能感知并刷新卡片
  const appPart = (apps || [])
    .map(app => {
      const id = app && (app.menuId != null ? app.menuId : app.id)
      const name = (app && app.name) || ''
      const path = (app && app.path) || ''
      const icon = (app && (app.icon || app.svgIcon)) || ''
      return `${id}:${name}:${path}:${icon}`
    })
    .join(';')
  return `${ids}|${locked}|${appPart}`
}

/** 加载成功后写入基线签名，避免首次探测误报「已更新」 */
export function seedQuickNavSignature(scopeKey, menuIds, lockedMenuIds, apps) {
  if (!scopeKey) {
    return
  }
  LAST_SIG[scopeKey] = quickNavSignature(menuIds, lockedMenuIds, apps)
}

/** @deprecated 使用 seedQuickNavSignature */
export function rememberQuickNavSignature(scopeKey, menuIds, lockedMenuIds, apps) {
  seedQuickNavSignature(scopeKey, menuIds, lockedMenuIds, apps)
}

function resolveSubSystemId(explicitId) {
  if (explicitId != null && Number(explicitId) > 0) {
    return Number(explicitId)
  }
  const current = store.getters.currentSystem
  if (!current || current === 'main') {
    return 0
  }
  const list = store.getters.portalSystemList || store.state.portal.systemList || []
  const hit = list.find(item => item && item.clientId === current)
  // 列表未就绪：返回 null，禁止误打主系统接口
  if (!hit || hit.subSystemId == null) {
    return null
  }
  return Number(hit.subSystemId) || null
}

function scopeKeyOf(subSystemId) {
  return subSystemId > 0 ? buildQuickNavScopeKey('x', subSystemId) : 'main'
}

/**
 * 用户切回页签 / 切路由时：强制拉一次快捷导航；有变化则提示并回写缓存。
 * @returns {Promise<{changed: boolean, config: object}|null>}
 */
export function checkQuickNavUpdateIfNeeded(subSystemId) {
  if (checking) {
    return Promise.resolve(null)
  }
  const now = Date.now()
  if (now - lastCheckAt < MIN_INTERVAL_MS) {
    return Promise.resolve(null)
  }
  const sid = resolveSubSystemId(subSystemId)
  if (sid == null) {
    return Promise.resolve(null)
  }
  lastCheckAt = now
  checking = true

  const scopeKey = scopeKeyOf(sid)
  const hadSig = Object.prototype.hasOwnProperty.call(LAST_SIG, scopeKey)
  const cached = getQuickNavCache(scopeKey) || {}
  const prev = hadSig
    ? LAST_SIG[scopeKey]
    : quickNavSignature(cached.menuIds, cached.lockedMenuIds, cached.apps)

  return store.dispatch('portal/loadQuickNavConfig', {
    subSystemId: sid,
    force: true
  }).then(config => {
    const data = config || {}
    const menuIds = data.menuIds || []
    const lockedMenuIds = data.lockedMenuIds || []
    const configured = !!data.configured
    const apps = Object.prototype.hasOwnProperty.call(data, 'apps')
      ? (Array.isArray(data.apps) ? data.apps : [])
      : null
    const next = quickNavSignature(menuIds, lockedMenuIds, apps)
    setQuickNavCache(scopeKey, menuIds, configured, lockedMenuIds, apps)
    const changed = hadSig && prev !== next
    LAST_SIG[scopeKey] = next
    if (changed) {
      Message.info('快捷导航已更新')
      try {
        const root = store._vm && store._vm.$root
        if (root && root.$emit) {
          root.$emit('portal-quick-nav-changed', {
            menuIds: [...menuIds],
            lockedMenuIds: [...lockedMenuIds],
            configured,
            apps: Array.isArray(apps) ? apps : [],
            scopeKey,
            source: 'watch'
          })
        }
      } catch (e) { /* ignore */ }
    }
    return { changed, config: data }
  }).catch(() => null).finally(() => {
    checking = false
  })
}

function onVisible() {
  if (document.visibilityState === 'visible') {
    checkQuickNavUpdateIfNeeded()
  }
}

let started = false
let removeAfterEach = null

export function startQuickNavWatch(router) {
  stopQuickNavWatch()
  started = true
  document.addEventListener('visibilitychange', onVisible)
  if (router && typeof router.afterEach === 'function') {
    removeAfterEach = router.afterEach(() => {
      checkQuickNavUpdateIfNeeded()
    })
  }
}

export function stopQuickNavWatch() {
  if (!started) {
    return
  }
  started = false
  document.removeEventListener('visibilitychange', onVisible)
  if (typeof removeAfterEach === 'function') {
    removeAfterEach()
    removeAfterEach = null
  }
}
