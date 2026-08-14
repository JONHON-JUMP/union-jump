export const PERSON_CLASS_MAP = {
  '01': '正式员工',
  '02': '外包派遣'
}

export const REGULARIZATION_STATUS_MAP = {
  '01': '试用期',
  '02': '转正'
}

export const EMPLOYMENT_STATUS_MAP = {
  '01': '在职',
  '02': '停职',
  '03': '退二线',
  '04': '已退休'
}

export const DUTY_STATUS_MAP = {
  '01': '在岗',
  '02': '离岗',
  '03': '请假',
  '04': '出差'
}

export function formatCodeLabel(map, value) {
  if (!value) {
    return '-'
  }
  return map[value] || value
}

export function formatPersonClass(value) {
  return formatCodeLabel(PERSON_CLASS_MAP, value)
}

export function formatRegularizationStatus(value) {
  return formatCodeLabel(REGULARIZATION_STATUS_MAP, value)
}

export function formatEmploymentStatus(value) {
  return formatCodeLabel(EMPLOYMENT_STATUS_MAP, value)
}

export function formatDutyStatus(value) {
  return formatCodeLabel(DUTY_STATUS_MAP, value)
}

export function formatErpNos(erpNos) {
  if (!erpNos) {
    return '-'
  }
  if (Array.isArray(erpNos)) {
    return erpNos.join('、')
  }
  return String(erpNos)
}
