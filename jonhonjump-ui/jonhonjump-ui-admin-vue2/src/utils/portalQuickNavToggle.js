import { getUserQuickNavList, saveUserQuickNav, addUserQuickNavMenu, removeUserQuickNavMenu } from '@/api/system/user/quickNav'
import { getSubSystemUserQuickNavList, saveSubSystemUserQuickNav, addSubSystemUserQuickNavMenu, removeSubSystemUserQuickNavMenu } from '@/api/system/user/subSystemQuickNav'
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

function scopeKeyOf(subSystemId = 0, workshopCode = '') {
  return subSystemId > 0 ? buildQuickNavScopeKey('x', subSystemId, workshopCode) : 'main'
}

/**
 * 保存前的权威基准：缓存为空（配置未加载完/加载失败被置空）时，先 GET 一次服务端列表。
 * 保存接口是全量覆盖语义（后端先 delete 再插入传入列表），若以空列表为基准，
 * 一次收藏就会把用户已有的全部快捷导航覆盖成只剩一个。
 */
async function ensureAuthoritativeBase(scopeKey, subSystemId = 0, workshopCode = '') {
  const cached = getQuickNavCache(scopeKey)
  if (cached && Array.isArray(cached.menuIds) && cached.menuIds.length) {
    return cached
  }
  try {
    const res = subSystemId > 0
      ? await getSubSystemUserQuickNavList(subSystemId, workshopCode || undefined)
      : await getUserQuickNavList()
    const raw = res && res.data
    if (raw && typeof raw === 'object' && Array.isArray(raw.menuIds)) {
      setQuickNavCache(
        scopeKey,
        normalizeQuickNavMenuIds(raw.menuIds),
        raw.configured !== false,
        normalizeQuickNavMenuIds(raw.lockedMenuIds),
        Array.isArray(raw.apps) ? raw.apps : []
      )
      return getQuickNavCache(scopeKey)
    }
  } catch (e) {
    // GET 失败：保存走同一后端必然也失败，不会产生覆盖，退回传入基准
  }
  return cached || null
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

async function persistMenuIdsUnlocked(menuIds, subSystemId = 0, workshopCode = '') {
  const ids = normalizeQuickNavMenuIds(menuIds)
  const workshop = workshopCode == null ? '' : String(workshopCode).trim()
  const res = subSystemId > 0
    ? await saveSubSystemUserQuickNav({
      subSystemId,
      workshopCode: workshop || undefined,
      menuIds: ids
    })
    : await saveUserQuickNav({ menuIds: ids })
  const config = normalizeSavedConfig(res && res.data, ids)
  setQuickNavCache(scopeKeyOf(subSystemId, workshop), config.menuIds, config.configured, config.lockedMenuIds, config.apps)
  return config
}

/**
 * 保存快捷导航，返回服务端权威结果（含锁定合并后的 menuIds）。
 * 显式全量写入会串行排队；若队列前有其它保存，仍以本次传入列表为准（用于拖拽排序）。
 * 空列表直接拒绝：排序/全量写入不可能为空，空必是异常态，防止误清空用户全部快捷导航。
 */
export function saveQuickNavMenuIds(menuIds, subSystemId = 0, workshopCode = '') {
  const ids = normalizeQuickNavMenuIds(menuIds)
  if (!ids.length) {
    return Promise.reject(new Error('EMPTY_QUICK_NAV_LIST'))
  }
  const workshop = workshopCode == null ? '' : String(workshopCode).trim()
  return withSaveLock(scopeKeyOf(subSystemId, workshop), () => persistMenuIdsUnlocked(ids, subSystemId, workshop))
}

/**
 * 开关单个菜单：在锁内基于最新缓存计算，避免「取消后再收藏」用旧列表把已取消项写回。
 */
/**
 * 调后端单项增删接口（add-menu / remove-menu）：后端只插/删一行，绝不触碰其它项。
 * 星标开/关禁止走全量 save——前端列表不完整时全量覆盖会把用户其它快捷导航全部删掉。
 */
async function persistMenuDelta(subSystemId, workshop, menuId, add) {
  if (subSystemId > 0) {
    return add
      ? addSubSystemUserQuickNavMenu(subSystemId, menuId, workshop || undefined)
      : removeSubSystemUserQuickNavMenu(subSystemId, menuId, workshop || undefined)
  }
  return add ? addUserQuickNavMenu(menuId) : removeUserQuickNavMenu(menuId)
}

export function toggleQuickNavMenu(menuId, menuIds, subSystemId = 0, lockedMenuIds = [], configured = true, displayedMenuIds = [], workshopCode = '') {
  const numId = Number(menuId)
  const workshop = workshopCode == null ? '' : String(workshopCode).trim()
  const scopeKey = scopeKeyOf(subSystemId, workshop)
  return withSaveLock(scopeKey, async () => {
    const cached = await ensureAuthoritativeBase(scopeKey, subSystemId, workshop)
    const currentIds = cached && Array.isArray(cached.menuIds) ? cached.menuIds : (menuIds || [])
    const baseLocked = cached && Array.isArray(cached.lockedMenuIds) ? cached.lockedMenuIds : lockedMenuIds
    const wasIn = isMenuInQuickNav(numId, currentIds)
    if (wasIn && isQuickNavMenuLocked(numId, baseLocked)) {
      throw new Error('ROLE_QUICK_NAV_LOCKED')
    }
    const res = await persistMenuDelta(subSystemId, workshop, numId, !wasIn)
    const fallback = wasIn ? currentIds.filter(id => id !== numId) : [...currentIds, numId]
    const config = normalizeSavedConfig(res && res.data, fallback)
    setQuickNavCache(scopeKey, config.menuIds, config.configured, config.lockedMenuIds, config.apps)
    return config
  })
}

/**
 * 取消单个菜单：锁内基于最新缓存删除，供首页快捷导航取消订阅使用。
 */
export function removeQuickNavMenuId(menuId, menuIds, subSystemId = 0, lockedMenuIds = [], configured = true, displayedMenuIds = [], workshopCode = '') {
  const numId = Number(menuId)
  const workshop = workshopCode == null ? '' : String(workshopCode).trim()
  const scopeKey = scopeKeyOf(subSystemId, workshop)
  return withSaveLock(scopeKey, async () => {
    const cached = await ensureAuthoritativeBase(scopeKey, subSystemId, workshop)
    const currentIds = cached && Array.isArray(cached.menuIds) ? cached.menuIds : (menuIds || [])
    const baseLocked = cached && Array.isArray(cached.lockedMenuIds) ? cached.lockedMenuIds : lockedMenuIds
    if (isQuickNavMenuLocked(numId, baseLocked)) {
      throw new Error('ROLE_QUICK_NAV_LOCKED')
    }
    const res = await persistMenuDelta(subSystemId, workshop, numId, false)
    const config = normalizeSavedConfig(res && res.data, currentIds.filter(id => id !== numId))
    setQuickNavCache(scopeKey, config.menuIds, config.configured, config.lockedMenuIds, config.apps)
    return config
  })
}
