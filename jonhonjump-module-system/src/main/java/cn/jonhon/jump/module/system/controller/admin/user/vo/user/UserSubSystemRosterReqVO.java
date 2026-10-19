package cn.jonhon.jump.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 新建主系统用户时，登记业务系统的目标：业务系统 + 车间叶子。
 * <p>
 * 无车间维度的系统 workshopId 为空；多车间系统必须选到具体叶子，否则该行不会出现在门户。
 */
@Schema(description = "管理后台 - 用户登记业务系统目标")
@Data
public class UserSubSystemRosterReqVO {

    @Schema(description = "业务系统编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    @NotNull(message = "业务系统不能为空")
    private Long subSystemId;

    @Schema(description = "车间叶子编号；无车间维度时为空", example = "5501")
    private String workshopId;

}
