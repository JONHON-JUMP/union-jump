import { isExternal } from '@/utils/validate'

/**
 * 子系统菜单唯一二分规则（禁止模糊猜测、禁止互踩）：
 *
 * 1) Camstar / 外链：路由地址 path 为 http(s) 完整 URL（组件路径应为空）
 *    → 主系统 iframe 直开该 http，不跑子系统 OAuth（菜单仅挂在子系统树下）
 *
 * 2) 若依：组件路径有值（system/user/index 等），或相对路由地址
 *    → iframe {systemUrl}/#/{路由}，必须 OAuth
 *
 * 配置见：系统管理 → 外部系统菜单（路由地址 / 组件路径）
 */

export function isCamstarLikeUrl(url) {
  const s = String(url || '')
  // 4200 / CamstarPortal：主系统直开页，不是子系统 OAuth 业务
  return /:4200\b/i.test(s) || /\/4200\//i.test(s) || /camstarportal/i.test(s) || /\/camstar\//i.test(s)
}

/** 若依业务组件路径（非 InnerLink / Empty / 门户占位） */
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

export function isPureHttpUrl(url) {
  const s = String(url || '')
  return /^https?:\/\//i.test(s) && s.indexOf('/#/') < 0 && s.indexOf('#') < 0
}

/**
 * @returns {'camstar'|'ruoyi'}
 */
export function classifyPortalMenu({ path, component, link } = {}) {
  // 有若依组件 → 一律若依（即使 path 异样也不走 Camstar）
  if (isRuoyiComponent(component)) {
    return 'ruoyi'
  }
  const p = String(path || '')
  const l = String(link || '')
  // 已是 MES hash → 若依
  if (l.indexOf('/#/') >= 0 || (l.indexOf('#') >= 0 && !isPureHttpUrl(l))) {
    return 'ruoyi'
  }
  // 路由地址 / link 纯 http → Camstar/外链
  if (isPureHttpUrl(p) || isPureHttpUrl(l)) {
    return 'camstar'
  }
  if (isExternal(p) || isCamstarLikeUrl(p) || isCamstarLikeUrl(l)) {
    return 'camstar'
  }
  // 其余相对路由 → 若依
  return 'ruoyi'
}

export function isCamstarPortalMenu(route) {
  return classifyPortalMenu(route) === 'camstar'
}

export function isRuoyiPortalMenu(route) {
  return classifyPortalMenu(route) === 'ruoyi'
}
