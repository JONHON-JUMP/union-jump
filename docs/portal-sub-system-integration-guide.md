# 主系统访问子系统（Portal + SSO + iframe）集成指南

> 本文档基于当前 **JONHON-JUMP 主系统 + SCADA 子系统** 已跑通的方案整理。  
> 目标：新增子系统时，按清单配置即可，无需重复踩坑。

---

## 1. 总体架构

### 1.1 机器与职责（以 SCADA 为例）

| 角色 | 地址 | 职责 |
|------|------|------|
| **A 机 — 主系统门户** | `http://10.17.65.11:8081` | Nginx 统一入口；主系统 Vue；反代子系统前端 `/scada/`、子系统 API `/prod-api/` |
| **A 机 — 主系统后端** | `http://10.17.65.11:48080/admin-api` | OAuth2 授权中心、子系统菜单/用户、门户 API |
| **B 机 — 子系统前端** | `http://10.1.19.34:28080/scada/` | SCADA Vue 静态资源 |
| **B/C 机 — 子系统后端** | `http://10.1.19.39:7777` | SCADA Java API；调用主系统 OAuth 换 token |

### 1.2 用户看到的是什么

```
┌─ 主系统外壳（A 机 8081）────────────────────────────────────┐
│ 顶栏：工作台 | SCADA 数采系统                                  │
│ ┌─ 左侧菜单（主系统 DB）─┐ ┌─ iframe（子系统页面，无侧栏）─────┐ │
│ │ 来自 sub_system_menu  │ │ http://8081/scada/#/system/user │ │
│ └───────────────────────┘ └─────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
```

- **左侧菜单**：主系统 PostgreSQL 的 `sub_system_menu`（按用户角色过滤）
- **iframe 内容**：子系统真实页面（同域 `/scada/` 反代，避免跨域）
- **URL 形态**：`/portal/{client_id}/{菜单路径}`，例如 `/portal/scada/system/user`

### 1.3 三种「凭证」不要混淆

| 凭证 | 谁签发 | 存在哪 | 用途 |
|------|--------|--------|------|
| **主系统 Access Token** | 主系统 `/admin-api` 登录 | 浏览器 Cookie / localStorage | 访问主系统 API、顶栏切换、拉子系统菜单 |
| **OAuth2 Authorization Code** | 主系统 OAuth2 | 一次性，URL 参数 `?code=` | 子系统后端向主系统换 token |
| **子系统 JWT** | 子系统后端 `/sso/login` | 子系统 Cookie（path=`/scada/`） | iframe 内访问子系统 `/prod-api` |

主系统 token **不会**直接给子系统 API 用；子系统通过 OAuth code 自行换门户用户信息，再签发自己的 JWT。

---

## 2. 完整访问流程（时序）

### 2.1 用户登录主系统

1. 用户打开 `http://10.17.65.11:8081`，登录（含租户 `tenant-id`）。
2. 主系统返回 JWT，前端后续请求带 `Authorization` + `tenant-id`。
3. 主系统菜单来自 `system_users` 权限；子系统列表来自 `GET /system/sub-system-users/my-external-systems`。

### 2.2 用户点击顶栏「SCADA」

1. 前端 `portal/switchSystem('scada')` → `enterSubSystem({ clientId: 'scada' })`。
2. 调用 `GET /system/sub-system-users/my-menus?subSystemId=1` 拉菜单（**主库**）。
3. 动态生成路由 `/portal/scada/...`，左侧展示子系统菜单。
4. **静默 SSO**（`runSilentSso`）：
   - 主系统前端带登录态请求 `POST /admin-api/system/oauth2/authorize`（`client_id=scada`，`auto_approve=true`）。
   - 返回跳转 URL，放入 **隐藏 iframe** 加载。
5. 隐藏 iframe 打开主系统 `/sso?client_id=scada&redirect_uri=.../scada/sso/callback&...`。
6. 主系统 OAuth 自动授权，302 到  
   `http://10.17.65.11:8081/scada/sso/callback?code=xxx&state=scada`。
