package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 外部系统用户分配角色 Request VO")
@Data
public class SubSystemUsersAssignRoleReqVO {

    @Schema(description = "外部系统用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户编号不能为空")
    private Long id;

    @Schema(description = "角色编号列表")
    @NotNull(message = "角色编号列表不能为空")
    private List<Long> roleIds;

    @Schema(description = "是否同步到外部系统（勾选后调对方接口上挂/解除；本地已入库不回滚）", example = "false")
    private Boolean syncToExternal;

    @Schema(description = "接口目标系统 ID（勾选同步时必填，与人员注册的接口目标相同）", example = "9")
    private Long apiSubSystemId;

}
