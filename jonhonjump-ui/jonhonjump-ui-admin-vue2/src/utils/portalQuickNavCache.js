const MEMORY_CACHE = Object.create(null)
const SESSION_KEY = 'portal_quick_nav_cache_v3'
const LEGACY_SESSION_KEYS = [
  'portal_quick_nav_cache_v2',
  'portal_quick_nav_cache_v1'
]

function readSessionCache() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch (e) {
    return null
  }
}

function writeSessionCache(all) {
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(all || {}))
  } catch (e) { /* ignore quota */ }
}

/**
 * apps === null：缓存不完整（旧版/未预取），不可用于首屏渲染
 * apps === [] / [...]：服务端已确认，可直接画
 */
function normalizeCacheEntry(entry) {
  if (!entry || typeof entry !== 'object') {
    return null
  }
  return {
    menuIds: [...(entry.menuIds || [])],
    configured: !!entry.configured,
    lockedMenuIds: [...(entry.lockedMenuIds || [])],
    apps: Array.isArray(entry.apps) ? [...entry.apps] : null
  }
}

function hydrateMemoryFromSession() {
  const all = readSessionCache()
  if (!all) {
    return
  }
  Object.keys(all).forEach(scopeKey => {
    if (!MEMORY_CACHE[scopeKey] && all[scopeKey]) {
      MEMORY_CACHE[scopeKey] = normalizeCacheEntry(all[scopeKey])
    }
  })
}

hydrateMemoryFromSession()

export function buildQuickNavScopeKey(currentSystem, subSystemId) {
  if (currentSystem === 'main' || !subSystemId) {
    return 'main'
  }
  return `sub:${subSystemId}`
}

export function getQuickNavCache(scopeKey) {
  if (MEMORY_CACHE[scopeKey]) {
    return MEMORY_CACHE[scopeKey]
  }
  hydrateMemoryFromSession()
  return MEMORY_CACHE[scopeKey] || null
}

/**
 * @param {string} scopeKey
 * @param {Array} menuIds
 * @param {boolean} configured
 * @param {Array} lockedMenuIds
 * @param {Array|null|undefined} apps 传入数组则覆盖；undefined 且 menuIds 未变时保留旧 apps；null 表示不完整
 */
export function setQuickNavCache(scopeKey, menuIds, configured, lockedMenuIds, apps) {
  const prev = MEMORY_CACHE[scopeKey]
  const nextMenuIds = [...(menuIds || [])]
  let nextApps = null
  if (apps === null) {
    nextApps = null
  } else if (Array.isArray(apps)) {
    nextApps = [...apps]
  } else if (
    prev &&
    Array.isArray(prev.apps) &&
    JSON.stringify(prev.menuIds || []) === JSON.stringify(nextMenuIds)
  ) {
    nextApps = [...prev.apps]
  }
  MEMORY_CACHE[scopeKey] = {
    menuIds: nextMenuIds,
    configured: !!configured,
    lockedMenuIds: [...(lockedMenuIds || [])],
    apps: nextApps
  }
  // 不完整缓存不写 session，避免刷新后误用无 apps 的旧数据
  if (nextApps === null) {
    return
  }
  const all = readSessionCache() || {}
  all[scopeKey] = MEMORY_CACHE[scopeKey]
  writeSessionCache(all)
}

export function clearQuickNavCache(scopeKey) {
  delete MEMORY_CACHE[scopeKey]
  const all = readSessionCache() || {}
  delete all[scopeKey]
  writeSessionCache(all)
}

export function clearAllQuickNavCache() {
  Object.keys(MEMORY_CACHE).forEach(key => {
    delete MEMORY_CACHE[key]
  })
  try {
    sessionStorage.removeItem(SESSION_KEY)
    LEGACY_SESSION_KEYS.forEach(key => sessionStorage.removeItem(key))
  } catch (e) { /* ignore */ }
}

/** 缓存是否含可渲染 apps（含空数组） */
export function hasQuickNavApps(cache) {
  return !!(cache && Array.isArray(cache.apps))
}
