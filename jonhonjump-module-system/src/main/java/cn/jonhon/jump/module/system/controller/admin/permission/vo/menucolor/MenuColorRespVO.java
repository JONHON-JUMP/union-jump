package cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 菜单样式 Response VO")
@Data
public class MenuColorRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "图标形状：rounded/square/circle/pill")
    private String shape;

    @Schema(description = "样式名称")
    private String name;

    @Schema(description = "主色 HEX")
    private String color;

    @Schema(description = "MES 大类编码")
    private String mesCategory;

    @Schema(description = "适用场景说明")
    private String remark;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
