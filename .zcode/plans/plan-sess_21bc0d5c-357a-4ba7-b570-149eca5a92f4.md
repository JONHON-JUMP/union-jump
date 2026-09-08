# 方案（修订）：接口管理页鉴权重构 + 测试响应修复 + 本次更新独立 SQL

## 一、SQL：拆出干净的本次升级脚本

### 1. 新建 `sql/postgresql/upgrade_20260829_employee_register.sql`（只含本次更新所需）
```sql
-- 1) 花名册「人员接口注册状态」列（幂等）
ALTER TABLE "sub_system_users" ADD COLUMN IF NOT EXISTS "employee_registered" varchar(2) NOT NULL DEFAULT '0';
COMMENT ON COLUMN "sub_system_users"."employee_registered" IS '子系统人员接口注册状态（0未注册 1已注册；调新增人员接口成功自动置1，页面可改）';

-- 2) Camstar 接口真实地址修正（幂等）：占位 127.0.0.1:8090 → http://192.168.240.125:8888
--    覆盖 base_url / auth_config / api_query / api_create / api_update / api_delete / api_team_combo
UPDATE "sub_system_api_config"
SET "base_url"   = replace("base_url", '127.0.0.1:8090', '192.168.240.125:8888'),
    "auth_config" = replace("auth_config", '127.0.0.1:8090', '192.168.240.125:8888'),
    "api_query"   = replace("api_query", '127.0.0.1:8090', '192.168.240.125:8888'),
    ...（各 api_* 列同）, "update_time" = CURRENT_TIMESTAMP
WHERE "deleted" = 0 AND (各列 LIKE '%127.0.0.1:8090%');

-- 3) 若「Camstar人员管理」尚无任何接口配置：插入默认配置（地址用 192.168.240.125:8888，幂等 NOT EXISTS）
```

### 2. 旧文件 `sub_system_workshop_and_api.sql` 恢复原样
删掉我上一轮追加的「第 5 段 employee_registered」（已挪入新文件），其余历史内容不动。

## 二、Bug 修复：测试响应被清空（apiConfig/index.vue）

1. `handleNodeClick` 记录 `lastNodeKey`，**仅节点变化时**清空 `testResult`/`testBody`（`loadAll` 重选同节点不再清掉结果）；
2. `ExternalApiHttpClient` 异常消息响应体截断 300 → 1000 字符。

## 三、鉴权重构：系统级会话设置 + 接口级 Cookie 开关

### 后端（2 个文件）
- `EndpointSpec` 加 `Boolean withSession`（null=携带，兼容存量 JSON）：`isWithSession()` 默认 true；
- `CamstarEmployeeApiAdapter.doExecute`：`endpoint.isWithSession()` 为 false 时不加 Cookie 头、失败不触发重登录（`executeWithRelogin` 照旧但跳过无会话调用）；`SubSystemApiConfigServiceImpl.buildAuthHeaders`（testInvoke 用）同样按该接口的 `withSession` 决定是否带头——测试行为与真实调用一致；
- `GenericHttpEmployeeApiAdapter` 不受影响（本就不带头，忽略该字段）。

### 前端 `apiConfig/index.vue`（核心）
1. **会话状态**：`sessionMap[subSystemId] = { enabled, url, method, userCode, cookieName }`，从 `authConfig` 列解析（兼容 url|path|loginPath）；
2. **目录树**：`buildDefaultCatalog` 不再生成「鉴权」目录/叶子；存量 auth 叶子在加载时提取到 sessionMap 并从树上隐藏；`buildPayload` 的 `authType`/`authConfig` 由 sessionMap 生成，持久化 `apiCatalog` 时剔除 auth 叶子；purpose 选项去掉「鉴权登录」，删 auth 唯一性处理；
3. **系统节点面板**加「会话鉴权」卡片：启用开关 + 登录地址 + 请求方法 + 调用工号 + Cookie 名 + 「测试会话登录」（apiKey='auth'）+ 保存；
4. **叶子表单**：
   - 删掉 auth 专属字段块；原「自动使用鉴权接口」提示改为「本系统接口默认携带会话 Cookie：<登录地址 / 未启用>」；
   - **加「携带会话 Cookie」开关**（仅系统会话启用时显示；默认开=兼容现状；不需要 Cookie 的接口关掉即可），`stringifyEndpoint` 输出 `"withSession":true/false`；
5. 存量迁移：叶子无 `withSession` 字段 → 默认显示为「携带」（后端 null 也视为携带），行为零变化。

## 验证

1. `mvn compile -pl jonhonjump-module-system -am`；
2. 手动：① 存量配置打开页面：树上无鉴权叶子、会话卡片有值、叶子默认「携带 Cookie」；② 某接口关掉「携带会话」→ 测试与真实注册调用都不带 Cookie 头；③ 会话卡片「测试会话登录」有响应；④ 点测试后响应持续显示不被清空；⑤ 执行新 SQL 后 Camstar 配置地址变为 192.168.240.125:8888、注册状态列生效。