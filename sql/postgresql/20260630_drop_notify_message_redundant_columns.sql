-- 站内信表冗余字段移除（通知内容统一由 system_notice 维护）
ALTER TABLE system_notify_message
    DROP COLUMN IF EXISTS title,
    DROP COLUMN IF EXISTS publisher_name,
    DROP COLUMN IF EXISTS dept_name,
    DROP COLUMN IF EXISTS notice_id,
    DROP COLUMN IF EXISTS attachments;
