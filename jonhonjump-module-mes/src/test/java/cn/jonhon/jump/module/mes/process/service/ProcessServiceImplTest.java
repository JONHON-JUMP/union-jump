package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.framework.common.exception.ServiceException;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardDetailsRespVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardRespVO;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.CaoeTableMapper;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.CaoeDocInfoDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jonhon.route.ThirdPartyRouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessServiceImplTest {

    @InjectMocks
    private ProcessServiceImpl service;

    @Mock
    private ThirdPartyRouteService thirdPartyRouteService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CaoeTableMapper caoeTableMapper;
    @Mock
    private TemporaryProcessTreeAssembler treeAssembler;

    private ProcessCardDetailsRespVO detail;

    @BeforeEach
    void setUp() {
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

        try (MockedStatic<ThirdPartyRouteService> routeService = mockRoute(body, requestJson)) {
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
    }

    @Test
    void queryCard_buildsNonRepairRequest() {
        JSONObject body = normalResponseBody();
        when(caoeTableMapper.queryDocInfo("DOC-2")).thenReturn(publishedDoc("oid-2"));
        when(treeAssembler.assemble(any(JSONArray.class), eq("oid-2"))).thenReturn(Collections.emptyList());
        AtomicReference<String> requestJson = new AtomicReference<>();

        try (MockedStatic<ThirdPartyRouteService> routeService = mockRoute(body, requestJson)) {
            ProcessCardRespVO result = service.queryCard(request("4309")).get(0);

            JSONObject thirdPartyRequest = JSON.parseObject(requestJson.get());
            assertEquals("4309", thirdPartyRequest.getString("accno"));
            assertEquals("4309", thirdPartyRequest.getString("plndept"));
            assertEquals("0", thirdPartyRequest.getString("fxtype"));
            assertEquals(0, result.getIsFix());
        }
    }

    @Test
    void queryCard_rejectsShortProcessNumber() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.queryCard(request("430")));

        assertEquals("工艺规程号至少需要4位", exception.getMessage());
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

        try (MockedStatic<ThirdPartyRouteService> routeService = mockRoute(body, new AtomicReference<>())) {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> service.queryCard(request("43091")));
            assertEquals("临时工艺信息查询失败", exception.getMessage());
        }
    }

    @Test
    void queryCard_rejectsMalformedRouteEnvelope() {
        try (MockedStatic<ThirdPartyRouteService> routeService = mockEnvelope("{\"retCode\":null,\"responseBody\":{}}")) {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> service.queryCard(request("43091")));
            assertEquals("临时工艺信息查询失败", exception.getMessage());
        }
    }

    @Test
    void queryCard_rejectsNullRouteEnvelope() {
        try (MockedStatic<ThirdPartyRouteService> routeService = mockEnvelope("null")) {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> service.queryCard(request("43091")));
            assertEquals("临时工艺信息查询失败", exception.getMessage());
        }
    }

    @Test
    void queryCard_rejectsNonObjectDetail() {
        JSONObject body = JSON.parseObject("{\"oid\":\"response-oid\",\"details\":[null]}");

        try (MockedStatic<ThirdPartyRouteService> routeService = mockRoute(body, new AtomicReference<>())) {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> service.queryCard(request("4309")));
            assertEquals("临时工艺信息查询失败", exception.getMessage());
        }
    }

    private void assertDocumentError(CaoeDocInfoDTO doc, String expectedMessage) {
        when(caoeTableMapper.queryDocInfo("DOC-1")).thenReturn(doc);
        try (MockedStatic<ThirdPartyRouteService> routeService = mockRoute(repairResponseBody(), new AtomicReference<>())) {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> service.queryCard(request("43091")));
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    private MockedStatic<ThirdPartyRouteService> mockRoute(JSONObject body, AtomicReference<String> requestJson) {
        MockedStatic<ThirdPartyRouteService> routeService = mockStatic(ThirdPartyRouteService.class);
        routeService.when(() -> ThirdPartyRouteService.invoke(
                        eq("1"), eq("JUMP"), eq("WXD"), anyString(), isNull(), any()))
                .thenAnswer(invocation -> {
                    requestJson.set(invocation.getArgument(3));
                    return body;
                });
        return routeService;
    }

    @SuppressWarnings("unchecked")
    private MockedStatic<ThirdPartyRouteService> mockEnvelope(String envelope) {
        MockedStatic<ThirdPartyRouteService> routeService = mockStatic(ThirdPartyRouteService.class);
        routeService.when(() -> ThirdPartyRouteService.invoke(
                        eq("1"), eq("JUMP"), eq("WXD"), anyString(), isNull(), any()))
                .thenAnswer(invocation -> {
                    Function<ResponseEntity<String>, JSONObject> handler = invocation.getArgument(5);
                    return handler.apply(ResponseEntity.ok(envelope));
                });
        return routeService;
    }

    private ProcessCardReqVO request(String accno) {
        return ProcessCardReqVO.builder().prtno("21ET0-009-39095-B1").accno(accno).build();
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
