# 删除 OAuth SSO 流程（方案 B）

## 目标
主系统不再通过 OAuth SSO 访问子系统页面。所有子系统页面统一走 Camstar 直链方式（iframe + Cookie）。保留权限包和菜单逻辑。不监听任何子系统消息，删除 `/sso` 授权页。

## 改动分 5 步执行

### 第 1 步：迁移菜单缓存函数（避免删 portalSso.js 时断链）

**新建** `src/utils/portalMenuCache.js`，把 `portalSso.js:120-139` 的三个函数 + `PORTAL_CACHE_STORAGE_KEY` 常量搬过去：
- `loadPersistedPortalCache()`
- `persistPortalCache(snapshot)`
- `clearPersistedPortalCache()`

**改** `store/modules/portal.js:21-23` 的 import 来源从 `portalSso.js` 改为 `portalMenuCache.js`。

### 第 2 步：删除整文件（6 个）

| 文件 | 用途 |
|------|------|
| `utils/portalSso.js` | SSO 消息/持久化工具（菜单缓存函数已迁出） |
| `utils/portalBootWatch.js` | 主系统 bootId 探测 → 清 SSO → reauth |
| `utils/portalSessionBridge.js` | 监听子系统 session-expired / require-relogin 消息 |
| `utils/portalReloginSnapshot.js` | 重登 SSO 快照 |
| `utils/portalReloginSsoRestoreConfig.js` | 快照 TTL 配置 |
| `utils/subsystemHealth.js` | 子系统健康探测（仅服务 SSO 恢复） |

同时删 `views/sso.vue` 和 `router/index.js:49-50` 的 `/sso` 路由定义。

### 第 3 步：重构 `store/modules/portal.js`（核心）

**删 11 个 SSO state**：`ssoDone`、`loadingSso`、`ssoError`、`ssoRetrying`、`iframeReloadNonce`、`reauthInFlight`、`ssoEpoch`、`ssoTaskGen`、`pendingSsoRefresh`、`subSystemAuthRbacVersions`、`subsystemHealthy`

**删 12 个 mutation**：`MARK_SSO_DONE`、`CLEAR_SSO_DONE`、`SET_SSO_ERROR`、`CLEAR_SSO_ERROR`、`SET_SSO_RETRYING`、`RESTORE_SSO_DONE`、`SET_PENDING_SSO_REFRESH`、`CLEAR_PENDING_SSO_REFRESH`、`CLEAR_ALL_SSO_STATE`、`ABORT_SSO_IN_FLIGHT`、`BUMP_IFRAME_RELOAD`、`SET_SUBSYSTEM_HEALTHY`

**删 9 个 action**：`runSilentSso`、`reauthSubSystem`、`handleSubsystemSessionExpired`、`escalateToMainReloginIfNeeded`、`flushPendingSsoRefreshAfterRelogin`、`preAuthSso`、`syncHealthProbe`、`startHealthProbe`、`stopHealthProbe`

**删 2 个辅助函数**：`loadHiddenSsoIframe`、`parseSsoParams`

**重构 `ensureSubSystemReady`**：塌缩为只做菜单加载——
```js
ensureSubSystemReady({ dispatch, state, commit }, clientIdOrOpts) {
  const opts = parseOpts(clientIdOrOpts)
  const key = normalizeSystemKey(opts.clientId)
  return dispatch('resolveSubSystemReload', key).then(force => {
    return dispatch('ensureSubSystemLoaded', { clientId: key, activate: true, force })
      .then(() => key)
  })
}
```
（调用方签名不变，`skipSso` 入参变 noop）

**瘦身 4 处**：
- `MARK_SUB_SYSTEM_LOADED`：删 `if (state.ssoDone[...])` 那 6 行
- `RESET_PORTAL`：删 SSO 字段重置行
- `bootstrapAfterAuth`：删末尾 `flushPendingSsoRefreshAfterRelogin` 调用
- `warmSubSystemDefaultInBackground`：删 `if (!ssoDone) runSilentSso` 分支

**改 import**：第 2 行 `authorize`、第 15-24 行 portalSso 的 SSO 符号、第 25 行 `pingSubsystemUi` 全删

### 第 4 步：瘦身组件（2 个）

**`layout/components/IframeToggle/index.vue`**：
- 删模板第 3-28 行（SSO 等待/失败/重试/健康探测 三块遮罩）
- 删计算属性 `ssoRetrying`/`waitingSso`/`ssoFailed`/`ssoErrorMessage`/`healthDown`
- 删 watch `waitingSso`/`routeClientId`
- 删方法 `retrySso`/`reloginMain`/`retryHealth`/`reloadNonceFor`
- `isIframeVisible` 删第 300-302 行的 `ssoDone` 门控
- 简化 `showFramePlaceholder` 条件

**`layout/components/InnerLink/index.vue`**：
- 删模板两个"重新连接"按钮（slow/failed 遮罩里的）
- 删 prop `reloadNonce`
- 删 watch `reloadNonce`
- 删方法 `reauthSubSystem`、data `pendingReloadAfterAuth` 及引用

### 第 5 步：清理入口（5 个文件）

**`main.js`**：删 import（第 18-19 行）+ 安装（第 104-105 行）

**`App.vue`**：删 bootWatch/snapshot import 和调用

**`store/modules/user.js`**：删 `notifySubsystemMainTokenRefreshed` import + 调用；删 snap→pending SSO 逻辑（保留 `ensureLocalCamstarCookie`）

**`utils/request.js`**：删 `notifySubsystemMainTokenRefreshed`（第 157-159 行）+ `savePortalReloginSnapshot`（第 264-265 行）

**`utils/switchUser.js`**：删 snapshot/bootWatch import 和调用

## 完全不动（确认与 SSO 无关）
- `store/modules/permission.js`（菜单路由生成）
- 菜单加载链路：`loadSubSystemPortal` → `getMyPortalMenus` → `GenerateSubSystemRoutes`
- 权限包：`PortalPermContextRedisDAO`、`SubSystemPermissionContextServiceImpl`（后端）
- Camstar Cookie：`camstarCookie.js`、`camstarPrefetch.js`
- `utils/portalIframe.js`、`PortalDock.vue`

## 删除后的效果
- 所有子系统页面统一走 iframe 直链 + Camstar Cookie
- 不再有任何隐藏 iframe、postMessage、OAuth 换 token、健康探测
- 菜单权限控制照常（`my-menus` + 权限包）
- 代码大幅简化，portal.js 从 ~1772 行缩减到 ~1100 行

## 风险
- 低。菜单加载和 SSO 是弱耦合，删 SSO 不影响菜单/权限包
- 唯一逻辑改动是 `ensureSubSystemReady` 重构（从"菜单+SSO"塌缩为"纯菜单"），签名不变
- 子系统 401 后主系统不再处理（按你要求，子系统自理）