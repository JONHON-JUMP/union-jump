package cn.jonhon.jump.module.system.framework.subsystemapi.http;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import lombok.Data;

import java.util.Locale;

/**
 * 单个接口描述 JSON：
 * {"name":"新增","method":"POST","url":"http://host/path","enabled":true}
 * 兼容旧字段 path（相对或绝对）。
 */
@Data
public class EndpointSpec {

    /** 完整地址优先；兼容旧相对 path */
    private String url;
    /** 相对路径（拼在 baseUrl 后）；若 url 为空则用 path */
    private String path;
    /** HTTP 方法，默认 POST */
    private String method = "POST";
    /** 展示名称 */
    private String name;
    /** 是否启用，默认 true */
    private Boolean enabled = true;

    public static EndpointSpec parse(String json, String fieldName) {
        if (StrUtil.isBlank(json)) {
            throw new ExternalApiException(fieldName + " 未配置");
        }
        try {
            EndpointSpec spec = cn.jonhon.jump.framework.common.util.json.JsonUtils.parseObject(json, EndpointSpec.class);
            if (spec == null) {
                throw new ExternalApiException(fieldName + " JSON 格式错误：" + json);
            }
            return spec;
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalApiException(fieldName + " JSON 格式错误：" + json);
        }
    }

    /** 未配置或显式 enabled=false 视为停用 */
    public boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled);
    }

    public String methodUpper() {
        return StrUtil.isBlank(method) ? "POST" : method.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析最终请求地址：url/path 若已是 http(s) 绝对地址则直接用；否则 baseUrl + path。
     */
    public String fullUrl(String baseUrl) {
        String absolute = firstAbsolute(url, path);
        if (absolute != null) {
            return absolute;
        }
        String base = StrUtil.nullToEmpty(baseUrl).replaceAll("/+$", "");
        String p = StrUtil.nullToEmpty(StrUtil.blankToDefault(path, url));
        if (StrUtil.isBlank(p)) {
            return base;
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return base + p;
    }

    private static String firstAbsolute(String... candidates) {
        for (String c : candidates) {
            if (StrUtil.isBlank(c)) {
                continue;
            }
            String t = c.trim();
            if (t.startsWith("http://") || t.startsWith("https://")) {
                return t;
            }
        }
        return null;
    }

}
