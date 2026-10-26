/**
 * 角色/套餐「分配菜单」树回显 —— 与芋道原版一致。
 *
 * 1. checkStrictly=true 时 setCheckedKeys（含半选父节点 ID）
 * 2. 再 nextTick 后恢复 checkStrictly=false
 *
 * 不要自行过滤「叶子」：菜单下常有按钮，过滤叶子会导致整树空白。
 */

import { SystemMenuTypeEnum } from '@/utils/constants'

/**
 * 叶子勾选时补上祖先目录 id（SQL 只导叶子时，父节点「基础信息」等才能勾上）
 */
export function expandMenuIdsWithAncestors(allMenus, menuIds) {
  const byId = new Map()
  ;(allMenus || []).forEach(m => {
    if (m && m.id != null) {
      byId.set(Number(m.id), m)
    }
  })
  const result = new Set()
  ;(menuIds || []).forEach(id => {
    let cur = Number(id)
    let guard = 0
    while (cur && !Number.isNaN(cur) && cur > 0 && guard++ < 32) {
      if (result.has(cur)) {
        break
      }
      result.add(cur)
      const node = byId.get(cur)
      if (!node) {
        break
      }
      const pid = Number(node.parentId)
      cur = pid > 0 ? pid : 0
    }
  })
  return [...result]
}

/**
 * @param {object} vm Vue 实例
 * @param {object} treeRef el-tree
 * @param {Array} menuIds 接口返回的菜单 ID（原样传入，勿过滤）
 * @param {function(boolean): void} setCheckStrictly
 * @param {function(): void} [onDone]
 * @param {Array} [allMenus] 扁平菜单列表；传入时自动补全父目录 id
 */
export function restoreRoleMenuCheckedKeys(vm, treeRef, menuIds, setCheckStrictly, onDone, allMenus) {
  if (!treeRef || typeof setCheckStrictly !== 'function') {
    return
  }
  const raw = (menuIds || []).map(id => Number(id)).filter(id => !Number.isNaN(id) && id > 0)
  const keys = allMenus && allMenus.length
    ? expandMenuIdsWithAncestors(allMenus, raw)
    : raw
  setCheckStrictly(true)
  vm.$nextTick(() => {
    treeRef.setCheckedKeys([])
    treeRef.setCheckedKeys(keys)
    vm.$nextTick(() => {
      setCheckStrictly(false)
      if (typeof onDone === 'function') {
        onDone()
      }
    })
  })
}

/**
 * 门户子系统角色授权：树只展示目录/菜单，隐藏按钮。
 */
export function filterOutButtonMenus(menus) {
  return (menus || []).filter(item => {
    const type = item && item.type
    return type !== SystemMenuTypeEnum.BUTTON && type !== 3 && type !== 'F'
  })
}

/**
 * 回显时：库里可能存了按钮 ID，映射为页面菜单勾选。
 */
export function pageMenuIdsForRoleRestore(allMenus, roleMenuIds) {
  const idSet = new Set((roleMenuIds || []).map(id => Number(id)).filter(id => !Number.isNaN(id) && id > 0))
  if (!idSet.size) {
    return []
  }
  const list = allMenus || []
  const result = []
  list.forEach(menu => {
    if (!menu || menu.id == null) {
      return
    }
    const type = menu.type
    const isButton = type === SystemMenuTypeEnum.BUTTON || type === 3 || type === 'F'
    if (isButton) {
      return
    }
    const mid = Number(menu.id)
    if (idSet.has(mid)) {
      result.push(mid)
      return
    }
    const hasButtonChild = list.some(child => {
      if (!child || Number(child.parentId) !== mid) {
        return false
      }
      const ct = child.type
      const childBtn = ct === SystemMenuTypeEnum.BUTTON || ct === 3 || ct === 'F'
      return childBtn && idSet.has(Number(child.id))
    })
    if (hasButtonChild) {
      result.push(mid)
    }
  })
  return result
}

/**
 * 快捷导航树回显：接口只存叶子菜单 ID。
 */
export function restoreQuickNavLeafCheckedKeys(vm, treeRef, leafMenuIds, setCheckStrictly, onDone) {
  if (!treeRef || typeof setCheckStrictly !== 'function') {
    return
  }
  const keys = (leafMenuIds || []).map(id => Number(id)).filter(id => !Number.isNaN(id) && id > 0)
  setCheckStrictly(false)
  vm.$nextTick(() => {
    treeRef.setCheckedKeys([])
    keys.forEach(key => {
      const node = treeRef.getNode(key)
      if (!node || (node.data && node.data.disabled)) {
        return
      }
      treeRef.setChecked(key, true, true)
      let parent = node.parent
      while (parent && parent.level > 0) {
        parent.expanded = true
        parent = parent.parent
      }
    })
    vm.$nextTick(() => {
      if (typeof onDone === 'function') {
        onDone()
      }
    })
  })
}
