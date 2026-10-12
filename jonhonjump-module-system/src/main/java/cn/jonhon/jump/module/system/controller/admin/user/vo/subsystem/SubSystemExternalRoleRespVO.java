package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 外部系统角色（如 Camstar ROLEDEF）精简信息 VO")
@Data
@Accessors(chain = true)
public class SubSystemExternalRoleRespVO {

    @Schema(description = "角色名称", example = "4200_班组长")
    private String roleName;

    @Schema(description = "外部角色 ID（Camstar roleId）", example = "001bda8000001234")
    private String roleId;

}
