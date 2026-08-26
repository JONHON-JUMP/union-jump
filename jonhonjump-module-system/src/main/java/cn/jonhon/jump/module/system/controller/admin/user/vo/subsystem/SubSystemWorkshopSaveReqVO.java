package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 外部系统车间创建/更新 Request VO")
@Data
public class SubSystemWorkshopSaveReqVO {

    @Schema(description = "主键编号")
    private Long id;

    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "外部系统不能为空")
    private Long subSystemId;

    @Schema(description = "JUMP 部门 ID", example = "100")
    private Long deptId;

    @Schema(description = "车间编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "4200")
    @NotBlank(message = "车间编码不能为空")
    @Size(max = 100, message = "车间编码长度不能超过 100 个字符")
    private String workshopCode;

    @Schema(description = "车间名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "制造二部")
    @NotBlank(message = "车间名称不能为空")
    @Size(max = 100, message = "车间名称长度不能超过 100 个字符")
    private String workshopName;

    @Schema(description = "车间描述", example = "二部车间")
    @Size(max = 200, message = "车间描述长度不能超过 200 个字符")
    private String description;

}
