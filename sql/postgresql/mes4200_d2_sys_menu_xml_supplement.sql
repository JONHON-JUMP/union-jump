-- =============================================================================
-- JUMP 主库（PostgreSQL）：补充 XML 中有、D2_SYS_MENU 原表没有的 4200 菜单
-- 来源：docs/MESDB1_D2_SYS_MENU.xlsx  sheet「XML补充菜单」
-- 约定与此前 mes4200_d2_sys_import_to_sub_system 一致：
--   sub_system_id = 3
--   id / parent_id = 420000000 + D2 menu_id（parent 为空则 0）
-- 可重复执行：已存在同 id 则跳过
-- =============================================================================

BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM sub_system WHERE id = 3 AND deleted = 0) THEN
    RAISE EXCEPTION 'sub_system.id=3 不存在，请确认 MES4200 子系统已建好';
  END IF;
END $$;

-- 11263 排产引擎参数配置 (d2_aps_engineConfig)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011263, 3, '排产引擎参数配置', 420002001, 105,
  'http://192.168.240.127:4221/aps/engineConfig/index', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_aps_engineConfig',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011263 AND "deleted" = 0
);

-- 11264 产品全周期标准数据 (d2_aps_productCycle)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011264, 3, '产品全周期标准数据', 420002001, 110,
  'http://192.168.240.127:4221/aps/productCycle/index', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_aps_productCycle',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011264 AND "deleted" = 0
);

-- 11265 超期订单查询 (d2_ExceedOrderIndex)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011265, 3, '超期订单查询', 420002001, 115,
  'http://192.168.240.127:4200/WorkOrder/OrderManager/ExceedOrderIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ExceedOrderIndex',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011265 AND "deleted" = 0
);

-- 11266 工单解限审批 (d2_lantai_keeponfilenew)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011266, 3, '工单解限审批', 420002001, 120,
  'http://192.168.240.127:4221/d2/lantai/keeponfilenew', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_lantai_keeponfilenew',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011266 AND "deleted" = 0
);

-- 11267 待拣选工单导入 (d2_PickTaskImport)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011267, 3, '待拣选工单导入', 420002001, 125,
  'https://192.168.240.127:4200/Base//FunctionalArea/ImportUnpickWorkOrderIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_PickTaskImport',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011267 AND "deleted" = 0
);

-- 11268 在制品工单详情 (d2_workorder_detail)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011268, 3, '在制品工单详情', 420002001, 130,
  'http://192.168.240.127:4221/workorder/manage/detail', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_workorder_detail',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011268 AND "deleted" = 0
);

-- 11269 在制品工单管理 (d2_workorder_manage)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011269, 3, '在制品工单管理', 420002001, 135,
  'http://192.168.240.127:4221/workorder/manage', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_workorder_manage',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011269 AND "deleted" = 0
);

-- 11270 应聘人页面 (d2_applicantIndex_vp)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011270, 3, '应聘人页面', 420002002, 160,
  'http://192.168.240.127:4221/innovation/applicantIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_applicantIndex_vp',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011270 AND "deleted" = 0
);

-- 11271 辅材维护页面 (d2_auxiliary_vp)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011271, 3, '辅材维护页面', 420002002, 165,
  'http://192.168.240.127:4221/auxiliary/index', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_auxiliary_vp',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011271 AND "deleted" = 0
);

-- 11272 辅材领用申请页面 (d2_auxiliaryApply_vp)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011272, 3, '辅材领用申请页面', 420002002, 170,
  'http://192.168.240.127:4221/auxiliary/applyIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_auxiliaryApply_vp',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011272 AND "deleted" = 0
);

-- 11273 辅材领用管理员页面 (d2_auxiliaryCustodian_vp)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011273, 3, '辅材领用管理员页面', 420002002, 175,
  'http://192.168.240.127:4221/auxiliary/custodianIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_auxiliaryCustodian_vp',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011273 AND "deleted" = 0
);

