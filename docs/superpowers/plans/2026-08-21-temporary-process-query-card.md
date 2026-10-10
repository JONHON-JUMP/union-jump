# 临时工艺 queryCard 树形展示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补全临时工艺查询参数，把第三方 `Seqno` 明细转换为可直接展示的工艺树，并让 Vue 2 页面使用真实 `queryCard` 接口。

**Architecture:** 后端新增无外部依赖的 `TemporaryProcessTreeAssembler`，专门完成字段映射、稳定数值排序和父子组树；`ProcessServiceImpl` 只负责第三方调用、文档发行校验和卡片组装。前端 API 模块封装现有 `/mes/process/query/card` POST 端点，页面只负责表单、响应归一化和 Element UI 树展示。

**Tech Stack:** Java 8、Spring Boot、Fastjson、JUnit 5、Mockito、Vue 2.7、Element UI 2.15、Node `node:test`

## Global Constraints

- 正式工艺分支不在本次范围内，不改变其前缀判断或第三方协议。
- 临时接口参数固定为 `prtno`、原始 `accno`、`accno` 前 4 位的 `plndept`、以及长度规则生成的 `fxtype`。
- 临时工艺名称取 `Seqdesc`，工序号取原始 `Seqno`，工序编码为空。
- 子工序按最后一个点号寻找直接父节点；缺父、非法和重复序号不能导致记录丢失。
- 查看 URL 必须使用 `CaoeDocInfoDTO.oid`，不能使用第三方 `responseBody.oid`。
- 不引入新依赖，不修改用户已有的无关工作区改动。

---

## File Structure

- Create `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/TemporaryProcessTreeAssembler.java`: 纯数据转换单元，隔离排序和组树规则。
- Create `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/TemporaryProcessTreeAssemblerTest.java`: 覆盖排序、层级、异常序号、重复序号、字段和 URL。
- Modify `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessCardDetailsRespVO.java`: 增加递归 `children`。
- Modify `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImpl.java`: 构造完整请求、校验文档并返回临时工艺卡。
- Create `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImplTest.java`: 隔离验证服务编排和异常。
- Create `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/api/mes/process/card.js`: 封装工艺卡查询请求。
- Modify `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue`: 真实表单和树形响应展示。
- Modify `jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs`: 覆盖 API 参数、响应归一化、失败清理和现有交互。

---

### Task 1: 临时工艺明细排序与组树

**Files:**
- Create: `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/TemporaryProcessTreeAssemblerTest.java`
- Create: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/TemporaryProcessTreeAssembler.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessCardDetailsRespVO.java`

**Interfaces:**
- Consumes: `JSONArray details`, `String documentOid`。
- Produces: `List<ProcessCardDetailsRespVO> assemble(JSONArray details, String documentOid)`；每个响应节点包含 `idx/name/code/no/url/children`。

- [ ] **Step 1: 写排序和父子关系失败测试**

创建测试，用真实 Fastjson 明细验证数值顺序和多级结构：

```java
@Test
void assemble_sortsNumericallyAndBuildsTree() {
    JSONArray details = JSON.parseArray("["
            + "{\"Seqno\":\"10.2\",\"Seqdesc\":\"子工序2\"},"
            + "{\"Seqno\":\"2\",\"Seqdesc\":\"工序2\"},"
            + "{\"Seqno\":\"10.1.2\",\"Seqdesc\":\"孙工序\"},"
            + "{\"Seqno\":\"10\",\"Seqdesc\":\"工序10\"},"
            + "{\"Seqno\":\"10.1\",\"Seqdesc\":\"子工序1\"}]");

    List<ProcessCardDetailsRespVO> result = assembler.assemble(details, "OR:wt.doc.WTDocument:1");

    assertEquals(Arrays.asList("2", "10"), result.stream().map(ProcessCardDetailsRespVO::getNo).collect(toList()));
    assertEquals(Arrays.asList("10.1", "10.2"), result.get(1).getChildren().stream().map(ProcessCardDetailsRespVO::getNo).collect(toList()));
    assertEquals("10.1.2", result.get(1).getChildren().get(0).getChildren().get(0).getNo());
}
```

- [ ] **Step 2: 写保留异常记录和字段映射失败测试**

```java
@Test
void assemble_keepsOrphansInvalidAndDuplicateRows() {
    JSONArray details = JSON.parseArray("["
            + "{\"Seqno\":\"20.1\",\"Seqdesc\":\"缺父\"},"
            + "{\"Seqno\":\"bad\",\"Seqdesc\":\"非法\"},"
            + "{\"Seqno\":\"10\",\"Seqdesc\":\"第一条\"},"
            + "{\"Seqno\":\"10\",\"Seqdesc\":\"重复条\"}]");

    List<ProcessCardDetailsRespVO> result = assembler.assemble(details, "OID-1");

    assertEquals(4, result.size());
    assertEquals("第一条", result.get(0).getName());
    assertNull(result.get(0).getCode());
    assertEquals("http://MESloginUser:MESloginUseradmin@pdm.caoe.com/Windchill/netmarkets/jsp/ext/caoe/mes/export.jsp?oid=OID-1", result.get(0).getUrl());
    assertEquals(Arrays.asList(1L, 2L, 3L, 4L), flatten(result).stream().map(ProcessCardDetailsRespVO::getIdx).collect(toList()));
}
```

- [ ] **Step 3: 运行测试确认 RED**

Run:

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -pl jonhonjump-module-mes -am '-Dtest=TemporaryProcessTreeAssemblerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: FAIL，提示 `TemporaryProcessTreeAssembler` 和 `children` 尚不存在。

- [ ] **Step 4: 给响应明细增加递归 children**

在 `ProcessCardDetailsRespVO` 增加：

```java
@Schema(description = "子工序")
@Builder.Default
private List<ProcessCardDetailsRespVO> children = new ArrayList<>();
```

并添加 `java.util.ArrayList`、`java.util.List` import。

- [ ] **Step 5: 实现最小纯转换器**

实现公开入口和内部记录模型：

```java
@Component
public class TemporaryProcessTreeAssembler {
    static final String VIEW_URL_PREFIX = "http://MESloginUser:MESloginUseradmin@pdm.caoe.com/"
            + "Windchill/netmarkets/jsp/ext/caoe/mes/export.jsp?oid=";