7. SCADA 前端 `ssoRedirect.js` 把 query 转到 hash：`/scada/#/sso/callback?code=xxx`。
8. `callback.vue` → `POST /prod-api/sso/login`（同域 Nginx 反代到 SCADA 后端）。
9. SCADA 后端 `PortalSsoService`：
   - `POST {api-url}/system/oauth2/token`（Basic `scada:secret`，**Header: tenant-id**）
   - `GET {api-url}/system/oauth2/user/get` → 得到门户 `username`
   - 用 `username` 查 SCADA 本地 `sys_user` → 签发 SCADA JWT → 写 Cookie
10. `callback.vue` `postMessage({ type: 'scada-sso-done', success: true })` 通知主系统。
11. 主系统隐藏 iframe 结束，加载 **可见 iframe**（菜单 link，如 `/scada/#/system/user`）。
12. SCADA `permission.js` 检测 `window.self !== window.top`，**隐藏** SCADA 自带侧栏/顶栏（embed 模式）。

### 2.3 流程图（Mermaid）

```mermaid
sequenceDiagram
    participant U as 用户浏览器
    participant P as 主系统前端 8081
    participant PA as 主系统后端 48080
    participant N as Nginx A机
    participant S as SCADA前端 /scada/
    participant SB as SCADA后端 7777

    U->>P: 登录主系统
    P->>PA: 账号密码 + tenant-id
    PA-->>P: 主系统 JWT

    U->>P: 点击 SCADA 顶栏
    P->>PA: GET my-menus?subSystemId=1
    PA-->>P: sub_system_menu 树 + iframe link

    P->>PA: POST oauth2/authorize (静默, client_id=scada)
    PA-->>P: 授权跳转 URL
    P->>N: 隐藏 iframe → /sso → callback?code=
    N->>S: /scada/sso/callback?code=
    S->>SB: POST /prod-api/sso/login {code}
    SB->>PA: POST oauth2/token (tenant-id)
    PA-->>SB: access_token
    SB->>PA: GET oauth2/user/get
    PA-->>SB: username
    SB->>SB: 查 sys_user，签发 SCADA JWT
    SB-->>S: token
    S-->>P: postMessage scada-sso-done
    P->>N: 可见 iframe /scada/#/system/user
    S->>SB: 业务 API（SCADA JWT）
```

---

## 3. 主系统需要配置什么

### 3.1 数据库（PostgreSQL）

#### ① 注册子系统 `sub_system`

| 字段 | SCADA 示例 | 说明 |
|------|------------|------|
| `id` | `1` | 内部主键，API 用 `subSystemId` |
| `client_id` | `scada` | **URL 段** `/portal/scada/...`，且等于 OAuth2 client_id |
| `system_name` | SCADA 数采系统 | 顶栏/侧栏显示名 |
| `system_url` | `http://10.17.65.11:8081/scada` | iframe 基础地址（**同域反代**后的入口） |
| `status` | `0` | 启用 |

参考 SQL：`sql/postgresql/sub-system-import-scada-2026-06-15.sql`

#### ② OAuth2 客户端 `system_oauth2_client`

| 字段 | 要求 |
|------|------|
| `client_id` | 与 `sub_system.client_id` 一致，如 `scada` |
| `secret` | 与子系统 `application.yml` 中 `client-secret` 一致 |
| `redirect_uris` | 子系统**前端**回调，如 `http://10.17.65.11:8081/scada/sso/callback` |
| `authorized_grant_types` | `authorization_code`, `refresh_token` |
| `scopes` / `auto_approve_scopes` | `user.read`（静默授权必需） |

参考 SQL：`sql/postgresql/sub-system-scada-nginx-proxy-2026-06-15.sql`

**改 OAuth 后必须**：重启主系统后端，或 `redis-cli DEL "oauth_client::scada"`。

#### ③ 子系统菜单 `sub_system_menu`

- 从子系统 `sys_menu` **导入**到主库（非实时同步）。
- 类型 `M` 目录 / `C` 菜单；`path` + `component` 用于拼 iframe URL：  
  `{system_url}/#/{父path}/{子path}`，如 `/scada/#/system/user`。
- 在主系统「外部系统 → 菜单管理」维护。

#### ④ 用户与权限

