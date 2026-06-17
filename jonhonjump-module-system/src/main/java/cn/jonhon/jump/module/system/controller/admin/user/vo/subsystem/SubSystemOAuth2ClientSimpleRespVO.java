package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - OAuth2 客户端精简 Response VO（外部系统关联用）")
@Data
public class SubSystemOAuth2ClientSimpleRespVO {

    @Schema(description = "客户端编号")
    private String clientId;

    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "应用图标")
    private String logo;

    @Schema(description = "应用描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "是否已被外部系统关联")
    private Boolean bound;

}
