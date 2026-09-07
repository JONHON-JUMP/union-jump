package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.framework.common.exception.ServiceException;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardRespVO;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.CaoeTableMapper;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.CaoeDocInfoDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessServiceImplTest {

    @InjectMocks
    private ProcessServiceImpl service;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CaoeTableMapper caoeTableMapper;
    @Mock
    private TemporaryProcessTreeAssembler temporaryProcessTreeAssembler;
    @Mock
    private FormalProcessTreeAssembler formalProcessTreeAssembler;

    private ArgumentCaptor<HttpEntity<String>> temporaryRequestCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "temporaryProcessUrl", "temporary-url");
        ReflectionTestUtils.setField(service, "temporaryProcessToken", "temporary-token");
        ReflectionTestUtils.setField(service, "formalProcessUrl", "formal-url");
        ReflectionTestUtils.setField(service, "mpmAccessToken", "mpm-token");
        temporaryRequestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    }

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
    void explicitFormalQueryBuildsFormalCard() {
        stubFormalVersion("C-10", "A.1");
        when(caoeTableMapper.queryProcessState("C-10", "A")).thenReturn("已发行");

        ProcessCardRespVO card = service.queryCard(ProcessCardReqVO.builder().accno("C-10").build()).get(0);

        assertEquals("C-10", card.getAccno());
        assertEquals("A", card.getVersion());
        assertEquals(1, card.getIsFormal());
        verify(formalProcessTreeAssembler).assemble("C-10", "A", false);
    }

    @Test
    void explicitOrdinaryTemporaryQueryDisplaysRequestNumberAndResolvesDocumentNumber() {
        JSONArray details = new JSONArray();
        details.add(temporaryDetail("DOC-ORD"));
        stubExplicitTemporaryResponse(details, "TEMP-RESPONSE-OID", null);
        when(caoeTableMapper.queryDocInfo("DOC-ORD")).thenReturn(publishedDocument("DOC-OID-ORD"));

        ProcessCardRespVO card = service.queryCard(
                ProcessCardReqVO.builder().prtno("MAT-1").accno("4309").build()).get(0);

        assertEquals("4309", card.getAccno());
        assertEquals(0, card.getIsFix());
        verify(caoeTableMapper).queryDocInfo("DOC-ORD");
        verify(temporaryProcessTreeAssembler).assemble(details, "DOC-OID-ORD");
    }

    @Test
    void explicitRepairTemporaryQueryDisplaysRequestNumberAndResolvesResponseDocumentNumber() {
        JSONArray details = new JSONArray();
        details.add(temporaryDetail("DETAIL-ROUTE"));
        stubExplicitTemporaryResponse(details, "TEMP-RESPONSE-OID", "DOC-REPAIR");
        when(caoeTableMapper.queryDocInfo("DOC-REPAIR")).thenReturn(publishedDocument("DOC-OID-REPAIR"));

        ProcessCardRespVO card = service.queryCard(
                ProcessCardReqVO.builder().prtno("MAT-1").accno("43091").build()).get(0);

        assertEquals("43091", card.getAccno());
        assertEquals(1, card.getIsFix());
        verify(caoeTableMapper).queryDocInfo("DOC-REPAIR");
        verify(temporaryProcessTreeAssembler).assemble(details, "DOC-OID-REPAIR");
    }

    @Test
    void materialOnlyQuerySendsAllFourFields() {
        JSONArray details = new JSONArray();
        details.add(temporaryDetail("T-10"));
        stubTemporaryResponse(details);
        CaoeDocInfoDTO doc = publishedDocument("DOC-OID-10");
        when(caoeTableMapper.queryDocInfo("T-10")).thenReturn(doc);

        service.queryCard(ProcessCardReqVO.builder().prtno("MAT-1").accno("").build());

        JSONObject request = JSONObject.parseObject(capturedTemporaryRequestBody());
        assertEquals("MAT-1", request.getString("prtno"));
        assertEquals("", request.getString("plndept"));
        assertEquals("", request.getString("accno"));
        assertEquals("0", request.getString("fxtype"));
    }

    @Test
    void materialOnlyQueryGroupsOrdersAndIsolatesEachProcess() {
        JSONArray details = new JSONArray();
        details.add(temporaryDetail("T-20"));
        details.add(temporaryDetail("CX-20"));
        details.add(new JSONObject());
        details.add(temporaryDetail("T-10"));
        details.add(temporaryDetail("C-10"));
        stubTemporaryResponse(details);
        stubFormalVersions();
        when(caoeTableMapper.queryDocInfo("T-10")).thenReturn(publishedDocument("DOC-OID-10"));
        when(caoeTableMapper.queryDocInfo("T-20")).thenReturn(publishedDocument("DOC-OID-20"));
        when(caoeTableMapper.queryProcessState("C-10", "A")).thenReturn("已发行");
        when(caoeTableMapper.queryProcessState("CX-20", "B")).thenReturn("已发行");

        List<ProcessCardRespVO> cards = service.queryCard(
                ProcessCardReqVO.builder().prtno("MAT-1").accno("").build());

        assertEquals(Arrays.asList("C-10", "CX-20", "T-10", "T-20"),
                cards.stream().map(ProcessCardRespVO::getAccno).collect(Collectors.toList()));
        assertEquals(Arrays.asList(1, 1, 0, 0),
                cards.stream().map(ProcessCardRespVO::getIsFormal).collect(Collectors.toList()));
        verify(formalProcessTreeAssembler).assemble("C-10", "A", false);
        verify(formalProcessTreeAssembler).assemble("CX-20", "B", true);

        ArgumentCaptor<JSONArray> groupDetailsCaptor = ArgumentCaptor.forClass(JSONArray.class);
        ArgumentCaptor<String> oidCaptor = ArgumentCaptor.forClass(String.class);
        verify(temporaryProcessTreeAssembler, times(2)).assemble(groupDetailsCaptor.capture(), oidCaptor.capture());
        assertEquals(Arrays.asList("DOC-OID-10", "DOC-OID-20"), oidCaptor.getAllValues());
        assertEquals(Arrays.asList("T-10", "T-20"), groupDetailsCaptor.getAllValues().stream()
                .map(group -> group.getJSONObject(0).getString("ROUTREMARK"))
                .collect(Collectors.toList()));
        assertEquals(Arrays.asList(1, 1), groupDetailsCaptor.getAllValues().stream()
                .map(JSONArray::size).collect(Collectors.toList()));
    }

    @Test
    void materialOnlyQuerySkipsUnpublishedFormalGroupWhenTemporaryGroupIsValid() {
        JSONArray details = new JSONArray();
        details.add(temporaryDetail("C-10"));
        details.add(temporaryDetail("T-10"));
        stubTemporaryResponse(details);
        stubFormalVersions();
        when(caoeTableMapper.queryProcessState("C-10", "A")).thenReturn("未发行");
        when(caoeTableMapper.queryDocInfo("T-10")).thenReturn(publishedDocument("DOC-OID-10"));

        List<ProcessCardRespVO> cards = service.queryCard(
                ProcessCardReqVO.builder().prtno("MAT-1").accno("").build());

        assertEquals(Collections.singletonList("T-10"),
                cards.stream().map(ProcessCardRespVO::getAccno).collect(Collectors.toList()));
    }

    @Test
    void materialOnlyQueryRejectsWhenNoGroupsProduceValidCards() {
        JSONArray details = new JSONArray();
        details.add(new JSONObject());
        details.add(temporaryDetail("T-10"));
        stubTemporaryResponse(details);
        when(caoeTableMapper.queryDocInfo("T-10")).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.queryCard(ProcessCardReqVO.builder().prtno("MAT-1").accno("").build()));

        assertEquals("未查询到有效工艺信息", error.getMessage());
    }

    @Test
    void materialOnlyQueryMergesRouteRemarksAfterTrimming() {
        JSONArray details = new JSONArray();
        details.add(temporaryDetail(" T-10 "));
        details.add(temporaryDetail("T-10"));
        stubTemporaryResponse(details);
        when(caoeTableMapper.queryDocInfo("T-10")).thenReturn(publishedDocument("DOC-OID-10"));

        List<ProcessCardRespVO> cards = service.queryCard(
                ProcessCardReqVO.builder().prtno("MAT-1").accno("").build());

        assertEquals(Collections.singletonList("T-10"),
                cards.stream().map(ProcessCardRespVO::getAccno).collect(Collectors.toList()));
        ArgumentCaptor<JSONArray> groupDetailsCaptor = ArgumentCaptor.forClass(JSONArray.class);
        verify(temporaryProcessTreeAssembler).assemble(groupDetailsCaptor.capture(), eq("DOC-OID-10"));
        assertEquals(2, groupDetailsCaptor.getValue().size());
    }

    private void stubTemporaryResponse(JSONArray details) {
        when(restTemplate.postForEntity(eq("temporary-url"), temporaryRequestCaptor.capture(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(temporaryEnvelope(details), HttpStatus.OK));
    }

    private void stubExplicitTemporaryResponse(JSONArray details, String responseOid, String routNumber) {
        JSONObject body = new JSONObject();
        body.put("details", details);
        body.put("oid", responseOid);
        if (routNumber != null) {
            body.put("routnumebr", routNumber);
        }
        JSONObject envelope = new JSONObject();
        envelope.put("retCode", "200");
        envelope.put("responseBody", body);
        when(restTemplate.postForEntity(eq("temporary-url"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(envelope.toJSONString(), HttpStatus.OK));
    }

    private String capturedTemporaryRequestBody() {
        return temporaryRequestCaptor.getValue().getBody();
    }

    private JSONObject temporaryDetail(String routRemark) {
        JSONObject detail = new JSONObject();
        detail.put("ROUTREMARK", routRemark);
        return detail;
    }

    private void stubFormalVersions() {
        when(restTemplate.postForEntity(eq("formal-url"), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    HttpEntity<String> entity = invocation.getArgument(1);
                    JSONArray numbers = JSONObject.parseObject(entity.getBody()).getJSONArray("objNumbers");
                    String accno = numbers.getString(0);
                    return new ResponseEntity<>(formalEnvelope("C-10".equals(accno) ? "A.1" : "B.1"), HttpStatus.OK);
                });
    }

    private void stubFormalVersion(String accno, String version) {
        when(restTemplate.postForEntity(eq("formal-url"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(formalEnvelope(version), HttpStatus.OK));
    }

    private CaoeDocInfoDTO publishedDocument(String oid) {
        CaoeDocInfoDTO document = new CaoeDocInfoDTO();
        document.setDocState("已发行");
        document.setOid(oid);
        return document;
    }

    private String temporaryEnvelope(JSONArray details) {
        JSONObject body = new JSONObject();
        body.put("details", details);
        JSONObject envelope = new JSONObject();
        envelope.put("retCode", "200");
        envelope.put("responseBody", body);
        return JSON.toJSONString(envelope);
    }

    private String formalEnvelope(String version) {
        JSONObject versionObject = new JSONObject();
        versionObject.put("version", version);
        JSONArray result = new JSONArray();
        result.add(versionObject);
        JSONObject envelope = new JSONObject();
        envelope.put("success", true);
        envelope.put("code", 200);
        envelope.put("result", result);
        return envelope.toJSONString();
    }
}