-- 11274 班组人员接收详情 (d2_BzzWorkOrderReceive)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011274, 3, '班组人员接收详情', 420002002, 180,
  'http://192.168.240.127:4200/ProExecute/ProExecuteManager/BzzWorkorderReceive', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_BzzWorkOrderReceive',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011274 AND "deleted" = 0
);

-- 11275 物流线在制订单查询 (d2_LogisExcute_vp)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011275, 3, '物流线在制订单查询', 420002002, 185,
  'http://192.168.240.127:4200/ProExecute/ProExecuteManager/LogtisticsInExcuteIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_LogisExcute_vp',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011275 AND "deleted" = 0
);

-- 11276 打字班/裁线班发料页面 (d2_MaterialIssuance)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011276, 3, '打字班/裁线班发料页面', 420002002, 190,
  'http://192.168.240.127:4200/ProExecute/ProExecuteManager/MaterialIssuanceIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_MaterialIssuance',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011276 AND "deleted" = 0
);

-- 11277 在制订单查询 (d2_OrderInProcess)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011277, 3, '在制订单查询', 420002002, 195,
  'http://192.168.240.127:4200/ProExecute/ProExecuteManager/OrderInProcess', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_OrderInProcess',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011277 AND "deleted" = 0
);

-- 11278 问题处理 (d2_ProblemHandle)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011278, 3, '问题处理', 420002002, 200,
  'http://192.168.240.127:4200/base/problembasicData/ProblemHandleIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ProblemHandle',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011278 AND "deleted" = 0
);

-- 11279 问题提交 (d2_ProblemSubmit)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011279, 3, '问题提交', 420002002, 205,
  'http://192.168.240.127:4200/base/problembasicData/ProblemsubmitIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ProblemSubmit',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011279 AND "deleted" = 0
);

-- 11280 招聘人页面 (d2_recruiterIndex_vp)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011280, 3, '招聘人页面', 420002002, 210,
  'http://192.168.240.127:4221/innovation/recruiterIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_recruiterIndex_vp',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011280 AND "deleted" = 0
);

-- 11281 排产计划 (d2_workorderinfo)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011281, 3, '排产计划', 420002002, 215,
  'http://192.168.240.127:4200/ProExecute/SubmitWorkManager/WorkOrderInfo', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_workorderinfo',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011281 AND "deleted" = 0
);

-- 11282 装配班组物料齐套 (d2_WorkOrderKitting_vp)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011282, 3, '装配班组物料齐套', 420002002, 220,
  'http://192.168.240.127:4200/ProExecute/ProExecuteManager/WorkOrderKittingIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_WorkOrderKitting_vp',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011282 AND "deleted" = 0
);

-- 11283 工艺能力信息维护 (d2_ex_functeam)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011283, 3, '工艺能力信息维护', 420002003, 105,
  'http://192.168.240.127:4221/process/ex/functeam', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ex_functeam',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011283 AND "deleted" = 0
);

-- 11284 无工艺产品页面 (d2_NoProcess)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011284, 3, '无工艺产品页面', 420002003, 110,
  'http://192.168.240.127:4200/Process/ProcessManager/NoProcess', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_NoProcess',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011284 AND "deleted" = 0
);

-- 11285 工艺工序错误页面 (d2_ProcessProcedureError)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011285, 3, '工艺工序错误页面', 420002003, 115,
  'http://192.168.240.127:4200/Process/ProcessManager/ProcessProcedureError', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ProcessProcedureError',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011285 AND "deleted" = 0
);

-- 11286 工艺派工页面 (d2_ProcessSendJob)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011286, 3, '工艺派工页面', 420002003, 120,
  'http://192.168.240.127:4200/Process/ProcessManager/ProcessSendJob', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ProcessSendJob',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011286 AND "deleted" = 0
);

-- 11287 工艺版本错误页面 (d2_ProcessVersionError)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011287, 3, '工艺版本错误页面', 420002003, 125,
  'http://192.168.240.127:4200/Process/ProcessManager/ProcessVersionError', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ProcessVersionError',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011287 AND "deleted" = 0
);

-- 11288 投影布线文件上传 (d2_touying_fileuplod)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011288, 3, '投影布线文件上传', 420002003, 130,
  'http://192.168.240.127:4221/procss/touying/fileupload', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_touying_fileuplod',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011288 AND "deleted" = 0
);

