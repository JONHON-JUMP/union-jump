import request from '@/utils/request'

export function getSubSystemUsersByMainUserId(mainUserId) {
  return request({
    url: '/system/sub-system-users/list-by-main-user-id',
    method: 'get',
    params: { mainUserId }
  })
}

export function getMyExternalSystemList() {
  return request({
    url: '/system/sub-system-users/my-list',
    method: 'get',
    // 全局请求：门户入口列表，路由切换不取消
    isGlobal: true
  })
}

// 当前用户在 Camstar 侧的登录用户名（拼接车间时为 车间编号_工号，否则为主登录工号）
export function getMyCamstarUsername() {
  return request({
    url: '/system/sub-system-users/my-camstar-username',
    method: 'get',
    isGlobal: true
  })
}

export function getMyPortalMenus(subSystemId, workshopCode) {
  return request({
    url: '/system/sub-system-users/my-menus',
    method: 'get',
    isGlobal: true,
    params: {
      subSystemId,
      workshopCode: workshopCode || undefined
    }
  })
}

/** 子系统 RBAC 版本（菜单/角色变更递增）；与本地缓存比对，有变化才重拉 my-menus。
 *  独立短超时：JUMP 后端与各子系统共机部署，子系统一慢此接口跟着慢，
 *  它只服务"要不要刷新缓存"的判断，慢了就按本地缓存走，绝不能拖住打开菜单 */
export function getMyPortalMenusVersion(subSystemId) {
  return request({
    url: '/system/sub-system-users/my-menus-version',
    method: 'get',
    isGlobal: true,
    timeout: 8000,
    params: { subSystemId }
  })
}

export function getSubSystemClientSimpleList(portalOnly) {
  return request({
    url: '/system/sub-system-users/client-simple-list',
    method: 'get',
    params: portalOnly === true ? { portalOnly: true } : undefined
  })
}

export function getSubSystemUserPage(query) {
  return request({
    url: '/system/sub-system-users/page',
    method: 'get',
    params: query
  })
}

export function getSubSystemUser(id) {
  return request({
    url: '/system/sub-system-users/get?id=' + id,
    method: 'get'
  })
}

export function createSubSystemUser(data) {
  return request({
    url: '/system/sub-system-users/create',
    method: 'post',
    data: data
  })
}

export function updateSubSystemUser(data) {
  return request({
    url: '/system/sub-system-users/update',
    method: 'put',
    data: data
  })
}

export function deleteSubSystemUser(id) {
  return request({
    url: '/system/sub-system-users/delete?id=' + id,
    method: 'delete'
  })
}

export function deleteSubSystemUserList(ids) {
  return request({
    url: '/system/sub-system-users/delete-list?ids=' + ids.join(','),
    method: 'delete'
  })
}

export function updateSubSystemUserStatus(id, status) {
  return request({
    url: '/system/sub-system-users/update-status?id=' + id + '&status=' + status,
    method: 'put'
  })
}

// 修改人员接口注册状态（0未注册 1已注册）
export function bindExternalEmployee(id, userCode, workshopCode, apiSubSystemId) {
  return request({
    url: '/system/sub-system-users/bind-external-employee',
    method: 'put',
    params: { id, userCode, workshopCode, apiSubSystemId }
  })
}

export function updateSubSystemUserRegisterStatus(id, employeeRegistered) {
  return request({
    url: '/system/sub-system-users/update-register-status?id=' + id + '&employeeRegistered=' + employeeRegistered,
    method: 'put'
  })
}

// 同步域账号/刷卡卡号到业务系统。erpNo 不传则不同步 ERP。data: { id, apiSubSystemId, erpNo? }
export function syncSubSystemUserAccount(data) {
  return request({
    url: '/system/sub-system-users/sync-user-account',
    method: 'post',
    data: data
  })
}

export function getSubSystemRoleSimpleList(subSystemId) {
  return request({
    url: '/system/sub-system-users/role-simple-list',
    method: 'get',
    params: { subSystemId }
  })
}

export function getSubSystemPostSimpleList(subSystemId) {
  return request({
    url: '/system/sub-system-users/post-simple-list',
    method: 'get',
    params: { subSystemId }
  })
}

export function getSubSystemTeamSimpleList(subSystemId, deptId) {
  return request({
    url: '/system/sub-system-users/team-simple-list',
    method: 'get',
    params: { subSystemId, deptId }
  })
}

export function getSubSystemUserHomeMenuTree(subSystemId, roleIds) {
  return request({
    url: '/system/sub-system-users/home-menu-tree-list',
    method: 'get',
    params: {
      subSystemId,
      roleIds: roleIds && roleIds.length > 0 ? roleIds.join(',') : undefined
    }
  })
}

export function getSubSystemUserRoleIds(id) {
  return request({
    url: '/system/sub-system-users/list-role-ids?id=' + id,
    method: 'get'
  })
}

export function assignSubSystemUserRole(data) {
  return request({
    url: '/system/sub-system-users/assign-role',
    method: 'put',
    data: data
  })
}

/** 下载子系统用户导入模板 */
export function importSubSystemUserTemplate() {
  return request({
    url: '/system/sub-system-users/get-import-template',
    method: 'get',
    responseType: 'blob'
  })
}

/** 按用户名查询子系统用户（可选车间叶子） */
export function getSubSystemUserByUsername(subSystemId, username, workshopId) {
  return request({
    url: '/system/sub-system-users/get-by-username',
    method: 'get',
    params: {
      subSystemId,
      username,
      workshopId: workshopId || undefined
    }
  })
}

