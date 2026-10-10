package cn.jonhon.jump.module.system.framework.subsystemapi;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiEndpointDO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeePageRespDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeQueryDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemTeamComboDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.EndpointSpec;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.ExternalApiHttpClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Camstar 人员接口适配器
 *
 * 对接 camstar_api（ASP.NET MVC，区域路由）：
 * - 鉴权本身也是一条接口（authConfig）：完整 URL + 工号/Cookie 名；
 *   Cookie Nancal_Cam_SessionId = 原始 Base64(调用账号工号)，保留末尾的 =，不要百分号编码。
 *   业务接口失败时，若鉴权接口启用则调 SSO 激活会话后重试一次。
 * - 响应：AjaxResult JSON {code:200/500, message, data, total, rows}
 * - 各业务接口端点存 sub_system_api_endpoint（一行一接口，按 purpose 区分）
 */
@Slf4j
public class CamstarEmployeeApiAdapter implements SubSystemEmployeeApi {

    private static final String DEFAULT_COOKIE_NAME = "Nancal_Cam_SessionId";
    private static final String DEFAULT_LOGIN_PATH = "/Base/SSOLogin/SSOLoginIn";

    private final ExternalApiHttpClient httpClient;
    private final EndpointSpec queryEndpoint;
    private final EndpointSpec createEndpoint;
    private final EndpointSpec updateEndpoint;
    private final EndpointSpec deleteEndpoint;
    private final EndpointSpec teamComboEndpoint;
    private final EndpointSpec assignRoleEndpoint;
    private final EndpointSpec authEndpoint;
    private final String cookieName;
    private final String authUserCode;
    /** 会话 Cookie 值（= Base64(authUserCode)） */
    private volatile String cookieValue;

    public CamstarEmployeeApiAdapter(SubSystemApiConfigDO config, List<SubSystemApiEndpointDO> endpoints) {
        this.httpClient = new ExternalApiHttpClient(config.getBaseUrl(),
                config.getConnectTimeoutMs(), config.getReadTimeoutMs());
        Map<String, EndpointSpec> specs = ApiEndpointSpecs.toSpecMap(endpoints);
        this.queryEndpoint = requireSpec(specs, ApiEndpointSpecs.QUERY, "查询接口");
        this.createEndpoint = requireSpec(specs, ApiEndpointSpecs.CREATE, "新增接口");
        this.updateEndpoint = requireSpec(specs, ApiEndpointSpecs.UPDATE, "修改接口");
        this.deleteEndpoint = requireSpec(specs, ApiEndpointSpecs.DELETE, "删除接口");
        // 可选接口：未配置接口行时为 null（调用前判空）
        this.teamComboEndpoint = specs.get(ApiEndpointSpecs.TEAM_COMBO);
        this.assignRoleEndpoint = specs.get(ApiEndpointSpecs.ASSIGN_ROLE);
        JsonNode auth = parseJson(StrUtil.blankToDefault(config.getAuthConfig(), "{}"));
        this.authUserCode = text(auth, "userCode", "");
        this.cookieName = text(auth, "cookieName", DEFAULT_COOKIE_NAME);
        this.authEndpoint = buildAuthEndpoint(auth);
        // 会话关闭（enabled=false）：完全不带 Cookie、不重登；接口级 withSession=false 另行控制
        if (this.authEndpoint.isEnabled() && StrUtil.isNotBlank(this.authUserCode)) {
            this.cookieValue = encodeSessionCookie(this.authUserCode);
        }
    }

    @Override
    public SubSystemEmployeePageRespDTO page(SubSystemEmployeeQueryDTO query) {
        requireEnabled(queryEndpoint, "查询");
        Map<String, Object> body = new HashMap<>();
        // 空字符串不要传。Camstar 会把空工号、空姓名当成查询条件，结果是一条都没有
        putIfNotBlank(body, "userCode", query.getUserCode());
        putIfNotBlank(body, "userName", query.getUserName());
        putIfNotBlank(body, "workshopCode", query.getWorkshopCode());
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
        requireEnabled(createEndpoint, "新增");
        // C# 端方法签名：addOrUpdateUser(List<EmployeeEntity> list) —— 请求体是 JSON 数组
        Map<String, Object> item = toCamstarEmployee(employee);
        JsonNode resp = executeWithRelogin(createEndpoint, Collections.singletonList(item));
        checkSuccess(resp);
    }

    @Override
    public void update(SubSystemEmployeeDTO employee) {
        requireEnabled(updateEndpoint, "修改");
        Map<String, Object> item = toCamstarEmployee(employee);
        JsonNode resp = executeWithRelogin(updateEndpoint, Collections.singletonList(item));
        checkSuccess(resp);
    }

