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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 HTTP 人员接口适配器（纯配置驱动，标准 REST 系统零代码接入）
 *
 * 配置约定（sub_system_api_config）：
 * - apiQuery/apiCreate/apiUpdate/apiDelete/apiTeamCombo：{"path":"/api/user/page","method":"POST"}
 * - paramMapping：JUMP标准参数名→对方参数名，如 {"userCode":"empNo","page":"pageNo","rows":"pageSize"}
 * - responseMapping：{"successField":"code","successValue":200,"listPath":"data.list",
 *                     "totalPath":"data.total","fields":{"userCode":"empNo","userName":"empName"}}
 * - authType 仅支持 none；需要特殊认证的系统请写专用适配器（apiType 自定义）
 */
@Slf4j
public class GenericHttpEmployeeApiAdapter implements SubSystemEmployeeApi {

    private final ExternalApiHttpClient httpClient;
    private final EndpointSpec queryEndpoint;
    private final EndpointSpec createEndpoint;
    private final EndpointSpec updateEndpoint;
    private final EndpointSpec deleteEndpoint;
    private final EndpointSpec teamComboEndpoint;
    /** 参数映射：标准名→对方名 */
    private final Map<String, String> paramMapping = new HashMap<>();
    /** 响应映射 */
    private final JsonNode responseMapping;

    public GenericHttpEmployeeApiAdapter(SubSystemApiConfigDO config) {
        if (!"none".equals(StrUtil.nullToEmpty(config.getAuthType()))) {
            throw new ExternalApiException("通用 HTTP 适配器仅支持 authType=none，当前：" + config.getAuthType());
        }
        this.httpClient = new ExternalApiHttpClient(config.getBaseUrl(),
                config.getConnectTimeoutMs(), config.getReadTimeoutMs());
        this.queryEndpoint = EndpointSpec.parse(config.getApiQuery(), "查询接口");
        this.createEndpoint = EndpointSpec.parse(config.getApiCreate(), "新增接口");
        this.updateEndpoint = EndpointSpec.parse(config.getApiUpdate(), "修改接口");
        this.deleteEndpoint = EndpointSpec.parse(config.getApiDelete(), "删除接口");
        this.teamComboEndpoint = EndpointSpec.parse(config.getApiTeamCombo(), "班组下拉接口");
        JsonNode pm = safeParse(config.getParamMapping());
        if (pm != null) {
            pm.fields().forEachRemaining(e -> paramMapping.put(e.getKey(), e.getValue().asText()));
        }
        this.responseMapping = safeParse(config.getResponseMapping());
    }

    @Override
    public SubSystemEmployeePageRespDTO page(SubSystemEmployeeQueryDTO query) {
        requireEnabled(queryEndpoint, "查询");
        Map<String, Object> body = new HashMap<>();
        body.put("page", query.getPage());
        body.put("rows", query.getRows());
        body.put("userCode", query.getUserCode());
        body.put("userName", query.getUserName());
        body.put("workshopCode", query.getWorkshopCode());
        JsonNode resp = doExecute(queryEndpoint, mapParams(body));
        long total = nodeLong(resp, textOf("totalPath", "data.total"));
        List<SubSystemEmployeeDTO> list = new ArrayList<>();
        JsonNode rows = nodeAt(resp, textOf("listPath", "data.list"));
        if (rows != null && rows.isArray()) {
            for (JsonNode row : rows) {
                list.add(toEmployee(row));
            }
        }
        return new SubSystemEmployeePageRespDTO(total, list);
    }

    @Override
    public void create(SubSystemEmployeeDTO employee) {
        requireEnabled(createEndpoint, "新增");
        JsonNode resp = doExecute(createEndpoint, mapParams(toEmployeeMap(employee)));
        checkSuccess(resp);
    }

    @Override
    public void update(SubSystemEmployeeDTO employee) {
        requireEnabled(updateEndpoint, "修改");
        JsonNode resp = doExecute(updateEndpoint, mapParams(toEmployeeMap(employee)));
        checkSuccess(resp);
    }

    @Override
    public void delete(String userCode) {
        requireEnabled(deleteEndpoint, "删除");
        Map<String, Object> body = new HashMap<>();
        body.put("userCode", userCode);
        JsonNode resp = doExecute(deleteEndpoint, mapParams(body));
        checkSuccess(resp);
    }

