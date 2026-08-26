package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.jonhon.jump.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 外部系统车间的分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SubSystemWorkshopPageReqVO extends PageParam {

    @Schema(description = "外部系统 ID", example = "1")
    private Long subSystemId;

    @Schema(description = "JUMP 部门 ID", example = "100")
    private Long deptId;

    @Schema(description = "车间编码", example = "4200")
    private String workshopCode;

    @Schema(description = "车间名称", example = "制造二部")
    private String workshopName;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