    @Override
    public void delete(String userCode) {
        requireEnabled(deleteEndpoint, "删除");
        Map<String, Object> body = new HashMap<>();
        body.put("userCode", userCode);
        JsonNode resp = executeWithRelogin(deleteEndpoint, body);
        checkSuccess(resp);
    }

    @Override
    public List<SubSystemTeamComboDTO> teamCombo(String workshopCode) {
        if (teamComboEndpoint == null || !teamComboEndpoint.isEnabled()) {
            return Collections.emptyList();
        }
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

    /**
     * 同步人员角色（查 → 合并 → 回写），只改角色不动其他字段：
     *
     * 1. 查：getEmployeeInfo 按 userCode 拉人员在 Camstar 的完整现状（含现有角色 userRoleIdStr）；
     * 2. 合并：目标角色集 = (现有角色 − JUMP 管辖角色) ∪ 本次分配角色 ——
     *    Camstar 侧人工挂的角色（含内置 Login）不在 JUMP 管辖范围内，永不增删；
     * 3. 回写：现状字段原样回显 + 替换 userRoleIdStr，调 addOrUpdateUser（数组单元素）。
     *    Camstar 的 updateUser 是全量覆盖语义（班组传空会删关系、密码非空会重置域账号密码），
     *    因此必须回显原值、且不传 password；userRoleIdStr 是全量差量，合并后传回不会误删。
     *
     * @param userCode       对方系统用户名（拼接车间的为 车间编号_工号）
     * @param managedRoleIds JUMP 管辖的外部角色 ID 全集（本花名册系统所有已关联角色）
     * @param targetRoleIds  本次分配后应生效的 JUMP 管辖角色 ID 集合（可为空=全部解除）
     */
    public void assignRoles(String userCode, Set<String> managedRoleIds, Set<String> targetRoleIds) {
        requireEnabled(queryEndpoint, "查询");
        EndpointSpec writeEndpoint = assignRoleEndpoint != null && assignRoleEndpoint.isEnabled()
                ? assignRoleEndpoint : updateEndpoint;
        requireEnabled(writeEndpoint, "人员分配角色");
        // 1. 查现状
        Map<String, Object> query = new HashMap<>();
        query.put("userCode", userCode);
        query.put("page", 1);
        query.put("rows", 20);
        JsonNode resp = executeWithRelogin(queryEndpoint, query);
        JsonNode matched = null;
        JsonNode rows = resp.path("rows");
        String wanted = userCode.trim();
        if (rows.isArray()) {
            for (JsonNode row : rows) {
                String code = text(row, "userCode", "").trim();
                if (wanted.equalsIgnoreCase(code)) {
                    matched = row;
                    if (wanted.equals(code)) {
                        break;
                    }
                }
            }
        }
        if (matched == null) {
            throw new ExternalApiException("对方系统未找到用户【" + userCode + "】，请先在花名册完成人员注册");
        }
        // 2. 合并角色：保留非 JUMP 管辖的现有角色，再并入本次目标角色（ID 比较忽略大小写）
        Set<String> managedLower = new LinkedHashSet<>();
        if (managedRoleIds != null) {
            for (String roleId : managedRoleIds) {
                if (StrUtil.isNotBlank(roleId)) {
                    managedLower.add(roleId.trim().toLowerCase());
                }
            }
        }
        Set<String> finalRoleIds = new LinkedHashSet<>();
        for (String roleId : StrUtil.splitTrim(text(matched, "userRoleIdStr", ""), ',')) {
            if (StrUtil.isNotBlank(roleId) && !managedLower.contains(roleId.toLowerCase())) {
                finalRoleIds.add(roleId);
            }
        }
        if (targetRoleIds != null) {
            for (String roleId : targetRoleIds) {
                if (StrUtil.isNotBlank(roleId)) {
                    finalRoleIds.add(roleId.trim());
                }
            }
        }
        // 3. 原样回写：去掉 password（空密码对方跳过更新）、分页字段和角色对象列表，只替换 userRoleIdStr
        Map<String, Object> item = JsonUtils.parseObject(matched.toString(),
                new TypeReference<Map<String, Object>>() { });
        if (item == null) {
            throw new ExternalApiException("对方系统人员数据解析失败，已中止回写");
        }
        item.remove("password");
        item.remove("Password");
        item.remove("page");
        item.remove("rows");
        item.remove("userRoleList");
        String userName = item.get("userName") == null ? "" : String.valueOf(item.get("userName")).trim();
        String workshopCode = item.get("workshopCode") == null ? "" : String.valueOf(item.get("workshopCode")).trim();
        if (StrUtil.isBlank(userName) || StrUtil.isBlank(workshopCode)) {
            throw new ExternalApiException("查询到的人员缺少用户名或车间，已中止回写，避免覆盖对方其他字段");
        }
        item.put("userRoleIdStr", String.join(",", finalRoleIds));
        JsonNode writeResp = executeWithRelogin(writeEndpoint, Collections.singletonList(item));
        checkSuccess(writeResp);
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

    /** 必选接口：未配置接口行时直接抛错（与原 JSON 列「未配置」行为一致） */
    private static EndpointSpec requireSpec(Map<String, EndpointSpec> specs, String purpose, String label) {
        EndpointSpec spec = specs.get(purpose);
        if (spec == null) {
            throw new ExternalApiException(label + "未配置");
        }
        return spec;
    }

    /**
     * 执行请求；若失败（HTTP 错误或业务 code!=200）且该接口携带会话且尚未重登过，
     * 则先激活会话重试一次（不携带会话的接口失败直接抛出）。
     */
    private void requireEnabled(EndpointSpec endpoint, String label) {
        if (endpoint == null || !endpoint.isEnabled()) {
            throw new ExternalApiException(label + "接口已停用");
        }
    }

    private EndpointSpec buildAuthEndpoint(JsonNode auth) {
        EndpointSpec login = new EndpointSpec();
        String url = firstNonBlank(text(auth, "url", ""), text(auth, "path", ""), text(auth, "loginPath", DEFAULT_LOGIN_PATH));
        if (url.startsWith("http://") || url.startsWith("https://")) {
            login.setUrl(url);
        } else {
            login.setPath(StrUtil.blankToDefault(url, DEFAULT_LOGIN_PATH));
        }
        login.setMethod(text(auth, "method", "GET"));
        login.setName(text(auth, "name", "SSO登录"));
        if (auth.has("enabled") && !auth.get("enabled").asBoolean(true)) {
            login.setEnabled(false);
        }
        return login;
    }

    private JsonNode executeWithRelogin(EndpointSpec endpoint, Object body) {
        try {
            return doExecute(endpoint, body);
        } catch (ExternalApiException first) {
            // 不携带会话的接口与会话无关，失败直接抛出，不做重登
            if (!endpoint.isWithSession()
                    || authEndpoint == null || !authEndpoint.isEnabled() || StrUtil.isBlank(authUserCode)) {
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
        if (endpoint.isWithSession() && StrUtil.isNotBlank(cookieValue)) {
            headers.put("Cookie", cookieName + "=" + cookieValue);
        }
        String respBody = httpClient.execute(endpoint, body, headers);
        JsonNode resp = parseJson(respBody);
        if (!isSuccess(resp)) {
            throw new ExternalApiException("接口返回错误：" + resp.path("message").asText("未知错误"));
        }
        return resp;
    }

    private void checkSuccess(JsonNode resp) {
        // doExecute 已校验，此处兜底
        if (!isSuccess(resp)) {
            throw new ExternalApiException("接口返回错误：" + resp.path("message").asText("未知错误"));
        }
    }

    /** Camstar AjaxResult.code 可能是数字 200，也可能是枚举名 success */
    private static boolean isSuccess(JsonNode resp) {
        JsonNode code = resp.get("code");
        if (code == null || code.isNull()) {
            return false;
        }
        if (code.isNumber()) {
            return code.asInt() == 200;
        }
        String text = code.asText("");
        return "200".equals(text) || "success".equalsIgnoreCase(text);
    }

    /** 激活会话：调鉴权接口 ?token=Base64(工号)，成功后 cookieValue 即 token */
    private void activateSession() {
        String token = Base64.getEncoder().encodeToString(authUserCode.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        httpClient.execute(authEndpoint, params, null);
        this.cookieValue = encodeSessionCookie(authUserCode);
    }

    /**
     * Cookie 值 = 原始 Base64(UTF-8 工号)，必须保留 + / =。
     * Camstar 的 Request.Cookies 不会百分号解码，直接交给 Convert.FromBase64String。
     * 把 = 编成 %3D 后，% 就是非法 Base64 字符，控制器构造时抛黄屏
     * 「输入的不是有效的 Base-64 字符串」。本机 System.Web 实测：原始 MTYzNTg= 能解出工号，%3D 不能。
     */
    public static String encodeSessionCookie(String userCode) {
        return Base64.getEncoder().encodeToString(userCode.getBytes(StandardCharsets.UTF_8));
    }

    private static void putIfNotBlank(Map<String, Object> body, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            body.put(key, value.trim());
        }
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
