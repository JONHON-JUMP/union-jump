# Material-Only Multi-Process Query Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow MES users to query by material number alone and display every valid formal and temporary process group returned for that material.

**Architecture:** Keep `ProcessServiceImpl` as the orchestration boundary. It selects one of three query modes, groups material-only temporary-interface details by `ROUTREMARK`, reuses the existing formal-card builder for `C` groups, and builds isolated temporary cards for other groups. The Vue 2 page only changes cross-field validation, request construction, recent-query labels, and multi-card summary semantics because its existing normalizer already accepts a card array.

**Tech Stack:** Java 8, Spring Boot, Fastjson, MyBatis mapper mocks, JUnit 5/Mockito, Vue 2, Element UI, Node.js built-in test runner.

## Global Constraints

- Material-only third-party request must contain `prtno`, `plndept: ""`, `accno: ""`, and `fxtype: "0"`.
- Group by trimmed `ROUTREMARK`; skip blank `ROUTREMARK` rows.
- A group whose `ROUTREMARK` starts with uppercase `C` uses the existing formal process flow.
- Formal groups precede temporary groups; each category is sorted ascending by `ROUTREMARK`.
- A temporary group card displays its `ROUTREMARK` as `accno` and uses only that group's rows and document OID.
- Skip a group that fails with an expected business error; return valid groups. If none succeed, report `未查询到有效工艺信息`.
- Preserve explicit formal and explicit temporary query behavior.
- Preserve the user's staged `jonhonjump-ui/jonhonjump-ui-admin-vue2/build.bat`; never include it in feature commits.

---

## File Structure

- Create `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImplTest.java`: focused service orchestration tests using mocked HTTP, mapper, and assemblers.
- Modify `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessCardReqVO.java`: make `accno` optional at DTO binding level; service owns conditional validation.
- Modify `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImpl.java`: query-mode dispatch, material-only grouping, ordering, partial success, and reusable card builders.
- Modify `jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs`: cross-field validation, material-only request, multi-card summary, and recent-query regression tests.
- Modify `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue`: conditional form rules and multi-card presentation.

### Task 1: Backend query modes and material grouping

**Files:**
- Create: `jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImplTest.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessCardReqVO.java`
- Modify: `jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImpl.java`

**Interfaces:**
- Consumes: `TemporaryProcessReqVO`, `TemporaryProcessTreeAssembler.assemble(JSONArray, String)`, `FormalProcessTreeAssembler.assemble(String, String, boolean)`, and existing `CaoeTableMapper` queries.
- Produces: unchanged public signature `List<ProcessCardRespVO> queryCard(ProcessCardReqVO reqVO)` with one or many ordered cards.
- Produces internally: `buildFormalCard(String)`, `queryExplicitTemporaryCard(String, String)`, `queryCardsByMaterial(String)`, `buildTemporaryCard(String, JSONArray, int)`, and `requireTemporaryDetails(JSONObject)`.

- [ ] **Step 1: Write failing service tests for validation and the four-field material-only request**

Create a Mockito-based test fixture that injects mock collaborators and fixed endpoint URLs. Capture the temporary request JSON and assert these cases:

```java
@Test
void rejectsEmptyMaterialAndProcessNumbers() {
    ServiceException error = assertThrows(ServiceException.class,
            () -> service.queryCard(ProcessCardReqVO.builder().prtno(" ").accno(null).build()));
    assertEquals("物料号和工艺规程号不能同时为空", error.getMessage());
}

@Test
void rejectsExplicitTemporaryProcessWithoutMaterial() {
    ServiceException error = assertThrows(ServiceException.class,
            () -> service.queryCard(ProcessCardReqVO.builder().accno("43091").build()));
    assertEquals("临时工艺必须输入物料号", error.getMessage());
}

@Test
void materialOnlyQuerySendsAllFourFields() {
    // Stub the temporary endpoint with one published temporary ROUTREMARK group.
    service.queryCard(ProcessCardReqVO.builder().prtno("MAT-1").accno("").build());

    JSONObject request = JSONObject.parseObject(capturedTemporaryRequestBody());
    assertEquals("MAT-1", request.getString("prtno"));
    assertEquals("", request.getString("plndept"));
    assertEquals("", request.getString("accno"));
    assertEquals("0", request.getString("fxtype"));
}
```

The fixture must stub the temporary response in the real envelope shape:

