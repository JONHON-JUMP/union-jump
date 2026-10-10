package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiEndpointDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiConfigMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiEndpointMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.framework.subsystemapi.ApiEndpointSpecs;
import cn.jonhon.jump.module.system.framework.subsystemapi.CamstarEmployeeApiAdapter;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import cn.jonhon.jump.module.system.framework.subsystemapi.SubSystemEmployeeApiFactory;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.EndpointSpec;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.ExternalApiHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

/**
 * 子系统人员接口配置 Service 实现
 */
@Service
@Validated
public class SubSystemApiConfigServiceImpl implements SubSystemApiConfigService {

    @Resource
    private SubSystemApiConfigMapper subSystemApiConfigMapper;
    @Resource
    private SubSystemApiEndpointMapper subSystemApiEndpointMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemService subSystemService;
    @Resource
    private SubSystemEmployeeApiFactory subSystemEmployeeApiFactory;

    @Override
    public List<SubSystemApiConfigRespVO> getApiConfigList() {
        List<SubSystemApiConfigDO> list = subSystemApiConfigMapper.selectList();
        return buildRespList(list);
    }

    @Override
    public SubSystemApiConfigRespVO getApiConfig(Long id) {
        SubSystemApiConfigDO config = validateApiConfigExists(id);
        return buildResp(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApiConfig(SubSystemApiConfigSaveReqVO createReqVO) {
        Long subSystemId = resolveSubSystemIdForCreate(createReqVO);
        createReqVO.setSubSystemId(subSystemId);
        validateSubSystemExists(subSystemId);
        validateDuplicate(subSystemId, null);
        validateJsonFields(createReqVO);

        SubSystemApiConfigDO config = BeanUtils.toBean(createReqVO, SubSystemApiConfigDO.class);
        subSystemApiConfigMapper.insert(config);
        saveEndpoints(config.getSubSystemId(), createReqVO.getEndpoints());
        return config.getId();
    }

    /** 选择已有系统，或按名称新建仅接口接入的系统 */
    private Long resolveSubSystemIdForCreate(SubSystemApiConfigSaveReqVO createReqVO) {
        if (createReqVO.getSubSystemId() != null) {
            return createReqVO.getSubSystemId();
        }
        if (StrUtil.isNotBlank(createReqVO.getSystemName())) {
            return subSystemService.createApiOnlySubSystem(createReqVO.getSystemName());
        }
        throw exception(SUB_SYSTEM_API_CONFIG_TARGET_REQUIRED);
    }

    @Override
    public void renameAccessSystem(Long subSystemId, String systemName) {
        subSystemService.updateSystemName(subSystemId, systemName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApiConfig(SubSystemApiConfigSaveReqVO updateReqVO) {
        SubSystemApiConfigDO config = validateApiConfigExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validateDuplicate(updateReqVO.getSubSystemId(), updateReqVO.getId());
        validateJsonFields(updateReqVO);

        SubSystemApiConfigDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemApiConfigDO.class);
        updateObj.setSubSystemId(config.getSubSystemId());
        subSystemApiConfigMapper.updateById(updateObj);
        saveEndpoints(config.getSubSystemId(), updateReqVO.getEndpoints());
        // 配置变更：重建适配器（含旧 Cookie 会话作废）
        subSystemEmployeeApiFactory.invalidate(config.getSubSystemId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApiConfig(Long id) {
        SubSystemApiConfigDO config = validateApiConfigExists(id);
        subSystemApiConfigMapper.deleteById(id);
        // 接入系统删除：接口行是纯配置字典，物理级联清理，不留 deleted=1 死行
        subSystemApiEndpointMapper.physicalDeleteBySubSystemId(config.getSubSystemId());
        subSystemEmployeeApiFactory.invalidate(config.getSubSystemId());
    }

    @Override
    public SubSystemApiConfigDO getEnabledConfigBySubSystemId(Long subSystemId) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(subSystemId);
        if (config == null || !isCreateEndpointEnabled(subSystemId)) {
            return null;
        }
        return config;
    }

    @Override
    public boolean isCreateEndpointEnabled(Long subSystemId) {
        if (subSystemId == null) {
            return false;
        }
        SubSystemApiEndpointDO endpoint = subSystemApiEndpointMapper
                .selectBySubSystemIdAndPurpose(subSystemId, ApiEndpointSpecs.CREATE);
        return endpoint != null && (endpoint.getEnabled() == null || endpoint.getEnabled() == 1);
    }

    @Override
    public String testConnection(Long id) {
        SubSystemApiConfigDO config = validateApiConfigExists(id);
        try {
            return subSystemEmployeeApiFactory.getApi(config.getSubSystemId()).ping();
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        } catch (Exception e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
    }

    @Override
    public SubSystemApiTestRespVO testInvoke(SubSystemApiTestReqVO reqVO) {
        SubSystemApiConfigDO config = validateApiConfigExists(reqVO.getId());
        EndpointSpec endpoint = resolveEndpointSpec(config, reqVO.getApiKey());
        if (!endpoint.isEnabled()) {
            throw exception(SUB_SYSTEM_API_ENDPOINT_DISABLED, reqVO.getApiKey());
        }
        Object body = parseTestBody(reqVO.getRequestBody());
        boolean isAuthKey = "auth".equalsIgnoreCase(StrUtil.nullToEmpty(reqVO.getApiKey()).trim());
        // auth 接口：测试体可覆盖 token；默认用 authConfig.userCode 生成
        // 其它接口去掉空字符串。Camstar 会把空工号、空姓名当成查询条件，结果一条都没有
        if (isAuthKey) {
            body = enrichAuthTestBody(config, body);
        } else {
            body = omitBlankValues(body);
        }
        ExternalApiHttpClient httpClient = new ExternalApiHttpClient(
                config.getBaseUrl(), config.getConnectTimeoutMs(), config.getReadTimeoutMs());
        // 与真实调用一致：该接口声明不携带会话时不带 Cookie 头
        Map<String, String> headers = isAuthKey ? null : buildAuthHeaders(config, endpoint);
        SubSystemApiTestRespVO resp = new SubSystemApiTestRespVO();
        resp.setUrl(endpoint.fullUrl(config.getBaseUrl()));
        resp.setMethod(endpoint.methodUpper());
        resp.setRequestBody(body == null ? "" : JsonUtils.toJsonString(body));
        try {
            String raw = httpClient.execute(endpoint, body, headers);
            resp.setResponseBody(raw);
            resp.setSuccess(true);
        } catch (ExternalApiException e) {
            resp.setResponseBody(e.getMessage());
            resp.setSuccess(false);
        } catch (Exception e) {
            resp.setResponseBody(e.getMessage());
            resp.setSuccess(false);
        }
        return resp;
    }

    @Override
    public String createExternalRole(SubSystemExternalRoleCreateReqVO reqVO) {
        // 角色名固定拼接 车间编号_角色名称（与 Camstar 现网命名一致，如 5000_管理员）
        String workshopCode = reqVO.getWorkshopCode().trim();
        String roleName = workshopCode + "_" + reqVO.getRoleName().trim();
        pushExternalRoleCreate(reqVO.getSubSystemId(), workshopCode, roleName);
        return roleName;
    }

    @Override
    public void pushExternalRoleCreate(Long subSystemId, String workshopCode, String externalRoleName) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(subSystemId);
        if (config == null) {
            throw exception(SUB_SYSTEM_API_CONFIG_NOT_EXISTS);
        }
        SubSystemApiEndpointDO roleCreate = subSystemApiEndpointMapper
                .selectBySubSystemIdAndPurpose(subSystemId, ApiEndpointSpecs.ROLE_CREATE);
        if (roleCreate == null) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, "角色新增接口未配置");
        }
        EndpointSpec endpoint = ApiEndpointSpecs.toSpec(roleCreate);
        if (!endpoint.isEnabled()) {
            throw exception(SUB_SYSTEM_API_ENDPOINT_DISABLED, "role_create");
        }
        // 与人员新增相同：JSON 数组 + Base64 Cookie。roleId 为空走新增，不传 pageCodeStr 不挂页面。
        Map<String, Object> item = new HashMap<>();
        item.put("roleName", externalRoleName);
        item.put("workshopCode", workshopCode);
        ExternalApiHttpClient httpClient = new ExternalApiHttpClient(
                config.getBaseUrl(), config.getConnectTimeoutMs(), config.getReadTimeoutMs());
        String raw;
        try {
            raw = httpClient.execute(endpoint, Collections.singletonList(item), buildAuthHeaders(config, endpoint));
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, toRoleCreateError(e.getMessage()));
        }
        JsonNode resp;
        try {
            resp = JsonUtils.parseObject(raw, JsonNode.class);
        } catch (Exception e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, "响应解析失败：" + StrUtil.brief(raw, 300));
        }
        if (!isExternalSuccess(resp)) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, toRoleCreateError(resp.path("message").asText("未知错误")));
        }
    }

    /** Camstar 同名（WCF New 失败）转成可操作提示，其余错误原样抛出 */
    private static String toRoleCreateError(String message) {
        String text = StrUtil.blankToDefault(message, "未知错误");
        String lower = text.toLowerCase();
        if (text.contains("已存在") || text.contains("重复")
                || lower.contains("already exist") || lower.contains("duplicate")) {
            return "Camstar 已存在同名角色。若本地角色还没建，请取消同步先保存，再使用【关联外部】；本地角色已存在则直接【关联外部】";
        }
        return text;
    }

    @Override
    public List<SubSystemRegisterableApiRespVO> listRoleCreateApis() {
        List<SubSystemApiConfigDO> configs = subSystemApiConfigMapper.selectList();
        if (CollUtil.isEmpty(configs)) {
            return Collections.emptyList();
        }
        List<Long> apiSubSystemIds = configs.stream()
                .filter(this::isRoleCreateEndpointEnabled)
                .map(SubSystemApiConfigDO::getSubSystemId)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(apiSubSystemIds)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(apiSubSystemIds), SubSystemDO::getId);
        // 保序：按配置列表出现顺序
        return apiSubSystemIds.stream()
                .filter(subSystemMap::containsKey)
                .map(id -> new SubSystemRegisterableApiRespVO()
                        .setSubSystemId(id)
                        .setSystemName(subSystemMap.get(id).getSystemName()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> queryExternalRoleIds(Long subSystemId, String workshopCode) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(subSystemId);
        if (config == null) {
            throw exception(SUB_SYSTEM_API_CONFIG_NOT_EXISTS);
        }
        SubSystemApiEndpointDO roleQuery = subSystemApiEndpointMapper
                .selectBySubSystemIdAndPurpose(subSystemId, ApiEndpointSpecs.ROLE_QUERY);
        if (roleQuery == null) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, "角色查询接口未配置");
        }
        EndpointSpec endpoint = ApiEndpointSpecs.toSpec(roleQuery);
        if (!endpoint.isEnabled()) {
            throw exception(SUB_SYSTEM_API_ENDPOINT_DISABLED, "role_query");
        }
        // 角色查询固定按 POST 发（JSON 请求体，Camstar MVC 靠 POST+JSON 绑定参数）：
        // 接口行即使被配成 GET 也按 POST 调用，避免 GET 请求被网关/对方系统拦截
        endpoint.setMethod("POST");
        Map<String, Object> body = new HashMap<>();
        body.put("workshopCode", workshopCode);
        ExternalApiHttpClient httpClient = new ExternalApiHttpClient(
                config.getBaseUrl(), config.getConnectTimeoutMs(), config.getReadTimeoutMs());
        JsonNode resp;
        try {
            String raw = httpClient.execute(endpoint, body, buildAuthHeaders(config, endpoint));
            resp = JsonUtils.parseObject(raw, JsonNode.class);
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        } catch (Exception e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
        if (!isExternalSuccess(resp)) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, resp.path("message").asText("角色查询失败"));
        }
        JsonNode rows = firstArray(resp, "rows", "Rows", "data", "Data");
        if (rows == null && resp.path("data").isObject()) {
            rows = firstArray(resp.get("data"), "rows", "Rows", "list", "data");
        }
        Map<String, String> nameToId = new HashMap<>();
        if (rows == null) {
            return nameToId;
        }
        for (JsonNode row : rows) {
            if (row == null || !row.isObject()) {
                continue;
            }
            // getRoleInfo 是 roleName/roleId；getRoleComboByFactory 是 text/value
            String roleName = firstText(row, "roleName", "RoleName", "ROLENAME", "text", "Text");
            String roleId = firstText(row, "roleId", "RoleId", "ROLEID", "roleID", "value", "Value");
            if (StrUtil.isNotBlank(roleName) && StrUtil.isNotBlank(roleId)) {
                nameToId.put(roleName.trim(), roleId.trim());
            }
        }
        return nameToId;
    }

    /** Camstar AjaxResult.code 可能是数字 200，也可能是枚举名 success */
    private static boolean isExternalSuccess(JsonNode resp) {
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

    private static JsonNode firstArray(JsonNode node, String... fields) {
        if (node == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode child = node.get(field);
            if (child != null && child.isArray()) {
                return child;
            }
        }
        return null;
    }

    private String firstNonBlank(String a, String b) {
        return StrUtil.isNotBlank(a) ? a : (b == null ? "" : b);
    }

    private boolean isRoleCreateEndpointEnabled(SubSystemApiConfigDO config) {
        if (config == null) {
            return false;
        }
        SubSystemApiEndpointDO roleCreate = subSystemApiEndpointMapper
                .selectBySubSystemIdAndPurpose(config.getSubSystemId(), ApiEndpointSpecs.ROLE_CREATE);
        return roleCreate != null && (roleCreate.getEnabled() == null || roleCreate.getEnabled() == 1);
    }

    /** apiKey（=purpose，auth 除外）→ 接口行构建的 EndpointSpec */
    private EndpointSpec resolveEndpointSpec(SubSystemApiConfigDO config, String apiKey) {
        if (apiKey == null) {
            throw exception(SUB_SYSTEM_API_CONFIG_INVALID_JSON, "apiKey");
        }
        if ("auth".equalsIgnoreCase(apiKey.trim())) {
            return EndpointSpec.parse(toAuthEndpointJson(config), "auth");
        }
        SubSystemApiEndpointDO endpoint = subSystemApiEndpointMapper
                .selectBySubSystemIdAndPurpose(config.getSubSystemId(), apiKey.trim().toLowerCase());
        if (endpoint == null) {
            throw exception(SUB_SYSTEM_API_CONFIG_INVALID_JSON, apiKey);
        }
        return ApiEndpointSpecs.toSpec(endpoint);
    }

    /** authConfig → 可被 EndpointSpec 解析的 JSON（完整 url / path / method / enabled） */
    private String toAuthEndpointJson(SubSystemApiConfigDO config) {
        if (StrUtil.isBlank(config.getAuthConfig())) {
            throw exception(SUB_SYSTEM_API_CONFIG_INVALID_JSON, "auth");
        }
        try {
            JsonNode auth = JsonUtils.parseObject(config.getAuthConfig(), JsonNode.class);
            Map<String, Object> spec = new HashMap<>();
            String url = firstText(auth, "url", "path", "loginPath");
            if (StrUtil.isNotBlank(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
                spec.put("url", url);
            } else {
                spec.put("path", StrUtil.blankToDefault(url, "/Base/SSOLogin/SSOLoginIn"));
            }
            spec.put("method", firstText(auth, "method").isEmpty() ? "GET" : firstText(auth, "method"));
            spec.put("name", firstText(auth, "name").isEmpty() ? "SSO登录" : firstText(auth, "name"));
            if (auth.has("enabled")) {
                spec.put("enabled", auth.get("enabled").asBoolean(true));
            }
            return JsonUtils.toJsonString(spec);
        } catch (Exception e) {
            throw exception(SUB_SYSTEM_API_CONFIG_INVALID_JSON, config.getAuthConfig());
        }
    }

    private Object enrichAuthTestBody(SubSystemApiConfigDO config, Object body) {
        Map<String, Object> params = new HashMap<>();
        if (body instanceof Map) {
            // noinspection unchecked
            params.putAll((Map<String, Object>) body);
        }
        if (params.containsKey("token") && params.get("token") != null
                && StrUtil.isNotBlank(String.valueOf(params.get("token")))) {
            return params;
        }
        try {
            JsonNode auth = JsonUtils.parseObject(config.getAuthConfig(), JsonNode.class);
            String userCode = auth.path("userCode").asText("");
            if (StrUtil.isNotBlank(userCode)) {
                params.put("token", Base64.getEncoder()
                        .encodeToString(userCode.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception ignored) {
            // keep body as-is
        }
        return params;
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null || fields == null) {
            return "";
        }
        for (String f : fields) {
            String v = node.path(f).asText("");
            if (StrUtil.isNotBlank(v)) {
                return v;
            }
        }
        return "";
    }

    /** 去掉空字符串，避免 Camstar 把空工号/空姓名写成查询条件后一条都查不出来 */
    @SuppressWarnings("unchecked")
    private Object omitBlankValues(Object body) {
        if (body instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) body).entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                Object value = omitBlankValues(entry.getValue());
                if (value == null || (value instanceof String && StrUtil.isBlank((String) value))) {
                    continue;
                }
                out.put(String.valueOf(entry.getKey()), value);
            }
            return out;
        }
        if (body instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) body) {
                out.add(omitBlankValues(item));
            }
            return out;
        }
        return body;
    }

    private Object parseTestBody(String requestBody) {
        if (StrUtil.isBlank(requestBody)) {
            return Collections.emptyMap();
        }
        try {
            return JsonUtils.parseObject(requestBody, Object.class);
        } catch (Exception e) {
            throw exception(SUB_SYSTEM_API_CONFIG_INVALID_JSON, requestBody);
        }
    }

    private Map<String, String> buildAuthHeaders(SubSystemApiConfigDO config, EndpointSpec endpoint) {
        // 接口声明不携带会话 Cookie（withSession=false）→ 裸调
        if (endpoint != null && !endpoint.isWithSession()) {
            return null;
        }
        if (!"cookie_sso".equals(config.getAuthType()) || StrUtil.isBlank(config.getAuthConfig())) {
            return null;
        }
        try {
            JsonNode auth = JsonUtils.parseObject(config.getAuthConfig(), JsonNode.class);
            // 与人员注册同一规则：会话关闭时不带 Cookie，避免 Camstar 解一个坏 Cookie 直接 500
            if (auth.has("enabled") && !auth.get("enabled").asBoolean(true)) {
                return null;
            }
            String userCode = auth.path("userCode").asText("");
            String cookieName = auth.path("cookieName").asText("Nancal_Cam_SessionId");
            if (StrUtil.isBlank(userCode)) {
                return null;
            }
            String cookieValue = CamstarEmployeeApiAdapter.encodeSessionCookie(userCode);
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", cookieName + "=" + cookieValue);
            return headers;
        } catch (Exception e) {
            return null;
        }
    }

    // ===================== 私有方法 =====================

    private List<SubSystemApiConfigRespVO> buildRespList(List<SubSystemApiConfigDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(convertSet(list, SubSystemApiConfigDO::getSubSystemId)),
                SubSystemDO::getId);
        Map<Long, List<SubSystemApiEndpointDO>> endpointMap = subSystemApiEndpointMapper
                .selectList().stream()
                .collect(Collectors.groupingBy(SubSystemApiEndpointDO::getSubSystemId));
        return list.stream().map(config -> {
            SubSystemApiConfigRespVO vo = BeanUtils.toBean(config, SubSystemApiConfigRespVO.class);
            SubSystemDO subSystem = subSystemMap.get(config.getSubSystemId());
            if (subSystem != null) {
                vo.setClientName(subSystem.getSystemName());
            }
            vo.setEndpoints(buildEndpointVOList(endpointMap.get(config.getSubSystemId())));
            return vo;
        }).collect(Collectors.toList());
    }

    private List<SubSystemApiEndpointVO> buildEndpointVOList(List<SubSystemApiEndpointDO> endpoints) {
        if (CollUtil.isEmpty(endpoints)) {
            return Collections.emptyList();
        }
        return endpoints.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getSort() == null ? 0 : a.getSort(),
                        b.getSort() == null ? 0 : b.getSort()))
                .map(endpoint -> {
                    SubSystemApiEndpointVO vo = new SubSystemApiEndpointVO();
                    vo.setId(endpoint.getId());
                    vo.setPurpose(endpoint.getPurpose());
                    vo.setGroupCode(endpoint.getGroupCode());
                    vo.setName(endpoint.getName());
                    vo.setUrl(endpoint.getUrl());
                    vo.setMethod(endpoint.getMethod());
                    vo.setEnabled(endpoint.getEnabled() == null || endpoint.getEnabled() == 1);
                    vo.setWithSession(endpoint.getWithSession() == null || endpoint.getWithSession() == 1);
                    vo.setSort(endpoint.getSort());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 差量保存接口行：按传入 id / 用途 复用现有行，只插入新增、只物理删除真正移除的行。
     * 行 ID 跨保存保持稳定（前端树节点与测试结果不再错位），表里也不再堆积 deleted=1 的死行
     * （旧实现是整批逻辑删 + 重插，每保存一次就留下一整套死行且 ID 全变）。
     * 同一用途只能一个接口（空用途除外）。
     */
    private void saveEndpoints(Long subSystemId, List<SubSystemApiEndpointVO> endpoints) {
        List<SubSystemApiEndpointDO> existingList = subSystemApiEndpointMapper.selectListBySubSystemId(subSystemId);
        Map<Long, SubSystemApiEndpointDO> byId = convertMap(existingList, SubSystemApiEndpointDO::getId);
        Map<String, SubSystemApiEndpointDO> byPurpose = existingList.stream()
                .filter(e -> StrUtil.isNotBlank(e.getPurpose()))
                .collect(Collectors.toMap(SubSystemApiEndpointDO::getPurpose, e -> e, (a, b) -> a));

        Set<String> purposes = new HashSet<>();
        Set<Long> usedIds = new HashSet<>();
        List<SubSystemApiEndpointDO> toInsert = new ArrayList<>();
        int index = 0;
        for (SubSystemApiEndpointVO vo : endpoints) {
            String purpose = StrUtil.nullToEmpty(vo.getPurpose()).trim();
            if (!purpose.isEmpty() && !purposes.add(purpose)) {
                throw exception0(BAD_REQUEST.getCode(), "接口用途「" + purpose + "」重复，同一用途只能配置一个接口");
            }
            SubSystemApiEndpointDO endpoint = new SubSystemApiEndpointDO();
            endpoint.setSubSystemId(subSystemId);
            endpoint.setPurpose(purpose);
            // 分组按用途推导：role_* → role，空用途 → other，其余 → person
            endpoint.setGroupCode(ApiEndpointSpecs.groupOf(purpose));
            endpoint.setName(StrUtil.nullToEmpty(vo.getName()).trim());
            endpoint.setUrl(StrUtil.nullToEmpty(vo.getUrl()).trim());
            endpoint.setMethod(StrUtil.blankToDefault(vo.getMethod(), "POST").trim().toUpperCase());
            endpoint.setEnabled(!Boolean.FALSE.equals(vo.getEnabled()) ? 1 : 0);
            endpoint.setWithSession(!Boolean.FALSE.equals(vo.getWithSession()) ? 1 : 0);
            endpoint.setSort(vo.getSort() == null ? (index + 1) * 10 : vo.getSort());

            // 复用现有行：优先按传入 id（前端编辑时保留行 id），其次按用途匹配（同一系统一个用途一行）
            SubSystemApiEndpointDO target = null;
            if (vo.getId() != null) {
                SubSystemApiEndpointDO idRow = byId.get(vo.getId());
                if (idRow != null && usedIds.add(idRow.getId())) {
                    target = idRow;
                }
            }
            if (target == null && !purpose.isEmpty()) {
                SubSystemApiEndpointDO purposeRow = byPurpose.get(purpose);
                if (purposeRow != null && usedIds.add(purposeRow.getId())) {
                    target = purposeRow;
                }
            }
            if (target != null) {
                endpoint.setId(target.getId());
                subSystemApiEndpointMapper.updateById(endpoint);
            } else {
                toInsert.add(endpoint);
            }
            index++;
        }
        if (CollUtil.isNotEmpty(toInsert)) {
            subSystemApiEndpointMapper.insertBatch(toInsert);
        }
        // 只物理删除本次提交中不存在的行（真正被移除的接口）
        for (SubSystemApiEndpointDO existing : existingList) {
            if (!usedIds.contains(existing.getId())) {
                subSystemApiEndpointMapper.physicalDeleteById(existing.getId());
            }
        }
    }

    private SubSystemApiConfigRespVO buildResp(SubSystemApiConfigDO config) {
        List<SubSystemApiConfigRespVO> list = buildRespList(Collections.singletonList(config));
        return list.isEmpty() ? BeanUtils.toBean(config, SubSystemApiConfigRespVO.class) : list.get(0);
    }

    private SubSystemApiConfigDO validateApiConfigExists(Long id) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectById(id);
        if (config == null) {
            throw exception(SUB_SYSTEM_API_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateSubSystemExists(Long subSystemId) {
        if (subSystemMapper.selectById(subSystemId) == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
    }

    private void validateDuplicate(Long subSystemId, Long excludeId) {
        SubSystemApiConfigDO existing = subSystemApiConfigMapper.selectBySubSystemId(subSystemId);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(SUB_SYSTEM_API_CONFIG_DUPLICATE);
        }
    }

    private void validateJsonFields(SubSystemApiConfigSaveReqVO reqVO) {
        String[] jsonFields = {reqVO.getAuthConfig(), reqVO.getParamMapping(), reqVO.getResponseMapping()};
        for (String json : jsonFields) {
            if (StrUtil.isBlank(json)) {
                continue;
            }
            try {
                JsonUtils.parseObject(json, Object.class);
            } catch (Exception e) {
                throw exception(SUB_SYSTEM_API_CONFIG_INVALID_JSON, json);
            }
        }
    }

}