| 表 | 作用 |
|----|------|
| `sub_system_users` | 门户用户 ↔ 子系统（`main_user_id` + `sub_system_id`） |
| `sub_system_user_role` | 子系统内角色 |
| `sub_system_role_menu` | 角色可见菜单 |

门户用户要在 SCADA **也有同名** `sys_user`（SSO 按 `username` 匹配，不校验密码）。

#### ⑤ 主页面（可选）`sub_system_home_page`

- `home_page_url`：如 `http://10.17.65.11:8081/scada/#/index`

### 3.2 Nginx（A 机 8081）

每个子系统需要两段 location（名称以 SCADA 为例）：

```nginx
# 无尾斜杠重定向（必加，否则 /scada 会进主系统 Vue 404）
location = /scada {
    return 301 /scada/;
}

# 子系统前端 → B 机
location /scada/ {
    proxy_pass http://10.1.19.34:28080/scada/;
    # ... proxy_set_header 等
}

# 子系统后端 API（可与其它子系统共用前缀或各用各的）
location /prod-api/ {
    proxy_pass http://10.1.19.39:7777/;
}
```

完整示例：`sql/nginx/portal-a-machine-8081-user.conf.example`

### 3.3 主系统后端

- 多租户环境：OAuth token 接口要求 `tenant-id` 请求头（子系统后端调用时已支持）。
- 关键 API：
  - `GET /system/sub-system-users/my-external-systems` — 顶栏子系统列表
  - `GET /system/sub-system-users/my-menus?subSystemId=` — 门户侧栏菜单
  - `POST /system/oauth2/authorize` — 静默授权
  - `POST /system/oauth2/token` — 子系统后端换 token
  - `GET /system/oauth2/user/get` — 子系统后端取 username

### 3.4 主系统前端（一般新增子系统**不用改代码**）

已实现通用门户模块，新子系统只要 `client_id` 正确、菜单入库即可。

| 文件 | 作用 |
|------|------|
| `src/store/modules/portal.js` | 切换子系统、静默 SSO、菜单加载 |
| `src/store/modules/permission.js` | `GenerateSubSystemRoutes` 生成 `/portal/{clientId}/...` |
| `src/router/index.js` | 静态路由 `/portal/:clientId/...` → `PortalFrame.vue` |
| `src/views/system/subSystem/portal/PortalFrame.vue` | 注册 iframe 到 TagsView |
| `src/permission.js` | 进入 portal 前加载子系统；旧 URL `/portal/1/` 自动重定向 |
| `src/utils/portalRoute.js` | `client_id` 解析、菜单路径工具 |

---

## 4. 子系统需要配置什么

### 4.1 后端（以 SCADA/RuoYi 为例）

`scada-admin/src/main/resources/application.yml`：

```yaml
portal:
  sso:
    enabled: true
    portal-url: http://10.17.65.11:8081          # 主系统前端（跳转 /sso 授权页）
    api-url: http://10.17.65.11:48080/admin-api    # 主系统后端（换 token）
    client-id: scada                               # = sub_system.client_id
    client-secret: Scada@2026                      # = system_oauth2_client.secret
    redirect-uri: http://10.17.65.11:8081/scada/sso/callback  # 必须完全一致
    scope: user.read
    state: scada
    tenant-id: 1                                   # = 门户登录租户 ID
```

关键 Java（已实现，新子系统可复制模式）：

| 类 | 作用 |
|----|------|
| `PortalSsoProperties` | 读取上述配置 |
| `PortalSsoService` | 调主系统 token/user 接口（带 `tenant-id`） |
| `SysSsoController` | `GET /sso/config`、`POST /sso/login` |
| `SysLoginService.loginByPortal` | username 匹配本地用户并签发 JWT |

**网络**：子系统后端必须能访问 `10.17.65.11:48080`。

### 4.2 前端

| 配置/文件 | 要求 |
|-----------|------|
| `.env.production` | `PUBLIC_PATH=/scada/`，`VUE_APP_BASE_API=/prod-api` |
| `src/utils/ssoRedirect.js` | OAuth 回调保留 `/scada/` 前缀 |
| `src/utils/auth.js` | Cookie path 为 `/scada/` |
| `src/utils/embed.js` | iframe 内隐藏侧栏/顶栏 |
| `src/permission.js` | iframe 内等待 SSO token，禁止跳 `/sso` 死循环 |
| `src/views/sso/callback.vue` | SSO 完成后 `postMessage` 通知主系统 |