```java
private String temporaryEnvelope(JSONArray details) {
    JSONObject body = new JSONObject();
    body.put("details", details);
    JSONObject envelope = new JSONObject();
    envelope.put("retCode", "200");
    envelope.put("responseBody", body);
    return envelope.toJSONString();
}
```

- [ ] **Step 2: Run the focused backend test and verify it fails**

Run:

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -pl jonhonjump-module-mes -am -Pprocess-tests "-Dtest=ProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL because blank `accno` is rejected or dereferenced and material-only dispatch does not exist.

- [ ] **Step 3: Write failing mixed-group, ordering, isolation, and partial-success tests**

Add a mixed response in deliberately scrambled order: temporary `T-20`, formal `CX-20`, blank, temporary `T-10`, formal `C-10`. Stub formal versions/states and temporary documents. Assert:

```java
List<ProcessCardRespVO> cards = service.queryCard(
        ProcessCardReqVO.builder().prtno("MAT-1").accno("").build());

assertEquals(Arrays.asList("C-10", "CX-20", "T-10", "T-20"),
        cards.stream().map(ProcessCardRespVO::getAccno).collect(Collectors.toList()));
assertEquals(Arrays.asList(1, 1, 0, 0),
        cards.stream().map(ProcessCardRespVO::getIsFormal).collect(Collectors.toList()));
verify(formalProcessTreeAssembler).assemble("C-10", "A", false);
verify(formalProcessTreeAssembler).assemble("CX-20", "B", true);
```

Capture both temporary assembler arguments and assert each `JSONArray` contains only its own `ROUTREMARK`, and that `T-10` uses `DOC-OID-10` while `T-20` uses `DOC-OID-20`.

Add a test where one formal group is unpublished but a temporary group is valid; assert only the temporary card is returned. Add a test where every group is blank or fails and assert `未查询到有效工艺信息`.

- [ ] **Step 4: Run the expanded backend test and verify it fails**

Run the same Maven command. Expected: FAIL because the response is not grouped and `queryFormalCard` cannot be reused as a single-card builder.

- [ ] **Step 5: Implement query-mode dispatch and reusable builders**

Remove `@NotEmpty` from `ProcessCardReqVO.accno` and change its schema mode to `NOT_REQUIRED`.

Refactor the service entry point to normalize inputs before calling `startsWith`:

```java
@Override
public List<ProcessCardRespVO> queryCard(ProcessCardReqVO reqVO) {
    String prtno = reqVO == null ? "" : StringUtils.trimToEmpty(reqVO.getPrtno());
    String accno = reqVO == null ? "" : StringUtils.trimToEmpty(reqVO.getAccno());
    if (StringUtils.isBlank(prtno) && StringUtils.isBlank(accno)) {
        throw exception(new ErrorCode(500, "物料号和工艺规程号不能同时为空"));
    }
    if (accno.startsWith(CommonConstant.PDM_FORMAL_ACCNO_PREFIX)) {
        return Collections.singletonList(buildFormalCard(accno));
    }
    if (StringUtils.isNotBlank(accno)) {
        if (StringUtils.isBlank(prtno)) {
            throw exception(new ErrorCode(500, "临时工艺必须输入物料号"));
        }
        return Collections.singletonList(queryExplicitTemporaryCard(prtno, accno));
    }
    return queryCardsByMaterial(prtno);
}
```

Extract the current formal code into `private ProcessCardRespVO buildFormalCard(String accno)`. Extract the current explicit temporary branch into `private ProcessCardRespVO queryExplicitTemporaryCard(String prtno, String accno)` without changing its existing `isFix`, `plndept`, response-level OID validation, repair `routnumebr`, or document checks.

- [ ] **Step 6: Implement material-only grouping, sorting, and partial success**

Build the third-party request exactly as confirmed, then group only JSON objects with nonblank trimmed `ROUTREMARK`:

