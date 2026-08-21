# 正式工艺查询与工艺文件查看设计

## 目标

在现有 `POST /mes/process/query/card` 中补齐正式工艺查询：获取工艺大版本、校验发行状态、递归查询 Oracle 工序关系并返回可直接展示的工艺树。同时支持 PDM 工序直接打开工艺文件，以及 MPM 工序在用户点击“查看”时动态获取发布 URL。

## 范围与约束

- 正式工艺号以 `C` 开头；其中 `CX` 为 MPM 正式工艺，其余 `C` 前缀为 PDM 正式工艺。
- 查询版本统一调用 MPM `queryObjectInfo`，不从前端接受版本号。
- 正式工艺查询只要求 `accno`；`prtno` 仅在临时工艺查询时必填。
- 工艺树在一次 `queryCard` 请求内完整构造，不实现前端懒加载。
- MPM 发布 URL 仅在用户点击查看时获取，不在查询工艺树时批量调用。
- 第三方系统认证、token 和路由配置继续由现有 `ThirdPartyRouteService` 及 invokeId 管理，业务代码不保存登录凭据。
- 临时工艺现有行为保持不变。

## 外部接口契约

### 查询正式工艺版本

- URL：`http://mpm.caoe.com/plm-service/api/gate/object/v1/queryObjectInfo`
- 方法：POST
- invokeId：`2`（`InvokeIdConstant.queryRouteStructInfo`）
- 请求体：

```json
{
  "objType": "com.glaway.dtp.business.model.process.ProcessModel",
  "objNumbers": ["CX0000000048"],
  "isLatest": "true"
}
```

`objNumbers` 只包含用户输入的 `accno`。成功响应必须满足 `success=true`、`code=200` 且 `result[0].version` 非空。版本示例为 `B.1`，系统只取第一个 `.` 之前的 `B` 作为数据库查询版本；没有 `.` 时使用完整非空值。

### 获取 MPM 工艺文件 URL

- URL：`http://mpm.caoe.com/mpm-service/api/gate/release/v1/processReleaseForMes`
- 方法：POST
- invokeId：`3`（`InvokeIdConstant.processReleaseForMes`）
- 外部请求 Content-Type：form-data
- 参数名：`oid`（小写）
- 参数值：`OperationEntity:<CAOE_OP.oid>`
- 额外外部请求头由路由配置提供：`X-Access-Token`、`Accept-User: MPMViewUser`

成功响应必须满足 `success=true`、`code=200`，最终地址读取自 `result.url`。

## 后端接口

### 查询工艺卡

沿用：

```text
POST /mes/process/query/card
```

正式工艺返回单元素 `List<ProcessCardRespVO>`：

- `accno`：用户输入的工艺号；
- `version`：截取后大版本；
- `isFormal=1`；
- `isFix=0`；
- `details`：递归工序树。

### 获取工艺文件地址

新增：

```text
POST /mes/process/query/file-url
```

请求体 `ProcessFileUrlReqVO`：

```json
{
  "oid": "308233850219293315"
}
```

该接口保持登录鉴权。服务端补齐 `OperationEntity:` 前缀，调用 invokeId `3`，返回：

```json
{
  "url": "http://mpm.caoe.com/..."
}
```

前端不得直接调用 MPM 外部接口，也不得自行拼接 `OperationEntity:`。

## Oracle 数据访问

`CaoeTableMapper` 增加四类查询。

### 工艺发行状态

```sql
SELECT pp_state
FROM CAOE_PP
WHERE PP_NUMBER = #{accno}
  AND PP_VERSION = #{version}
```

查询不到记录或 `pp_state` 不等于“已发行”均提示“工艺未发行，无法查看”。

### 子工序列表

```sql
SELECT cname, cnumber, label, cversion
FROM CAOE_PPOPLINK
WHERE FNUMBER = #{number}
  AND FVERSION = #{version}
ORDER BY label ASC
```

第一次以正式工艺 `accno + version` 查询一级工序；之后以父节点的 `cnumber + cversion` 查询下一层。`label` 按数据库字符串升序，适配 `0010`、`0040` 等工序号。

### PDM 工艺文件信息

```sql
SELECT op_link
FROM CAOE_OP
WHERE OP_NUMBER = #{cnumber}
  AND OP_VERSION = #{cversion}
```

从 `op_link` 第一个 `?` 之后截取完整查询串，例如 `oid=kkk`，拼到：

```text
http://pdm.caoe.com/Windchill/netmarkets/jsp/ext/caoe/mpml/routCard.jsp?
```

若 `op_link` 为空、没有 `?` 或 `?` 后无内容，视为正式工艺数据不完整。

### MPM 工艺文件信息

```sql
SELECT oid
FROM CAOE_OP
WHERE OP_NUMBER = #{cnumber}
  AND OP_VERSION = #{cversion}
```

树查询阶段只保存原始 `oid`，不调用发布接口。

## 数据模型与组件边界

### DTO / VO

- `FormalProcessObjectReqVO`：`objType`、`List<String> objNumbers`、`isLatest`。
- `ProcessOperationDTO`：`cname`、`cnumber`、`label`、`cversion`。
- Mapper 使用两个明确的标量查询方法分别返回 PDM `op_link` 与 MPM `oid`。
- `ProcessFileUrlReqVO`：非空 `oid`。
- `ProcessFileUrlRespVO`：`url`。
- `ProcessCardDetailsRespVO` 增加 `oid`；仅 MPM 正式工序使用。

