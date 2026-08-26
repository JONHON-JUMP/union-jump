package cn.jonhon.jump.module.system.framework.subsystemapi;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeePageRespDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeQueryDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemTeamComboDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.EndpointSpec;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.ExternalApiHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camstar 人员接口适配器
 *
 * 对接 camstar_api（ASP.NET MVC，区域路由）：
 * - 认证：Cookie 会话。Cookie Nancal_Cam_SessionId = Base64(调用账号工号)；
 *   C# 侧 CurrentUser 解码 Cookie 拿工号查库还原用户（CurrentUser.cs:40-48）。
 *   先直接带 Cookie 调业务，失败时调 /Base/SSOLogin/SSOLoginIn?token= 激活会话后重试一次。
 * - 响应：AjaxResult JSON {code:200/500, message, data, total, rows}
 * - 路由：人员 /BasicData/Employee/*，班组 /BasicData/Team/getTeamComboByFactory
 *
 * authConfig JSON 约定：
 * {"userCode":"调用账号工号","cookieName":"Nancal_Cam_SessionId","loginPath":"/Base/SSOLogin/SSOLoginIn"}
 */
@Slf4j
public class CamstarEmployeeApiAdapter implements SubSystemEmployeeApi {

    private static final String DEFAULT_COOKIE_NAME = "Nancal_Cam_SessionId";
    private static final String DEFAULT_LOGIN_PATH = "/Base/SSOLogin/SSOLoginIn";

    private final SubSystemApiConfigDO config;
    private final ExternalApiHttpClient httpClient;
    private final EndpointSpec queryEndpoint;
    private final EndpointSpec createEndpoint;
    private final EndpointSpec updateEndpoint;
    private final EndpointSpec deleteEndpoint;
    private final EndpointSpec teamComboEndpoint;
    private final String cookieName;
    private final String loginPath;
    private final String authUserCode;
    /** 会话 Cookie 值（= Base64(authUserCode)） */
    private volatile String cookieValue;

