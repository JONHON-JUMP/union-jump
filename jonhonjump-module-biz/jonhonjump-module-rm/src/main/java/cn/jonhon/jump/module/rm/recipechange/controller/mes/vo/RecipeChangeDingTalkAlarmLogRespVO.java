package cn.jonhon.jump.module.rm.recipechange.controller.mes.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MES - 钉钉告警发送结果日志响应
 */
@Schema(description = "MES - 钉钉告警发送结果日志响应")
@Data
public class RecipeChangeDingTalkAlarmLogRespVO {

    /**
     * JUMP 是否已成功写入本次钉钉告警操作日志
     */
    @Schema(description = "JUMP 是否已成功写入本次钉钉告警操作日志")
    private Boolean recorded;
    /**
     * JUMP 完成本次日志写入的时间
     */
    @Schema(description = "JUMP 完成本次日志写入的时间")
    private LocalDateTime processedTime;

}
