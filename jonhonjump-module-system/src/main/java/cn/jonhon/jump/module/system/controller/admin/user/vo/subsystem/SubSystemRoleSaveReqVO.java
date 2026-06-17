package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 外部系统角色创建/更新 Request VO")
@Data
public class SubSystemRoleSaveReqVO {

    @Schema(description = "角色编号")
    private Long id;

    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "外部系统不能为空")
    private Long subSystemId;

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "SCADA管理员")
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 30, message = "角色名称长度不能超过 30 个字符")
    private String name;

    @Schema(description = "角色标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "scada_admin")
    @NotBlank(message = "角色标识不能为空")
    @Size(max = 100, message = "角色标识长度不能超过 100 个字符")
    private String code;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "显示顺序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
