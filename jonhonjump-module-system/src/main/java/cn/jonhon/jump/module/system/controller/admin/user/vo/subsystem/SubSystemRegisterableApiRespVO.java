package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 可选「新增人员」接口目标 Response VO")
@Data
@Accessors(chain = true)
public class SubSystemRegisterableApiRespVO {

    @Schema(description = "接口目标对应的业务系统 ID", example = "9")
    private Long subSystemId;

    @Schema(description = "接口目标系统名称", example = "Camstar人员管理")
    private String systemName;

}
