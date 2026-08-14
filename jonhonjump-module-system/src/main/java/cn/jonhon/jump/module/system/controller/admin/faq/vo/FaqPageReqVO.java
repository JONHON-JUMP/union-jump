package cn.jonhon.jump.module.system.controller.admin.faq.vo;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.jonhon.jump.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 常见 QA 分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FaqPageReqVO extends PageParam {

    @Schema(description = "标题，模糊匹配")
    private String title;

    @Schema(description = "分类，对应 system_faq_category 字典")
    private Integer category;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类")
    private Integer status;

    @Schema(description = "发布人，模糊匹配")
    private String publisherName;

    @Schema(description = "发布部门，模糊匹配")
    private String deptName;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
