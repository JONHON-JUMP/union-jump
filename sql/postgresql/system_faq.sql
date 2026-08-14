-- 常见 QA：表结构、字典、菜单、示例数据

DROP TABLE IF EXISTS system_faq;
CREATE TABLE system_faq (
    id int8 NOT NULL,
    category int2 NOT NULL,
    title varchar(100) NOT NULL,
    content text NOT NULL,
    sort int4 NOT NULL DEFAULT 0,
    status int2 NOT NULL DEFAULT 0,
    publisher_name varchar(64) NULL,
    dept_name varchar(255) NULL,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    tenant_id int8 NOT NULL DEFAULT 0
);

ALTER TABLE system_faq ADD CONSTRAINT pk_system_faq PRIMARY KEY (id);

COMMENT ON TABLE system_faq IS '常见 QA 表';
COMMENT ON COLUMN system_faq.id IS '编号';
COMMENT ON COLUMN system_faq.category IS '分类，对应 system_faq_category 字典';
COMMENT ON COLUMN system_faq.title IS '标题';
COMMENT ON COLUMN system_faq.content IS '内容';
COMMENT ON COLUMN system_faq.sort IS '显示顺序，越大越靠前';
COMMENT ON COLUMN system_faq.status IS '状态（0开启 1关闭）';
COMMENT ON COLUMN system_faq.publisher_name IS '发布人';
COMMENT ON COLUMN system_faq.dept_name IS '发布部门';

DROP SEQUENCE IF EXISTS system_faq_seq;
CREATE SEQUENCE system_faq_seq START WITH 1 INCREMENT BY 1;

