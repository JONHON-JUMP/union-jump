import { SystemMenuTypeEnum } from '@/utils/constants'
import { handleTree } from '@/utils/ruoyi'

function isMenuVisible(menu) {
  return menu.visible !== false && menu.visible !== 1
}

function isMenuShownInSidebar(menu, menuMap) {
  if (!isMenuVisible(menu)) {
    return false
  }
  const parentId = menu.parentId
  if (parentId == null || parentId === 0) {
    return true
  }
  const parent = menuMap[parentId]
  if (!parent) {
    return true
  }
  return isMenuShownInSidebar(parent, menuMap)
}

function buildMenuMap(menus) {
  const map = {}
  ;(menus || []).forEach(menu => {
    map[menu.id] = menu
  })
  return map
}

function isMainQuickNavLeaf(menu, roleIdSet, menuMap) {
  return roleIdSet.has(menu.id)
    && menu.type === SystemMenuTypeEnum.MENU
    && isMenuShownInSidebar(menu, menuMap)
}

function isMainTreeNode(menu) {
  return menu.type === SystemMenuTypeEnum.DIR || menu.type === SystemMenuTypeEnum.MENU
}

function hasQuickNavSelectableDescendant(menuId, allMenus, roleIdSet, menuMap) {
  const children = (allMenus || []).filter(menu => menu.parentId === menuId && isMainTreeNode(menu))
  for (const child of children) {
    if (child.type === SystemMenuTypeEnum.MENU && isMainQuickNavLeaf(child, roleIdSet, menuMap)) {
      return true
    }
    if (child.type === SystemMenuTypeEnum.DIR
      && hasQuickNavSelectableDescendant(child.id, allMenus, roleIdSet, menuMap)) {
      return true
    }
  }
  return false
}

function isQuickNavNodeDisabled(menu, allMenus, roleIdSet, menuMap) {
  if (menu.type === SystemMenuTypeEnum.MENU) {
    return !isMainQuickNavLeaf(menu, roleIdSet, menuMap)
  }
  if (menu.type === SystemMenuTypeEnum.DIR) {
    return !hasQuickNavSelectableDescendant(menu.id, allMenus, roleIdSet, menuMap)
  }
  return true
}

function buildRoleQuickNavCheckTree(allMenus, roleMenuIds, isTreeNode) {
  const roleIdSet = new Set(roleMenuIds || [])
  const menuMap = buildMenuMap(allMenus)
  const nodes = (allMenus || [])
    .filter(menu => isTreeNode(menu))
    .map(menu => ({
      id: menu.id,
      name: menu.name,
      parentId: menu.parentId,
      type: menu.type,
      disabled: isQuickNavNodeDisabled(menu, allMenus, roleIdSet, menuMap)
    }))
  return handleTree(nodes, 'id', 'parentId')
}

function walkTreeInOrder(nodes, visitor) {
  ;(nodes || []).forEach(node => {
    visitor(node)
    if (node.children && node.children.length) {
      walkTreeInOrder(node.children, visitor)
    }
  })
}

function isQuickNavSavableNode(node) {
  if (node.disabled) {
    return false
  }
  return node.type === SystemMenuTypeEnum.MENU
}

/**
 * 按树形顺序收集已勾选的快捷导航菜单 ID（仅可保存的叶子菜单）
 */
export function collectOrderedQuickNavMenuIds(menuTree, checkedIds) {
  const checkedSet = new Set((checkedIds || []).map(id => Number(id)).filter(id => id > 0))
  const ordered = []
  walkTreeInOrder(menuTree, node => {
    if (isQuickNavSavableNode(node) && checkedSet.has(Number(node.id))) {
      ordered.push(Number(node.id))
    }
  })
  return ordered
}

/**
 * 主系统：完整菜单树，无角色权限的节点灰色不可选（与菜单权限一致）
 */
export function buildMainRoleQuickNavCheckTree(allMenus, roleMenuIds) {
  return buildRoleQuickNavCheckTree(allMenus, roleMenuIds, isMainTreeNode)
}

/**
 * 外部子系统：完整菜单树，无角色权限的节点灰色不可选
 */
export function buildSubSystemRoleQuickNavCheckTree(allMenus, roleMenuIds) {
  return buildMainRoleQuickNavCheckTree(allMenus, roleMenuIds)
}

export function getMainQuickNavLeafIds(allMenus, roleMenuIds) {
  const roleIdSet = new Set(roleMenuIds || [])
  const menuMap = buildMenuMap(allMenus)
  return (allMenus || [])
    .filter(menu => isMainQuickNavLeaf(menu, roleIdSet, menuMap))
    .map(menu => menu.id)
}

export function getSubSystemQuickNavLeafIds(allMenus, roleMenuIds) {
  return getMainQuickNavLeafIds(allMenus, roleMenuIds)
}
