package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 业务系统子表叶子分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SubSystemWorkshopPageReqVO extends PageParam {

    @Schema(description = "业务系统 ID", example = "1")
    private Long subSystemId;

    @Schema(description = "叶子编号", example = "5501")
    private String workshopCode;

    @Schema(description = "叶子名称", example = "环控管路")
    private String workshopName;

    @Schema(description = "状态（0正常 1停用）", example = "0")
    private Integer status;

}
