-- ============================================================================
-- 废弃业务车间管理：删除对照表与菜单
-- 执行库：JUMP 主库（PostgreSQL）
-- 说明：用户/角色/班组/岗位车间编号已写在各自表上，不再使用 sub_system_workshop
-- ============================================================================

-- 1. 菜单：业务车间 / 车间对照（按组件路径匹配，含按钮权限子菜单）
UPDATE system_menu
SET deleted = 1,
    updater = 'system',
    update_time = CURRENT_TIMESTAMP
WHERE deleted = 0
  AND (
      component = 'system/subSystem/workshop/index'
      OR path = 'workshop'
      OR permission LIKE 'sub-system:workshop:%'
      OR name IN ('业务车间管理', '车间对照', '业务车间')
  );

-- 子菜单随父菜单 component 已删；再按 permission 清一次按钮
UPDATE system_menu
SET deleted = 1,
    updater = 'system',
    update_time = CURRENT_TIMESTAMP
WHERE deleted = 0
  AND permission LIKE 'sub-system:workshop:%';

-- 2. 删表
DROP TABLE IF EXISTS "sub_system_workshop";
DROP SEQUENCE IF EXISTS sub_system_workshop_seq;
