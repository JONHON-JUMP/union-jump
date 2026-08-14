import request from '@/utils/request'

export function listFaq(query) {
  return request({
    url: '/system/faq/page',
    method: 'get',
    params: query
  })
}

export function listFaqWorkbench(query) {
  return request({
    url: '/system/faq/workbench-page',
    method: 'get',
    params: query
  })
}

export function getAppFaq(faqId) {
  return request({
    url: '/system/faq/app-get?id=' + faqId,
    method: 'get'
  })
}

export function getFaq(faqId) {
  return request({
    url: '/system/faq/get?id=' + faqId,
    method: 'get'
  })
}

export function addFaq(data) {
  return request({
    url: '/system/faq/create',
    method: 'post',
    data: data
  })
}

export function updateFaq(data) {
  return request({
    url: '/system/faq/update',
    method: 'put',
    data: data
  })
}

export function publishFaq(id) {
  return request({
    url: '/system/faq/publish?id=' + id,
    method: 'put'
  })
}

export function revokeFaq(id) {
  return request({
    url: '/system/faq/revoke?id=' + id,
    method: 'put'
  })
}

export function delFaq(faqId) {
  return request({
    url: '/system/faq/delete?id=' + faqId,
    method: 'delete'
  })
}

export function delFaqList(ids) {
  return request({
    url: `/system/faq/delete-list?ids=${ids.join(',')}`,
    method: 'delete'
  })
}