    @Override
    public List<SubSystemTeamComboDTO> teamCombo(String workshopCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("workshopCode", workshopCode);
        JsonNode resp = doExecute(teamComboEndpoint, mapParams(body));
        List<SubSystemTeamComboDTO> list = new ArrayList<>();
        JsonNode rows = nodeAt(resp, textOf("listPath", "data"));
        if (rows != null && rows.isArray()) {
            String codeField = fieldOf("teamCode", "teamCode");
            String nameField = fieldOf("teamName", "teamName");
            for (JsonNode item : rows) {
                list.add(new SubSystemTeamComboDTO(
                        item.path(codeField).asText(""),
                        item.path(nameField).asText("")));
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
        return "连接成功，耗时 " + (System.currentTimeMillis() - start) + "ms，共 " + page.getTotal() + " 人";
    }

    // ===================== 私有方法 =====================

    private void requireEnabled(EndpointSpec endpoint, String label) {
        if (endpoint == null || !endpoint.isEnabled()) {
            throw new ExternalApiException(label + "接口已停用");
        }
    }

    private JsonNode doExecute(EndpointSpec endpoint, Map<String, Object> body) {
        String respBody = httpClient.execute(endpoint, body, null);
        JsonNode resp = safeParse(respBody);
        if (resp == null) {
            throw new ExternalApiException("响应解析失败：" + respBody);
        }
        return resp;
    }

    private void checkSuccess(JsonNode resp) {
        String field = textOf("successField", "code");
        String value = textOf("successValue", "200");
        JsonNode actual = resp.get(field);
        if (actual == null || !value.equals(actual.asText(""))) {
            String msgField = textOf("messageField", "msg");
            throw new ExternalApiException("接口返回错误：" + resp.path(msgField).asText("未知错误"));
        }
    }

    private Map<String, Object> mapParams(Map<String, Object> src) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> e : src.entrySet()) {
            if (e.getValue() == null || (e.getValue() instanceof String && StrUtil.isBlank((String) e.getValue()))) {
                continue; // 空值不传
            }
            out.put(paramMapping.getOrDefault(e.getKey(), e.getKey()), e.getValue());
        }
        return out;
    }

    private Map<String, Object> toEmployeeMap(SubSystemEmployeeDTO dto) {
        Map<String, Object> m = new HashMap<>();
        m.put("userCode", dto.getUserCode());
        m.put("userName", dto.getUserName());
        m.put("workshopCode", dto.getWorkshopCode());
        m.put("teamCode", dto.getTeamCode());
        m.put("domainName", dto.getDomainName());
        m.put("erpNo", dto.getErpNo());
        m.put("cardNo", dto.getCardNo());
        m.put("onDuty", dto.getOnDuty());
        return m;
    }

    private SubSystemEmployeeDTO toEmployee(JsonNode row) {
        SubSystemEmployeeDTO dto = new SubSystemEmployeeDTO();
        dto.setUserCode(row.path(fieldOf("userCode", "userCode")).asText(""));
        dto.setUserName(row.path(fieldOf("userName", "userName")).asText(""));
        dto.setWorkshopCode(row.path(fieldOf("workshopCode", "workshopCode")).asText(""));
        dto.setWorkshopName(row.path(fieldOf("workshopName", "workshopName")).asText(""));
        dto.setTeamCode(row.path(fieldOf("teamCode", "teamCode")).asText(""));
        dto.setTeamName(row.path(fieldOf("teamName", "teamName")).asText(""));
        dto.setDomainName(row.path(fieldOf("domainName", "domainName")).asText(""));
        dto.setErpNo(row.path(fieldOf("erpNo", "erpNo")).asText(""));
        dto.setCardNo(row.path(fieldOf("cardNo", "cardNo")).asText(""));
        dto.setOnDuty(row.path(fieldOf("onDuty", "onDuty")).asText(""));
        return dto;
    }

    /** 响应中取行字段名（优先 responseMapping.fields 里的映射） */
    private String fieldOf(String stdName, String def) {
        JsonNode fields = responseMapping == null ? null : responseMapping.get("fields");
        if (fields != null && fields.has(stdName)) {
            return fields.get(stdName).asText();
        }
        return def;
    }

    private String textOf(String field, String def) {
        return responseMapping == null || !responseMapping.has(field)
                ? def : responseMapping.get(field).asText(def);
    }

    private JsonNode nodeAt(JsonNode root, String path) {
        if (root == null || StrUtil.isBlank(path)) {
            return root;
        }
        JsonNode cur = root;
        for (String seg : path.split("\\.")) {
            if (cur == null) {
                return null;
            }
            cur = cur.get(seg);
        }
        return cur;
    }

    private long nodeLong(JsonNode root, String path) {
        JsonNode node = nodeAt(root, path);
        return node == null ? 0L : node.asLong(0L);
    }

    private JsonNode safeParse(String body) {
        if (StrUtil.isBlank(body)) {
            return null;
        }
        try {
            return JsonUtils.parseObject(body, JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

}