    public CamstarEmployeeApiAdapter(SubSystemApiConfigDO config) {
        this.config = config;
        this.httpClient = new ExternalApiHttpClient(config.getBaseUrl(),
                config.getConnectTimeoutMs(), config.getReadTimeoutMs());
        this.queryEndpoint = EndpointSpec.parse(config.getApiQuery(), "查询接口");
        this.createEndpoint = EndpointSpec.parse(config.getApiCreate(), "新增接口");
        this.updateEndpoint = EndpointSpec.parse(config.getApiUpdate(), "修改接口");
        this.deleteEndpoint = EndpointSpec.parse(config.getApiDelete(), "删除接口");
        this.teamComboEndpoint = EndpointSpec.parse(config.getApiTeamCombo(), "班组下拉接口");
        JsonNode auth = parseJson(config.getAuthConfig());
        this.authUserCode = text(auth, "userCode", "");
        this.cookieName = text(auth, "cookieName", DEFAULT_COOKIE_NAME);
        this.loginPath = text(auth, "loginPath", DEFAULT_LOGIN_PATH);
        if (StrUtil.isNotBlank(this.authUserCode)) {
            this.cookieValue = Base64.getEncoder()
                    .encodeToString(this.authUserCode.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public SubSystemEmployeePageRespDTO page(SubSystemEmployeeQueryDTO query) {
        Map<String, Object> body = new HashMap<>();
        body.put("userCode", query.getUserCode());
        body.put("userName", query.getUserName());
        body.put("workshopCode", query.getWorkshopCode());
        body.put("page", query.getPage());
        body.put("rows", query.getRows());
        JsonNode resp = executeWithRelogin(queryEndpoint, body);
        long total = resp.path("total").asLong(0);
        List<SubSystemEmployeeDTO> list = new ArrayList<>();
        JsonNode rows = resp.path("rows");
        if (rows.isArray()) {
            for (JsonNode row : rows) {
                list.add(toEmployee(row));
            }
        }
        return new SubSystemEmployeePageRespDTO(total, list);
    }

    @Override
    public void create(SubSystemEmployeeDTO employee) {
        // C# 端方法签名：addOrUpdateUser(List<EmployeeEntity> list) —— 请求体是 JSON 数组
        Map<String, Object> item = toCamstarEmployee(employee);
        JsonNode resp = executeWithRelogin(createEndpoint, Collections.singletonList(item));
        checkSuccess(resp);
    }

    @Override
    public void update(SubSystemEmployeeDTO employee) {
        Map<String, Object> item = toCamstarEmployee(employee);
        JsonNode resp = executeWithRelogin(updateEndpoint, Collections.singletonList(item));
        checkSuccess(resp);
    }

    @Override
    public void delete(String userCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("userCode", userCode);
        JsonNode resp = executeWithRelogin(deleteEndpoint, body);
        checkSuccess(resp);
    }

    @Override
    public List<SubSystemTeamComboDTO> teamCombo(String workshopCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("workshopCode", workshopCode);
        JsonNode resp = executeWithRelogin(teamComboEndpoint, params);
        List<SubSystemTeamComboDTO> list = new ArrayList<>();
        JsonNode data = resp.hasNonNull("data") ? resp.get("data") : resp.path("rows");
        if (data != null && data.isArray()) {
            for (JsonNode item : data) {
                String code = firstNonBlank(text(item, "teamCode", ""), text(item, "TEAMID", ""));
                String name = firstNonBlank(text(item, "teamName", ""), text(item, "TEAMNAME", ""));
                list.add(new SubSystemTeamComboDTO(code, name));
            }
        }
        return list;
    }

    @Override
    public String ping() {
        SubSystemEmployeeQueryDTO query = new SubSystemEmployeeQueryDTO();
        query.setPage(1);
        query.setRows(1);
        long start = System.currentTimeMillis();
        SubSystemEmployeePageRespDTO page = page(query);
        long cost = System.currentTimeMillis() - start;
        return "连接成功，耗时 " + cost + "ms，共 " + page.getTotal() + " 人";
    }

    // ===================== 私有方法 =====================

    /**
     * 执行请求；若失败（HTTP 错误或业务 code!=200）且尚未重登过，则先激活会话重试一次。
     */
    private JsonNode executeWithRelogin(EndpointSpec endpoint, Object body) {
        try {
            return doExecute(endpoint, body);
        } catch (ExternalApiException first) {
            if (StrUtil.isBlank(loginPath) || StrUtil.isBlank(authUserCode)) {
                throw first;
            }
            try {
                activateSession();
            } catch (Exception loginEx) {
                log.warn("[camstar] 会话激活失败：{}", loginEx.getMessage());
                throw first;
            }
            return doExecute(endpoint, body);
        }
    }

    private JsonNode doExecute(EndpointSpec endpoint, Object body) {
        Map<String, String> headers = new HashMap<>();
        if (StrUtil.isNotBlank(cookieValue)) {
            headers.put("Cookie", cookieName + "=" + cookieValue);
        }
        String respBody = httpClient.execute(endpoint, body, headers);
        JsonNode resp = parseJson(respBody);
        if (resp.path("code").asInt(0) != 200) {
            throw new ExternalApiException("接口返回错误：" + resp.path("message").asText("未知错误"));
        }
        return resp;
    }

    private void checkSuccess(JsonNode resp) {
        // doExecute 已校验 code==200，此处兜底
        if (resp.path("code").asInt(0) != 200) {
            throw new ExternalApiException("接口返回错误：" + resp.path("message").asText("未知错误"));
        }
    }

    /** 激活会话：调 SSOLoginIn?token=Base64(工号)，成功后 cookieValue 即 token */
    private void activateSession() {
        EndpointSpec login = new EndpointSpec();
        login.setPath(loginPath);
        login.setMethod("GET");
        String token = Base64.getEncoder().encodeToString(authUserCode.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        httpClient.execute(login, params, null);
        this.cookieValue = token;
    }

    private Map<String, Object> toCamstarEmployee(SubSystemEmployeeDTO dto) {
        Map<String, Object> item = new HashMap<>();
        item.put("userCode", dto.getUserCode());
        item.put("userName", dto.getUserName());
        item.put("workshopCode", dto.getWorkshopCode());
        if (StrUtil.isNotBlank(dto.getTeamCode())) {
            item.put("teamCode", dto.getTeamCode());
        }
        if (StrUtil.isNotBlank(dto.getDomainName())) {
            item.put("domainName", dto.getDomainName());
        }
        if (StrUtil.isNotBlank(dto.getErpNo())) {
            item.put("erpNo", dto.getErpNo());
        }
        if (StrUtil.isNotBlank(dto.getCardNo())) {
            item.put("cardNo", dto.getCardNo());
        }
        return item;
    }

    private SubSystemEmployeeDTO toEmployee(JsonNode row) {
        SubSystemEmployeeDTO dto = new SubSystemEmployeeDTO();
        dto.setUserCode(text(row, "userCode", ""));
        dto.setUserName(text(row, "userName", ""));
        dto.setWorkshopCode(text(row, "workshopCode", ""));
        dto.setWorkshopName(text(row, "workshopName", ""));
        dto.setTeamCode(text(row, "teamCode", ""));
        dto.setTeamName(text(row, "teamName", ""));
        dto.setDomainName(text(row, "domainName", ""));
        dto.setErpNo(text(row, "erpNo", ""));
        dto.setCardNo(text(row, "cardNo", ""));
        dto.setOnDuty(text(row, "onDuty", ""));
        return dto;
    }

    private JsonNode parseJson(String body) {
        try {
            return JsonUtils.parseObject(body, JsonNode.class);
        } catch (Exception e) {
            throw new ExternalApiException("响应解析失败：" + body, e);
        }
    }

    private String text(JsonNode node, String field, String def) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? def : v.asText(def);
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (StrUtil.isNotBlank(v)) {
                return v;
            }
        }
        return "";
    }

}
