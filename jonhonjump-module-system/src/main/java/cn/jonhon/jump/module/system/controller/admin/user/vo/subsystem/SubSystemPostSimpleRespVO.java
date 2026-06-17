package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 子系统岗位精简 Response VO")
@Data
public class SubSystemPostSimpleRespVO {

    @Schema(description = "岗位编号")
    private Long id;

    @Schema(description = "岗位名称")
    private String name;

    @Schema(description = "岗位编码")
    private String code;

}
