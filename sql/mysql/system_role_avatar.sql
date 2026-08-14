-- 角色系统头像配置表（MySQL）

CREATE TABLE IF NOT EXISTS `system_role_avatar` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
    `role_code`   varchar(64)  NOT NULL                COMMENT '角色标识，对应 system_role.code',
    `avatar_url`  varchar(512) NOT NULL                COMMENT '头像访问 URL',
    `sort`        int          NOT NULL DEFAULT 0      COMMENT '显示排序，越小优先级越高',
    `status`      tinyint      NOT NULL DEFAULT 0      COMMENT '状态（0正常 1停用）',
    `remark`      varchar(255) NULL DEFAULT NULL        COMMENT '备注',
    `creator`     varchar(64)  NULL DEFAULT ''         COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  NULL DEFAULT ''         COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0'   COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_system_role_avatar_role_code` (`role_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色系统头像配置';
