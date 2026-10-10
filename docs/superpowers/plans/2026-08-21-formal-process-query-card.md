# 正式工艺查询与工艺文件查看 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有工艺卡查询中实现正式工艺版本获取、发行校验、Oracle 多层工序树，以及 PDM/MPM 两类工艺文件查看。

**Architecture:** `ProcessServiceImpl` 编排第三方接口与发行校验，`FormalProcessTreeAssembler` 递归读取 `CaoeTableMapper` 并构树；PDM 节点查询时直接生成 URL，MPM 节点点击时通过独立后端接口将 oid 换成 `result.url`。前端复用现有树表，仅增加条件表单校验和 MPM 查看状态。

**Tech Stack:** Java 8、Spring Boot、MyBatis XML、Oracle、Fastjson、JUnit 5/Mockito、Vue 2、Element UI、Node test runner。

## Global Constraints

- 正式工艺：`CX` 是 MPM，其余 `C` 前缀是 PDM；临时工艺现有行为不得改变。
- `queryObjectInfo` 请求使用 invokeId `2`，版本只取第一个 `.` 前内容。
- `processReleaseForMes` 请求使用 invokeId `3`，外部 form-data 参数名必须是小写 `oid`，值必须为 `OperationEntity:<oid>`，URL 读取 `result.url`。
- `CAOE_PPOPLINK` 每层必须 `ORDER BY label ASC`，下一层查询参数必须是父节点 `cnumber + cversion`。
- 正式工艺只要求 `accno`；临时工艺仍要求 `prtno + accno`。
- 当前工作区已有用户未提交修改：`ProcessServiceImpl.java` 删除了临时工艺“至少4位”校验。实现、暂存和提交时必须保留该修改，禁止覆盖或误提交；对该文件使用交互式分块暂存并核对 `git diff --cached`。
- 用户的 `application-local.yaml`、`yarn.lock` 和既有 staged docs 不在本任务范围，禁止修改或提交。

---

### Task 1: Oracle 正式工艺查询契约