-- 11289 工作订单页面 (d2_WoNoIndex)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011289, 3, '工作订单页面', 420002003, 135,
  'http://192.168.240.127:4200/Process/ProcessManager/WoIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_WoNoIndex',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011289 AND "deleted" = 0
);

-- 11290 工序与检测项关联 (d2_Operationitemlinkindex)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011290, 3, '工序与检测项关联', 420002004, 85,
  'http://192.168.240.127:4200/Base/testitemBasicData/Operationitemlinkindex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_Operationitemlinkindex',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011290 AND "deleted" = 0
);

-- 11291 班组拣选任务 (d2_ConfirmPickTaskOut)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011291, 3, '班组拣选任务', 420002005, 250,
  'https://192.168.240.127:4200/Base/FunctionalArea/TaskGroupByTeam', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ConfirmPickTaskOut',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011291 AND "deleted" = 0
);

-- 11292 裁线人员对应班组若依版 (d2_CroperForTeamRuoYiIndex)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011292, 3, '裁线人员对应班组若依版', 420002005, 255,
  'http://192.168.240.127:4221/assemblyteam/index', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_CroperForTeamRuoYiIndex',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011292 AND "deleted" = 0
);

-- 11293 字典管理 (d2_DictionaryIndex)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011293, 3, '字典管理', 420002005, 260,
  'http://192.168.240.124:9011/Common/Dictionary/DictionaryIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_DictionaryIndex',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011293 AND "deleted" = 0
);

-- 11294 物料确认 (d2_MaterialConfirm)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011294, 3, '物料确认', 420002005, 265,
  'http://192.168.240.127:4200/base/MaterialConfirm/MaterialConfirm', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_MaterialConfirm',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011294 AND "deleted" = 0
);

-- 11295 复合胶配制 (d2_MultGlueConfect)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011295, 3, '复合胶配制', 420002005, 270,
  'http://192.168.240.124:9011/multiglue/multigluelist', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_MultGlueConfect',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011295 AND "deleted" = 0
);

-- 11296 复合胶维护 (d2_MultGlueList)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011296, 3, '复合胶维护', 420002005, 275,
  'http://192.168.240.124:9011/multiglue/multigluelist/MultiGlueMaintenance', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_MultGlueList',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011296 AND "deleted" = 0
);

-- 11297 每日上线率看板 (d2_dayMesRateBoard)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011297, 3, '每日上线率看板', 420002007, 175,
  'http://192.168.240.127:4221/orderManger/mesOnlineRate/dayMesRateBoard', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_dayMesRateBoard',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011297 AND "deleted" = 0
);

-- 11298 异常订单未处理看板 (d2_ErrorWorkOrderUnHandleTimeBoard)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011298, 3, '异常订单未处理看板', 420002007, 180,
  'http://192.168.240.127:4200/SealReport/MPOProductQuality/ErrorWorkOrderUnHandleTimeBoard', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ErrorWorkOrderUnHandleTimeBoard',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011298 AND "deleted" = 0
);

-- 11299 非标工艺看板 (d2_NonStandardProcessBoard)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011299, 3, '非标工艺看板', 420002007, 185,
  'http://192.168.240.127:4200/sealreport/bulletinBoard/NonStandardProcessBoard', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_NonStandardProcessBoard',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011299 AND "deleted" = 0
);

-- 11300 二部上线率报表 (d2_OnLineRateBoard)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011300, 3, '二部上线率报表', 420002007, 190,
  'http://192.168.240.127:4200/SealReport/MPOProductQuality/OnLineRateBoard', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_OnLineRateBoard',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011300 AND "deleted" = 0
);

-- 11301 二部功能区滞留时间看板 (d2_RetentionTimeBoard)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011301, 3, '二部功能区滞留时间看板', 420002007, 195,
  'http://192.168.240.127:4200/SealReport/MPOProductQuality/RetenrionTimeBoard', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_RetentionTimeBoard',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011301 AND "deleted" = 0
);

