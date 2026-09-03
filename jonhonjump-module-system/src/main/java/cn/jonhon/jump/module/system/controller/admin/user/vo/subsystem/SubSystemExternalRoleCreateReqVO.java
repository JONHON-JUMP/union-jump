package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 外部系统新增角色 Request VO")
@Data
public class SubSystemExternalRoleCreateReqVO {

    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "请选择外部系统")
    private Long subSystemId;

    @Schema(description = "车间编号（同时作为角色名前缀）", requiredMode = Schema.RequiredMode.REQUIRED, example = "4200")
    @NotBlank(message = "请选择车间")
    @Size(max = 32, message = "车间编号长度不能超过 32 个字符")
    private String workshopCode;

    @Schema(description = "角色名称（不含车间前缀；提交时自动拼接为 车间编号_角色名称）", requiredMode = Schema.RequiredMode.REQUIRED, example = "操作员")
    @NotBlank(message = "请输入角色名称")
    @Size(max = 50, message = "角色名称长度不能超过 50 个字符")
    @Pattern(regexp = "^[^_\\s]+$", message = "角色名称不能包含下划线或空白（车间前缀会自动拼接）")
    private String roleName;

}
