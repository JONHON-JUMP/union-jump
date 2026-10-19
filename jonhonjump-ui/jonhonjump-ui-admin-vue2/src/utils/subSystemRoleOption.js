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
