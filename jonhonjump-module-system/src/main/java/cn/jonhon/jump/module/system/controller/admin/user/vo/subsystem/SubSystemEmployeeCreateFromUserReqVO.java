package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 从主系统用户创建子系统人员 Request VO（用户管理页联动）")
@Data
public class SubSystemEmployeeCreateFromUserReqVO {

    @Schema(description = "主系统用户 ID（system_users.id）", requiredMode = Schema.RequiredMode.REQUIRED, example = "146")
    @NotNull(message = "主系统用户不能为空")
    private Long userId;

    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "外部系统不能为空")
    private Long subSystemId;

}
