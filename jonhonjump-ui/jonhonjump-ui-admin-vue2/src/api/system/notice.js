import request from '@/utils/request'

// 查询公告列表
export function listNotice(query) {
  return request({
    url: '/system/notice/page',
    method: 'get',
    params: query
  })
}

// 查询工作台/我的通知列表（仅已发布）
export function listNoticeWorkbench(query) {
  return request({
    url: '/system/notice/workbench-page',
    method: 'get',
    params: query
  })
}

// 查询通知详情（工作台、我的通知）
export function getAppNotice(noticeId) {
  return request({
    url: '/system/notice/app-get?id=' + noticeId,
    method: 'get'
  })
}

// 查询公告详细
export function getNotice(noticeId) {
  return request({
    url: '/system/notice/get?id=' + noticeId,
    method: 'get'
  })
}

// 新增公告
export function addNotice(data) {
  return request({
    url: '/system/notice/create',
    method: 'post',
    data: data
  })
}

// 修改公告
export function updateNotice(data) {
  return request({
    url: '/system/notice/update',
    method: 'put',
    data: data
  })
}

// 发布
export function publishNotice(id) {
  return request({
    url: '/system/notice/publish?id=' + id,
    method: 'put'
  })
}

// 撤回
export function revokeNotice(id) {
  return request({
    url: '/system/notice/revoke?id=' + id,
    method: 'put'
  })
}

// 删除公告（业务软删除）
export function delNotice(noticeId) {
  return request({
    url: '/system/notice/delete?id=' + noticeId,
    method: 'delete'
  })
}

// 批量删除公告
export function delNoticeList(ids) {
  return request({
    url: `/system/notice/delete-list?ids=${ids.join(',')}`,
    method: 'delete'
  })
}
