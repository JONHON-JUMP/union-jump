/**
 * 业务系统角色下拉的展示/禁用规则（业务用户页与用户详情页共用，避免两套逻辑漂移）。
 */

export function subSystemRoleOptionLabel(item, options = {}) {
  if (!item) {
    return ''
  }
  let label = item.name || ''
  if (item.workshopCode) {
    label += '（车间 ' + item.workshopCode + '）'
  }
  if (Number(item.status) === 1) {
    return label + '（已停用）'
  }
  if (options.syncToExternal && !item.externalRoleId) {
    label += '（未关联外部）'
  }
  return label
}

export function isSubSystemRoleOptionDisabled(item, workshopId) {
  if (!item) {
    return false
  }
  if (Number(item.status) === 1) {
    return true
  }
  if (!workshopId || !item.workshopCode) {
    return false
  }
  return String(item.workshopCode) !== String(workshopId)
}

/**
 * 角色下拉只展示当前车间可选的角色：保留 未打车间（如超管）/ 当前车间 的角色，
 * 其它车间的角色直接隐藏（比置灰占位清爽）。
 * 已被用户选中的其它车间角色保留展示：既能回显名称，也让提交前的车间校验能找到它。
 */
export function filterSubSystemRoleOptions(roles, workshopId, selectedRoleIds) {
  const list = roles || []
  if (!workshopId) {
    return list
  }
  const selected = new Set((selectedRoleIds || []).map(id => Number(id)))
  return list.filter(item => {
    if (!item.workshopCode || String(item.workshopCode) === String(workshopId)) {
      return true
    }
    return item.id != null && selected.has(Number(item.id))
  })
}
