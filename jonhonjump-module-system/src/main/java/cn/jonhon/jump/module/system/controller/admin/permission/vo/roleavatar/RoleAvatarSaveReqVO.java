package cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 角色头像创建/修改 Request VO")
@Data
public class RoleAvatarSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "角色标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "super_admin")
    @NotBlank(message = "角色标识不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "角色标识格式不正确")
    @Size(max = 64, message = "角色标识长度不能超过64个字符")
    private String roleCode;

    @Schema(description = "头像地址（static:文件名 或 http URL）", requiredMode = Schema.RequiredMode.REQUIRED, example = "static:super_admin")
    @NotBlank(message = "头像不能为空")
    @Pattern(regexp = "^(static:[a-z][a-z0-9_-]*|https?://\\S+)$", message = "头像地址格式不正确")
    @Size(max = 512, message = "头像地址长度不能超过512个字符")
    private String avatarUrl;

    @Schema(description = "显示排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "显示排序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;

}
