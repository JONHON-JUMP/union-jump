import request from '@/utils/request'

export function getLeafApiBindings(bizSubSystemId, workshopCode) {
  return request({
    url: '/system/sub-system-api-binding/list',
    method: 'get',
    params: { bizSubSystemId, workshopCode }
  })
}

export function getLeafApiOptions() {
  return request({
    url: '/system/sub-system-api-binding/options',
    method: 'get'
  })
}

export function saveLeafApiBindings(data) {
  return request({
    url: '/system/sub-system-api-binding/save',
    method: 'put',
    data
  })
}

export function bindingOf(data, scene) {
  const list = (data && data.bindings) || []
  return list.find(item => item.scene === scene) || null
}

export function bindingLabel(binding) {
  if (!binding) {
    return ''
  }
  const systemName = binding.apiSystemName || ''
  const endpointName = binding.endpointName || ''
  if (systemName && endpointName) {
    return systemName + ' / ' + endpointName
  }
  return systemName || endpointName
}