-- 11302 班组创建工单异常看板 (d2_TeamCreateWorkAbnormalBoard)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011302, 3, '班组创建工单异常看板', 420002007, 200,
  'http://192.168.240.127:4200/SealReport/MPOProductQuality/TeamCreateWorkAbnormalBoard', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_TeamCreateWorkAbnormalBoard',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011302 AND "deleted" = 0
);

-- 11303 待处理问题看板 (d2_unHandleBoard_vp)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011303, 3, '待处理问题看板', 420002007, 205,
  'http://192.168.240.127:4221/unHandleBoard/index', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_unHandleBoard_vp',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011303 AND "deleted" = 0
);

-- 11304 每周上线率看板 (d2_weekMesRateBoard)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011304, 3, '每周上线率看板', 420002007, 210,
  'http://192.168.240.127:4221/orderManger/mesOnlineRate/weekMesRateBoard', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_weekMesRateBoard',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011304 AND "deleted" = 0
);

-- 11305 工时复核审批评审 (d2_process_workHourReviewIndex)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011305, 3, '工时复核审批评审', 420002008, 15,
  'http://192.168.240.127:4221/procss/workHourReview/index', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_process_workHourReviewIndex',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011305 AND "deleted" = 0
);

-- 11306 功能区料盒监控 (d2_AreaBoxMonitor)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011306, 3, '功能区料盒监控', 420002010, 100,
  'http://192.168.240.127:4200/MaterialFlow/StoreIssue/AreaBoxMonitor', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_AreaBoxMonitor',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011306 AND "deleted" = 0
);

-- 11307 库位和料盒信息展示 (D2_BoxInfo)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011307, 3, '库位和料盒信息展示', 420002010, 105,
  'http://192.168.240.127:4200/materialflow/warehousearea/warehouseboxinfo', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'D2_BoxInfo',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011307 AND "deleted" = 0
);

-- 11308 人员工位基础信息维护 (d2_EmployeeWorkCenterJobView)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011308, 3, '人员工位基础信息维护', 420002010, 110,
  'http://192.168.240.127:4200/MaterialFlow/StoreIssue/EmployeeWorkCenterJobView', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_EmployeeWorkCenterJobView',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011308 AND "deleted" = 0
);

-- 11309 试验线流转任务监控 (d2_MoveTaskMonitor)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011309, 3, '试验线流转任务监控', 420002010, 115,
  'http://192.168.240.127:4200/MaterialFlow/StoreIssue/MoveTaskMonitor', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_MoveTaskMonitor',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011309 AND "deleted" = 0
);

-- 11310 库管员发料 (d2_StoreIssue)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011310, 3, '库管员发料', 420002010, 120,
  'http://192.168.240.127:4200/MaterialFlow/StoreIssue/StoreIssue', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_StoreIssue',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011310 AND "deleted" = 0
);

-- 11311 库管员发料(新) (d2_StoreIssue_New)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011311, 3, '库管员发料(新)', 420002010, 125,
  'http://192.168.240.127:4200/MaterialFlow/StoreIssue/MaterialOutStore', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_StoreIssue_New',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011311 AND "deleted" = 0
);

-- 11312 连接器装配班货架详情 (D2_TypingClassShelfInfo)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011312, 3, '连接器装配班货架详情', 420002010, 130,
  'http://192.168.240.127:4200/materialflow/warehousearea/typingclassshelfinfo', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'D2_TypingClassShelfInfo',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011312 AND "deleted" = 0
);

-- 11313 空料盒解绑记录 (d2_UnTieBox)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011313, 3, '空料盒解绑记录', 420002010, 135,
  'http://192.168.240.127:4200/MaterialFlow/StoreIssue/UnTieBoxRecord', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_UnTieBox',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011313 AND "deleted" = 0
);

-- 11314 库区管理 (d2_WarehouseAreaManageIndex)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011314, 3, '库区管理', 420002010, 140,
  'http://192.168.240.127:4200/MaterialFlow/warehousearea/WarehouseAreaManageIndex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_WarehouseAreaManageIndex',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011314 AND "deleted" = 0
);

