import request from '@/utils/request'

// 查询工艺变更通知管理页面的分页数据
export function getRecipeChangeNoticePage(query) {
  return request({
    url: '/rm/recipe-change-log/notice-page',
    method: 'get',
    params: query
  })
}

// 按当前筛选条件导出全部工艺变更通知 Excel
export function exportRecipeChangeNoticeExcel(query) {
  return request({
    url: '/rm/recipe-change-log/export-notice-excel',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

// 查询工艺变更通知的原始 changeContent
export function getRecipeChangeNoticeContent(noticeId) {
  return request({
    url: '/rm/recipe-change-log/notice-content',
    method: 'get',
    params: { noticeId }
  })
}

// 查询指定工艺变更通知的操作日志分页数据
export function getRecipeChangeOperationLogPage(query) {
  return request({
    url: '/rm/recipe-change-log/operation-log-page',
    method: 'get',
    params: query
  })
}

// 对选中的失败通知执行人工重发
export function manualRetryRecipeChangeNotice(noticeIds) {
  return request({
    url: '/rm/recipe-change-log/manual-retry',
    method: 'post',
    data: { noticeIds }
  })
}

// 将选中的异常工艺变更通知批量标记为完成
export function batchMarkCompleteRecipeChangeNotice(noticeIds) {
  return request({
    url: '/rm/recipe-change-log/batch-mark-complete',
    method: 'post',
    data: { noticeIds }
  })
}
