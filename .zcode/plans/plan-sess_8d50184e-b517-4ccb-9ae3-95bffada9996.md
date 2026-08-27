# 外部车间管理 + 通用子系统人员接口集成（修订版）

## 核心架构：适配器 + 数据库配置（新增系统零/低代码）

```
人员管理页/用户联动 ──> SubSystemEmployeeController（统一契约）
                            │ SubSystemEmployeeApiFactory：按 subSystemId 查配置表
                            ├── CamstarEmployeeApiAdapter   （apiType=camstar：Cookie SSOLoginIn 会话等特有逻辑）
                            └── GenericHttpEmployeeApiAdapter（apiType=http：纯配置驱动，标准 REST 系统零代码接入）
                        配置全部存表 sub_system_api_config，页面维护，新系统不发版
```

---

## Phase 1：外部车间映射管理（不变）

- 新表 `sub_system_workshop`（id/sub_system_id/dept_id/workshop_code/workshop_name/description + BaseDO 5 列）
- 后端六件套照 SubSystemTeam 模板：DO(@KeySequence/@TenantIgnore)/Mapper/VO×4/Service/Controller（`/system/sub-system-workshop`，权限 `sub-system:workshop:*`）
- 校验：系统存在、部门存在、编码/名称查重、被 sub_system_users.workshop_id 引用禁删
- ErrorCode 从 1_002_003_046 起新段
- 前端 `views/system/subSystem/workshop/index.vue`（照 team 页模板）+ `api/system/subSystemWorkshop.js`；表单：车间编码\*/名称\*/关联部门(treeselect)/描述
- DDL：`sql/postgresql/sub_system_workshop.sql` + 测试库 create_tables.sql/clean.sql

## Phase 2：通用子系统人员接口（修订重点）

**① 新表 `sub_system_api_config`（每系统一行，页面维护）**
```
id / sub_system_id(unique) / api_type(camstar|http) / base_url
auth_type(none|cookie_sso) / auth_config(json: 登录url、token、cookie名)
api_query / api_create / api_update / api_delete / api_team_combo（各为 json：{path,method}，给默认值）
param_mapping(json：JUMP标准参数名→对方参数名，如 {"userCode":"empNo"})
response_mapping(json：success判断/list路径/total路径/字段名映射，http 适配器用)
connect_timeout_ms / read_timeout_ms / status + BaseDO
```

**② 后端 `framework/subsystemapi` 包（system 模块）**
- `SubSystemEmployeeApi` 接口：page/create/update/delete/teamCombo，入出参用统一 DTO（userCode/userName/workshopCode/teamCode/domainName/erpNo/cardNo/onDuty）
- `ExternalApiHttpClient`：OkHttp 公共封装（超时、异常 CamstarApiException 风格的 ExternalApiException、JSON 解析），两适配器共用
- `CamstarEmployeeApiAdapter`：读配置的 base_url/路径；CookieJar 存 `Nancal_Cam_SessionId`，失效自动重登 SSOLoginIn；解析 C# AjaxResult
- `GenericHttpEmployeeApiAdapter`：按 param_mapping/response_mapping 调任意 REST 接口
- `SubSystemEmployeeApiFactory`：subSystemId → 配置 → apiType → 适配器；未配置抛业务异常"该系统未配置人员接口"
- `SubSystemApiConfigService(Impl)` + Controller（`/system/sub-system-api-config`，权限 `sub-system:apiconfig:*`，含"测试连接"端点）
- DTO/VO 若干

**③ 人员操作入口**：`SubSystemEmployeeController`（`/system/sub-system-employee`，权限 `sub-system:employee:*`）——page/create/update/delete/teamCombo，全部经 factory 分发；**物理删除带强确认**（前端弹窗明示"将同时删除域账号"——删除的连带行为取决于目标系统，弹窗文案按配置可带提示语）

**④ 前端**
- `views/system/subSystem/apiConfig/index.vue`：左系统卡片 + 右配置表单（apiType 下拉、baseUrl、5 个操作的路径/方法、鉴权、参数映射常用项表单化+JSON 兜底、测试连接按钮）
- `views/system/subSystem/employee/index.vue`：左系统卡片（仅显示已配置接口的系统）+ 筛选（车间下拉=映射表/工号/姓名）+ 表格（车间/班组/工号/姓名/域账号/ERP/卡号/在职）+ 新增/编辑弹窗（车间\*、班组联动、工号\*、姓名\*）+ 删除强确认
- 对应 api/*.js × 2

## Phase 3：用户管理页联动（调统一接口，不绑 Camstar）

- 后端 `POST /system/user/create-sub-system-user`：userId + subSystemId + workshopCode + teamCode(可选) → JUMP 用户信息组装统一 DTO → factory 分发到目标系统 → 成功后 sub_system_users 落映射行
- 前端 user/index.vue 新增弹窗加"同步创建子系统账号"区块：系统下拉（已配置接口的）→ 车间下拉（按 form.deptId 过滤映射，未映射提示先维护）→ 班组联动
- 提交：先建 JUMP 用户，成功后同步；外部系统失败不回滚，提示可重试

## 菜单 SQL

`sql/postgresql/sub_system_workshop_menu.sql`：外部车间管理 + 子系统接口配置 + 子系统人员管理 3 个菜单 + 按钮权限 INSERT（parent_id 注明对准现场"外部系统"目录）

## 初始数据

- `sub_system_api_config` 给 4200（sub_system_id=3）一条 camstar 型示例配置（base_url 占位待现场填，status 停用——配好地址再启用）

## 验证

- 后端 `mvn compile -pl jonhonjump-module-system`；前端改动文件 eslint
- 全部新功能走新表新接口，不触碰存量逻辑；联调需现场提供 camstar_api 测试地址 + defaultAdmin 权限确认（checkIsTeamLeader）

## 文件清单

- 后端新增 ~20 文件（workshop 六件套 + apiconfig 六件套 + subsystemapi 包 6-7 个类 + DTO）+ 修改 3（ErrorCodeConstants/application 无需改配置·全走表/create_tables.sql）
- 前端新增 7 文件 + 修改 1（user/index.vue）
- SQL 2 个（建表+菜单+初始数据合一）