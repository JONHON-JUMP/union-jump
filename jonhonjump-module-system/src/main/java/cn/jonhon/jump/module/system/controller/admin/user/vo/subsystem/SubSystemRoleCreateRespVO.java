package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 创建业务角色结果")
@Data
@Accessors(chain = true)
public class SubSystemRoleCreateRespVO {

    @Schema(description = "本地角色编号")
    private Long id;

    @Schema(description = "外部角色已创建但未查回 roleId 时的提示；为空表示未同步或已关联")
    private String warning;

}
