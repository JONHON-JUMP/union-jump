import request from '@/utils/request'

export function createRoleAvatar(data) {
  return request({
    url: '/system/role-avatar/create',
    method: 'post',
    data: data
  })
}

export function updateRoleAvatar(data) {
  return request({
    url: '/system/role-avatar/update',
    method: 'put',
    data: data
  })
}

export function deleteRoleAvatar(id) {
  return request({
    url: '/system/role-avatar/delete?id=' + id,
    method: 'delete'
  })
}

export function deleteRoleAvatarList(ids) {
  return request({
    url: `/system/role-avatar/delete-list?ids=${ids.join(',')}`,
    method: 'delete'
  })
}

export function getRoleAvatar(id) {
  return request({
    url: '/system/role-avatar/get?id=' + id,
    method: 'get'
  })
}

export function getRoleAvatarPage(query) {
  return request({
    url: '/system/role-avatar/page',
    method: 'get',
    params: query
  })
}

export function getRoleAvatarSimpleList() {
  return request({
    url: '/system/role-avatar/simple-list',
    method: 'get'
  })
}
