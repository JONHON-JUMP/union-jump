package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 查询外部系统角色列表 Request VO")
@Data
public class SubSystemExternalRoleQueryReqVO {

    @Schema(description = "接口目标系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    @NotNull(message = "接口目标不能为空")
    private Long apiSubSystemId;

    @Schema(description = "车间编号；不传时查询对方系统全部角色", example = "4200")
    @Size(max = 32, message = "车间编号长度不能超过 32 个字符")
    private String workshopCode;

    @Schema(description = "角色名称；传入时按角色名全系统补查，避免车间列表截断漏掉新建角色", example = "4200_操作员")
    @Size(max = 64, message = "角色名称长度不能超过 64 个字符")
    private String roleName;

}
