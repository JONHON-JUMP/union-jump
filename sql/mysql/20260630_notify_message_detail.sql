-- 通知公告：发布人、部门、附件
ALTER TABLE `system_notice`
    ADD COLUMN `publisher_name` varchar(64) NULL DEFAULT NULL COMMENT '发布人' AFTER `status`,
    ADD COLUMN `dept_name` varchar(255) NULL DEFAULT NULL COMMENT '发布部门' AFTER `publisher_name`,
    ADD COLUMN `attachments` json NULL COMMENT '附件列表' AFTER `dept_name`;
