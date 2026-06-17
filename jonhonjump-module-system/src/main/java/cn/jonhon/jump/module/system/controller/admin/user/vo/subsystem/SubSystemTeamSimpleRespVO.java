package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 子系统班组精简 Response VO")
@Data
public class SubSystemTeamSimpleRespVO {

    @Schema(description = "班组编号")
    private Long id;

    @Schema(description = "班组编码")
    private String teamCode;

    @Schema(description = "班组名称")
    private String teamName;

}
