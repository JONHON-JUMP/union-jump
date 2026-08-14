package cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 菜单颜色精简 Response VO")
@Data
public class MenuColorSimpleRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "样式名称")
    private String name;

    @Schema(description = "图标形状：rounded/square/circle/pill")
    private String shape;

    @Schema(description = "主色 HEX")
    private String color;

    @Schema(description = "MES 大类编码")
    private String mesCategory;

    @Schema(description = "适用场景说明")
    private String remark;

}
