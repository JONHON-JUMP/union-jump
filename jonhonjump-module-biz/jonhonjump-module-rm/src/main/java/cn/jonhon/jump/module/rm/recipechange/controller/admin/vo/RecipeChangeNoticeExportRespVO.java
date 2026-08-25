package cn.jonhon.jump.module.rm.recipechange.controller.admin.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工艺变更通知 Excel 导出行数据
 */
@Schema(description = "管理后台 - 工艺变更通知 Excel 导出行数据")
@Data
@ExcelIgnoreUnannotated
public class RecipeChangeNoticeExportRespVO {

    /** MPM 通知唯一标识 */
    @ExcelProperty("MPM通知ID")
    private String notifyId;
    /** 目标车间编码 */
    @ExcelProperty("车间")
    private String workshopCode;
    /** 通知当前状态编码，仅用于在导出前转换状态中文名称，不写入 Excel */
    private Integer status;
    /** 通知当前状态中文名称 */
    @ExcelProperty("状态")
    private String statusName;
    /** 当前已执行的自动重试次数 */
    @ExcelProperty("自动重试次数")
    private Integer retryCount;
    /** 最大允许自动重试次数 */
    @ExcelProperty("最大重试次数")
    private Integer maxRetry;
    /** 通知记录创建时间 */
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
