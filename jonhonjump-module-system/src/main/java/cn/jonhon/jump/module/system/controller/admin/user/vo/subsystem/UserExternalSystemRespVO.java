package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 当前用户外部系统 Response VO")
@Data
public class UserExternalSystemRespVO {

    @Schema(description = "子系统关系编号")
    private Long id;

    @Schema(description = "外部系统编号")
    private Long subSystemId;

    @Schema(description = "外部系统访问地址")
    private String systemUrl;

    @Schema(description = "子系统客户端编号")
    private String clientId;

    @Schema(description = "子系统名称")
    private String clientName;

    @Schema(description = "应用图标")
    private String logo;

    @Schema(description = "SSO 跳转地址")
    private String ssoUrl;

    @Schema(description = "用户在该子系统的主页面菜单编号")
    private Long homeMenuId;

    @Schema(description = "子系统默认主页面名称")
    private String homePageName;

    @Schema(description = "子系统默认主页面 URL")
    private String homePageUrl;

    @Schema(description = "车间编号")
    private String workshopId;

    @Schema(description = "班组编号")
    private String teamId;

}
