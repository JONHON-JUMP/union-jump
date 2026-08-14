-- 常见 QA 补充示例数据（id 9 起，可重复执行）
INSERT INTO system_faq (id, category, title, content, sort, status, publisher_name, dept_name, creator, create_time, updater, update_time, deleted, tenant_id) VALUES
(9, 1, '请假审批通过后如何撤销？', '<p>若流程尚未结束，可在「我的流程」中找到对应实例点击撤回；若已全部审批完成，需联系人事部门办理销假手续。</p>', 92, 0, '人事部', '人事部', 'admin', NOW(), 'admin', NOW(), 0, 1),
(10, 2, '门户首页快捷入口如何自定义？', '<p>在门户首页长按应用图标进入编辑模式，可添加、删除或调整顺序；保存后仅对当前账号生效。</p>', 91, 0, '系统管理员', '信息中心', 'admin', NOW(), 'admin', NOW(), 0, 1),
(11, 2, '如何查看已办任务历史？', '<p>进入「工作流程 → 已办任务」，可按流程名称、发起时间筛选，点击记录可查看审批详情与流转记录。</p>', 90, 0, '系统管理员', '信息中心', 'admin', NOW(), 'admin', NOW(), 0, 1),
(12, 3, '忘记登录账号怎么办？', '<p>请联系本部门管理员或信息中心，提供工号与姓名核实身份后，由管理员重置账号或绑定手机号。</p>', 89, 0, 'IT 运维', '信息中心', 'admin', NOW(), 'admin', NOW(), 0, 1),
(13, 4, '设备点检记录在哪里填写？', '<p>进入 MES「设备管理 → 点检任务」，选择当日任务后填写点检项结果并提交，异常项需上传现场照片。</p>', 88, 0, '设备部', '设备部', 'admin', NOW(), 'admin', NOW(), 0, 1),
(14, 5, '报表导出为空是什么原因？', '<p>请检查查询时间范围是否过窄、筛选条件是否过多，以及当前账号是否有对应数据权限；仍无法导出请联系系统管理员。</p>', 87, 0, '系统管理员', '信息中心', 'admin', NOW(), 'admin', NOW(), 0, 1),
(15, 6, '如何申请新增菜单权限？', '<p>提交权限变更申请，说明需访问的菜单与业务理由，经部门负责人审批后由系统管理员在「角色管理」中配置。</p>', 86, 0, '系统管理员', '信息中心', 'admin', NOW(), 'admin', NOW(), 0, 1),
(16, 1, '通知公告和常见 QA 有什么区别？', '<p>通知公告用于发布时效性通知；常见 QA 用于沉淀操作说明与常见问题，可在门户首页「常见 QA」页签长期查阅。</p>', 85, 0, '系统管理员', '信息中心', 'admin', NOW(), 'admin', NOW(), 0, 1)
ON CONFLICT (id) DO UPDATE SET
    category = EXCLUDED.category,
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    sort = EXCLUDED.sort,
    status = EXCLUDED.status,
    publisher_name = EXCLUDED.publisher_name,
    dept_name = EXCLUDED.dept_name,
    updater = EXCLUDED.updater,
    update_time = EXCLUDED.update_time;

SELECT setval('system_faq_seq', (SELECT COALESCE(MAX(id), 1) FROM system_faq));
