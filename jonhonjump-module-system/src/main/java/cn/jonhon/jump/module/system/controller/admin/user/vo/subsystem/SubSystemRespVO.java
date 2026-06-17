package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 外部系统 Response VO")
@Data
public class SubSystemRespVO {

    @Schema(description = "系统编号")
    private Long id;

    @Schema(description = "OAuth2 客户端编号")
    private String clientId;

    @Schema(description = "系统名称")
    private String systemName;

    @Schema(description = "系统描述")
    private String description;

    @Schema(description = "系统访问地址")
    private String systemUrl;

    @Schema(description = "系统图标")
    private String systemIcon;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "OAuth2 应用名称")
    private String clientName;

    @Schema(description = "OAuth2 应用图标")
    private String clientLogo;

    @Schema(description = "OAuth2 应用描述")
    private String clientDescription;

    @Schema(description = "OAuth2 应用状态")
    private Integer clientStatus;

}
