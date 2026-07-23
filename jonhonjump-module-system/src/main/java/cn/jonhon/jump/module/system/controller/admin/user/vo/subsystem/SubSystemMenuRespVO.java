package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;



import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



import java.time.LocalDateTime;



@Schema(description = "管理后台 - 外部系统菜单 Response VO")

@Data

public class SubSystemMenuRespVO {



    @Schema(description = "菜单编号")

    private Long id;



    @Schema(description = "外部系统 ID")

    private Long subSystemId;



    @Schema(description = "外部系统名称")

    private String clientName;



    @Schema(description = "菜单名称")

    private String name;



    @Schema(description = "权限标识")

    private String permission;



    @Schema(description = "菜单类型，参见 MenuTypeEnum")

    private Integer type;



    @Schema(description = "显示顺序")

    private Integer sort;



    @Schema(description = "父菜单 ID")

    private Long parentId;



    @Schema(description = "路由地址")

    private String path;



    @Schema(description = "菜单图标")

    private String icon;



    @Schema(description = "菜单样式编号")

    private Long styleId;



    @Schema(description = "组件路径")

    private String component;



    @Schema(description = "组件名称")

    private String componentName;



    @Schema(description = "状态")

    private Integer status;



    @Schema(description = "是否可见")

    private Boolean visible;



    @Schema(description = "是否缓存")

    private Boolean keepAlive;



    @Schema(description = "是否总是显示")

    private Boolean alwaysShow;



    @Schema(description = "菜单说明书文件地址")

    private String manualUrl;



    @Schema(description = "创建时间")

    private LocalDateTime createTime;



}