    public List<ProcessCardDetailsRespVO> assemble(JSONArray details, String documentOid) {
        List<Row> rows = IntStream.range(0, details.size())
                .mapToObj(index -> toRow(details.getJSONObject(index), index, documentOid))
                .sorted(this::compareRows)
                .collect(Collectors.toList());
        return buildTreeAndAssignIndexes(rows);
    }

    private Row toRow(JSONObject json, int sourceIndex, String oid) {
        String no = StringUtils.trimToEmpty(json.getString("Seqno"));
        ProcessCardDetailsRespVO node = ProcessCardDetailsRespVO.builder()
                .name(json.getString("Seqdesc"))
                .code(null)
                .no(no)
                .url(VIEW_URL_PREFIX + oid)
                .children(new ArrayList<>())
                .build();
        return new Row(no, sourceIndex, parseParts(no), node);
    }
}
```

`compareRows` 先比较序号是否可解析，再逐段使用 `BigInteger` 比较，段都相同后按段数、最后按 `sourceIndex` 比较。`buildTreeAndAssignIndexes` 先把每个合法 `no` 的第一条放入父索引，再按排序后顺序尝试挂到 `no.substring(0, no.lastIndexOf('.'))`；父不存在、非法或重复记录留在根列表。最后深度优先遍历根列表，为 `idx` 从 `1L` 连续赋值。

- [ ] **Step 6: 运行测试确认 GREEN**

重复 Step 3 命令。

Expected: `TemporaryProcessTreeAssemblerTest` 全部 PASS，exit code `0`。

- [ ] **Step 7: 提交这一独立转换单元**

```powershell
git add -- 'jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/TemporaryProcessTreeAssembler.java' 'jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessCardDetailsRespVO.java' 'jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/TemporaryProcessTreeAssemblerTest.java'
git commit -m "feat: 组织临时工艺工序树"
```

---

### Task 2: queryCard 临时工艺服务编排

**Files:**
- Create: `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImplTest.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImpl.java`

**Interfaces:**
- Consumes: `TemporaryProcessTreeAssembler.assemble(JSONArray, String)`、`ThirdPartyRouteService.invoke(...)`、`CaoeTableMapper.queryDocInfo(String)`。
- Produces: `queryCard(ProcessCardReqVO)` 在临时分支返回单元素 `List<ProcessCardRespVO>`。

- [ ] **Step 1: 写完整参数和卡片结果失败测试**

使用 `@ExtendWith(MockitoExtension.class)`、`@InjectMocks ProcessServiceImpl`，mock `ThirdPartyRouteService`、`CaoeTableMapper`、`TemporaryProcessTreeAssembler`。让泛型 `invoke` 直接返回构造好的 `JSONObject responseBody`，并用 `ArgumentCaptor<String>` 捕获第四个参数：

```java
@Test
void queryCard_buildsTemporaryRequestAndCard() {
    JSONObject body = JSON.parseObject("{\"oid\":\"response-oid\",\"details\":[{\"Seqno\":\"10\",\"ROUTREMARK\":\"DOC-1\"}]}");
    when(thirdPartyRouteService.invoke(eq("1"), eq("JUMP"), eq("WXD"), anyString(), isNull(), any())).thenReturn(body);
    CaoeDocInfoDTO doc = new CaoeDocInfoDTO();
    doc.setDocSate("已发行");
    doc.setOid("document-oid");
    when(caoeTableMapper.queryDocInfo("DOC-1")).thenReturn(doc);
    when(treeAssembler.assemble(any(JSONArray.class), eq("document-oid"))).thenReturn(Collections.singletonList(detail));

    List<ProcessCardRespVO> result = service.queryCard(ProcessCardReqVO.builder()
            .prtno("21ET0-009-39095-B1").accno("43091").build());

    verify(thirdPartyRouteService).invoke(eq("1"), eq("JUMP"), eq("WXD"), requestCaptor.capture(), isNull(), any());
    JSONObject request = JSON.parseObject(requestCaptor.getValue());
    assertEquals("21ET0-009-39095-B1", request.getString("prtno"));
    assertEquals("43091", request.getString("accno"));
    assertEquals("4309", request.getString("plndept"));
    assertEquals("1", request.getString("fxtype"));
    assertEquals("43091", result.get(0).getAccno());
    assertEquals(0, result.get(0).getIsFormal());
    assertEquals(1, result.get(0).getIsFix());
    verify(treeAssembler).assemble(any(JSONArray.class), eq("document-oid"));
}
```

- [ ] **Step 2: 写非返修和文档校验失败测试**

增加 `accno="4309"` 时 `plndept="4309"`、`fxtype="0"`、`isFix=0` 的测试，并分别断言以下消息：

```java
assertEquals("工艺规程号至少需要4位", assertThrows(ServiceException.class,
        () -> service.queryCard(request("430"))).getMessage());
