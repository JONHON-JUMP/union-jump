-- 菜单样式主数据表（PostgreSQL）
-- 门户图标仅使用主色 color（底色）+ 白色图标

CREATE TABLE IF NOT EXISTS system_menu_style (
    id int8 NOT NULL,
    name varchar(50) NOT NULL,
    shape varchar(20) NOT NULL DEFAULT 'rounded',
    color varchar(7) NOT NULL,
    mes_category varchar(32) NULL,
    remark varchar(500) NULL,
    sort int4 NOT NULL DEFAULT 0,
    status int2 NOT NULL DEFAULT 0,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0
);

ALTER TABLE system_menu_style ADD CONSTRAINT pk_system_menu_style PRIMARY KEY (id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_system_menu_style_name ON system_menu_style (name) WHERE deleted = 0;

COMMENT ON TABLE system_menu_style IS '菜单样式主数据表（主色+形状）';
COMMENT ON COLUMN system_menu_style.color IS '主色 HEX，门户图标底色';

INSERT INTO system_menu_style (id, name, shape, color, mes_category, remark, sort, status, creator, updater, deleted) VALUES
(1, 'JUMP蓝·计划调度', 'rounded', '#087CE5', 'M01-计划调度', '适用于：生产计划、生产调度、订单管理、APS 排程、委外计划等', 1, 0, '1', '1', 0),
(2, '产线绿·生产执行', 'rounded', '#0D9F6E', 'M02-生产执行', '适用于：生产执行、报工、在制品 WIP、生产查询/统计、批次追溯等', 2, 0, '1', '1', 0),
(3, '精密紫·工艺质量', 'rounded', '#597EF7', 'M03-工艺质量', '适用于：工艺管理、质量管理、来料/过程/出厂检验、SPC、不合格品等', 3, 0, '1', '1', 0),
(4, '装备灰·设备工装', 'rounded', '#5D718C', 'M04-设备工装', '适用于：设备管理、工装/工具管理、TPM 维保、模具、OEE、基础设施等', 4, 0, '1', '1', 0),
(5, '流转橙·物流物料', 'rounded', '#E88A08', 'M05-物流物料', '适用于：物流管理、物料管理、仓储 WMS、料盒/成品信息、标签打印、配送等', 5, 0, '1', '1', 0),
(6, '数据青·看板分析', 'rounded', '#13C2C2', 'M06-数据看板', '适用于：报表看板、数据看板、数据统计、数据采集/查询、监控、KPI 等', 6, 0, '1', '1', 0),
(7, '组织紫·人员绩效', 'rounded', '#722ED1', 'M07-人员绩效', '适用于：人员管理、工时管理、班组、技能矩阵、培训、绩效考核等', 7, 0, '1', '1', 0),
(8, '平台墨·系统基础', 'rounded', '#10233E', 'M08-系统基础', '适用于：基础数据/信息、系统管理、异常/安灯管理、接口集成等', 8, 0, '1', '1', 0),
(9, '警示红·异常告警', 'rounded', '#E5484D', '通用-异常', '适用于：报警管理、异常处理、停线、不合格紧急处置等', 9, 0, '1', '1', 0),
(10, '中性蓝·通用默认', 'rounded', '#3A71A8', '通用-默认', '未明确分类的一级菜单默认色', 10, 0, '1', '1', 0)
ON CONFLICT (id) DO NOTHING;