-- 字典类型
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (9200, '常见QA分类', 'system_faq_category', 0, '常见 QA 分类', 'admin', NOW(), 'admin', NOW(), '0', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES
(9201, 1, '流程说明', '1', 'system_faq_category', 0, 'primary', '', '流程说明', 'admin', NOW(), 'admin', NOW(), '0'),
(9202, 2, '操作指南', '2', 'system_faq_category', 0, 'success', '', '操作指南', 'admin', NOW(), 'admin', NOW(), '0'),
(9203, 3, '账号安全', '3', 'system_faq_category', 0, 'warning', '', '账号安全', 'admin', NOW(), 'admin', NOW(), '0'),
(9204, 4, '设备管理', '4', 'system_faq_category', 0, 'info', '', '设备管理', 'admin', NOW(), 'admin', NOW(), '0'),
(9205, 5, '数据报表', '5', 'system_faq_category', 0, 'default', '', '数据报表', 'admin', NOW(), 'admin', NOW(), '0'),
(9206, 6, '权限管理', '6', 'system_faq_category', 0, 'danger', '', '权限管理', 'admin', NOW(), 'admin', NOW(), '0')
ON CONFLICT (id) DO NOTHING;

-- 菜单（挂在系统管理 2739 下，与通知公告并列；ID 避开 MES 5100 段）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
(9900, '常见QA', '', 2, 5, 2739, 'faq', 'ep:question-filled', 'system/faq/index', 'SystemFaq', 0, '1', '1', '1', 'admin', NOW(), 'admin', NOW(), '0'),
(9901, '常见QA查询', 'system:faq:query', 3, 1, 9900, '#', '#', '', NULL, 0, '1', '1', '1', 'admin', NOW(), 'admin', NOW(), '0'),
(9902, '常见QA新增', 'system:faq:create', 3, 2, 9900, '', '', '', NULL, 0, '1', '1', '1', 'admin', NOW(), 'admin', NOW(), '0'),
(9903, '常见QA修改', 'system:faq:update', 3, 3, 9900, '', '', '', NULL, 0, '1', '1', '1', 'admin', NOW(), 'admin', NOW(), '0'),
(9904, '常见QA删除', 'system:faq:delete', 3, 4, 9900, '', '', '', NULL, 0, '1', '1', '1', 'admin', NOW(), 'admin', NOW(), '0')
ON CONFLICT (id) DO NOTHING;

-- 管理员角色授权（超级管理员 + 普通管理员）
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
(990001, 1, 9900, 'admin', NOW(), 'admin', NOW(), '0', 1),
(990002, 1, 9901, 'admin', NOW(), 'admin', NOW(), '0', 1),
(990003, 1, 9902, 'admin', NOW(), 'admin', NOW(), '0', 1),
(990004, 1, 9903, 'admin', NOW(), 'admin', NOW(), '0', 1),
(990005, 1, 9904, 'admin', NOW(), 'admin', NOW(), '0', 1),
(990006, 2, 9900, 'admin', NOW(), 'admin', NOW(), '0', 1),
(990007, 2, 9901, 'admin', NOW(), 'admin', NOW(), '0', 1),
(990008, 2, 9902, 'admin', NOW(), 'admin', NOW(), '0', 1),
(990009, 2, 9903, 'admin', NOW(), 'admin', NOW(), '0', 1),
(990010, 2, 9904, 'admin', NOW(), 'admin', NOW(), '0', 1)
ON CONFLICT (id) DO NOTHING;

-- 示例数据（由原 quickNavData.js 迁移）
INSERT INTO system_faq (id, category, title, content, sort, status, publisher_name, dept_name, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
(1, 1, '如何发起请假申请？', '<p>进入「工作流程 - OA 请假」，点击「发起流程」，填写请假类型、起止时间与事由后提交即可。</p>', 100, 0, '系统管理员', '信息中心', 'admin', '2026-06-01 09:00:00', 'admin', '2026-06-01 09:00:00', 0, 1),
(2, 1, '费用报销需要哪些材料？', '<p>请准备：报销单、发票原件、审批流程截图及相关业务说明材料，按财务制度提交审批。</p>', 99, 0, '财务部', '财务部', 'admin', '2026-06-02 09:00:00', 'admin', '2026-06-02 09:00:00', 0, 1),
(3, 1, '采购审批流程说明', '<p>采购申请需先填写采购需求单，经部门负责人、采购部及分管领导审批后执行采购。</p>', 98, 0, '采购部', '采购部', 'admin', '2026-06-03 09:00:00', 'admin', '2026-06-03 09:00:00', 0, 1),
(4, 2, '如何查询我的待办任务？', '<p>可在门户首页「待办」页签查看，或进入「工作流程 - 待办任务」进行查询与处理。</p>', 97, 0, '系统管理员', '信息中心', 'admin', '2026-06-04 09:00:00', 'admin', '2026-06-04 09:00:00', 0, 1),
(5, 3, '系统登录密码如何重置？', '<p>请联系 IT 运维或通过个人中心修改密码；如账号被锁定，需管理员解锁后重新登录。</p>', 96, 0, 'IT 运维', '信息中心', 'admin', '2026-06-05 09:00:00', 'admin', '2026-06-05 09:00:00', 0, 1),
(6, 4, '设备报修流程是什么？', '<p>在设备管理模块提交报修单，填写设备编号与故障描述，设备部接单后安排维修并反馈结果。</p>', 95, 0, '设备部', '设备部', 'admin', '2026-06-06 09:00:00', 'admin', '2026-06-06 09:00:00', 0, 1),
(7, 5, '如何导出报表数据？', '<p>在报表页面设置查询条件后，点击「导出」按钮即可下载 Excel 文件。</p>', 94, 0, '系统管理员', '信息中心', 'admin', '2026-06-07 09:00:00', 'admin', '2026-06-07 09:00:00', 0, 1),
(8, 6, '权限申请找谁审批？', '<p>权限变更需提交权限申请单，由直属领导及系统管理员审批后生效。</p>', 93, 0, '人事部', '人事部', 'admin', '2026-06-08 09:00:00', 'admin', '2026-06-08 09:00:00', 0, 1),
(9, 1, '请假审批通过后如何撤销？', '<p>若流程尚未结束，可在「我的流程」中找到对应实例点击撤回；若已全部审批完成，需联系人事部门办理销假手续。</p>', 92, 0, '人事部', '人事部', 'admin', '2026-06-09 09:00:00', 'admin', '2026-06-09 09:00:00', 0, 1),
(10, 2, '门户首页快捷入口如何自定义？', '<p>在门户首页长按应用图标进入编辑模式，可添加、删除或调整顺序；保存后仅对当前账号生效。</p>', 91, 0, '系统管理员', '信息中心', 'admin', '2026-06-10 09:00:00', 'admin', '2026-06-10 09:00:00', 0, 1),
(11, 2, '如何查看已办任务历史？', '<p>进入「工作流程 → 已办任务」，可按流程名称、发起时间筛选，点击记录可查看审批详情与流转记录。</p>', 90, 0, '系统管理员', '信息中心', 'admin', '2026-06-11 09:00:00', 'admin', '2026-06-11 09:00:00', 0, 1),
(12, 3, '忘记登录账号怎么办？', '<p>请联系本部门管理员或信息中心，提供工号与姓名核实身份后，由管理员重置账号或绑定手机号。</p>', 89, 0, 'IT 运维', '信息中心', 'admin', '2026-06-12 09:00:00', 'admin', '2026-06-12 09:00:00', 0, 1),
(13, 4, '设备点检记录在哪里填写？', '<p>进入 MES「设备管理 → 点检任务」，选择当日任务后填写点检项结果并提交，异常项需上传现场照片。</p>', 88, 0, '设备部', '设备部', 'admin', '2026-06-13 09:00:00', 'admin', '2026-06-13 09:00:00', 0, 1),
(14, 5, '报表导出为空是什么原因？', '<p>请检查查询时间范围是否过窄、筛选条件是否过多，以及当前账号是否有对应数据权限；仍无法导出请联系系统管理员。</p>', 87, 0, '系统管理员', '信息中心', 'admin', '2026-06-14 09:00:00', 'admin', '2026-06-14 09:00:00', 0, 1),
(15, 6, '如何申请新增菜单权限？', '<p>提交权限变更申请，说明需访问的菜单与业务理由，经部门负责人审批后由系统管理员在「角色管理」中配置。</p>', 86, 0, '系统管理员', '信息中心', 'admin', '2026-06-15 09:00:00', 'admin', '2026-06-15 09:00:00', 0, 1),
(16, 1, '通知公告和常见 QA 有什么区别？', '<p>通知公告用于发布时效性通知；常见 QA 用于沉淀操作说明与常见问题，可在门户首页「常见 QA」页签长期查阅。</p>', 85, 0, '系统管理员', '信息中心', 'admin', '2026-06-16 09:00:00', 'admin', '2026-06-16 09:00:00', 0, 1);

SELECT setval('system_faq_seq', (SELECT COALESCE(MAX(id), 1) FROM system_faq));
