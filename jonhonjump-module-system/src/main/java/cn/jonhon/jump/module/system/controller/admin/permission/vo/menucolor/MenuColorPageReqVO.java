package cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 菜单颜色分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MenuColorPageReqVO extends PageParam {

    @Schema(description = "颜色名称", example = "计划")
    private String name;

    @Schema(description = "MES 大类", example = "M02")
    private String mesCategory;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