构建：`npm run build:prod`，部署到 B 机 Nginx 的 `html/dist/scada/`。

### 4.3 子系统本地用户

- SSO **不校验**子系统密码。
- 门户 OAuth 返回 `username` → 子系统查本地 `sys_user.user_name`。
- **必须**存在同名且启用用户，否则 SSO 失败。

---

## 5. 新增子系统 Checklist（复制 SCADA 流程）

假设新系统代号 **`mes`**，`client_id=mes`。

### Step 0：规划

- [ ] 定 `client_id`（字母开头，如 `mes`）
- [ ] 定 Nginx 前缀（如 `/mes/`）和 API 前缀（如 `/mes-api/` 或共用 `/prod-api/`）
- [ ] 定部署 IP：前端机、后端机

### Step 1：主库 PostgreSQL

- [ ] `INSERT INTO sub_system (... client_id='mes', system_url='http://门户IP:8081/mes')`
- [ ] `INSERT INTO system_oauth2_client`（`redirect_uris` = `http://门户IP:8081/mes/sso/callback`）
- [ ] 清 Redis OAuth 缓存或重启主系统后端
- [ ] 导入/配置 `sub_system_menu`（可从子系统菜单脚本导入）
- [ ] 配置 `sub_system_role`、`sub_system_users`、用户角色关联
- [ ] 门户 `system_users` 用户在子系统库有同名账号

### Step 2：A 机 Nginx

- [ ] `location = /mes { return 301 /mes/; }`
- [ ] `location /mes/ { proxy_pass 子系统前端; }`
- [ ] `location /mes-api/ { proxy_pass 子系统后端; }`（或复用现有规则）
- [ ] `nginx -s reload`

### Step 3：子系统后端

- [ ] 复制 SSO 模块（`PortalSsoProperties`、`PortalSsoService`、`SysSsoController`）
- [ ] `application.yml` 填 `portal.sso.*`（`client-id=mes`，`redirect-uri` 与 OAuth 一致）
- [ ] `tenant-id` 与门户一致
- [ ] Security 放行 `/sso/**`
- [ ] 重启后端

### Step 4：子系统前端

- [ ] `PUBLIC_PATH=/mes/`，`VUE_APP_BASE_API=/mes-api`（与 Nginx 一致）
- [ ] 实现 `/sso/callback` 路由 + `ssoRedirect.js` + `embed.js` + `postMessage`
- [ ] 构建部署到前端机

### Step 5：验证

```powershell
# ① 子系统前端直连
curl.exe -I http://子系统前端IP/mes/

# ② 门户反代
curl.exe -I http://10.17.65.11:8081/mes/

# ③ SSO token（假 code，测 tenant/client）
curl.exe -X POST "http://10.17.65.11:48080/admin-api/system/oauth2/token" ^
  -H "Authorization: Basic bWVz:你的secret的Base64" ^
  -H "tenant-id: 1" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "grant_type=authorization_code&code=test&redirect_uri=http://10.17.65.11:8081/mes/sso/callback"
```

浏览器：

- [ ] 登录门户 → 顶栏出现 MES
- [ ] URL 为 `/portal/mes/home/index`
- [ ] F12 → `POST .../mes-api/sso/login` 返回 token
- [ ] iframe 内业务页正常，无双菜单栏

---

## 6. 关键 URL 对照表（SCADA）

| 用途 | URL |
|------|-----|
| 门户入口 | `http://10.17.65.11:8081/portal/scada/home/index` |
| 门户菜单页 | `http://10.17.65.11:8081/portal/scada/system/user` |
| iframe 实际加载 | `http://10.17.65.11:8081/scada/#/system/user` |
| OAuth redirect_uri | `http://10.17.65.11:8081/scada/sso/callback` |
| 子系统 SSO API | `POST http://10.17.65.11:8081/prod-api/sso/login` |
| 主系统 OAuth token | `POST http://10.17.65.11:48080/admin-api/system/oauth2/token` |

