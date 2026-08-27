import store from '@/store'

/**
 * 菜单管理页保存/删除后：后台刷新门户菜单树，保留内存中的旧树直到新树就绪（避免全部应用闪「无权限」）。
 * @param {{ scope: 'main' | 'sub', clientId?: string, subSystemId?: number }} payload
 */
export function refreshPortalMenusAfterAdminChange(payload = {}) {
  return store.dispatch('portal/refreshMenusAfterAdminChange', payload).catch(() => {})
}
