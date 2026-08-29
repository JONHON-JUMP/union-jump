import request from '@/utils/request'
import { getSubSystemClientSimpleList } from '@/api/system/subSystemUsers'

export { getSubSystemClientSimpleList }

// 已配置人员接口的系统列表（页面左侧卡片用；此处复用 client-simple-list，页面里按 apiConfigList 过滤）
export function getSubSystemEmployeePage(query) {
  return request({ url: '/system/sub-system-employee/page', method: 'get', params: query })
}
// 新增子系统人员
export function createSubSystemEmployee(data) {
  return request({ url: '/system/sub-system-employee/create', method: 'post', data })
}
// 修改子系统人员
export function updateSubSystemEmployee(data) {
  return request({ url: '/system/sub-system-employee/update', method: 'put', data })
}
// 删除子系统人员
export function deleteSubSystemEmployee(subSystemId, userCode) {
  return request({
    url: '/system/sub-system-employee/delete?subSystemId=' + subSystemId + '&userCode=' + encodeURIComponent(userCode),
    method: 'delete'
  })
}
// 班组下拉（按车间）
export function getSubSystemEmployeeTeamCombo(subSystemId, workshopCode) {
  return request({
    url: '/system/sub-system-employee/team-combo',
    method: 'get',
    params: { subSystemId, workshopCode }
  })
}
// 删除二次确认提示语
export function getSubSystemEmployeeDeleteTip(subSystemId) {
  return request({ url: '/system/sub-system-employee/delete-tip?subSystemId=' + subSystemId, method: 'get' })
}
// 可选「新增人员」接口目标列表（接口管理中 create 已启用；与花名册系统解耦）
export function getSubSystemRegisterableApis() {
  return request({ url: '/system/sub-system-employee/registerable-apis', method: 'get' })
}
// 花名册人员手动调「新增人员」接口注册（成功自动置已注册，逐项返回结果）
export function registerSubSystemEmployee(data) {
  return request({ url: '/system/sub-system-employee/register-employee', method: 'post', data })
}
