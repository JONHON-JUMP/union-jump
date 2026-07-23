package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 子系统门户菜单 Response VO")
@Data
public class SubSystemPortalMenuRespVO {

    @Schema(description = "菜单编号")
    private Long id;

    @Schema(description = "父菜单编号")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "组件名")
    private String componentName;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "菜单样式编号")
    private Long styleId;

    @Schema(description = "图标形状")
    private String shape;

    @Schema(description = "主色 HEX")
    private String color;

    @Schema(description = "是否可见")
    private Boolean visible;

    @Schema(description = "是否缓存")
    private Boolean keepAlive;

    @Schema(description = "是否总是显示")
    private Boolean alwaysShow;

    @Schema(description = "Iframe 外链地址，仅菜单页面使用")
    private String link;

    @Schema(description = "菜单说明书文件地址")
    private String manualUrl;

    @Schema(description = "子菜单")
    private List<SubSystemPortalMenuRespVO> children;

}
