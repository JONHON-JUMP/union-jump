package cn.jonhon.jump.module.system.framework.subsystemapi.http;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import lombok.Data;

import java.util.Locale;

/**
 * 接口端点描述：{"path":"/BasicData/Employee/getEmployeeInfo","method":"POST"}
 */
@Data
public class EndpointSpec {

    /** 相对路径（拼在 baseUrl 后） */
    private String path;
    /** HTTP 方法，默认 POST */
    private String method = "POST";

    public static EndpointSpec parse(String json, String fieldName) {
        if (StrUtil.isBlank(json)) {
            throw new ExternalApiException(fieldName + " 未配置");
        }
        try {
            return cn.jonhon.jump.framework.common.util.json.JsonUtils.parseObject(json, EndpointSpec.class);
        } catch (Exception e) {
            throw new ExternalApiException(fieldName + " JSON 格式错误：" + json);
        }
    }

    public String methodUpper() {
        return StrUtil.isBlank(method) ? "POST" : method.trim().toUpperCase(Locale.ROOT);
    }

    public String fullUrl(String baseUrl) {
        String base = StrUtil.nullToEmpty(baseUrl).replaceAll("/+$", "");
        String p = StrUtil.nullToEmpty(path);
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return base + p;
    }

}
