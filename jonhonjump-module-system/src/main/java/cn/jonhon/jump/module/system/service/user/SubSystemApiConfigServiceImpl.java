package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiTestReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiTestRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiConfigMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import cn.jonhon.jump.module.system.framework.subsystemapi.SubSystemEmployeeApiFactory;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.EndpointSpec;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.ExternalApiHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
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
    private SubSystemMapper subSystemMapper;
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
    public Long createApiConfig(SubSystemApiConfigSaveReqVO createReqVO) {
        validateSubSystemExists(createReqVO.getSubSystemId());
        validateDuplicate(createReqVO.getSubSystemId(), null);
        validateJsonFields(createReqVO);

        SubSystemApiConfigDO config = BeanUtils.toBean(createReqVO, SubSystemApiConfigDO.class);
        subSystemApiConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateApiConfig(SubSystemApiConfigSaveReqVO updateReqVO) {
        SubSystemApiConfigDO config = validateApiConfigExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validateDuplicate(updateReqVO.getSubSystemId(), updateReqVO.getId());
        validateJsonFields(updateReqVO);

        SubSystemApiConfigDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemApiConfigDO.class);
        updateObj.setSubSystemId(config.getSubSystemId());
        subSystemApiConfigMapper.updateById(updateObj);
        // 配置变更：重建适配器（含旧 Cookie 会话作废）
        subSystemEmployeeApiFactory.invalidate(config.getSubSystemId());
    }

    @Override
    public void deleteApiConfig(Long id) {
        SubSystemApiConfigDO config = validateApiConfigExists(id);
        subSystemApiConfigMapper.deleteById(id);
        subSystemEmployeeApiFactory.invalidate(config.getSubSystemId());
    }

    @Override
    public SubSystemApiConfigDO getEnabledConfigBySubSystemId(Long subSystemId) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(subSystemId);
        if (config == null || !isCreateEndpointEnabled(config)) {
            return null;
        }
        return config;
    }

    @Override
    public List<Long> getEnabledSubSystemIds() {
        return subSystemApiConfigMapper.selectList().stream()
                .filter(this::isCreateEndpointEnabled)
                .map(SubSystemApiConfigDO::getSubSystemId)
                .collect(Collectors.toList());
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
        String jsonField = resolveEndpointJson(config, reqVO.getApiKey());
        EndpointSpec endpoint = EndpointSpec.parse(jsonField, reqVO.getApiKey());
        if (!endpoint.isEnabled()) {
            throw exception(SUB_SYSTEM_API_ENDPOINT_DISABLED, reqVO.getApiKey());
        }
        Object body = parseTestBody(reqVO.getRequestBody());
        // auth 接口：测试体可覆盖 token；默认用 authConfig.userCode 生成
        if ("auth".equalsIgnoreCase(StrUtil.nullToEmpty(reqVO.getApiKey()).trim())) {
            body = enrichAuthTestBody(config, body);
        }
        ExternalApiHttpClient httpClient = new ExternalApiHttpClient(
                config.getBaseUrl(), config.getConnectTimeoutMs(), config.getReadTimeoutMs());
        Map<String, String> headers = "auth".equalsIgnoreCase(StrUtil.nullToEmpty(reqVO.getApiKey()).trim())
                ? null : buildAuthHeaders(config);
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

    private String resolveEndpointJson(SubSystemApiConfigDO config, String apiKey) {
        if (apiKey == null) {
            throw exception(SUB_SYSTEM_API_CONFIG_INVALID_JSON, "apiKey");
        }
        switch (apiKey.trim().toLowerCase()) {
            case "auth":
                return toAuthEndpointJson(config);
            case "query":
                return config.getApiQuery();
            case "create":
                return config.getApiCreate();
            case "update":
                return config.getApiUpdate();
            case "delete":
                return config.getApiDelete();
            default:
                throw exception(SUB_SYSTEM_API_CONFIG_INVALID_JSON, apiKey);
        }
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

    private boolean isCreateEndpointEnabled(SubSystemApiConfigDO config) {
        if (config == null || StrUtil.isBlank(config.getApiCreate())) {
            return false;
        }
        try {
            return EndpointSpec.parse(config.getApiCreate(), "新增接口").isEnabled();
        } catch (Exception e) {
            return false;
        }
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

    private Map<String, String> buildAuthHeaders(SubSystemApiConfigDO config) {
        if (!"cookie_sso".equals(config.getAuthType()) || StrUtil.isBlank(config.getAuthConfig())) {
            return null;
        }
        try {
            JsonNode auth = JsonUtils.parseObject(config.getAuthConfig(), JsonNode.class);
            String userCode = auth.path("userCode").asText("");
            String cookieName = auth.path("cookieName").asText("Nancal_Cam_SessionId");
            if (StrUtil.isBlank(userCode)) {
                return null;
            }
            String cookieValue = Base64.getEncoder()
                    .encodeToString(userCode.getBytes(StandardCharsets.UTF_8));
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
        return list.stream().map(config -> {
            SubSystemApiConfigRespVO vo = BeanUtils.toBean(config, SubSystemApiConfigRespVO.class);
            SubSystemDO subSystem = subSystemMap.get(config.getSubSystemId());
            if (subSystem != null) {
                vo.setClientName(subSystem.getSystemName());
            }
            return vo;
        }).collect(Collectors.toList());
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
        String[] jsonFields = {reqVO.getAuthConfig(), reqVO.getApiQuery(), reqVO.getApiCreate(),
                reqVO.getApiUpdate(), reqVO.getApiDelete(), reqVO.getApiTeamCombo(),
                reqVO.getApiCatalog(), reqVO.getParamMapping(), reqVO.getResponseMapping()};
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
