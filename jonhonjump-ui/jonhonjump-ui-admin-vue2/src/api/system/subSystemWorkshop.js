import request from '@/utils/request'
import { getSubSystemClientSimpleList } from '@/api/system/subSystemUsers'

export { getSubSystemClientSimpleList }

// 外部系统车间分页
export function getSubSystemWorkshopPage(query) {
  return request({ url: '/system/sub-system-workshop/page', method: 'get', params: query })
}
// 外部系统车间详情
export function getSubSystemWorkshop(id) {
  return request({ url: '/system/sub-system-workshop/get?id=' + id, method: 'get' })
}
// 新增外部系统车间
export function createSubSystemWorkshop(data) {
  return request({ url: '/system/sub-system-workshop/create', method: 'post', data })
}
// 修改外部系统车间
export function updateSubSystemWorkshop(data) {
  return request({ url: '/system/sub-system-workshop/update', method: 'put', data })
}
// 删除外部系统车间
export function deleteSubSystemWorkshop(id) {
  return request({ url: '/system/sub-system-workshop/delete?id=' + id, method: 'delete' })
}
// 批量删除外部系统车间
export function deleteSubSystemWorkshopList(ids) {
  return request({ url: '/system/sub-system-workshop/delete-list?ids=' + ids.join(','), method: 'delete' })
}
// 车间精简列表（人员/用户表单下拉，可按部门过滤）
export function getSubSystemWorkshopSimpleList(subSystemId, deptId) {
  return request({
    url: '/system/sub-system-workshop/simple-list',
    method: 'get',
    params: { subSystemId, deptId }
  })
}
// 按部门查映射车间（用户创建联动）
export function getSubSystemWorkshopByDept(subSystemId, deptId) {
  return request({
    url: '/system/sub-system-workshop/by-dept',
    method: 'get',
    params: { subSystemId, deptId }
  })
}
