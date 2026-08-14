import { buildQuickNavItems } from '@/views/home/quickNavFromRoutes'
import { applyQuickNavDuplicateLabels } from '@/utils/quickNavLabel'
import { resolveMenuIconFields } from '@/utils/menuIcon'

function mapQuickNavApp(item) {
  const iconFields = resolveMenuIconFields(item.svgIcon || item.icon || '', {
    title: item.name,
    path: item.path
  })
  return {
    menuId: item.menuId,
    manualUrl: item.manualUrl || null,
    name: item.name,
    path: item.path,
    svgIcon: iconFields.svgIcon,
    icon: iconFields.icon,
    color: item.color,
    shape: item.shape,
    keywords: item.keywords || item.name,
    external: item.external
  }
}

function mapQuickNavApps(items) {
  return applyQuickNavDuplicateLabels(items).map(item => mapQuickNavApp(item))
}

/**
 * 门户首页快捷导航。
 * - serverApps 为数组（含空）：一律信接口，禁止再等全量侧栏（主 8s / 子 my-menus）
 * - serverApps 为 null/undefined：尚未就绪；仅过渡期用侧栏 id 匹配（不应依赖此路径首屏）
 */
export function buildPortalHomeApps(sidebarRouters, currentSystem, menuIds, configured, serverApps) {
  if (Array.isArray(serverApps)) {
    if (!serverApps.length) {
      return []
    }
    return mapQuickNavApps(serverApps.map(item => ({
      menuId: item.menuId,
      name: item.name,
      path: item.path,
      icon: item.icon,
      svgIcon: item.svgIcon || null,
      color: item.color,
      shape: item.shape,
      manualUrl: item.manualUrl,
      keywords: item.name
    })))
  }
  const ids = menuIds || []
  const fromSelection = mapQuickNavApps(buildQuickNavItems(sidebarRouters, ids))
  if (fromSelection.length) {
    return fromSelection
  }
  // 接口未回 / 无匹配：保持空白，绝不拿整棵侧栏冒充快捷导航
  return []
}