```java
private List<ProcessCardRespVO> queryCardsByMaterial(String prtno) {
    TemporaryProcessReqVO request = TemporaryProcessReqVO.builder()
            .prtno(prtno).plndept("").accno("").fxtype("0").build();
    JSONArray details = requireTemporaryDetails(
            queryTemporaryProcessInfo(JSON.toJSONString(request)));

    Map<String, JSONArray> groups = new HashMap<>();
    for (int index = 0; index < details.size(); index++) {
        JSONObject detail = details.getJSONObject(index);
        String routRemark = StringUtils.trimToEmpty(detail.getString(CommonConstant.ROUTREMARK));
        if (StringUtils.isBlank(routRemark)) {
            continue;
        }
        groups.computeIfAbsent(routRemark, ignored -> new JSONArray()).add(detail);
    }

    List<String> groupNumbers = new ArrayList<>(groups.keySet());
    groupNumbers.sort(Comparator
            .comparing((String number) -> !number.startsWith(CommonConstant.PDM_FORMAL_ACCNO_PREFIX))
            .thenComparing(Comparator.naturalOrder()));

    List<ProcessCardRespVO> cards = new ArrayList<>();
    for (String groupNumber : groupNumbers) {
        try {
            cards.add(groupNumber.startsWith(CommonConstant.PDM_FORMAL_ACCNO_PREFIX)
                    ? buildFormalCard(groupNumber)
                    : buildTemporaryCard(groupNumber, groups.get(groupNumber), YesOrNo.NO.getType()));
        } catch (ServiceException groupException) {
            log.warn("跳过无效工艺分组, routRemark: {}, reason: {}",
                    groupNumber, groupException.getMessage());
        }
    }
    if (cards.isEmpty()) {
        throw exception(new ErrorCode(500, "未查询到有效工艺信息"));
    }
    return cards;
}
```

`buildTemporaryCard` must query `CaoeDocInfoDTO` by the group number, check existence/state/OID with existing messages, call `temporaryProcessTreeAssembler.assemble(groupDetails, doc.getOid())`, and populate `accno=groupNumber`, `version=null`, `isFormal=0`, and the supplied `isFix`.

Keep interface-level failures outside the per-group `try/catch`. Catch only `ServiceException` so programming errors and infrastructure failures that were not converted to business exceptions remain visible.

- [ ] **Step 7: Run backend tests and commit the backend slice**

Run the focused Maven command. Expected: PASS for every `ProcessServiceImplTest` test.

Then commit only the three backend paths:

```powershell
git add -- jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/controller/admin/vo/ProcessCardReqVO.java jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImpl.java jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process/service/ProcessServiceImplTest.java
git commit -m "feat: support material-only multi-process queries"
```

### Task 2: Frontend cross-field validation and multi-card summary

**Files:**
- Modify: `jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs`
- Modify: `jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue`

**Interfaces:**
- Consumes: unchanged array response from `queryProcessCard`.
- Produces: `buildQueryRequest({ prtno, accno })` supporting material-only input; computed `summaryProcessNo`, `summaryVersion`, `summaryProcessType`, and `summaryOrderType`.

- [ ] **Step 1: Write failing request and cross-field validation tests**

Extend `createContext` so the form mock includes `clearValidate` and `validateField`. Add tests that call the validators from `component.computed.materialRules` and `component.computed.accnoRules` with contexts representing:

```javascript
// Valid: formal accno only
{ prtno: '', accno: 'CX0000000048' }
// Valid: material only
{ prtno: 'MAT-1', accno: '' }
// Valid: explicit temporary
{ prtno: 'MAT-1', accno: '43091' }
// Invalid: both blank
{ prtno: '', accno: '' }
// Invalid: non-C accno without material
{ prtno: '', accno: '43091' }
```

Extend the request test:

```javascript
const materialOnlyRequest = component.methods.buildQueryRequest.call(
  context, { prtno: 'MAT-1', accno: '' }
)
assert.equal(materialOnlyRequest.prtno, 'MAT-1')
assert.equal(materialOnlyRequest.accno, '')
```

- [ ] **Step 2: Write failing multi-card summary and material-only recent-query tests**

Create one formal and one temporary response card, normalize them, and assert:

```javascript
assert.equal(component.computed.summaryProcessNo.call(context), '2 组工艺')
assert.equal(component.computed.summaryVersion.call(context), '—')
assert.equal(component.computed.summaryProcessType.call(context), '混合工艺')
```

Execute `handleQuery` with `{ prtno: 'MAT-1', accno: '' }` and assert the saved recent item label is exactly `MAT-1`, with no slash. Replay that item and assert both fields are restored.

- [ ] **Step 3: Run the frontend tests and verify they fail**

Run:

```powershell
node --test tests/process-viewer.test.cjs
```

from `jonhonjump-ui/jonhonjump-ui-admin-vue2`. Expected: FAIL because `accno` is statically required and summary values come only from the first card.

- [ ] **Step 4: Implement conditional validators and related-field revalidation**

Replace the static `accno` rule with `:rules="accnoRules"` and add `@input="handlePrtnoInput"` to the material input.

