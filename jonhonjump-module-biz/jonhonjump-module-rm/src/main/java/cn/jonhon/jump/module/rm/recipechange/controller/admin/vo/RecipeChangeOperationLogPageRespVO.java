package cn.jonhon.jump.module.rm.recipechange.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志查看页面的列表行数据
 */
@Schema(description = "管理后台 - 工艺变更操作日志分页响应")
@Data
public class RecipeChangeOperationLogPageRespVO {

    /**
     * 操作日志主键
     */
    @Schema(description = "操作日志主键", example = "101")
    private Long id;

    /**
     * 操作发生时间
     */
    @Schema(description = "操作发生时间")
    private LocalDateTime operationTime;

    /**
     * 操作类型编码
     */
    @Schema(description = "操作类型编码", example = "10")
    private Integer operationType;

    /**
     * 操作类型中文名称
     */
    @Schema(description = "操作类型中文名称", example = "MPM调用")
    private String operationTypeName;

    /**
     * MPM 通知唯一标识
     */
    @Schema(description = "MPM 通知唯一标识", example = "MPM-20260821-001")
    private String notifyId;

    /**
     * 目标车间编码
     */
    @Schema(description = "目标车间编码", example = "5600")
    private String workshopCode;

    /**
     * 操作执行者或来源系统标识
     */
    @Schema(description = "操作人", example = "MPM")
    private String operator;

    /**
     * 操作结果编码
     */
    @Schema(description = "操作结果编码", example = "10")
    private Integer operationResult;

    /**
     * 操作结果中文名称
     */
    @Schema(description = "操作结果中文名称", example = "成功")
    private String operationResultName;

    /**
     * 操作失败原因，成功时为空
     */
    @Schema(description = "错误信息", example = "连接超时")
    private String errorMsg;

}
