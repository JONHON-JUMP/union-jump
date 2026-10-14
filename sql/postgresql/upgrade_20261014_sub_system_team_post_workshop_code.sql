-- ============================================================================
-- 业务班组、业务岗位增加车间编号：与业务系统解耦，不依赖车间对照
-- 执行库：JUMP 主库（PostgreSQL）
-- ============================================================================

ALTER TABLE "sub_system_team" ADD COLUMN IF NOT EXISTS "workshop_code" varchar(50) DEFAULT NULL;
COMMENT ON COLUMN "sub_system_team"."workshop_code"
    IS '车间编号（可空；写在班组自身，不依赖系统车间对照）';

ALTER TABLE "sub_system_post" ADD COLUMN IF NOT EXISTS "workshop_code" varchar(50) DEFAULT NULL;
COMMENT ON COLUMN "sub_system_post"."workshop_code"
    IS '车间编号（可空；写在岗位自身，不依赖系统车间对照）';

-- ============================================================================
-- 业务角色增加车间编号：与业务系统解耦，同步/关联外部用角色自身编号
-- 执行库：JUMP 主库（PostgreSQL）
-- ============================================================================

ALTER TABLE "sub_system_role" ADD COLUMN IF NOT EXISTS "workshop_code" varchar(50) DEFAULT NULL;
COMMENT ON COLUMN "sub_system_role"."workshop_code"
    IS '车间编号（可空；Camstar 同步/关联外部优先用本字段，不依赖系统车间对照）';
