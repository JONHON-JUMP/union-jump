/**
 * 门户子系统菜单/壳状态缓存（sessionStorage）。
 * F5 刷新后恢复 currentSystem / 菜单签名等，菜单树本身刷新后会重新请求 my-menus。
 */
const PORTAL_CACHE_STORAGE_KEY = 'portal_subsystem_cache'

export function loadPersistedPortalCache() {
  try {
    const raw = sessionStorage.getItem(PORTAL_CACHE_STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export function persistPortalCache(snapshot) {
  try {
    sessionStorage.setItem(PORTAL_CACHE_STORAGE_KEY, JSON.stringify(snapshot || {}))
  } catch (e) { /* ignore */ }
}

export function clearPersistedPortalCache() {
  try {
    sessionStorage.removeItem(PORTAL_CACHE_STORAGE_KEY)
  } catch (e) { /* ignore */ }
}