**Files:**
- Create: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/dal/process/oracle/dto/ProcessOperationDTO.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/dal/process/oracle/CaoeTableMapper.java`
- Modify: `jonhonjump-module-mes/src/main/resources/mapper/process/CaoePpMapper.xml`
- Create: `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/dal/process/oracle/CaoeTableMapperXmlTest.java`

**Interfaces:**
- Produces: `String queryProcessState(String accno, String version)`
- Produces: `List<ProcessOperationDTO> queryChildOperations(String number, String version)`
- Produces: `String queryPdmOperationLink(String number, String version)`
- Produces: `String queryMpmOperationOid(String number, String version)`

- [ ] **Step 1: Write the failing Mapper XML contract test**

读取 `CaoePpMapper.xml` 并标准化空白，断言四个 statement 与关键条件：

```java
@Test
void formalProcessSqlUsesExactTablesParametersAndOrdering() throws Exception {
    String xml = new String(Files.readAllBytes(Paths.get(
            "src/main/resources/mapper/process/CaoePpMapper.xml")), StandardCharsets.UTF_8)
            .replaceAll("\\s+", " ");
    assertTrue(xml.contains("id=\"queryProcessState\""));
    assertTrue(xml.matches("(?is).*FROM CAOE_PP.*PP_NUMBER = #\\{accno}.*PP_VERSION = #\\{version}.*"));
    assertTrue(xml.matches("(?is).*id=\"queryChildOperations\".*FROM CAOE_PPOPLINK.*FNUMBER = #\\{number}.*FVERSION = #\\{version}.*ORDER BY label ASC.*"));
    assertTrue(xml.matches("(?is).*id=\"queryPdmOperationLink\".*SELECT op_link.*FROM CAOE_OP.*"));
    assertTrue(xml.matches("(?is).*id=\"queryMpmOperationOid\".*SELECT oid.*FROM CAOE_OP.*"));
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -q -pl jonhonjump-module-mes -am -Pprocess-tests "-Dtest=CaoeTableMapperXmlTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.test.skip=false" test
```

Expected: FAIL because the four formal statements do not exist.

- [ ] **Step 3: Add DTO, Mapper methods, and exact SQL**

`ProcessOperationDTO` contains Lombok `@Data` fields:

```java
private String cname;
private String cnumber;
private String label;
private String cversion;
```

Mapper methods use `@Param` names exactly matching the XML. Add:

```xml
<select id="queryProcessState" resultType="java.lang.String">
    SELECT pp_state FROM CAOE_PP
    WHERE PP_NUMBER = #{accno} AND PP_VERSION = #{version}
</select>
<select id="queryChildOperations"
        resultType="cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.ProcessOperationDTO">
    SELECT cname, cnumber, label, cversion FROM CAOE_PPOPLINK
    WHERE FNUMBER = #{number} AND FVERSION = #{version}
    ORDER BY label ASC
</select>
<select id="queryPdmOperationLink" resultType="java.lang.String">
    SELECT op_link FROM CAOE_OP
    WHERE OP_NUMBER = #{number} AND OP_VERSION = #{version}
</select>
<select id="queryMpmOperationOid" resultType="java.lang.String">
    SELECT oid FROM CAOE_OP
    WHERE OP_NUMBER = #{number} AND OP_VERSION = #{version}
</select>
```

- [ ] **Step 4: Run the Mapper test and verify GREEN**

Expected: `CaoeTableMapperXmlTest` PASS.

- [ ] **Step 5: Commit only Task 1 files**

```powershell
git add jonhonjump-module-mes/src/main/resources/mapper/process/CaoePpMapper.xml jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/dal/process/oracle/CaoeTableMapper.java jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/dal/process/oracle/dto/ProcessOperationDTO.java jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/dal/process/oracle/CaoeTableMapperXmlTest.java
git commit -m "feat: 增加正式工艺 Oracle 查询"
```

---

### Task 2: 正式工艺递归树组装器

**Files:**
- Create: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/FormalProcessTreeAssembler.java`
- Create: `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/FormalProcessTreeAssemblerTest.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessCardDetailsRespVO.java`

**Interfaces:**
- Consumes: Task 1 Mapper methods and `ProcessOperationDTO`.
- Produces: `List<ProcessCardDetailsRespVO> assemble(String accno, String version, boolean mpm)`.
- Produces: `ProcessCardDetailsRespVO.oid` for MPM nodes.

- [ ] **Step 1: Write failing recursion, ordering, URL, and cycle tests**

Use Mockito to return root operations `0010/D1/A` and `0040/D4/B`, then return a child `0020/D2/A` under `D1/A`. Assert:

```java
List<ProcessCardDetailsRespVO> tree = assembler.assemble("C100", "A", false);
assertEquals(Arrays.asList("0010", "0040"), tree.stream()
        .map(ProcessCardDetailsRespVO::getNo).collect(Collectors.toList()));
assertEquals("0020", tree.get(0).getChildren().get(0).getNo());
assertEquals("D2", tree.get(0).getChildren().get(0).getCode());
assertEquals("http://pdm.caoe.com/Windchill/netmarkets/jsp/ext/caoe/mpml/routCard.jsp?oid=child",
        tree.get(0).getChildren().get(0).getUrl());
```

Add separate tests asserting MPM nodes set `oid` and leave `url=null`, malformed operation fields throw “正式工艺工序信息不完整”, missing file info throws “正式工艺文件信息不完整”, and `D1@A -> D1@A` throws “正式工艺层级存在循环”. Verify DFS indexes are `1..N`.

- [ ] **Step 2: Run `FormalProcessTreeAssemblerTest` and verify RED**

Expected: compilation failure because assembler and `oid` do not exist.

- [ ] **Step 3: Implement minimal recursive assembler**

Core method structure:

```java
public List<ProcessCardDetailsRespVO> assemble(String accno, String version, boolean mpm) {
    List<ProcessCardDetailsRespVO> roots = buildChildren(accno, version, mpm, new HashSet<>());
    if (roots.isEmpty()) throw exception(new ErrorCode(500, "未查询到正式工艺工序"));
    assignIndexes(roots, new long[]{1L});
    return roots;
}

private List<ProcessCardDetailsRespVO> buildChildren(
        String number, String version, boolean mpm, Set<String> path) {
    List<ProcessOperationDTO> operations = mapper.queryChildOperations(number, version);
    List<ProcessCardDetailsRespVO> nodes = new ArrayList<>();
    for (ProcessOperationDTO operation : operations == null ? Collections.emptyList() : operations) {
        validateOperation(operation);
        String key = operation.getCnumber() + "@" + operation.getCversion();
        if (!path.add(key)) throw exception(new ErrorCode(500, "正式工艺层级存在循环"));
        ProcessCardDetailsRespVO node = createNode(operation, mpm);
        node.setChildren(buildChildren(operation.getCnumber(), operation.getCversion(), mpm, path));
        path.remove(key);
        nodes.add(node);
    }
    return nodes;
}
```

PDM URL 仅接受第一个 `?` 后非空查询串；MPM 调用 `queryMpmOperationOid`。`idx` 在完整树创建后深度优先赋值。

- [ ] **Step 4: Run assembler tests and verify GREEN**

Expected: all assembler tests PASS.

- [ ] **Step 5: Commit Task 2**

```powershell
git add jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/FormalProcessTreeAssembler.java jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/FormalProcessTreeAssemblerTest.java jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessCardDetailsRespVO.java
git commit -m "feat: 递归组织正式工艺树"
```

---

### Task 3: 正式工艺版本、发行校验与 queryCard 编排

**Files:**
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/FormalProcessReqVO.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImpl.java`
- Modify: `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImplTest.java`

**Interfaces:**
- Consumes: `FormalProcessTreeAssembler.assemble(accno, version, mpm)`.
- Produces: formal `ProcessCardRespVO` from existing `queryCard`.

- [ ] **Step 1: Extend service tests first**

Capture the fourth `ThirdPartyRouteService.invoke` argument for invokeId `2`. Assert parsed JSON:

```java
assertEquals("com.glaway.dtp.business.model.process.ProcessModel", request.getString("objType"));
assertEquals(Collections.singletonList("CX0000000048"), request.getJSONArray("objNumbers").toJavaList(String.class));
assertEquals("true", request.getString("isLatest"));
```

Return `{success:true,code:200,result:[{version:"B.1"}]}` and assert Mapper is called with `("CX0000000048", "B")`, assembler with `mpm=true`, and response has `version=B,isFormal=1,isFix=0`. Add PDM `C100/A.3` coverage and failure cases for failed/malformed response, missing version, and non-published/null state.

- [ ] **Step 2: Run the new service tests and verify RED**

Expected: formal branch returns `null` or uses the old invokeId/request shape.

- [ ] **Step 3: Replace only the formal branch with validated orchestration**

Change `FormalProcessReqVO` fields to:

```java
private String objType;
private List<String> objNumbers;
private String isLatest;
```

The callback must catch JSON parse errors and validate `success`, numeric/string code `200`, non-empty result array, first object, and version. Extract version with:

```java
String rawVersion = result.getJSONObject(0).getString("version");
String version = StringUtils.substringBefore(rawVersion, ".");
if (StringUtils.isBlank(version)) throw formalVersionError();
```

Then require `CommonConstant.PUBLISHED.equals(mapper.queryProcessState(accno, version))`, call the assembler, and return a singleton formal card.

Before editing, re-read the current working copy. Preserve the user’s existing removal of the temporary `accno.length() < 4` check. Do not restore it.

- [ ] **Step 4: Run all process service and assembler tests**

Expected: formal tests and all existing temporary tests PASS.

- [ ] **Step 5: Stage only this task’s hunks and verify exclusion of user changes**

```powershell
git add jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/FormalProcessReqVO.java jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImplTest.java
git add -p jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImpl.java
git diff --cached --check
git diff --cached
```

The cached diff must not contain deletion of “工艺规程号至少需要4位”. Commit:

```powershell
git commit -m "feat: 返回正式工艺卡树"
```

---

### Task 4: MPM 点击查看后端接口

**Files:**
- Create: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessFileUrlReqVO.java`
- Create: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessFileUrlRespVO.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessService.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImpl.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/ProcessController.java`
- Modify: `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImplTest.java`
- Modify: `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/controller/admin/ProcessControllerSecurityTest.java`

**Interfaces:**
- Produces: `ProcessFileUrlRespVO queryFileUrl(ProcessFileUrlReqVO reqVO)`.
- Produces: authenticated `POST /mes/process/query/file-url`.

- [ ] **Step 1: Write failing release contract and security tests**

Capture invokeId `3` request JSON and assert:

```java
assertEquals("OperationEntity:308233850219293315", request.getString("oid"));
assertFalse(request.containsKey("Oid"));
assertEquals("http://mpm.example/process.pdf", service.queryFileUrl(req).getUrl());
```

Use success body `{success:true,code:200,result:{url:"http://mpm.example/process.pdf"}}`. Add cases for blank oid, failed response, `null`, malformed JSON, missing result, and blank URL, all producing “工艺文件地址获取失败”. Extend reflection security test to assert the new controller method has no `@PermitAll`.

- [ ] **Step 2: Run tests and verify RED**

Expected: missing DTO/service/controller method.

- [ ] **Step 3: Implement DTOs, service method, and endpoint**

`ProcessFileUrlReqVO.oid` uses `@NotBlank`; `ProcessFileUrlRespVO` is a Lombok builder DTO. Service sends:

```java
String payload = JSON.toJSONString(Collections.singletonMap(
        "oid", "OperationEntity:" + reqVO.getOid()));
```

The invokeId `3` callback validates the same success envelope rules and reads `result.url`. Controller returns `success(processService.queryFileUrl(reqVO))` and does not add `@PermitAll`.

- [ ] **Step 4: Run backend process tests and verify GREEN**

Expected: service, assembler, Mapper XML, and security tests PASS.

- [ ] **Step 5: Partial-stage `ProcessServiceImpl` again, inspect, and commit**

Use `git add -p` for `ProcessServiceImpl.java`; cached diff must still exclude the user’s temporary validation deletion.

```powershell
git commit -m "feat: 获取 MPM 工艺文件地址"
```

---

### Task 5: Vue 正式工艺查询与 MPM 查看交互

**Files:**
- Modify: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/api/mes/process/card.js`
- Modify: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue`
- Modify: `jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs`

**Interfaces:**
- Consumes: `POST /mes/process/query/file-url`, request `{oid}`, response `{url}` or common envelope `{data:{url}}`.
- Produces: conditional material validation and per-node MPM loading.

- [ ] **Step 1: Write failing frontend tests**

Extend the VM sandbox to inject `queryProcessFileUrl`. Add tests that:

1. Normalize `detail.oid` to node `oid`.
2. `canView(row)` is true for URL or oid.
3. Formal `accno=CX0000000048` validates/submits with blank `prtno`; temporary `accno=4309` requires `prtno`.
4. MPM click calls API with `{oid}`, opens `data.url`, and toggles only that row’s loading key.
5. Double-click while loading makes one request.
6. Release failure does not clear `processTree`.
7. Existing PDM/temporary direct URL behavior still passes.

- [ ] **Step 2: Run Node tests and verify RED**

```powershell
node --test tests/process-viewer.test.cjs
```

Expected: missing API/function/oid handling assertions fail.

- [ ] **Step 3: Implement API and UI changes**

Add:

```js
export function queryProcessFileUrl(data) {
  return request({ url: '/mes/process/query/file-url', method: 'post', data })
}
```

Use dynamic material validation based on `/^C/.test(queryParams.accno)`. Map `oid: detail.oid`. Replace button disabled condition with `!canView(row)` and loading with `viewLoadingId === row.id`. Make `handleView` async:

```js
if (row.externalUrl) return this.openExternalUrl(row.externalUrl)
if (!row.oid || this.viewLoadingId) return
this.viewLoadingId = row.id
try {
  const response = await queryProcessFileUrl({ oid: row.oid })
  const result = response && response.data ? response.data : response
  this.openExternalUrl(result.url)
} finally {
  if (this.viewLoadingId === row.id) this.viewLoadingId = ''
}
```

Extract `openExternalUrl` to retain `_blank`, `noopener,noreferrer`, and `opener=null` behavior. Do not clear the tree in file URL error handling.

- [ ] **Step 4: Run frontend tests and ESLint**

```powershell
node --test tests/process-viewer.test.cjs
& '.\node_modules\.bin\eslint.cmd' --no-ignore --rule 'vue/name-property-casing: off' 'src\views\mes\process\card\index.vue' 'src\api\mes\process\card.js'
```

Expected: all tests pass; ESLint exits 0.

- [ ] **Step 5: Commit frontend files**

```powershell
git add jonhonjump-ui/jonhonjump-ui-admin-vue2/src/api/mes/process/card.js jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs
git commit -m "feat: 查看正式工艺文件"
```

---

### Task 6: 全量验证与审查

**Files:**
- Verify only; modify only if a failing test identifies a task-scoped defect.

**Interfaces:**
- Consumes all prior deliverables.
- Produces verified feature branch/workspace state ready for integration.

- [ ] **Step 1: Run complete process backend suite**

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -q -pl jonhonjump-module-mes -am -Pprocess-tests "-Dtest=*process*" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.test.skip=false" test
```

If wildcard selection is not accepted by Surefire, enumerate `TemporaryProcessTreeAssemblerTest,FormalProcessTreeAssemblerTest,ProcessServiceImplTest,CaoeTableMapperXmlTest,ProcessControllerSecurityTest`.

- [ ] **Step 2: Compile the MES reactor**

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -pl jonhonjump-module-mes -am -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run frontend tests, lint, and production build**

```powershell
node --test tests/process-viewer.test.cjs
& '.\node_modules\.bin\eslint.cmd' --no-ignore --rule 'vue/name-property-casing: off' 'src\views\mes\process\card\index.vue' 'src\api\mes\process\card.js'
node --openssl-legacy-provider '.\node_modules\@vue\cli-service\bin\vue-cli-service.js' build --mode prod
```

Expected: tests and lint exit 0; build completes. Record existing unrelated warnings separately.

- [ ] **Step 4: Audit repository boundaries**

```powershell
git diff --check
git status --short
git diff --cached
```

Confirm user-owned edits remain present and unstaged/uncommitted by this task, especially the temporary validation deletion, `application-local.yaml`, and `yarn.lock`.

- [ ] **Step 5: Request code review and address findings with TDD**

Review the formal-query commit range against this specification. For every accepted defect, add a failing regression test, verify RED, implement the minimal fix, verify GREEN, and commit a focused correction.