---

## 7. 常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| `/portal/scada/...` 404 | 主系统前端未部署最新包 | 重新 build 主系统 Vue |
| `/scada/` 404 | Nginx 未配或 B 机无 dist | 查 A/B 机 Nginx 与静态文件 |
| `redirect_uri 不匹配` | OAuth DB 与 yml 不一致或 Redis 缓存旧值 | 执行 SQL + 清 Redis |
| `租户标识未传递` | 子系统换 token 未带 `tenant-id` | yml 配 `portal.sso.tenant-id` |
| SSO 成功但提示用户不存在 | 门户 username 在子系统无账号 | 子系统建同名用户 |
| 双菜单栏 | 子系统 embed 模式未生效 | 部署含 `embed.js` 的前端包 |
| 标签页都叫「外部系统」 | 未部署 portal 标题修复 | 部署最新主系统前端 |
| `GET not supported` on `/sso/login` | 用浏览器直接打开 API | 必须 POST，看 F12 Network |
| 旧链接 `/portal/1/` | 已支持自动重定向 | 可改用 `/portal/scada/` |

---

## 8. 代码地图（快速定位）

### 主系统 — 后端

```
jonhonjump-module-system/
  controller/admin/user/SubSystemUsersController.java   # my-menus, my-external-systems
  service/user/SubSystemUsersServiceImpl.java         # 菜单树、buildIframeLink、buildSsoUrl
  controller/admin/oauth2/OAuth2OpenController.java     # /system/oauth2/token
  controller/admin/oauth2/OAuth2UserController.java     # /system/oauth2/user/get
```

### 主系统 — 前端

```
jonhonjump-ui-admin-vue2/src/
  store/modules/portal.js          # 子系统切换、静默 SSO
  store/modules/permission.js      # 子系统路由生成
  router/index.js                  # /portal/:clientId/...
  permission.js                    # 路由守卫、legacy 重定向
  utils/portalRoute.js             # client_id 路径工具
  views/system/subSystem/portal/PortalFrame.vue
  layout/components/TopBar/        # 顶栏子系统切换
```

### 子系统 — SCADA 参考

```
scada/scada-framework/.../PortalSsoService.java
scada/scada-framework/.../PortalSsoProperties.java
scada/scada-framework/.../SysLoginService.java       # loginByPortal
scada/scada-admin/.../SysSsoController.java
scada/ui/src/utils/embed.js
scada/ui/src/utils/ssoRedirect.js
scada/ui/src/views/sso/callback.vue
scada/ui/src/permission.js
```

### SQL / Nginx 模板

```
sql/postgresql/sub-system-scada-nginx-proxy-2026-06-15.sql
sql/postgresql/sub-system-import-scada-2026-06-15.sql
sql/nginx/portal-a-machine-8081-user.conf.example
sql/nginx/scada-b-machine-28080.conf.example
```

---

## 9. 设计要点（给架构Review）

1. **菜单在主库、页面在子系统**：权限统一在门户管控；业务仍跑在子系统。
2. **同域反代**：`/scada/` 与主系统同域，Cookie、iframe、OAuth redirect 才简单。
3. **OAuth code 只给子系统后端换 token**：不在浏览器暴露主系统 token 给子系统。
4. **静默 SSO**：`auto_approve_scopes` + 隐藏 iframe + `postMessage` 避免闪屏和手工点授权。
5. **URL 用 client_id**：`/portal/scada/` 比 `/portal/1/` 可读且与 OAuth 一致。
6. **embed 模式**：子系统在 iframe 内自隐藏壳层，避免双菜单。

---

## 10. 版本记录

| 日期 | 说明 |
|------|------|
| 2026-06-15 | 首版：SCADA 同域反代 + 静默 SSO + client_id 路由 + embed 模式 |

---

**维护**：新增子系统时，优先复制 `sql/postgresql/sub-system-scada-nginx-proxy-2026-06-15.sql` 与 SCADA SSO 代码，全局替换 `scada` → 新 `client_id` 与 IP 即可。