节点字段映射：

| 返回字段 | 数据来源 |
|---|---|
| `name` | `cname` |
| `code` | `cnumber` |
| `no` | `label` |
| `url` | PDM 拼装地址；MPM 为 `null` |
| `oid` | MPM `CAOE_OP.oid`；PDM 为 `null` |
| `children` | 递归查询结果 |
| `idx` | 对最终树做深度优先遍历后生成的全局连续序号 |

### 服务职责

- `ProcessServiceImpl`：识别正式/临时工艺、编排版本查询、发行校验、工艺树组装和响应包装；提供 MPM 文件 URL 获取方法。
- `FormalProcessTreeAssembler`：递归查询工序、构造节点、生成 PDM URL 或保存 MPM oid、循环检测及最终 `idx` 分配。
- `CaoeTableMapper`：只负责 Oracle 查询，不承载树逻辑。
- `ProcessController`：暴露查询卡片和文件 URL 两个登录鉴权接口。

## 递归与一致性规则

1. 每层使用 Mapper 已按 `label ASC` 排序的结果，组装器不按数字重排。
2. 叶子节点的子工序查询返回空列表，正常结束递归。
3. 当前递归路径使用 `cnumber + "@" + cversion` 作为访问键。同一键再次出现在当前路径时，抛出“正式工艺层级存在循环”；不同分支复用同一工序不视为循环。
4. `cname`、`cnumber`、`label`、`cversion` 任一为空，抛出“正式工艺工序信息不完整”。
5. 任一节点缺少对应 PDM `op_link` 或 MPM `oid`，整次树查询失败，不返回半棵树。

## 前端交互

现有树形页面继续复用同一展示结构。

- 表单始终校验 `accno`；仅当 `accno` 不是 `C` 前缀时校验 `prtno`，使正式工艺可只输入工艺规程号查询。
- PDM 节点：`externalUrl` 有值，点击后按现有安全打开逻辑跳转。
- MPM 节点：`externalUrl` 为空但 `oid` 有值时，按钮仍可点击；前端调用 `/mes/process/query/file-url`，成功后安全打开响应 URL。
- 同一节点在请求期间显示加载态并防止重复点击。
- MPM 文件 URL 请求失败只显示接口业务错误，保留当前工艺树与查询条件。
- 节点既无 URL 也无 oid 时禁用“查看”。
- 使用请求序号或等价机制，避免较早的文件 URL 响应覆盖较新的点击结果。

## 错误处理

| 场景 | 用户提示 |
|---|---|
| 查询版本接口空响应、失败响应、结构错误、版本缺失 | 正式工艺版本信息查询失败 |
| `CAOE_PP` 无记录或状态非“已发行” | 工艺未发行，无法查看 |
| 一级工序为空 | 未查询到正式工艺工序 |
| 工序字段为空 | 正式工艺工序信息不完整 |
| 当前递归路径形成循环 | 正式工艺层级存在循环 |
| PDM 链接或 MPM oid 缺失 | 正式工艺文件信息不完整 |
| MPM 发布接口失败、结构错误或 URL 缺失 | 工艺文件地址获取失败 |

所有第三方响应解析必须先校验对象/数组类型和空值，不向前端暴露 `NullPointerException`、JSON 解析异常或数据库实现细节。

## 测试策略

### 后端单元测试

- `queryObjectInfo` 请求包含固定 `objType`、单元素 `objNumbers` 和字符串 `isLatest=true`。
- `B.1 -> B`、`A -> A`，以及空版本/空结果/失败响应。
- 已发行状态通过；无记录和非已发行状态均阻断。
- 工序树支持一至多层递归，每层保持字符串 `label` 升序。
- 叶子节点、字段缺失、一级为空和当前路径循环。
- PDM `op_link` 查询串截取与最终 URL 拼接。
- MPM 节点保存 oid，不在树查询期间调用发布接口。
- 发布请求使用小写 `oid`，值带 `OperationEntity:` 前缀，并读取 `result.url`。
- 发布接口的空响应、失败响应、畸形 JSON 和空 URL。
- Controller 的两个接口均不允许 `@PermitAll`。

### Mapper XML 测试

静态校验 SQL 使用准确的表名、字段名、参数名，并确保子工序查询包含 `ORDER BY label ASC`。

### 前端测试

- 正式工艺卡和多层节点映射。
- 正式工艺只输入 `accno` 可提交；临时工艺缺少 `prtno` 仍被阻止。
- PDM 节点直接打开 URL。
- MPM 节点点击后调用真实后端 API，再打开返回 URL。
- MPM 节点加载态、防重复点击、失败后保留树。
- 无 URL 且无 oid 的节点保持禁用。
- 临时工艺既有查询、并发保护和查看行为不回归。

## 验收标准

1. 输入正式工艺号后，页面展示版本、发行后的完整多层工艺树及正确顺序。
2. 非已发行工艺明确提示“工艺未发行，无法查看”。
3. PDM 工序点击后打开拼装后的 Windchill 地址。
4. MPM 工序点击后才调用发布接口，并打开 `result.url`。
5. 所有新增后端测试、前端测试、静态检查和生产构建通过，临时工艺行为保持不变。