assertEquals("临时工艺文档信息不存在", assertThrows(ServiceException.class,
        () -> service.queryCard(validRequest)).getMessage());
assertEquals("工艺未发行，无法查看", assertThrows(ServiceException.class,
        () -> service.queryCard(validRequest)).getMessage());
assertEquals("临时工艺查看地址缺失", assertThrows(ServiceException.class,
        () -> service.queryCard(validRequest)).getMessage());
```

- [ ] **Step 3: 运行测试确认 RED**

Run:

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -pl jonhonjump-module-mes -am '-Dtest=ProcessServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: FAIL，因为当前只发送 `prtno/fxtype`、临时分支返回 `null` 且缺少空文档校验。

- [ ] **Step 4: 注入转换器并构造完整请求**

```java
@Resource
private TemporaryProcessTreeAssembler temporaryProcessTreeAssembler;

if (reqVO.getAccno().length() < 4) {
    throw exception(new ErrorCode(500, "工艺规程号至少需要4位"));
}
int isFix = reqVO.getAccno().length() > 4 ? YesOrNo.YES.getType() : YesOrNo.NO.getType();
TemporaryProcessReqVO temporaryRequest = TemporaryProcessReqVO.builder()
        .prtno(reqVO.getPrtno())
        .accno(reqVO.getAccno())
        .plndept(reqVO.getAccno().substring(0, 4))
        .fxtype(String.valueOf(isFix))
        .build();
```

- [ ] **Step 5: 补齐文档校验并返回卡片**

在 `queryDocInfo` 后按顺序校验 `null`、发行状态和 OID，再返回：

```java
if (caoeDocInfoDTO == null) {
    throw exception(new ErrorCode(500, "临时工艺文档信息不存在"));
}
if (!CommonConstant.PUBLISHED.equals(caoeDocInfoDTO.getDocSate())) {
    throw exception(new ErrorCode(500, "工艺未发行，无法查看"));
}
if (StringUtils.isBlank(caoeDocInfoDTO.getOid())) {
    throw exception(new ErrorCode(500, "临时工艺查看地址缺失"));
}
List<ProcessCardDetailsRespVO> details = temporaryProcessTreeAssembler.assemble(jsonArray, caoeDocInfoDTO.getOid());
ProcessCardRespVO card = ProcessCardRespVO.builder()
        .accno(reqVO.getAccno())
        .version(null)
        .isFormal(YesOrNo.NO.getType())
        .isFix(isFix)
        .details(details)
        .build();