Use Element UI callback validators:

```javascript
materialRules() {
  return [{
    trigger: 'blur',
    validator: (rule, value, callback) => {
      const material = String(value || '').trim()
      const accno = String(this.queryParams.accno || '').trim()
      if (!material && !this.isFormalProcess(accno)) {
        callback(new Error(accno ? '临时工艺必须输入物料号' : '请输入物料号或工艺规程号'))
        return
      }
      callback()
    }
  }]
},
accnoRules() {
  return [{
    trigger: 'blur',
    validator: (rule, value, callback) => {
      if (!String(value || '').trim() && !String(this.queryParams.prtno || '').trim()) {
        callback(new Error('请输入物料号或工艺规程号'))
        return
      }
      callback()
    }
  }]
}
```

Both input handlers should clear and revalidate the counterpart when a form instance exists. Avoid recursive validation by calling `clearValidate` first and `validateField` only for the other property.

Keep request construction formal-compatible:

```javascript
buildQueryRequest(params) {
  const accno = String(params.accno || '').trim()
  const prtno = String(params.prtno || '').trim()
  if (this.isFormalProcess(accno)) return { accno }
  return { prtno, accno }
}
```

- [ ] **Step 5: Implement accurate multi-card summary and recent labels**

Add computed values:

```javascript
summaryProcessNo() {
  return this.visibleProcessCount > 1 ? `${this.visibleProcessCount} 组工艺` : this.activeProcess.processNo
},
summaryVersion() {
  return this.visibleProcessCount > 1 ? '—' : this.activeProcess.version
},
summaryProcessType() {
  const types = new Set(this.displayProcessTree.map(card => card.isFormal))
  if (types.size > 1) return '混合工艺'
  return this.activeProcess.name
},
summaryOrderType() {
  const fixTypes = new Set(this.displayProcessTree.map(card => card.isFix))
  if (fixTypes.size > 1) return '混合'
  return this.activeProcess.isFix === 1 ? '返修' : '普通'
}
```

Bind the summary template to these values. Generate recent labels with a small helper or explicit branches: both fields use `物料号 / 工艺规程号`, material-only uses the material number, and formal-only uses the process number.

- [ ] **Step 6: Run frontend tests and commit the frontend slice**

Run `node --test tests/process-viewer.test.cjs`. Expected: all tests PASS.

Then commit only the page and its test, leaving `build.bat` staged but uncommitted:

```powershell
git add -- jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs
git commit --only -m "feat: display material process groups" -- jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs
```

### Task 3: Full verification and handoff

**Files:**
- Verify only; no expected production edits.

**Interfaces:**
- Consumes: completed backend and frontend slices.
- Produces: fresh evidence that the feature works and unrelated user changes remain untouched.

- [ ] **Step 1: Run the focused backend suite**

```powershell
& 'D:\APP\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd' -pl jonhonjump-module-mes -am -Pprocess-tests "-Dtest=ProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: `BUILD SUCCESS`, zero failures and zero errors.

- [ ] **Step 2: Run frontend unit tests**

From `jonhonjump-ui/jonhonjump-ui-admin-vue2`:

```powershell
node --test tests/process-viewer.test.cjs
```

Expected: every subtest passes.

- [ ] **Step 3: Run frontend lint and production build**

```powershell
npm run lint
npm run build:prod
```

Expected: both exit successfully. Record pre-existing warnings separately; do not change unrelated files to silence them.

- [ ] **Step 4: Inspect diffs and whitespace**

```powershell
git diff --check
git status --short
git diff HEAD~2 -- jonhonjump-module-mes/src/main/java/cn/jonhon/jump/module/mes/process jonhonjump-module-mes/src/test/java/cn/jonhon/jump/module/mes/process jonhonjump-ui/jonhonjump-ui-admin-vue2/src/views/mes/process/card/index.vue jonhonjump-ui/jonhonjump-ui-admin-vue2/tests/process-viewer.test.cjs
```

Expected: no whitespace errors; the only unrelated status entry remains the user's staged `jonhonjump-ui/jonhonjump-ui-admin-vue2/build.bat`.

- [ ] **Step 5: Final review and report**

Confirm every design requirement maps to passing tests: three query modes, four-field request, blank-group filtering, ordering, formal reuse, temporary isolation, partial success, all-failed error, cross-field validation, multi-card summary, and material-only recent-query replay. Report changed files, verification commands/results, and the preserved unrelated staged file.
