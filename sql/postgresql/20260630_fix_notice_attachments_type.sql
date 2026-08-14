-- 修复 attachments 字段类型：JacksonTypeHandler 写入 varchar/text，与 jsonb 不兼容
ALTER TABLE system_notice
    ALTER COLUMN attachments TYPE text USING attachments::text;
