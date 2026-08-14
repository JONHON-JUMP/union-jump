-- 通知公告：发布人、部门、附件
ALTER TABLE system_notice
    ADD COLUMN IF NOT EXISTS publisher_name varchar(64) NULL,
    ADD COLUMN IF NOT EXISTS dept_name varchar(255) NULL,
    ADD COLUMN IF NOT EXISTS attachments text NULL;

COMMENT ON COLUMN system_notice.publisher_name IS '发布人';
COMMENT ON COLUMN system_notice.dept_name IS '发布部门';
COMMENT ON COLUMN system_notice.attachments IS '附件列表';
