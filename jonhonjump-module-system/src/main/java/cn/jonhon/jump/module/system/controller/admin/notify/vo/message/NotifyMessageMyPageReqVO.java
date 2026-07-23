package cn.jonhon.jump.module.system.controller.admin.notify.vo.message;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.jonhon.jump.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 站内信分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NotifyMessageMyPageReqVO extends PageParam {

    @Schema(description = "是否已读", example = "true")
    private Boolean readStatus;

    @Schema(description = "通知类型，对应 system_notify_template_type 字典", example = "1")
    private Integer templateType;

    @Schema(description = "标题，模糊匹配模版内容", example = "欢迎加入")
    private String title;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
