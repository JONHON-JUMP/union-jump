package cn.jonhon.jump.module.system.controller.admin.notice.vo;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.jonhon.jump.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 通知公告分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NoticePageReqVO extends PageParam {

    @Schema(description = "通知标题，模糊匹配", example = "维护通知")
    private String title;

    @Schema(description = "展示状态，参见 CommonStatusEnum 枚举类", example = "0")
    private Integer status;

    @Schema(description = "通知类型，对应 system_notify_template_type 字典", example = "1")
    private Integer type;

    @Schema(description = "发布人，模糊匹配", example = "刘爽")
    private String publisherName;

    @Schema(description = "发布部门，模糊匹配", example = "人力资源部")
    private String deptName;

    @Schema(description = "创建者，模糊匹配", example = "1")
    private String creator;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
