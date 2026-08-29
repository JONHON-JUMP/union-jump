package cn.jonhon.jump.module.system.framework.subsystemapi.http;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 外部接口 HTTP 客户端公共封装（基于 Hutool，module-system 已传递依赖 hutool-all）
 *
 * 职责：超时、异常归一化、JSON 请求/响应处理。
 * 会话类鉴权（Cookie 等）由各适配器自行处理，通过 {@link #extraHeaders} 传入。
 */
@Slf4j
public class ExternalApiHttpClient {

    private final String baseUrl;
    private final long connectTimeoutMs;
    private final long readTimeoutMs;

    public ExternalApiHttpClient(String baseUrl, Long connectTimeoutMs, Long readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs != null ? connectTimeoutMs : 10_000L;
        this.readTimeoutMs = readTimeoutMs != null ? readTimeoutMs : 30_000L;
    }

    /**
     * 执行 JSON 请求（请求体与响应均为 JSON）
     *
     * @param endpoint     端点
     * @param body         请求体对象（可为 null，GET 时作为 query 参数）
     * @param extraHeaders 额外请求头（如 Cookie）
     * @return 响应 JSON 字符串
     */
    public String execute(EndpointSpec endpoint, Object body, Map<String, String> extraHeaders) {
        String url = endpoint.fullUrl(baseUrl);
        try {
            HttpRequest request = HttpUtil.createRequest(
                    cn.hutool.http.Method.valueOf(endpoint.methodUpper()), url)
                    .timeout((int) Math.min(Integer.MAX_VALUE, connectTimeoutMs))
                    .setReadTimeout((int) Math.min(Integer.MAX_VALUE, readTimeoutMs));
            if (extraHeaders != null) {
                request.addHeaders(extraHeaders);
            }
            if ("GET".equals(endpoint.methodUpper()) && body instanceof Map) {
                request.form((Map<String, Object>) body);
            } else if (body != null) {
                request.body(JsonUtils.toJsonString(body), "application/json");
            }
            HttpResponse response = request.execute();
            String respBody = response.body();
            if (!response.isOk()) {
                throw new ExternalApiException(
                        "HTTP " + response.getStatus() + "：" + truncate(respBody, 1000));
            }
            return respBody;
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalApiException("调用失败 " + url + "：" + e.getMessage(), e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

}
