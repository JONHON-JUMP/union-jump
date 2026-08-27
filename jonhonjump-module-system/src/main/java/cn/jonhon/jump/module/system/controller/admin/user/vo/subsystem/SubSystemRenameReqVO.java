package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 业务系统重命名 Request VO")
@Data
public class SubSystemRenameReqVO {

    @Schema(description = "系统编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "系统编号不能为空")
    private Long id;

    @Schema(description = "系统名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "Camstar人员管理")
    @NotBlank(message = "系统名称不能为空")
    @Size(max = 100, message = "系统名称长度不能超过 100 个字符")
    private String systemName;

}
