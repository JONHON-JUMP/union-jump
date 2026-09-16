/**
 * 权限/菜单 cache-aside：写时后端清 Redis 并 bump 版本。
 * 前端不轮询、不 WebSocket。只在用户切回页签 / 打开全部应用时比对版本；
 * 失效则从库重拉（加载中），不强制重登。
 */
import store from '@/store'
import { checkPermission } from '@/api/login'
import { getMyPortalMenusVersion } from '@/api/system/subSystemUsers'
import { checkQuickNavUpdateIfNeeded } from '@/utils/portalQuickNavWatch'

let started = false
let checking = false
let onVisibility = null

function emitQuickNavChanged(config) {
  try {
    const data = config || {}
    const root = store._vm && store._vm.$root
    if (!root || !root.$emit) {
      return
    }
    root.$emit('portal-quick-nav-changed', {
      menuIds: [...(data.menuIds || [])],
      lockedMenuIds: [...(data.lockedMenuIds || [])],
      configured: !!data.configured,
      apps: Array.isArray(data.apps) ? data.apps : [],
      scopeKey: 'main',
      source: 'perm-watch'
    })
  } catch (e) { /* ignore */ }
}

async function reloadQuickNavFromDb() {
  const current = store.state.portal && store.state.portal.currentSystem
  if (current && current !== 'main') {
    await checkQuickNavUpdateIfNeeded()
    return
  }
  const config = await store.dispatch('portal/loadQuickNavConfig', {
    subSystemId: 0,
    force: true
  }).catch(() => null)
  if (config) {
    emitQuickNavChanged(config)
  }
}

/** Redis 已失效：GetInfo / 快捷导航走库重建，慢则前端已有「加载中」 */
async function reloadMainFromDb(rbacVersion) {
  // 不再预清菜单树（CLEAR_MENU_TREES / SET_MAIN_SIDEBAR_ROUTERS null / loading 遮罩）：
  // 预清会让全部应用抽屉在打开瞬间整树闪没再整树重渲染（低配 Chrome 82 上表现为「有时超级卡」）。
  // GenerateRoutes 拉到新菜单后是整体替换提交，旧树一直展示到新数据原子替换为止。
  try {
    await store.dispatch('LoadMainMenus', { force: true })
    await reloadQuickNavFromDb()
    if (rbacVersion != null) {
      store.commit('SET_PERM_RBAC_VERSION', rbacVersion)
    }
  } catch (e) {
    console.warn('[portalPermWatch] reload main menus failed:', e)
  }
}

async function reloadMainMenusIfStale() {
  if (checking) {
    return false
  }
  const version = store.state.user.permRbacVersion
  if (version == null) {
    return false
  }
  checking = true
  try {
    const res = await checkPermission(version)
    const data = (res && res.data) || {}
    if (data.alive !== false) {
      if (data.rbacVersion != null) {
        store.commit('SET_PERM_RBAC_VERSION', data.rbacVersion)
      }
      return false
    }
    await reloadMainFromDb(data.rbacVersion)
    return true
  } catch (e) {
    console.warn('[portalPermWatch] check failed:', e)
    return false
  } finally {
    checking = false
  }
}

async function reloadSubSystemIfStale() {
  if (checking) {
    return false
  }
  const portal = store.state.portal
  const current = portal && portal.currentSystem
  if (!current || current === 'main') {
    return false
  }
  const list = portal.systemList || []
  const hit = list.find(item => item && item.clientId === current)
  if (!hit || hit.subSystemId == null) {
    return false
  }
  const key = current
  const localVersion = portal.subSystemRbacVersions[key]
  checking = true
  try {
    const res = await getMyPortalMenusVersion(Number(hit.subSystemId))
    const remote = Number(res && res.data)
    const remoteVersion = Number.isFinite(remote) ? remote : 0
    // 本地还没版本号：只记版本，不重拉（避免当成「失效」反复打库）
    if (localVersion == null) {
      if (portal.loadedSubSystems[key]) {
        store.commit('portal/MARK_SUB_SYSTEM_LOADED', {
          clientId: key,
          signature: portal.subSystemMenuSignatures[key],
          entryPath: portal.subSystemEntryPaths[key],
          rbacVersion: remoteVersion
        })
      }
      return false
    }
    if (Number(localVersion) === remoteVersion) {
      return false
    }
    // 不切 loading 遮罩：抽屉继续展示旧菜单，force 重载完成后原子替换（同主系统 reloadMainFromDb 的处理）
    try {
      await store.dispatch('portal/ensureSubSystemLoaded', {
        clientId: key,
        activate: true,
        force: true
      })
      await store.dispatch('portal/loadQuickNavConfig', {
        subSystemId: Number(hit.subSystemId),
        force: true
      }).catch(() => null)
    } catch (e) {
      console.warn('[portalPermWatch] sub reload failed:', e)
    }
    return true
  } catch (e) {
    console.warn('[portalPermWatch] sub check failed:', e)
    return false
  } finally {
    checking = false
  }
}

function syncIfStale() {
  const current = store.state.portal && store.state.portal.currentSystem
  if (current && current !== 'main') {
    return reloadSubSystemIfStale()
  }
  return reloadMainMenusIfStale()
}

export function startPortalPermWatch() {
  if (started || typeof window === 'undefined') {
    return
  }
  started = true
  // 切回标签页的版本检查做最小间隔节流：频繁切窗时不再每次都发请求（低配机上撞上重建会卡顿一阵）
  let lastVisibilityCheckAt = 0
  onVisibility = () => {
    if (document.visibilityState === 'visible') {
      const now = Date.now()
      if (now - lastVisibilityCheckAt < 30000) {
        return
      }
      lastVisibilityCheckAt = now
      syncIfStale()
    }
  }
  document.addEventListener('visibilitychange', onVisibility)
}

export function stopPortalPermWatch() {
  if (!started) {
    return
  }
  started = false
  if (onVisibility) {
    document.removeEventListener('visibilitychange', onVisibility)
    onVisibility = null
  }
}

/** 打开全部应用：版本过期则 Redis 失效走库 */
export function syncPortalMenusBeforeAllApps() {
  return syncIfStale()
}
