package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.jonhon.jump.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 外部系统班组分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SubSystemTeamPageReqVO extends PageParam {

    @Schema(description = "外部系统 ID", example = "1")
    private Long subSystemId;

    @Schema(description = "班组编码", example = "WS01-T01")
    private String teamCode;

    @Schema(description = "班组名称", example = "一车间甲班")
    private String teamName;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
