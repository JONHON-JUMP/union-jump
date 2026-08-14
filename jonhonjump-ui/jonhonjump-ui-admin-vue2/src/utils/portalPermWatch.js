/**
 * 权限变更探测（已按产品策略停用即时反馈）。
 * 角色/菜单变更不在操作过程中提示；仅在「切换系统」时由 portal store
 * （ensureSubSystemReady / resolveSubSystemReload）比对版本并重拉菜单 / 重挂 SSO。
 */

export function startPortalPermWatch() {
  // no-op：不在路由/焦点/可见性变化时探测
}

export function stopPortalPermWatch() {
  // no-op
}
