package cn.jonhon.jump.module.rm.recipechange.controller.admin.vo;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.hutool.core.date.DatePattern.NORM_DATETIME_PATTERN;

/**
 * 工艺变更通知管理页面的分页查询条件
 */
@Schema(description = "管理后台 - 工艺变更通知分页查询条件")
@Data
public class RecipeChangeNoticePageReqVO extends PageParam {

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
     * 通知当前状态编码
     */
    @Schema(description = "通知当前状态编码", example = "10")
    private Integer status;

    /**
     * 通知创建时间范围，数组第一个元素为开始时间，第二个元素为结束时间
     */
    @Schema(description = "通知创建时间范围")
    @DateTimeFormat(pattern = NORM_DATETIME_PATTERN)
    private LocalDateTime[] createTime;

}
