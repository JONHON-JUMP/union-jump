import request from '@/utils/request'
import { getSubSystemClientSimpleList } from '@/api/system/subSystemUsers'

export { getSubSystemClientSimpleList }

// 接口配置列表
export function getSubSystemApiConfigList() {
  return request({ url: '/system/sub-system-api-config/list', method: 'get' })
}
// 接口配置详情
export function getSubSystemApiConfig(id) {
  return request({ url: '/system/sub-system-api-config/get?id=' + id, method: 'get' })
}
// 按外部系统查配置（未配置返回 null）
export function getSubSystemApiConfigBySubSystem(subSystemId) {
  return request({ url: '/system/sub-system-api-config/get-by-sub-system?subSystemId=' + subSystemId, method: 'get' })
}
// 新增配置
export function createSubSystemApiConfig(data) {
  return request({ url: '/system/sub-system-api-config/create', method: 'post', data })
}
// 重命名接入系统显示名
export function renameSubSystemApiAccess(data) {
  return request({ url: '/system/sub-system-api-config/rename-system', method: 'put', data })
}
// 修改配置
export function updateSubSystemApiConfig(data) {
  return request({ url: '/system/sub-system-api-config/update', method: 'put', data })
}
// 删除配置
export function deleteSubSystemApiConfig(id) {
  return request({ url: '/system/sub-system-api-config/delete?id=' + id, method: 'delete' })
}
// 测试连接
export function testSubSystemApiConnection(id) {
  return request({ url: '/system/sub-system-api-config/test-connection?id=' + id, method: 'get' })
}
// 本页调试指定接口
export function testSubSystemApiInvoke(data) {
  return request({ url: '/system/sub-system-api-config/test', method: 'post', data })
}
