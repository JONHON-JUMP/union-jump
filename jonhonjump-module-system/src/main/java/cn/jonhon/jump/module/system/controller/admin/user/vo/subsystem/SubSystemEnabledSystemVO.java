package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 已启用「新增人员」接口的业务系统精简 VO（用户创建同步下拉用）")
@Data
public class SubSystemEnabledSystemVO {

    @Schema(description = "业务系统 ID")
    private Long id;

    @Schema(description = "业务系统名称")
    private String name;

    @Schema(description = "是否 JUMP 门户业务系统（已绑定 OAuth）。true=同步时还会写入外部用户管理；false=仅调对方新增人员接口")
    private Boolean portalBound;

}
