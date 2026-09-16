package cn.jonhon.jump.framework.recipechange.consumer.client;

import cn.jonhon.jump.framework.recipechange.consumer.config.RecipeChangeConsumerProperties;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeDingTalkAlarmLogDTO;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeMessageDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 通过 HTTP 调用 JUMP 的工艺变更状态查询和 MES 回调接口
 */
public class RecipeChangeJUMPHttpClient implements RecipeChangeJUMPClient {

    private final RestTemplate restTemplate;
    private final RecipeChangeConsumerProperties recipeChangeConsumerProperties;

    public RecipeChangeJUMPHttpClient(RestTemplate restTemplate, RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        this.restTemplate = restTemplate;
        this.recipeChangeConsumerProperties = recipeChangeConsumerProperties;
    }

    /**
     * 调用 JUMP 原子领取接口，避免重复消息并发进入 MES 本地业务。
     */
    @Override
    @SuppressWarnings("unchecked")
    public RecipeChangeProcessingAcquireResult acquireProcessing(String notifyId, String workshopCode) {
        String requestUrl = UriComponentsBuilder.fromHttpUrl(recipeChangeConsumerProperties.getJumpBaseUrl())
                .path("/api/mes-recipe-change/processing-acquire")
                .queryParam("notifyId", notifyId)
                .queryParam("workshopCode", workshopCode)
                .toUriString();
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(requestUrl, null, Map.class);
        Map<String, Object> responseBody = responseEntity.getBody();
        ensureSuccessResponse(responseBody);
        if (!(responseBody.get("data") instanceof Map)) {
            throw new IllegalStateException("JUMP 处理权领取响应格式错误");
        }
        // JUMP 以三个字段表达领取结果，不能只按 HTTP 200 或单个布尔值决定是否执行 MES 业务
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        return new RecipeChangeProcessingAcquireResult(Boolean.TRUE.equals(data.get("acquired")),
                Boolean.TRUE.equals(data.get("processingNotRequired")), (String) data.get("processingToken"));
    }

    /**
     * 调用 JUMP 回调接口，将 MES 处理成功或失败结果落入通知主表和日志表
     */
    @Override
    public void callbackProcessResult(RecipeChangeMessageDTO message, String processingToken, boolean success, String errorMsg) {
        String requestUrl = recipeChangeConsumerProperties.getJumpBaseUrl() + "/api/mes-recipe-change/callback";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("notifyId", message.getNotifyId());
        requestBody.put("workshopCode", message.getWorkshopCode());
        // 回调携带领取令牌，JUMP 将以此拒绝租约过期或重复消息消费者的旧结果
        requestBody.put("processingToken", processingToken);
        requestBody.put("success", success);
        requestBody.put("errorMsg", errorMsg);
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(requestUrl, requestBody, Map.class);
        ensureSuccessResponse(responseEntity.getBody());
    }

    /**
     * 调用 JUMP 钉钉告警日志接口，完整保存 MES 请求参数和 JUMP 处理结果
     */
    @Override
    public void recordDingTalkAlarmResult(RecipeChangeDingTalkAlarmLogDTO dingTalkAlarmLog) {
        String requestUrl = recipeChangeConsumerProperties.getJumpBaseUrl() + "/api/mes-recipe-change/dingtalk-alarm-log";
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(requestUrl, dingTalkAlarmLog, Map.class);
        ensureSuccessResponse(responseEntity.getBody());
    }

    /**
     * 校验 JUMP 是否真正完成了本次接口请求的业务处理
     *
     * HTTP 状态码 200 仅表示 MES 已成功连接到 JUMP，不能表示 JUMP 已成功更新通知状态
     * 例如通知不存在、车间编码不匹配时，JUMP 仍可能返回 HTTP 200，但响应体中的 code 表示业务失败
     * 本方法发现响应体业务失败时抛出异常，让调用方明确记录或处理这次回调失败
     *
     * 本方法不直接执行 RabbitMQ 的 ack 或 nack
     * 消费监听器中是否 ack 只取决于 MES 本地业务是否成功
     * MES 业务成功后的回调异常只记录日志，不会撤销已执行的 ack
     * MES 业务失败后的回调异常不会改变重新入队和告警逻辑
     *
     * @param responseBody JUMP 返回的通用响应体，必须包含业务响应码 code
     * @throws IllegalStateException 响应体缺失、格式错误或 JUMP 业务处理失败时抛出
     */
    private void ensureSuccessResponse(Map<String, Object> responseBody) {
        // 没有响应体或没有数值类型的 code 时，MES 无法判断 JUMP 是否完成状态更新
        if (responseBody == null || !(responseBody.get("code") instanceof Number)) {
            throw new IllegalStateException("JUMP 接口响应格式错误");
        }
        // 读取 JUMP 响应体中的业务码，不使用 HTTP 状态码代替业务处理结果
        int responseCode = ((Number) responseBody.get("code")).intValue();
        // 同时兼容框架默认成功码 0 和接口方案约定的成功码 200
        if (responseCode != 0 && responseCode != 200) {
            // 将 JUMP 返回的业务失败消息带出，便于 MES 日志定位通知或参数问题
            throw new IllegalStateException("JUMP 接口业务处理失败: " + responseBody.get("msg"));
        }
    }

}
