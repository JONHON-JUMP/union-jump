/** 菜单颜色继承：仅一级菜单可选色，子菜单继承一级菜单颜色 */

export function flattenMenuTree(list, result = []) {
  if (!list || !list.length) {
    return result
  }
  list.forEach(item => {
    result.push(item)
    if (item.children && item.children.length) {
      flattenMenuTree(item.children, result)
    }
  })
  return result
}

export function findFirstLevelMenu(flatMenus, parentId) {
  if (!parentId || Number(parentId) === 0) {
    return null
  }
  let current = flatMenus.find(item => Number(item.id) === Number(parentId))
  while (current && Number(current.parentId) !== 0) {
    current = flatMenus.find(item => Number(item.id) === Number(current.parentId))
  }
  return current || null
}

export function isFirstLevelMenu(parentId) {
  return parentId === 0 || parentId === '0' || parentId == null
}

export function inheritedStyleId(flatMenus, parentId) {
  const firstLevel = findFirstLevelMenu(flatMenus, parentId)
  return firstLevel ? firstLevel.styleId : null
}
