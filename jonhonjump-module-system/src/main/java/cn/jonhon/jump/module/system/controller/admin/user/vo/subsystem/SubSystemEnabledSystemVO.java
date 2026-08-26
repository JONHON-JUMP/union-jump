package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 已配置人员接口的外部系统精简 VO（用户创建联动下拉用）")
@Data
public class SubSystemEnabledSystemVO {

    @Schema(description = "外部系统 ID")
    private Long id;

    @Schema(description = "外部系统名称")
    private String name;

}
