-- 角色默认快捷导航配置表（外部子系统，MySQL）
CREATE TABLE IF NOT EXISTS `sub_system_role_quick_nav` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `role_id` bigint NOT NULL COMMENT '外部子系统角色编号',
    `sub_system_id` bigint NOT NULL COMMENT '外部子系统编号',
    `menu_id` bigint NOT NULL COMMENT '子系统菜单编号',
    `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_sub_system_role_quick_nav_role_id` (`role_id`),
    KEY `idx_sub_system_role_quick_nav_sub_system_id` (`sub_system_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色默认快捷导航配置表（外部子系统）';
