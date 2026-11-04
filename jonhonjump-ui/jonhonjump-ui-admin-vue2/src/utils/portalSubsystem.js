export function shouldShowPortalDefaultSettings(systemList) {
  return (systemList || []).length >= 1
}

/**
 * 规则默认：仅 1 个外部系统（共 2 个入口）时默认子系统，否则默认统一门户。
 */
export function resolveRuleBasedPortalDefault(systemList) {
  const externals = systemList || []
  if (externals.length === 1) {
    return externals[0].clientId
  }
  return 'main'
}

export function getUniqueSubSystem(systemList) {
  if (!systemList || systemList.length !== 1) {
    return null
  }
  return systemList[0]
}

export function buildSubsystemOptions(systemList, { includeMain = true } = {}) {
  const options = []
  if (includeMain) {
    options.push({
      value: 'main',
      label: '统一门户',
      description: '工作台与授权应用',
      icon: 'el-icon-monitor'
    })
  }
  ;(systemList || []).forEach(sys => {
    options.push({
      value: sys.clientId,
      label: sys.clientName || sys.clientId,
      description: sys.systemUrl || '业务系统',
      icon: 'el-icon-connection',
      logo: sys.logo,
      subSystemId: sys.subSystemId,
      clientId: sys.clientId
    })
  })
  return options
}

export function resolveCurrentSubsystemLabel(currentSystem, systemList) {
  if (currentSystem === 'main') {
    return '统一门户'
  }
  const resolved = resolveClientIdInSystemList(systemList, currentSystem)
  if (!resolved || resolved === 'main') {
    // 会话里的旧 clientId 已不在列表时，勿把裸 id（如 HKMES）当系统名展示
    return '统一门户'
  }
  const sys = (systemList || []).find(item => item.clientId === resolved)
  return sys ? (sys.clientName || sys.clientId) : '统一门户'
}

/**
 * 将会话/缓存中的 clientId 对齐到当前可切换列表。
 * 叶子拆分后旧值（如 HKMES）可能变成 HKMES_5500；对不上则返回 null。
 */
export function resolveClientIdInSystemList(systemList, clientId) {
  if (!clientId || clientId === 'main') {
    return clientId || 'main'
  }
  const list = systemList || []
  if (!list.length) {
    return null
  }
  if (list.some(item => item && item.clientId === clientId)) {
    return clientId
  }
  const key = String(clientId)
  const byMesExact = list.filter(item => item && (
    item.mesClientId === key || String(item.subSystemId) === key
  ))
  if (byMesExact.length === 1) {
    return byMesExact[0].clientId
  }
  if (byMesExact.length > 1) {
    // 多叶子：旧裸 clientId 无法唯一对齐，回主系统，避免误进第一条车间
    return null
  }
  const byPrefix = list.filter(item => {
    if (!item || !item.clientId) {
      return false
    }
    const cid = String(item.clientId)
    const mes = item.mesClientId ? String(item.mesClientId) : ''
    return cid.startsWith(key + '_')
      || (mes && key.startsWith(mes + '_'))
      || (mes && cid.startsWith(mes + '_') && key === mes)
  })
  if (byPrefix.length === 1) {
    return byPrefix[0].clientId
  }
  // 多匹配同样不猜，交给上层回主系统
  return null
}
