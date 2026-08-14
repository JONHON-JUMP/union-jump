-- 用户外部子系统快捷导航配置表（MySQL，新环境建表）
CREATE TABLE IF NOT EXISTS `sub_system_user_quick_nav` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `user_id` bigint NOT NULL COMMENT '主系统用户编号',
    `sub_system_id` bigint NOT NULL COMMENT '外部子系统编号',
    `menu_id` bigint NOT NULL COMMENT '子系统菜单编号',
    `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户外部子系统快捷导航配置表';
