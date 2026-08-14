-- 菜单样式主数据表（MySQL）
-- 门户图标仅使用主色 color（底色）+ 白色图标

DROP TABLE IF EXISTS `system_menu_style`;
CREATE TABLE `system_menu_style` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name` varchar(50) NOT NULL COMMENT '样式名称',
    `shape` varchar(20) NOT NULL DEFAULT 'rounded' COMMENT '图标形状',
    `color` varchar(7) NOT NULL COMMENT '主色 HEX',
    `mes_category` varchar(32) NULL COMMENT 'MES 大类编码',
    `remark` varchar(500) NULL COMMENT '适用场景说明',
    `sort` int NOT NULL DEFAULT 0 COMMENT '显示排序',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态 0启用 1禁用',
    `creator` varchar(64) NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`, `deleted`)
) ENGINE=InnoDB COMMENT='菜单样式主数据表';

INSERT INTO `system_menu_style` (`id`, `name`, `shape`, `color`, `mes_category`, `remark`, `sort`, `status`, `creator`, `updater`, `deleted`) VALUES
(1, 'JUMP蓝·计划调度', 'rounded', '#087CE5', 'M01-计划调度', '适用于：生产计划、生产调度、订单管理、APS 排程、委外计划等', 1, 0, '1', '1', b'0'),
(2, '产线绿·生产执行', 'rounded', '#0D9F6E', 'M02-生产执行', '适用于：生产执行、报工、在制品 WIP、生产查询/统计、批次追溯等', 2, 0, '1', '1', b'0'),
(3, '精密紫·工艺质量', 'rounded', '#597EF7', 'M03-工艺质量', '适用于：工艺管理、质量管理、来料/过程/出厂检验、SPC、不合格品等', 3, 0, '1', '1', b'0'),
(4, '装备灰·设备工装', 'rounded', '#5D718C', 'M04-设备工装', '适用于：设备管理、工装/工具管理、TPM 维保、模具、OEE、基础设施等', 4, 0, '1', '1', b'0'),
(5, '流转橙·物流物料', 'rounded', '#E88A08', 'M05-物流物料', '适用于：物流管理、物料管理、仓储 WMS、料盒/成品信息、标签打印、配送等', 5, 0, '1', '1', b'0'),
(6, '数据青·看板分析', 'rounded', '#13C2C2', 'M06-数据看板', '适用于：报表看板、数据看板、数据统计、数据采集/查询、监控、KPI 等', 6, 0, '1', '1', b'0'),
(7, '组织紫·人员绩效', 'rounded', '#722ED1', 'M07-人员绩效', '适用于：人员管理、工时管理、班组、技能矩阵、培训、绩效考核等', 7, 0, '1', '1', b'0'),
(8, '平台墨·系统基础', 'rounded', '#10233E', 'M08-系统基础', '适用于：基础数据/信息、系统管理、异常/安灯管理、接口集成等', 8, 0, '1', '1', b'0'),
(9, '警示红·异常告警', 'rounded', '#E5484D', '通用-异常', '适用于：报警管理、异常处理、停线、不合格紧急处置等', 9, 0, '1', '1', b'0'),
(10, '中性蓝·通用默认', 'rounded', '#3A71A8', '通用-默认', '未明确分类的一级菜单默认色', 10, 0, '1', '1', b'0');
