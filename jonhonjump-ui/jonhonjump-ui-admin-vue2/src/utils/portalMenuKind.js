import { isExternal } from '@/utils/validate'

/**
 * 门户打开页只认菜单「路由地址」：
 * - 路由地址是完整 http(s) URL → iframe 原样打开（Cookie）
 * - 组件路径、组件名称不参与分类、不参与拼链
 *
 * 注意：直开不等于进入系统时预热全部叶子菜单。
 */

export function isCamstarLikeUrl(url) {
  const s = String(url || '')
  return /:4200\b/i.test(s) || /\/4200\//i.test(s) || /camstarportal/i.test(s) || /\/camstar\//i.test(s)
}

/** 若依业务组件路径（登记字段，打开页不用） */
export function isRuoyiComponent(component) {
  const c = String(component || '').trim()
  if (!c) {
    return false
  }
  const lower = c.toLowerCase()
  if (lower === 'innerlink' || lower.includes('empty') || lower.includes('portal/')) {
    return false
  }
  return true
}

/** 任意 http(s) 完整地址（可带 #，仍以路由地址原样打开） */
export function isHttpUrl(url) {
  return /^https?:\/\//i.test(String(url || ''))
}

/** 无 hash 的 http，用于 Camstar 源站预热等 */
export function isPureHttpUrl(url) {
  const s = String(url || '')
  return isHttpUrl(s) && s.indexOf('/#/') < 0 && s.indexOf('#') < 0
}

/**
 * 只看路由地址 / 已算出的 iframe link。component 一律忽略。
 * @returns {'camstar'|'ruoyi'}
 */
export function classifyPortalMenu({ path, link } = {}) {
  const p = String(path || '')
  const l = String(link || '')
  if (isHttpUrl(p) || isHttpUrl(l) || isExternal(p) || isCamstarLikeUrl(p) || isCamstarLikeUrl(l)) {
    return 'camstar'
  }
  return 'ruoyi'
}

export function isCamstarPortalMenu(route) {
  return classifyPortalMenu(route) === 'camstar'
}

export function isRuoyiPortalMenu(route) {
  return classifyPortalMenu(route) === 'ruoyi'
}
