package cn.jonhon.jump.module.rm.recipechange.controller.mes.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MES - 钉钉告警发送结果日志请求
 */
@Schema(description = "MES - 钉钉告警发送结果日志请求")
@Data
public class RecipeChangeDingTalkAlarmLogReqVO {

    /**
     * MPM 通知唯一标识
     */
    @Schema(description = "MPM 通知唯一标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String notifyId;
    /**
     * MES 所属车间编码
     */
    @Schema(description = "MES 所属车间编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workshopCode;
    /**
     * 实际发送的钉钉告警标题
     */
    @Schema(description = "实际发送的钉钉告警标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    /**
     * 实际发送给钉钉的完整告警文本
     */
    @Schema(description = "实际发送给钉钉的完整告警文本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String alarmContent;
    /**
     * 本次消费的 RabbitMQ 队列名称
     */
    @Schema(description = "本次消费的 RabbitMQ 队列名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String queueName;
    /**
     * MES 工艺变更处理失败时间，按 MES 上报的 ISO-8601 字符串原样保存
     */
    @Schema(description = "MES 工艺变更处理失败时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private String failureTime;
    /**
     * MES 工艺变更处理失败原因
     */
    @Schema(description = "MES 工艺变更处理失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processErrorMsg;
    /**
     * 钉钉告警处理器是否成功发送
     */
    @Schema(description = "钉钉告警处理器是否成功发送", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean sendSuccess;
    /**
     * 钉钉发送异常原因，发送成功时为空
     */
    @Schema(description = "钉钉发送异常原因")
    private String sendErrorMsg;

}
