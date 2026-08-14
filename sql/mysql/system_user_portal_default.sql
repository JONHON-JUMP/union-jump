-- 用户门户默认打开系统配置表
CREATE TABLE IF NOT EXISTS `system_user_portal_default` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `user_id` bigint NOT NULL COMMENT '用户编号',
    `sub_system_id` bigint NULL DEFAULT NULL COMMENT '默认打开的外部子系统编号，NULL 表示统一门户主页',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`, `deleted`),
    KEY `idx_sub_system_id` (`sub_system_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户门户默认打开系统配置表';