-- 11315 工单生产进度跟踪 (D2_WorkorderProSchedule)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011315, 3, '工单生产进度跟踪', 420002010, 145,
  'http://192.168.240.127:4200/materialflow/storeissue/workorderproschedule', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'D2_WorkorderProSchedule',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011315 AND "deleted" = 0
);

-- 11316 工装基础信息 (d2_ToolsBasicInfo)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011316, 3, '工装基础信息', 420002012, 75,
  'http://192.168.240.13:9071/tools/toolbasicinfo/toolbasicinfoindex', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ToolsBasicInfo',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011316 AND "deleted" = 0
);

-- 11317 工装借用信息(新) (tool_borrow_new)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011317, 3, '工装借用信息(新)', 420002013, 265,
  'http://192.168.240.123:8080/tool/borrowinfo/new', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'tool_borrow_new',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011317 AND "deleted" = 0
);

-- 11318 工装料盒入库 (tool_box_inbound)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011318, 3, '工装料盒入库', 420002013, 270,
  'http://192.168.240.123:8080/tool/box/inbound', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'tool_box_inbound',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011318 AND "deleted" = 0
);

-- 11319 工装柜维护 (tool_cabinet)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011319, 3, '工装柜维护', 420002013, 275,
  'http://192.168.240.123:8080/tool/cabinet', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'tool_cabinet',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011319 AND "deleted" = 0
);

-- 11320 工装分类维护 (tool_classify)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011320, 3, '工装分类维护', 420002013, 280,
  'http://192.168.240.123:8080/tool/baseinfo/classify', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'tool_classify',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011320 AND "deleted" = 0
);

-- 11321 工装入库工作台 (tool_inbound_workbench)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011321, 3, '工装入库工作台', 420002013, 285,
  'http://192.168.240.123:8080/tool/inbound/Workbench', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'tool_inbound_workbench',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011321 AND "deleted" = 0
);

-- 11322 工装拣选工作台 (tool_picking_Workbench)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011322, 3, '工装拣选工作台', 420002013, 290,
  'http://192.168.240.123:8080/tool/picking/Workbench', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'tool_picking_Workbench',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011322 AND "deleted" = 0
);

-- 11323 工装转入 (tool_transfer-in)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011323, 3, '工装转入', 420002013, 295,
  'http://192.168.240.123:8080/tool/transfer-in', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'tool_transfer-in',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011323 AND "deleted" = 0
);

-- 11324 工装转出 (tool_transfer-out)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011324, 3, '工装转出', 420002013, 300,
  'http://192.168.240.123:8080/tool/transfer-out', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'tool_transfer-out',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011324 AND "deleted" = 0
);

-- 11325 工装货架货格 (tool_ware_shelfgrid)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011325, 3, '工装货架货格', 420002013, 305,
  'http://192.168.240.123:8080/tool/ware/shelfgrid', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'tool_ware_shelfgrid',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011325 AND "deleted" = 0
);

-- 11326 测试页面 (d2_ruoyitest_vp)
INSERT INTO "sub_system_menu" (
  "id", "sub_system_id", "menu_name", "parent_id", "order_num",
  "path", "component", "query", "is_cache", "is_frame", "type",
  "visible", "status", "perms", "icon", "always_show", "remark",
  "creator", "create_time", "updater", "update_time", "deleted"
)
SELECT
  420011326, 3, '测试页面', 0, 70,
  'http://192.168.240.127:4221/test', NULL, NULL,
  0, 1, 'C',
  0, 0, NULL, '#',
  0, 'd2_ruoyitest_vp',
  '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1 FROM "sub_system_menu" WHERE "id" = 420011326 AND "deleted" = 0
);

SELECT setval('sub_system_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM "sub_system_menu"), 1));

-- 校验：本次应新增 64 条（重复执行时 already 会变少）
SELECT COUNT(*) AS xml_supplement_menus
FROM "sub_system_menu"
WHERE "sub_system_id" = 3
  AND "deleted" = 0
  AND "id" BETWEEN 420011263 AND 420011326;

COMMIT;
