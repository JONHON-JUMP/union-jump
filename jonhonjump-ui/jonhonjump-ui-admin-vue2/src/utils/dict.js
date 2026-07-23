/**
 * 数据字典工具类
 */
import store from '@/store'

const requestedTypes = new Set()

export const DICT_TYPE = {
  USER_TYPE: 'user_type',
  COMMON_STATUS: 'common_status',
  TERMINAL: 'terminal',

  // ========== SYSTEM 模块 ==========
  SYSTEM_USER_SEX: 'system_user_sex',
  SYSTEM_MENU_TYPE: 'system_menu_type',
  SYSTEM_ROLE_TYPE: 'system_role_type',
  SYSTEM_DATA_SCOPE: 'system_data_scope',
  SYSTEM_NOTICE_TYPE: 'system_notice_type',
  SYSTEM_NOTICE_STATUS: 'system_notice_status',
  SYSTEM_LOGIN_TYPE: 'system_login_type',
  SYSTEM_LOGIN_RESULT: 'system_login_result',
  SYSTEM_SMS_CHANNEL_CODE: 'system_sms_channel_code',
  SYSTEM_SMS_TEMPLATE_TYPE: 'system_sms_template_type',
  SYSTEM_SMS_SEND_STATUS: 'system_sms_send_status',
  SYSTEM_SMS_RECEIVE_STATUS: 'system_sms_receive_status',
  SYSTEM_ERROR_CODE_TYPE: 'system_error_code_type',
  SYSTEM_OAUTH2_GRANT_TYPE: 'system_oauth2_grant_type',
  SYSTEM_MAIL_SEND_STATUS: 'system_mail_send_status',
  SYSTEM_NOTIFY_TEMPLATE_TYPE: 'system_notify_template_type',
  SYSTEM_FAQ_CATEGORY: 'system_faq_category',

  // ========== INFRA 模块 ==========
  INFRA_BOOLEAN_STRING: 'infra_boolean_string',
  INFRA_REDIS_TIMEOUT_TYPE: 'infra_redis_timeout_type',
  INFRA_JOB_STATUS: 'infra_job_status',
  INFRA_JOB_LOG_STATUS: 'infra_job_log_status',
  INFRA_API_ERROR_LOG_PROCESS_STATUS: 'infra_api_error_log_process_status',
  INFRA_CONFIG_TYPE: 'infra_config_type',
  INFRA_CODEGEN_TEMPLATE_TYPE: 'infra_codegen_template_type',
  INFRA_CODEGEN_FRONT_TYPE: 'infra_codegen_front_type',
  INFRA_CODEGEN_SCENE: 'infra_codegen_scene',
  INFRA_FILE_STORAGE: 'infra_file_storage',
  INFRA_OPERATE_TYPE: 'infra_operate_type',

  // ========== BPM 模块 ==========
  BPM_MODEL_CATEGORY: 'bpm_model_category',
  BPM_MODEL_FORM_TYPE: 'bpm_model_form_type',
  BPM_TASK_ASSIGN_RULE_TYPE: 'bpm_task_candidate_strategy',
  BPM_PROCESS_INSTANCE_STATUS: 'bpm_process_instance_status',
  BPM_PROCESS_INSTANCE_RESULT: 'bpm_process_instance_result',
  BPM_TASK_STATUS: 'bpm_task_status',
  BPM_PROCESS_LISTENER_TYPE: 'bpm_process_listener_type',
  BPM_PROCESS_LISTENER_VALUE_TYPE: 'bpm_process_listener_value_type',
  BPM_TASK_ASSIGN_SCRIPT: 'bpm_task_assign_script',
  BPM_OA_LEAVE_TYPE: 'bpm_oa_leave_type',

  // ========== PAY 模块 ==========
  PAY_CHANNEL_WECHAT_VERSION: 'pay_channel_wechat_version',
  PAY_CHANNEL_CODE: 'pay_channel_code',
  PAY_ORDER_STATUS: 'pay_order_status',
  PAY_REFUND_STATUS: 'pay_refund_status',
  PAY_NOTIFY_STATUS: 'pay_notify_status',
  PAY_NOTIFY_TYPE: 'pay_notify_type',

  // ========== MP 模块 ==========
  MP_AUTO_REPLY_REQUEST_MATCH: 'mp_auto_reply_request_match',
  MP_MESSAGE_TYPE: 'mp_message_type',

  // ========== MALL - PRODUCT 模块 ==========
  PRODUCT_SPU_STATUS: 'product_spu_status',

  // ========== MALL - ORDER 模块 ==========
  TRADE_AFTER_SALE_STATUS: 'trade_after_sale_status',
  TRADE_AFTER_SALE_WAY: 'trade_after_sale_way',
  TRADE_AFTER_SALE_TYPE: 'trade_after_sale_type',
  TRADE_ORDER_TYPE: 'trade_order_type',
  TRADE_ORDER_STATUS: 'trade_order_status',
  TRADE_ORDER_ITEM_AFTER_SALE_STATUS: 'trade_order_item_after_sale_status',

  // ========== MALL - PROMOTION 模块 ==========
  PROMOTION_DISCOUNT_TYPE: 'promotion_discount_type',
  PROMOTION_PRODUCT_SCOPE: 'promotion_product_scope',
  PROMOTION_COUPON_TEMPLATE_VALIDITY_TYPE: 'promotion_coupon_template_validity_type',
  PROMOTION_COUPON_STATUS: 'promotion_coupon_status',
  PROMOTION_COUPON_TAKE_TYPE: 'promotion_coupon_take_type',
  PROMOTION_ACTIVITY_STATUS: 'promotion_activity_status',
  PROMOTION_CONDITION_TYPE: 'promotion_condition_type',
}

function triggerDictTypeLoad(dictType) {
  if (!dictType || requestedTypes.has(dictType)) {
    return
  }
  const cached = store.getters.dict_datas[dictType]
  if (cached && cached.length) {
    return
  }
  requestedTypes.add(dictType)
  store.dispatch('dict/loadDictType', dictType).finally(() => {
    requestedTypes.delete(dictType)
  })
}

/**
 * 获取 dictType 对应的数据字典数组（按需懒加载，渲染期间只读不写）
 */
export function getDictDatas(dictType) {
  triggerDictTypeLoad(dictType)
  return store.getters.dict_datas[dictType] || []
}

export function ensureDictDatas(dictType) {
  return store.dispatch('dict/loadDictType', dictType)
}

export function getDictDatas2(dictType, values) {
  if (values === undefined) {
    return []
  }
  if (!Array.isArray(values)) {
    values = [values]
  }
  const results = []
  for (const value of values) {
    const dict = getDictData(dictType, value)
    if (dict) {
      results.push(dict)
    }
  }
  return results
}

export function getDictData(dictType, value) {
  const dictDatas = getDictDatas(dictType)
  if (!dictDatas || !dictDatas.length) {
    return undefined
  }
  value = value + ''
  for (const dictData of dictDatas) {
    if (dictData.value === value) {
      return dictData
    }
  }
  return undefined
}

export function getDictDataLabel(dictType, value) {
  const dict = getDictData(dictType, value)
  return dict ? dict.label : ''
}
