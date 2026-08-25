package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.framework.common.exception.ServiceException;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardDetailsRespVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardRespVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessFileUrlReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessFileUrlRespVO;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.CaoeTableMapper;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.CaoeDocInfoDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessServiceImplTest {

    private static final String TEMPORARY_PROCESS_URL =
            "http://localhost:10011/api/jonhon/erpdata/mesdatainterface/sono/rest/SonoRest/GetPrtRoutInfo/v1";

    private static final String FORMAL_PROCESS_URL =
            "http://localhost/plm-service/api/gate/object/v1/queryObjectInfo";

    private static final String PROCESS_FILE_URL =
            "http://localhost/mpm-service/api/gate/release/v1/processReleaseForMes";

    @InjectMocks
    private ProcessServiceImpl service;

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CaoeTableMapper caoeTableMapper;
    @Mock
    private TemporaryProcessTreeAssembler treeAssembler;
    @Mock
    private FormalProcessTreeAssembler formalProcessTreeAssembler;

    private ProcessCardDetailsRespVO detail;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "temporaryProcessUrl", TEMPORARY_PROCESS_URL);
        ReflectionTestUtils.setField(service, "formalProcessUrl", FORMAL_PROCESS_URL);
        ReflectionTestUtils.setField(service, "processFileUrl", PROCESS_FILE_URL);
        ReflectionTestUtils.setField(service, "mpmAccessToken", "test-token");
        detail = ProcessCardDetailsRespVO.builder().idx(1L).name("铆接").no("10").build();
    }

    @Test
    void queryCard_buildsTemporaryRequestAndCard() {
        JSONObject body = repairResponseBody();
        CaoeDocInfoDTO doc = publishedDoc("document-oid");
        when(caoeTableMapper.queryDocInfo("DOC-1")).thenReturn(doc);
        when(treeAssembler.assemble(any(JSONArray.class), eq("document-oid")))
                .thenReturn(Collections.singletonList(detail));
        AtomicReference<String> requestJson = new AtomicReference<>();

        mockTemporaryRoute(body, requestJson);
        List<ProcessCardRespVO> result = service.queryCard(request("43091"));

        JSONObject thirdPartyRequest = JSON.parseObject(requestJson.get());
        assertEquals("21ET0-009-39095-B1", thirdPartyRequest.getString("prtno"));
        assertEquals("43091", thirdPartyRequest.getString("accno"));
        assertEquals("4309", thirdPartyRequest.getString("plndept"));
        assertEquals("1", thirdPartyRequest.getString("fxtype"));
        assertEquals(1, result.size());
        assertEquals("43091", result.get(0).getAccno());
        assertEquals(0, result.get(0).getIsFormal());
        assertEquals(1, result.get(0).getIsFix());
        assertEquals(Collections.singletonList(detail), result.get(0).getDetails());
        verify(treeAssembler).assemble(any(JSONArray.class), eq("document-oid"));
    }

    @Test
    void queryCard_buildsNonRepairRequest() {
        JSONObject body = normalResponseBody();
        when(caoeTableMapper.queryDocInfo("DOC-2")).thenReturn(publishedDoc("oid-2"));
        when(treeAssembler.assemble(any(JSONArray.class), eq("oid-2"))).thenReturn(Collections.emptyList());
        AtomicReference<String> requestJson = new AtomicReference<>();

        mockTemporaryRoute(body, requestJson);
        ProcessCardRespVO result = service.queryCard(request("4309")).get(0);

        JSONObject thirdPartyRequest = JSON.parseObject(requestJson.get());
        assertEquals("4309", thirdPartyRequest.getString("accno"));
        assertEquals("4309", thirdPartyRequest.getString("plndept"));
        assertEquals("0", thirdPartyRequest.getString("fxtype"));
        assertEquals(0, result.getIsFix());
    }

    @Test
    void queryCard_allowsShortNonRepairProcessNumber() {
        when(caoeTableMapper.queryDocInfo("DOC-2")).thenReturn(publishedDoc("oid-2"));
        when(treeAssembler.assemble(any(JSONArray.class), eq("oid-2"))).thenReturn(Collections.emptyList());
        AtomicReference<String> requestJson = new AtomicReference<>();

        mockTemporaryRoute(normalResponseBody(), requestJson);
        ProcessCardRespVO result = service.queryCard(request("430")).get(0);

        JSONObject thirdPartyRequest = JSON.parseObject(requestJson.get());
        assertEquals("430", thirdPartyRequest.getString("plndept"));
        assertEquals("0", thirdPartyRequest.getString("fxtype"));
        assertEquals(0, result.getIsFix());
    }

    @Test
    void queryCard_buildsMpmFormalProcessCardFromLatestVersion() {
        List<ProcessCardDetailsRespVO> details = Collections.singletonList(detail);
        when(caoeTableMapper.queryProcessState("CX0000000048", "B")).thenReturn("已发行");
        when(formalProcessTreeAssembler.assemble("CX0000000048", "B", true)).thenReturn(details);
        AtomicReference<HttpEntity<String>> requestEntity = new AtomicReference<>();

        when(restTemplate.postForEntity(eq(FORMAL_PROCESS_URL), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    requestEntity.set(invocation.getArgument(1));
                    return ResponseEntity.ok(
                            "{\"success\":true,\"code\":200,\"result\":[{\"version\":\"B.1\"}]}");
                });
        ProcessCardRespVO result = service.queryCard(requestWithoutMaterial("CX0000000048")).get(0);

        JSONObject thirdPartyRequest = JSON.parseObject(requestEntity.get().getBody());
        assertEquals("com.glaway.dtp.business.model.process.ProcessModel",
                thirdPartyRequest.getString("objType"));
        assertEquals("CX0000000048", thirdPartyRequest.getJSONArray("objNumbers").getString(0));
        assertEquals("true", thirdPartyRequest.getString("isLatest"));
        assertEquals("B", result.getVersion());
        assertEquals(1, result.getIsFormal());
        assertEquals(0, result.getIsFix());
        assertEquals(details, result.getDetails());
        verify(formalProcessTreeAssembler).assemble("CX0000000048", "B", true);
        assertEquals("test-token", requestEntity.get().getHeaders().getFirst("X-Access-Token"));
        assertEquals("MPMViewUser", requestEntity.get().getHeaders().getFirst("Accept-User"));
    }

    @Test
    void queryCard_buildsPdmFormalProcessCard() {
        when(caoeTableMapper.queryProcessState("C0000000048", "A")).thenReturn("已发行");
        when(formalProcessTreeAssembler.assemble("C0000000048", "A", false))
                .thenReturn(Collections.singletonList(detail));

        mockFormalRoute("A", new AtomicReference<>());
        ProcessCardRespVO result = service.queryCard(requestWithoutMaterial("C0000000048")).get(0);

        assertEquals("A", result.getVersion());
        verify(formalProcessTreeAssembler).assemble("C0000000048", "A", false);
    }

    @Test
    void queryCard_rejectsUnpublishedFormalProcess() {
        when(caoeTableMapper.queryProcessState("CX0000000048", "B")).thenReturn("正在工作");

        mockFormalRoute("B.1", new AtomicReference<>());
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(requestWithoutMaterial("CX0000000048")));

        assertEquals("工艺未发行，无法查看", exception.getMessage());
    }

    @Test
    void queryCard_rejectsFormalProcessHttpError() {
        when(restTemplate.postForEntity(eq(FORMAL_PROCESS_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("connect refused"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(requestWithoutMaterial("CX0000000048")));
        assertEquals("工艺版本信息查询失败", exception.getMessage());
    }

    @Test
    void queryCard_rejectsMalformedFormalVersionResponse() {
        mockFormalEnvelope("{\"success\":true,\"code\":200,\"result\":[]}", new AtomicReference<>());
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(requestWithoutMaterial("CX0000000048")));

        assertEquals("工艺版本信息查询失败", exception.getMessage());
    }

    @Test
    void queryCard_rejectsNonNumericFormalResponseCode() {
        assertFormalVersionError("{\"success\":true,\"code\":{},\"result\":[{\"version\":\"B.1\"}]}");
    }

    @Test
    void queryCard_rejectsNonStringFormalVersion() {
        assertFormalVersionError("{\"success\":true,\"code\":200,\"result\":[{\"version\":1}]}");
    }

    @Test
    void queryCard_rejectsBlankMajorFormalVersion() {
        assertFormalVersionError("{\"success\":true,\"code\":200,\"result\":[{\"version\":\".1\"}]}");
    }

    @Test
    void queryFileUrl_usesLowercaseOidAndReturnsMpmUrl() {
        AtomicReference<HttpEntity<String>> requestEntity = new AtomicReference<>();

        when(restTemplate.postForEntity(eq(PROCESS_FILE_URL), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    requestEntity.set(invocation.getArgument(1));
                    return ResponseEntity.ok(
                            "{\"success\":true,\"code\":200,\"result\":{\"url\":\"http://mpm/file\"}}");
                });
        ProcessFileUrlRespVO result = service.queryFileUrl(
                ProcessFileUrlReqVO.builder().oid("12345").build());

        JSONObject request = JSON.parseObject(requestEntity.get().getBody());
        assertEquals("OperationEntity:12345", request.getString("oid"));
        assertEquals("http://mpm/file", result.getUrl());
        assertEquals("test-token", requestEntity.get().getHeaders().getFirst("X-Access-Token"));
        assertEquals("MPMViewUser", requestEntity.get().getHeaders().getFirst("Accept-User"));
    }

    @Test
    void queryFileUrl_rejectsHttpError() {
        when(restTemplate.postForEntity(eq(PROCESS_FILE_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("connect refused"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryFileUrl(ProcessFileUrlReqVO.builder().oid("12345").build()));

        assertEquals("工艺文件地址获取失败", exception.getMessage());
    }

    @Test
    void queryFileUrl_rejectsMissingResultUrl() {
        mockFileUrlEnvelope("{\"success\":true,\"code\":200,\"result\":{}}");
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryFileUrl(ProcessFileUrlReqVO.builder().oid("12345").build()));

        assertEquals("工艺文件地址获取失败", exception.getMessage());
    }

    @Test
    void queryFileUrl_rejectsNonStringResultUrl() {
        mockFileUrlEnvelope("{\"success\":true,\"code\":200,\"result\":{\"url\":123}}");
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryFileUrl(ProcessFileUrlReqVO.builder().oid("12345").build()));

        assertEquals("工艺文件地址获取失败", exception.getMessage());
    }

    private void assertFormalVersionError(String envelope) {
        mockFormalEnvelope(envelope, new AtomicReference<>());
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(requestWithoutMaterial("CX0000000048")));
        assertEquals("工艺版本信息查询失败", exception.getMessage());
    }

    @Test
    void queryCard_rejectsMissingDocument() {
        assertDocumentError(null, "临时工艺文档信息不存在");
    }

    @Test
    void queryCard_rejectsUnpublishedDocument() {
        CaoeDocInfoDTO doc = publishedDoc("document-oid");
        doc.setDocSate("正在工作");
        assertDocumentError(doc, "工艺未发行，无法查看");
    }

    @Test
    void queryCard_rejectsMissingDocumentOid() {
        assertDocumentError(publishedDoc(""), "临时工艺查看地址缺失");
    }

    @Test
    void queryCard_rejectsEmptyDetails() {
        JSONObject body = JSON.parseObject("{\"oid\":\"response-oid\",\"routnumebr\":\"DOC-1\",\"details\":[]}");

        mockTemporaryRoute(body, new AtomicReference<>());
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(request("43091")));
        assertEquals("临时工艺信息查询失败", exception.getMessage());
    }

    @Test
    void queryCard_rejectsTemporaryProcessHttpError() {
        when(restTemplate.postForEntity(eq(TEMPORARY_PROCESS_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("connect refused"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(request("43091")));
        assertEquals("临时工艺信息查询失败", exception.getMessage());
    }

    @Test
    void queryCard_rejectsMalformedRouteEnvelope() {
        mockTemporaryEnvelope("{\"retCode\":null,\"responseBody\":{}}", new AtomicReference<>());
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(request("43091")));
        assertEquals("临时工艺信息查询失败", exception.getMessage());
    }

    @Test
    void queryCard_rejectsNullRouteEnvelope() {
        mockTemporaryEnvelope("null", new AtomicReference<>());
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(request("43091")));
        assertEquals("临时工艺信息查询失败", exception.getMessage());
    }

    @Test
    void queryCard_rejectsNonObjectDetail() {
        JSONObject body = JSON.parseObject("{\"oid\":\"response-oid\",\"details\":[null]}");

        mockTemporaryRoute(body, new AtomicReference<>());
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(request("4309")));
        assertEquals("临时工艺信息查询失败", exception.getMessage());
    }

    private void assertDocumentError(CaoeDocInfoDTO doc, String expectedMessage) {
        when(caoeTableMapper.queryDocInfo("DOC-1")).thenReturn(doc);
        mockTemporaryRoute(repairResponseBody(), new AtomicReference<>());
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(request("43091")));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void mockTemporaryRoute(JSONObject responseBody, AtomicReference<String> requestJson) {
        JSONObject envelope = new JSONObject();
        envelope.put("retCode", "200");
        envelope.put("responseBody", responseBody);
        mockTemporaryEnvelope(envelope.toJSONString(), requestJson);
    }

    @SuppressWarnings("unchecked")
    private void mockTemporaryEnvelope(String envelope, AtomicReference<String> requestJson) {
        when(restTemplate.postForEntity(eq(TEMPORARY_PROCESS_URL), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    HttpEntity<String> entity = invocation.getArgument(1);
                    requestJson.set(entity.getBody());
                    return ResponseEntity.ok(envelope);
                });
    }

    private void mockFormalRoute(String version, AtomicReference<String> requestJson) {
        mockFormalEnvelope("{\"success\":true,\"code\":200,\"result\":[{\"version\":\""
                + version + "\"}]}", requestJson);
    }

    @SuppressWarnings("unchecked")
    private void mockFormalEnvelope(String envelope, AtomicReference<String> requestJson) {
        when(restTemplate.postForEntity(eq(FORMAL_PROCESS_URL), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    HttpEntity<String> entity = invocation.getArgument(1);
                    requestJson.set(entity.getBody());
                    return ResponseEntity.ok(envelope);
                });
    }

    private void mockFileUrlEnvelope(String envelope) {
        when(restTemplate.postForEntity(eq(PROCESS_FILE_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(envelope));
    }

    private ProcessCardReqVO request(String accno) {
        return ProcessCardReqVO.builder().prtno("21ET0-009-39095-B1").accno(accno).build();
    }

    private ProcessCardReqVO requestWithoutMaterial(String accno) {
        return ProcessCardReqVO.builder().accno(accno).build();
    }

    private JSONObject repairResponseBody() {
        return JSON.parseObject("{\"oid\":\"response-oid\",\"routnumebr\":\"DOC-1\","
                + "\"details\":[{\"Seqno\":\"10\",\"Seqdesc\":\"铆接\"}]}");
    }

    private JSONObject normalResponseBody() {
        return JSON.parseObject("{\"oid\":\"response-oid\","
                + "\"details\":[{\"Seqno\":\"10\",\"Seqdesc\":\"铆接\",\"ROUTREMARK\":\"DOC-2\"}]}");
    }

    private CaoeDocInfoDTO publishedDoc(String oid) {
        CaoeDocInfoDTO doc = new CaoeDocInfoDTO();
        doc.setDocSate("已发行");
        doc.setOid(oid);
        return doc;
    }
}