return Collections.singletonList(card);
```

- [ ] **Step 6: 运行两个后端测试确认 GREEN**

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -pl jonhonjump-module-mes -am '-Dtest=TemporaryProcessTreeAssemblerTest,ProcessServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: 两个测试类全部 PASS，exit code `0`。

- [ ] **Step 7: 提交服务编排**

```powershell
git add -- 'jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImpl.java' 'jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImplTest.java'
git commit -m "feat: 返回临时工艺卡树"
```

---

### Task 3: Vue 页面接入真实 queryCard

**Files:**
- Create: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/api/mes/process/card.js`
- Modify: `jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs`
- Modify: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue`

**Interfaces:**
- Consumes: `queryProcessCard(data): Promise<CommonResultPayload>`；请求数据 `{ prtno: string, accno: string }`；经过请求拦截器后组件获得卡片数组。
- Produces: `normalizeCards(cards): ProcessNode[]`、异步 `handleQuery(): Promise<void>`，以及使用 `details/children` 的树表格。

- [ ] **Step 1: 扩展组件测试加载器并写真实查询失败测试**

测试加载器将 API import 替换为沙箱函数：

```js
function loadComponent(options = {}) {
  const queryProcessCard = options.queryProcessCard || (() => Promise.resolve([]))
  const executableScript = scriptMatch[1]
    .replace(/import \{ queryProcessCard \} from '@\/api\/mes\/process\/card'\s*/, '')
    .replace('export default', 'module.exports =')
  const sandbox = { module: { exports: {} }, exports: {}, window: { open: options.openWindow || (() => null) }, queryProcessCard }
  vm.runInNewContext(executableScript, sandbox, { filename: 'index.vue' })
  return sandbox.module.exports
}
```

新增测试，调用 `handleQuery` 时提供 `$refs.queryForm.validate(callback)`、`$nextTick(fn)` 和响应卡片，断言：

```js
assert.deepEqual(calls, [{ prtno: 'MAT-1', accno: '43091' }])
assert.equal(context.processTree[0].processNo, '43091')
assert.equal(context.processTree[0].children[0].operationNo, '10')
assert.equal(context.processTree[0].children[0].children[0].operationNo, '10.1')
```

再让 API reject，断言 `processTree` 和 `displayProcessTree` 均清空且 `loading === false`。

- [ ] **Step 2: 写 API 文件静态契约失败测试**

读取 `src/api/mes/process/card.js` 并断言包含准确端点、POST 和 `data`：

```js
assert.match(apiSource, /url:\s*['"]\/mes\/process\/query\/card['"]/) 
assert.match(apiSource, /method:\s*['"]post['"]/) 
assert.match(apiSource, /data\s*[,}]/)
```

- [ ] **Step 3: 运行前端测试确认 RED**

Run（工作目录 `jonhonjump-ui/jonhonjump-ui-admin-vue2`）：

```powershell
node --test tests/process-viewer.test.cjs
```

Expected: FAIL，因为 API 文件、真实查询方法和响应归一化尚不存在。

- [ ] **Step 4: 创建 API 模块**

```js
import request from '@/utils/request'

export function queryProcessCard(data) {
  return request({
    url: '/mes/process/query/card',
    method: 'post',
    data
  })
}
```

- [ ] **Step 5: 将页面数据源改为真实卡片**

脚本导入 API，数据初始化为：

```js
data() {
  return {
    loading: false,
    queryParams: { prtno: '', accno: '' },
    processTree: [],
    displayProcessTree: [],
    recentQueries: []
  }
}
```

实现响应归一化，根卡片负责摘要和承载明细，明细字段映射为当前表格使用的名字：

```js
normalizeCards(cards) {
  const mapDetails = (details, parentName, cardIndex) => (details || []).map((detail, index) => ({
    id: `card-${cardIndex}-${detail.no || 'invalid'}-${detail.idx || index}`,
    name: detail.name,
    processNo: '',
    operationNo: detail.no,
    code: detail.code,
    externalUrl: detail.url,
    parentName,
    children: mapDetails(detail.children, detail.name, cardIndex)
  }))
  return (cards || []).map((card, cardIndex) => ({
    id: `card-${cardIndex}-${card.accno}`,
    name: card.isFormal === 1 ? '正式工艺' : '临时工艺',
    processNo: card.accno,
    operationNo: '',
    version: card.version || '—',
    isFormal: card.isFormal,
    isFix: card.isFix,
    children: mapDetails(card.details, card.accno, cardIndex)
  }))
}
```

- [ ] **Step 6: 实现校验、加载和失败清理**

```js
async handleQuery() {
  const valid = await new Promise(resolve => this.$refs.queryForm.validate(resolve))
  if (!valid) return
  this.loading = true
  try {
    const cards = await queryProcessCard({ ...this.queryParams })
    this.processTree = this.normalizeCards(cards)
    this.displayProcessTree = this.processTree
    const key = `${this.queryParams.prtno} / ${this.queryParams.accno}`
    this.recentQueries = [key, ...this.recentQueries.filter(item => item !== key)].slice(0, 3)
    await this.$nextTick()
    this.setAllExpanded(true)
  } catch (error) {
    this.processTree = []
    this.displayProcessTree = []
    throw error
  } finally {
    this.loading = false
  }
}
```

为便于最近查询重放，实际存储对象 `{ prtno, accno, label }`，点击时分别恢复两个字段；测试按对象结构断言。若项目 `$nextTick` 不返回 Promise，则用回调包装成 Promise，保证 `loading` 的测试确定性。

- [ ] **Step 7: 更新模板字段和占位显示**

把单输入改为两个带规则的 `el-form-item`：

```vue
<el-form-item prop="prtno" :rules="[{ required: true, message: '请输入物料号', trigger: 'blur' }]">
  <el-input v-model.trim="queryParams.prtno" placeholder="请输入物料号" @keyup.enter.native="handleQuery" />
</el-form-item>
<el-form-item prop="accno" :rules="[{ required: true, message: '请输入工艺规程号', trigger: 'blur' }]">
  <el-input v-model.trim="queryParams.accno" placeholder="请输入工艺规程号" @keyup.enter.native="handleQuery" />
</el-form-item>
```

表格的工艺编码单元格使用 `{{ scope.row.code || scope.row.processNo || '—' }}`，工序号使用 `{{ scope.row.operationNo || '—' }}`；查看按钮仅对 `externalUrl` 非空的工序启用。摘要中版本和类型均使用后端卡片值，不再引用 Mock 的 `updatedAt/name`。

- [ ] **Step 8: 运行前端测试确认 GREEN**

```powershell
node --test tests/process-viewer.test.cjs
```

Expected: 全部测试 PASS，exit code `0`。

- [ ] **Step 9: 运行目标文件 ESLint**

```powershell
npx eslint --no-ignore --rule "vue/name-property-casing: off" src/api/mes/process/card.js src/views/mes/process/card/index.vue
```

Expected: exit code `0`，无本次文件错误。

- [ ] **Step 10: 提交前端接入**

```powershell
git add -- 'jonhonjump-ui/jonhonjump-ui-admin-vue2/src/api/mes/process/card.js' 'jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue' 'jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs'
git commit -m "feat: 展示临时工艺树"
```

---

### Task 4: 全量验证与交付检查

**Files:**
- Verify only; no production files should change in this task.

**Interfaces:**
- Consumes: Tasks 1–3 的后端服务、树转换器、API 模块和 Vue 页面。
- Produces: 可复现的测试、静态检查、构建和差异证据。

- [ ] **Step 1: 运行后端目标测试**

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -pl jonhonjump-module-mes -am '-Dtest=TemporaryProcessTreeAssemblerTest,ProcessServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: exit code `0`，两个测试类无失败。

- [ ] **Step 2: 运行前端行为测试**

工作目录：`jonhonjump-ui/jonhonjump-ui-admin-vue2`

```powershell
node --test tests/process-viewer.test.cjs
```

Expected: exit code `0`，无失败。

- [ ] **Step 3: 运行前端静态检查**

```powershell
npx eslint --no-ignore --rule "vue/name-property-casing: off" src/api/mes/process/card.js src/views/mes/process/card/index.vue
```

Expected: exit code `0`。

- [ ] **Step 4: 运行 MES 编译与前端生产构建**

仓库根目录：

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -pl jonhonjump-module-mes -am -DskipTests compile
```

前端目录：

```powershell
node --openssl-legacy-provider node_modules/@vue/cli-service/bin/vue-cli-service.js build --mode prod --no-progress
```

Expected: 两条命令 exit code `0`；允许仓库既有的依赖、CSS 顺序和包体积 warning，但不能有本功能编译错误。

- [ ] **Step 5: 检查需求与差异范围**

```powershell
git diff --check HEAD^..HEAD
git status --short
```

逐项确认：四个临时请求参数、`Seqno` 数值排序、点号父子关系、`Seqdesc` 名称、空编码、`CaoeDocInfoDTO.oid` URL、真实前端 API、失败清理均有实现和测试。保留并报告任务开始前已有的 `application-local.yaml`、`yarn.lock` 和旧文档改动，不把它们归为本次工作。
