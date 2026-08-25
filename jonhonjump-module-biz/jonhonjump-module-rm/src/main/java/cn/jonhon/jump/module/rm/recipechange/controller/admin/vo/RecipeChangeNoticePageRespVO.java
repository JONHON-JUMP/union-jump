package cn.jonhon.jump.module.rm.recipechange.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工艺变更通知管理页面的列表行数据
 */
@Schema(description = "管理后台 - 工艺变更通知分页响应")
@Data
public class RecipeChangeNoticePageRespVO {

    /**
     * 通知主键，供日志查询和内容查询操作使用
     */
    @Schema(description = "通知主键", example = "1")
    private Long id;

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
     * 通知当前状态编码，用于前端筛选和控制操作按钮
     */
    @Schema(description = "通知当前状态编码", example = "10")
    private Integer status;

    /**
     * 通知当前状态中文名称
     */
    @Schema(description = "通知当前状态中文名称", example = "已发送MQ")
    private String statusName;

    /**
     * 当前已执行的自动重试次数
     */
    @Schema(description = "当前已执行的自动重试次数", example = "0")
    private Integer retryCount;

    /**
     * 最大允许自动重试次数
     */
    @Schema(description = "最大允许自动重试次数", example = "3")
    private Integer maxRetry;

    /**
     * 通知记录创建时间
     */
    @Schema(description = "通知记录创建时间")
    private LocalDateTime createTime;

}
