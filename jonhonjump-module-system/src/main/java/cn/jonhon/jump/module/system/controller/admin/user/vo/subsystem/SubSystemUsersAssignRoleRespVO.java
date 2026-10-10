package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 外部系统用户分配角色 Response VO")
@Data
public class SubSystemUsersAssignRoleRespVO {

    @Schema(description = "是否同步了外部系统（未勾选同步时为 false）", example = "true")
    private Boolean externalSynced;

    @Schema(description = "外部同步是否成功（未同步时为 null）；失败时本地分配已生效，不回滚", example = "true")
    private Boolean externalSuccess;

    @Schema(description = "外部同步失败原因（成功时为空）", example = "对方系统未找到用户【4200_00078】")
    private String externalMessage;

}
