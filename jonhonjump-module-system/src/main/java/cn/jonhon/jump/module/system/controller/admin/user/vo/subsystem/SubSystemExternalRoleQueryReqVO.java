package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 查询外部系统角色列表 Request VO")
@Data
public class SubSystemExternalRoleQueryReqVO {

    @Schema(description = "接口目标系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    @NotNull(message = "接口目标不能为空")
    private Long apiSubSystemId;

    @Schema(description = "车间编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4200")
    @NotBlank(message = "车间编号不能为空")
    @Size(max = 32, message = "车间编号长度不能超过 32 个字符")
    private String workshopCode;

}
