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
// 从主系统用户创建子系统人员（用户管理页联动）
export function createSubSystemEmployeeFromMainUser(data) {
  return request({ url: '/system/sub-system-employee/create-from-main-user', method: 'post', data })
}
// 已配置且启用人员接口的系统列表（用户创建联动下拉）
export function getSubSystemEmployeeEnabledSystems() {
  return request({ url: '/system/sub-system-employee/enabled-systems', method: 'get' })
}
// 联动下拉：按部门过滤车间
export function getSubSystemEmployeeWorkshopOptions(subSystemId, deptId) {
  return request({
    url: '/system/sub-system-employee/workshop-options',
    method: 'get',
    params: { subSystemId, deptId }
  })
}
