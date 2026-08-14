-- 子系统用户花名册：去掉工号/卡号/OA/ERP（这些属主系统用户）
-- PostgreSQL

ALTER TABLE sub_system_users DROP COLUMN IF EXISTS employee_no;
ALTER TABLE sub_system_users DROP COLUMN IF EXISTS card_no;
ALTER TABLE sub_system_users DROP COLUMN IF EXISTS domain_no;
ALTER TABLE sub_system_users DROP COLUMN IF EXISTS erp_nos;
