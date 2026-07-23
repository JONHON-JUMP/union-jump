/**
 * 操作权限处理
 * Copyright (c) 2019 ruoyi
 */
import store from '@/store'

const all_permission = '*:*:*'
const super_admin = 'super_admin'

function normalizePermissions(permissions) {
  if (!permissions) {
    return []
  }
  const list = Array.isArray(permissions) ? permissions : Object.values(permissions)
  return list.filter(permission => permission && String(permission).trim())
}

function hasPermission(value) {
  const roles = (store.getters && store.getters.roles) || []
  if (roles.includes(super_admin)) {
    return true
  }
  const permissions = normalizePermissions(store.getters && store.getters.permissions)
  if (!value || !value.length) {
    return false
  }
  return permissions.some(permission => {
    return all_permission === permission || value.includes(permission)
  })
}

function applyPermission(el, binding) {
  const { value } = binding
  if (!value || !(value instanceof Array) || value.length === 0) {
    throw new Error('请设置操作权限标签值')
  }
  const permissions = normalizePermissions(store.getters && store.getters.permissions)
  // 权限尚未加载时不隐藏，避免登录后操作栏空白
  if (!permissions.length) {
    el.style.display = ''
    return
  }
  el.style.display = hasPermission(value) ? '' : 'none'
}

export default {
  inserted(el, binding) {
    applyPermission(el, binding)
    el._permissionUnwatch = store.watch(
      () => normalizePermissions(store.getters && store.getters.permissions),
      () => applyPermission(el, binding)
    )
  },
  update(el, binding) {
    applyPermission(el, binding)
  },
  unbind(el) {
    if (el._permissionUnwatch) {
      el._permissionUnwatch()
      el._permissionUnwatch = null
    }
  }
}
