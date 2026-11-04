import request from '@/utils/request'

export function getSubSystemUserQuickNavList(subSystemId, workshopCode) {
  return request({
    url: '/system/sub-system-user/quick-nav/list',
    method: 'get',
    // 全局请求：门户快捷导航，路由切换不取消
    isGlobal: true,
    params: {
      subSystemId,
      workshopCode: workshopCode || undefined
    }
  })
}

export function saveSubSystemUserQuickNav(data) {
  return request({
    url: '/system/sub-system-user/quick-nav/save',
    method: 'put',
    data
  })
}

/** 单项加入（增量，只动这一项；星标开关专用，全量 save 列表不完整时会覆盖丢项） */
export function addSubSystemUserQuickNavMenu(subSystemId, menuId, workshopCode) {
  return request({
    url: '/system/sub-system-user/quick-nav/add-menu',
    method: 'put',
    params: {
      subSystemId,
      menuId,
      workshopCode: workshopCode || undefined
    }
  })
}

/** 单项移除（增量，只动这一项） */
export function removeSubSystemUserQuickNavMenu(subSystemId, menuId, workshopCode) {
  return request({
    url: '/system/sub-system-user/quick-nav/remove-menu',
    method: 'put',
    params: {
      subSystemId,
      menuId,
      workshopCode: workshopCode || undefined
    }
  })
}

export function syncSubSystemUserQuickNavFromRole(subSystemId) {
  return request({
    url: '/system/sub-system-user/quick-nav/sync-from-role',
    method: 'post',
    params: { subSystemId }
  })
}
