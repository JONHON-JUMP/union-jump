/**
 * 快捷导航展示文案：用上级目录名区分同名菜单（如「系统管理 · 用户管理」）
 */

export function formatQuickNavMenuLabel(name, groupName) {
  if (!name) {
    return ''
  }
  if (!groupName || groupName === name) {
    return name
  }
  return `${groupName} · ${name}`
}

export function formatQuickNavSubtitle(groupName, fallback = '快捷导航') {
  return groupName || fallback
}

export function quickNavSearchText(name, groupName) {
  return [name, groupName].filter(Boolean).join(' ').toLowerCase()
}

/**
 * 首页快捷导航：仅当菜单名重复时，用上级目录名作副标题区分
 */
export function applyQuickNavDuplicateLabels(items, fallbackSubtitle = '快捷导航') {
  if (!items || !items.length) {
    return []
  }

  const nameCounts = items.reduce((acc, item) => {
    const name = item.name || ''
    acc[name] = (acc[name] || 0) + 1
    return acc
  }, {})

  return items.map(item => {
    const group = item.group || item.groupName || ''
    const isDuplicate = (nameCounts[item.name] || 0) > 1
    const subtitle = isDuplicate && group ? group : fallbackSubtitle
    return {
      ...item,
      subtitle,
      keywords: quickNavSearchText(item.name, isDuplicate ? group : '')
    }
  })
}

export function quickNavLabelFromItem(item) {
  if (!item) {
    return ''
  }
  const groupName = item.groupName || item.group || ''
  return formatQuickNavMenuLabel(item.name, groupName)
}

/**
 * 从候选菜单树推导上级目录名（纯前端，不依赖后端 groupName 字段）
 */
export function buildCandidateMapWithGroup(nodes, isMenuNode) {
  const map = {}
  if (!nodes || !nodes.length) {
    return map
  }

  function walk(list, parentName = '') {
    list.forEach(node => {
      const groupName = isMenuNode(node) ? parentName : ''
      map[node.id] = { ...node, groupName }
      if (node.children && node.children.length) {
        walk(node.children, node.name || parentName)
      }
    })
  }

  walk(nodes)
  return map
}
