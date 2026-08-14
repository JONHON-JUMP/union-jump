package cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 菜单样式创建/修改 Request VO")
@Data
public class MenuColorSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "样式名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "JUMP蓝·计划调度")
    @NotBlank(message = "样式名称不能为空")
    @Size(max = 50, message = "样式名称长度不能超过50个字符")
    private String name;

    @Schema(description = "图标形状（固定默认 rounded，前端不可选）", example = "rounded")
    private String shape;

    @Schema(description = "主色 HEX", example = "#087CE5")
    @NotBlank(message = "主色不能为空")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "主色格式必须为 #RRGGBB")
    private String color;

    @Schema(description = "MES 大类编码", example = "M01-计划调度")
    @Size(max = 32, message = "MES 大类编码长度不能超过32个字符")
    private String mesCategory;

    @Schema(description = "适用场景说明", example = "适用于生产计划、排产、订单管理等")
    @Size(max = 500, message = "说明长度不能超过500个字符")
    private String remark;

    @Schema(description = "显示排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "显示排序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
