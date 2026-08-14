import { saveUserQuickNav } from '@/api/system/user/quickNav'
import { saveSubSystemUserQuickNav } from '@/api/system/user/subSystemQuickNav'
import { buildQuickNavScopeKey, getQuickNavCache, setQuickNavCache } from '@/utils/portalQuickNavCache'

/** 同一 scope 下串行保存，避免并发开关把对方结果覆盖掉 */
const saveLocks = Object.create(null)

export function isMenuInQuickNav(menuId, menuIds) {
  if (menuId == null) {
    return false
  }
  const targetId = Number(menuId)
  return (menuIds || []).some(id => Number(id) === targetId)
}

export function isQuickNavMenuLocked(menuId, lockedMenuIds) {
  if (menuId == null) {
    return false
  }
  const targetId = Number(menuId)
  return (lockedMenuIds || []).some(id => Number(id) === targetId)
}

/**
 * 解析当前应持久化的快捷导航 menuId 列表。
 * 未个人配置时，首页可能展示侧边栏兜底菜单，需以当前展示列表为基准再增删。
 */
export function resolveQuickNavMenuIds(menuIds, configured, displayedMenuIds) {
  if (configured) {
    return normalizeQuickNavMenuIds(menuIds)
  }
  const displayed = normalizeQuickNavMenuIds(displayedMenuIds)
  if (displayed.length) {
    return displayed
  }
  return normalizeQuickNavMenuIds(menuIds)
}

export function removeFromQuickNavMenuIds(menuIds, configured, displayedMenuIds, targetMenuId) {
  const base = resolveQuickNavMenuIds(menuIds, configured, displayedMenuIds)
  const target = Number(targetMenuId)
  return base.filter(id => id !== target)
}

function normalizeQuickNavMenuIds(menuIds) {
  return (menuIds || [])
    .map(id => Number(id))
    .filter(id => !Number.isNaN(id) && id > 0)
}

function normalizeSavedConfig(data, fallbackMenuIds) {
  if (data && typeof data === 'object' && Array.isArray(data.menuIds)) {
    return {
      menuIds: normalizeQuickNavMenuIds(data.menuIds),
      configured: data.configured !== false,
      lockedMenuIds: normalizeQuickNavMenuIds(data.lockedMenuIds),
      apps: Array.isArray(data.apps) ? data.apps : []
    }
  }
  return {
    menuIds: normalizeQuickNavMenuIds(fallbackMenuIds),
    configured: true,
    lockedMenuIds: [],
    apps: []
  }
}

function scopeKeyOf(subSystemId = 0) {
  return subSystemId > 0 ? buildQuickNavScopeKey('x', subSystemId) : 'main'
}

function withSaveLock(scopeKey, runner) {
  const prev = saveLocks[scopeKey] || Promise.resolve()
  const task = prev.catch(() => {}).then(runner)
  saveLocks[scopeKey] = task.finally(() => {
    if (saveLocks[scopeKey] === task) {
      delete saveLocks[scopeKey]
    }
  })
  return task
}

async function persistMenuIdsUnlocked(menuIds, subSystemId = 0) {
  const ids = normalizeQuickNavMenuIds(menuIds)
  const res = subSystemId > 0
    ? await saveSubSystemUserQuickNav({ subSystemId, menuIds: ids })
    : await saveUserQuickNav({ menuIds: ids })
  const config = normalizeSavedConfig(res && res.data, ids)
  setQuickNavCache(scopeKeyOf(subSystemId), config.menuIds, config.configured, config.lockedMenuIds, config.apps)
  return config
}

/**
 * 保存快捷导航，返回服务端权威结果（含锁定合并后的 menuIds）。
 * 显式全量写入会串行排队；若队列前有其它保存，仍以本次传入列表为准（用于拖拽排序）。
 */
export function saveQuickNavMenuIds(menuIds, subSystemId = 0) {
  return withSaveLock(scopeKeyOf(subSystemId), () => persistMenuIdsUnlocked(menuIds, subSystemId))
}

/**
 * 开关单个菜单：在锁内基于最新缓存计算，避免「取消后再收藏」用旧列表把已取消项写回。
 */
export function toggleQuickNavMenu(menuId, menuIds, subSystemId = 0, lockedMenuIds = [], configured = true, displayedMenuIds = []) {
  const numId = Number(menuId)
  const scopeKey = scopeKeyOf(subSystemId)
  return withSaveLock(scopeKey, async () => {
    const cached = getQuickNavCache(scopeKey)
    const baseMenuIds = cached && Array.isArray(cached.menuIds) ? cached.menuIds : menuIds
    const baseConfigured = cached ? !!cached.configured : configured
    const baseLocked = cached && Array.isArray(cached.lockedMenuIds) ? cached.lockedMenuIds : lockedMenuIds
    const displayBase = (displayedMenuIds && displayedMenuIds.length)
      ? displayedMenuIds
      : baseMenuIds
    const ids = resolveQuickNavMenuIds(baseMenuIds, baseConfigured, displayBase)
    const index = ids.findIndex(id => id === numId)
    if (index > -1) {
      if (isQuickNavMenuLocked(numId, baseLocked)) {
        throw new Error('ROLE_QUICK_NAV_LOCKED')
      }
      ids.splice(index, 1)
    } else {
      ids.push(numId)
    }
    return persistMenuIdsUnlocked(ids, subSystemId)
  })
}

/**
 * 取消单个菜单：锁内基于最新缓存删除，供首页快捷导航取消订阅使用。
 */
export function removeQuickNavMenuId(menuId, menuIds, subSystemId = 0, lockedMenuIds = [], configured = true, displayedMenuIds = []) {
  const numId = Number(menuId)
  const scopeKey = scopeKeyOf(subSystemId)
  return withSaveLock(scopeKey, async () => {
    const cached = getQuickNavCache(scopeKey)
    const baseMenuIds = cached && Array.isArray(cached.menuIds) ? cached.menuIds : menuIds
    const baseConfigured = cached ? !!cached.configured : configured
    const baseLocked = cached && Array.isArray(cached.lockedMenuIds) ? cached.lockedMenuIds : lockedMenuIds
    if (isQuickNavMenuLocked(numId, baseLocked)) {
      throw new Error('ROLE_QUICK_NAV_LOCKED')
    }
    const displayBase = (displayedMenuIds && displayedMenuIds.length)
      ? displayedMenuIds
      : baseMenuIds
    const ids = removeFromQuickNavMenuIds(baseMenuIds, baseConfigured, displayBase, numId)
    return persistMenuIdsUnlocked(ids, subSystemId)
  })
}
