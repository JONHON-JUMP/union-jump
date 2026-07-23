import store from '@/store'

/**
 * 字符权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkPermi(value) {
  if (value && value instanceof Array && value.length > 0) {
    const roles = store.getters && store.getters.roles
    if (roles && roles.includes('super_admin')) {
      return true
    }
    const permissions = normalizePermissions(store.getters && store.getters.permissions)
    const permissionDatas = value
    const all_permission = "*:*:*";

    return permissions.some(permission => {
      return all_permission === permission || permissionDatas.includes(permission)
    })

  } else {
    console.error(`need roles! Like checkPermi="['system:user:add','system:user:edit']"`)
    return false
  }
}

function normalizePermissions(permissions) {
  if (!permissions) {
    return []
  }
  const list = Array.isArray(permissions) ? permissions : Object.values(permissions)
  return list.filter(permission => permission && String(permission).trim())
}

/**
 * 角色权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkRole(value) {
  if (value && value instanceof Array && value.length > 0) {
    const roles = store.getters && store.getters.roles
    const permissionRoles = value
    const super_admin = "admin";

    const hasRole = roles.some(role => {
      return super_admin === role || permissionRoles.includes(role)
    })

    return hasRole;

  } else {
    console.error(`need roles! Like checkRole="['admin','editor']"`)
    return false
  }
}
