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
      description: sys.systemUrl || '外部系统',
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
  const sys = (systemList || []).find(item => item.clientId === currentSystem)
  return sys ? (sys.clientName || sys.clientId) : currentSystem
}
