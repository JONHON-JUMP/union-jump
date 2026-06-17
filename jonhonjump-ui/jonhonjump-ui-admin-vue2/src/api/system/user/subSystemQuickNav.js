import request from '@/utils/request'

export function getSubSystemUserQuickNavList(subSystemId) {
  return request({
    url: '/system/user/sub-system-quick-nav/list',
    method: 'get',
    params: { subSystemId }
  })
}

export function getSubSystemUserQuickNavCandidates(subSystemId) {
  return request({
    url: '/system/user/sub-system-quick-nav/candidate-list',
    method: 'get',
    params: { subSystemId }
  })
}

export function saveSubSystemUserQuickNav(data) {
  return request({
    url: '/system/user/sub-system-quick-nav/save',
    method: 'put',
    data
  })
}
