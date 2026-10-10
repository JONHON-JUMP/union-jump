# 业务系统管理页面按角色限制"可管系统"实施计划

## 目标与语义

- 在**主系统角色**上配置"可管业务系统"（一个角色可管一个或多个系统，留空=不限）；
- 受限角色登录后，**业务菜单管理、业务角色管理、业务用户管理、业务班组管理** 4 个页面：左侧只显示授权系统、查询只返回授权系统数据、写操作越权直接报错；
- 兼容性：现有角色默认不配置=不限，超管等零回归；多角色取并集，任一启用角色不限则不限。

## 1. SQL（`sql/postgresql/upgrade_20260928_role_sub_system_scope.sql`，幂等格式照 upgrade_20260911 先例）

`ALTER TABLE "system_role" ADD COLUMN IF NOT EXISTS sub_system_ids varchar(500)`（JSON 数组字符串，对应 JacksonTypeHandler）+ `COMMENT ON COLUMN`。

## 2. 后端——角色侧配置能力

- `RoleDO`（`dal/dataobject/permission/RoleDO.java`）：加 `@TableField(typeHandler = JacksonTypeHandler.class) private Set<Long> subSystemIds`（照抄 dataScopeDeptIds 先例，autoResultMap 已开）；
- `RoleSaveReqVO` / `RoleRespVO` 加 `Set<Long> subSystemIds`；`RoleServiceImpl.createRole/updateRole` 走 BeanUtils 自动拷贝无需改方法体，仅追加"subSystemIds 存在性校验"（仿 validateRoleDuplicate 风格，用 subSystemMapper 校验）；
- 缓存无需新增处理（updateRole 已有 `@CacheEvict(ROLE)` + `evictUsersByRoleId`）。

## 3. 后端——访问范围计算服务（新文件 `service/user/SubSystemAccessService(Impl)`）

- `boolean isUnrestricted(Long userId)` / `Set<Long> getAllowedSubSystemIds(Long userId)`：
  - 登录人为空（内部调用，如 registerFromMainUser）→ 不受限；
  - `permissionService.getUserRoleIdListByUserIdFromCache(userId)` → `roleMapper` 查角色、**过滤启用状态** → 任一启用角色 subSystemIds 为空 → 不受限；否则取并集；
- 新错误码：`ErrorCodeConstants` 中 `1_002_003_068 SUB_SYSTEM_NO_PERMISSION "无权管理该业务系统"`（段内顺延，加在 L120 之后）。

## 4. 后端——4 个页面的读过滤与写校验

统一规则：查询传了不在允许集内的 subSystemId → 抛 `SUB_SYSTEM_NO_PERMISSION`；未传 → 追加 `IN (allowed)`；写操作按 VO 的 subSystemId 或按 id 查库换算后校验。

| 模块 | 读过滤 | 写校验 |
|---|---|---|
| 用户管理（SubSystemUsers*） | `/page`；`/client-simple-list`（左侧系统列表，改为按 allowed 过滤后返回） | create/update、delete/delete-list、assign-role、update-status、update-register-status、import（subSystemId 参数）、role/team/post-simple-list、home-menu-tree-list、list-by-main-user-id（按 id 换算） |
| 角色管理（SubSystemRole*） | `/page` | create/update、delete/delete-list、assign-role-menu、list-role-menu-ids、register、update-status、update-register-status、import、menu-simple-list（菜单权限弹窗的树） |
| 菜单管理（SubSystemMenu*） | `/list`、`menu-simple-list` | create/update、delete/delete-list（按 id 换算）、通用菜单 common-create/update 的 `subSystemIds` 挂载目标逐个校验 |
| 班组管理（SubSystemTeam*） | `/page`、`team-simple-list`；**班组页左侧数据源 `/system/sub-system-workshop/page` 按 subSystemId 过滤**（该行带 subSystemId） | create/update、delete/delete-list |

注：`/my-*` 最终用户接口不动。

## 5. 顺带生效说明（需知情确认）

- `client-simple-list` 是共用接口：**车间对照页、接口配置页**的左侧列表、主用户新增弹窗"同时登记业务系统用户"的多选也会同样按授权系统收敛——行为一致、符合预期，一并生效不再单独排除；
- 车间对照页因 workshop/page 被过滤（班组页左侧依赖），受限角色也只会看到授权系统的对照数据；
- `client-simple-list` 的 `@PreAuthorize` 需追加 `'system:role:query'`（否则只有角色权限的管理员打不开配置下拉）。

## 6. 前端（`jonhonjump-ui/jonhonjump-ui-admin-vue2`）

- **主系统角色页 `views/system/role/index.vue`**：新增/修改弹窗加"可管业务系统"多选下拉（`el-select multiple`，选项来自 `getSubSystemClientSimpleList(true)`，提示"留空=全部系统"，写法参考 `views/system/user/index.vue` 的 syncForm.subSystemIds）；form 重置加 `subSystemIds: []`；列表加"可管系统"列（空显示"全部"）；
- 4 个业务系统管理页面：左侧列表与下拉由后端过滤自动收敛，**无结构性改动**（不做提示条，保持改动面最小）。

## 7. 测试与验证

- 单测：SubSystemAccessService（不限/受限/空集/停用角色/内部调用）、4 个 Service 的越权校验与列表过滤；
- 回归：`mvn -pl jonhonjump-module-system test`（编译+既有测试）；前端 `npm run dev` 手工验证角色配置→受限账号登录→4 个页面可见性与越权报错；
- 手册补充（轻量）：`doc/JUMP使用人员操作手册.md` 第七章加一段"可管系统范围"说明。

## 不做的事

- 不做部门/车间级数据范围（后续如需按之前完整方案另行实施）；
- 菜单、岗位管理不做额外限制（随所属系统的 simple-list 入口校验，页面本身不单列）；
- 不动 `assign-role-data-scope`（业务系统角色数据范围仍为保留 no-op）。