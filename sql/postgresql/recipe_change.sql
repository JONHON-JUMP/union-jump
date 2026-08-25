-- 工艺变更信息统一接收及分发管理
-- PostgreSQL 建表脚本

-- 工艺变更通知主记录：用于幂等校验、状态追踪、分发重试及回调超时告警。
CREATE TABLE IF NOT EXISTS recipe_change_notice (
    id BIGSERIAL PRIMARY KEY,
    notify_id VARCHAR(64) NOT NULL,
    workshop_code VARCHAR(10) NOT NULL,
    change_content JSONB NOT NULL,
    status SMALLINT NOT NULL,
    retry_count SMALLINT NOT NULL DEFAULT 0,
    max_retry SMALLINT NOT NULL DEFAULT 3,
    error_msg TEXT,
    mq_send_time TIMESTAMP,
    processing_token VARCHAR(64),
    processing_lease_until TIMESTAMP,
    callback_deadline_time TIMESTAMP,
    callback_alarm_time TIMESTAMP,
    callback_alarm_count SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    remark VARCHAR(500) DEFAULT '',
    deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_recipe_change_notice_notify_id UNIQUE (notify_id)
);

-- 兼容已存在的通知主表；先补列，再执行下方字段注释和索引创建。
ALTER TABLE recipe_change_notice ADD COLUMN IF NOT EXISTS processing_token VARCHAR(64);
ALTER TABLE recipe_change_notice ADD COLUMN IF NOT EXISTS processing_lease_until TIMESTAMP;

COMMENT ON TABLE recipe_change_notice IS '工艺变更通知记录表';
COMMENT ON COLUMN recipe_change_notice.notify_id IS 'MPM 通知唯一标识，幂等键';
COMMENT ON COLUMN recipe_change_notice.workshop_code IS '目标车间编码';
COMMENT ON COLUMN recipe_change_notice.change_content IS '工艺变更内容';
COMMENT ON COLUMN recipe_change_notice.status IS '通知状态：5接收成功、10已发送MQ、15发送失败、18MES处理中、20MES处理成功、25MES处理失败、30待人工处理、35已标记完成';
COMMENT ON COLUMN recipe_change_notice.retry_count IS '当前自动重试次数';
COMMENT ON COLUMN recipe_change_notice.max_retry IS '最大自动重试次数';
COMMENT ON COLUMN recipe_change_notice.error_msg IS '最后一次错误信息';
COMMENT ON COLUMN recipe_change_notice.mq_send_time IS '最近一次成功发送 MQ 的时间';
COMMENT ON COLUMN recipe_change_notice.processing_token IS '当前 MES 消费者的处理令牌';
COMMENT ON COLUMN recipe_change_notice.processing_lease_until IS '当前 MES 处理令牌的租约到期时间';
COMMENT ON COLUMN recipe_change_notice.callback_deadline_time IS 'MES 回调超时时间';
COMMENT ON COLUMN recipe_change_notice.callback_alarm_time IS '最近一次回调超时告警时间';
COMMENT ON COLUMN recipe_change_notice.callback_alarm_count IS '回调超时告警次数';

CREATE INDEX IF NOT EXISTS idx_recipe_change_notice_workshop_status_time
    ON recipe_change_notice (workshop_code, status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_recipe_change_notice_retry
    ON recipe_change_notice (status, retry_count)
    WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_recipe_change_notice_callback_timeout
    ON recipe_change_notice (callback_deadline_time, callback_alarm_time)
    WHERE status = 10 AND deleted = 0;
CREATE INDEX IF NOT EXISTS idx_recipe_change_notice_processing_lease
    ON recipe_change_notice (status, processing_lease_until)
    WHERE deleted = 0;

-- 工艺变更操作日志：记录调用、投递、回调、重试、人工处理及超时告警等操作。
CREATE TABLE IF NOT EXISTS recipe_change_operation_log (
    id BIGSERIAL PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    notify_id VARCHAR(64) NOT NULL,
    workshop_code VARCHAR(10) NOT NULL,
    operation_type SMALLINT NOT NULL,
    operation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator VARCHAR(64) NOT NULL,
    operation_result SMALLINT NOT NULL,
    error_msg VARCHAR(500),
    request_params JSONB NOT NULL DEFAULT '{}'::JSONB,
    response_params JSONB NOT NULL DEFAULT '{}'::JSONB,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    remark VARCHAR(500) DEFAULT '',
    deleted SMALLINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE recipe_change_operation_log IS '工艺变更操作日志表';
COMMENT ON COLUMN recipe_change_operation_log.notice_id IS '关联的工艺变更通知记录编号';
COMMENT ON COLUMN recipe_change_operation_log.operation_type IS '操作类型：10 MPM调用、20 MQ消息发送、30消费回调、40手动重试、50定时重试、60批量标记完成、70回调超时告警';
COMMENT ON COLUMN recipe_change_operation_log.operation_result IS '操作结果：10成功、20失败';
COMMENT ON COLUMN recipe_change_operation_log.request_params IS '操作请求参数';
COMMENT ON COLUMN recipe_change_operation_log.response_params IS '操作响应参数';

CREATE INDEX IF NOT EXISTS idx_recipe_change_operation_log_notice_time
    ON recipe_change_operation_log (notice_id, operation_time DESC);
CREATE INDEX IF NOT EXISTS idx_recipe_change_operation_log_notify_workshop
    ON recipe_change_operation_log (notify_id, workshop_code);

-- 工艺变更状态日志：记录通知状态的完整迁移过程。
CREATE TABLE IF NOT EXISTS recipe_change_status_log (
    id BIGSERIAL PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    notify_id VARCHAR(64) NOT NULL,
    from_status SMALLINT,
    to_status SMALLINT NOT NULL,
    change_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trigger_type SMALLINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    remark VARCHAR(500) DEFAULT '',
    deleted SMALLINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE recipe_change_status_log IS '工艺变更状态变更日志表';
COMMENT ON COLUMN recipe_change_status_log.notice_id IS '关联的工艺变更通知记录编号';
COMMENT ON COLUMN recipe_change_status_log.from_status IS '变更前通知状态';
COMMENT ON COLUMN recipe_change_status_log.to_status IS '变更后通知状态';
COMMENT ON COLUMN recipe_change_status_log.trigger_type IS '触发方式：10系统触发、20人工触发';

CREATE INDEX IF NOT EXISTS idx_recipe_change_status_log_notice_time
    ON recipe_change_status_log (notice_id, change_time DESC);
CREATE INDEX IF NOT EXISTS idx_recipe_change_status_log_notify_id
    ON recipe_change_status_log (notify_id);

-- 兼容已部署环境：三张工艺变更表的通知标识均扩容至 64 字符，避免长通知标识写入主表或日志表失败。
ALTER TABLE recipe_change_notice ALTER COLUMN notify_id TYPE VARCHAR(64);
ALTER TABLE recipe_change_status_log ALTER COLUMN notify_id TYPE VARCHAR(64);
ALTER TABLE recipe_change_operation_log ALTER COLUMN notify_id TYPE VARCHAR(64);
