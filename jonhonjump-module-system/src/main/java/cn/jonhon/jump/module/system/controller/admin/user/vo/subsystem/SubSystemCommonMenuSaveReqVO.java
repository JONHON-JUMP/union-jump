package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 通用菜单（模板）创建/修改 Request VO")
@Data
public class SubSystemCommonMenuSaveReqVO {

    @Schema(description = "模板编号（修改时必填）")
    private Long id;

    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50个字符")
    private String name;

    @Schema(description = "菜单类型（1目录 2菜单 3按钮）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    @Schema(description = "路由地址")
    @Size(max = 200, message = "路由地址长度不能超过200个字符")
    private String path;

    @Schema(description = "权限标识")
    @Size(max = 100, message = "权限标识长度不能超过100个字符")
    private String permission;

    @Schema(description = "菜单图标")
    @Size(max = 100, message = "菜单图标长度不能超过100个字符")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态（0开启 1关闭）")
    private Integer status;

    @Schema(description = "菜单说明书文件地址")
    @Size(max = 1024, message = "菜单说明书地址不能超过1024个字符")
    private String manualUrl;

    @Schema(description = "挂载的子系统编号列表（可为空，之后再挂）")
    private List<Long> subSystemIds;

}
