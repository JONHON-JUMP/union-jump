package cn.jonhon.jump.module.system.framework.subsystemapi.http;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
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
            if (extraHeaders != null && !extraHeaders.isEmpty()) {
                String cookieHeader = null;
                Map<String, String> rest = new HashMap<>();
                for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                    if (entry.getKey() != null && "cookie".equalsIgnoreCase(entry.getKey())) {
                        cookieHeader = entry.getValue();
                    } else if (entry.getKey() != null) {
                        rest.put(entry.getKey(), entry.getValue());
                    }
                }
                if (!rest.isEmpty()) {
                    request.addHeaders(rest);
                }
                // 走 cookie() 覆盖整条 Cookie 头。addHeaders 再叠加 Hutool 的 Cookie 仓库时，
                // 两条 Cookie 可能被逗号拼成一条，Base64 末尾的 = 后面会跟上非法字符。
                if (cookieHeader != null && !cookieHeader.isEmpty()) {
                    request.cookie(cookieHeader);
                }
            }
            if ("GET".equals(endpoint.methodUpper()) && body instanceof Map) {
                request.form((Map<String, Object>) body);
            } else if (body != null) {
                request.body(JsonUtils.toJsonString(body), "application/json");
            }
            HttpResponse response = request.execute();
            String respBody = response.body();
            if (!response.isOk()) {
                log.warn("[externalApi][HTTP {}] url={} body={}", response.getStatus(), url, truncate(respBody, 500));
                throw new ExternalApiException(briefHttpError(response.getStatus(), respBody));
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

    /**
     * 对方站点没挂上接口时，常见返回是一整页 HTML（IIS「无法找到资源」）。
     * 不要把网页源码塞进给用户的提示。
     */
    private String briefHttpError(int status, String body) {
        if (isHtml(body)) {
            String title = htmlTitle(body);
            if (status == 404 || title.contains("无法找到资源")) {
                return "对方系统没有这个接口（HTTP " + status + "），请核对接口地址是否已在对方系统发布";
            }
            return "对方系统返回了错误页面（HTTP " + status + "）"
                    + (title.isEmpty() ? "" : "：" + title);
        }
        return "HTTP " + status + "：" + truncate(body, 200);
    }

    private boolean isHtml(String body) {
        if (body == null) {
            return false;
        }
        String text = body.trim().toLowerCase(java.util.Locale.ROOT);
        return text.startsWith("<!doctype") || text.startsWith("<html") || text.contains("<head>");
    }

    private String htmlTitle(String body) {
        if (body == null) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)<title>(.*?)</title>", java.util.regex.Pattern.DOTALL)
                .matcher(body);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }

}
