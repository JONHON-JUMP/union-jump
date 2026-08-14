-- 站内信表冗余字段移除（通知内容统一由 system_notice 维护）
ALTER TABLE `system_notify_message`
    DROP COLUMN `title`,
    DROP COLUMN `publisher_name`,
    DROP COLUMN `dept_name`,
    DROP COLUMN `notice_id`,
    DROP COLUMN `attachments`;
