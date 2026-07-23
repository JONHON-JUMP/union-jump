package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;



import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



import javax.validation.constraints.NotBlank;

import javax.validation.constraints.NotNull;

import javax.validation.constraints.Size;



@Schema(description = "管理后台 - 外部系统菜单创建/修改 Request VO")

@Data

public class SubSystemMenuSaveReqVO {



    @Schema(description = "菜单编号")

    private Long id;



    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "外部系统不能为空")

    private Long subSystemId;



    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotBlank(message = "菜单名称不能为空")

    @Size(max = 50, message = "菜单名称长度不能超过50个字符")

    private String name;



    @Schema(description = "权限标识")

    @Size(max = 100, message = "权限标识长度不能超过100个字符")

    private String permission;



    @Schema(description = "菜单类型", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "菜单类型不能为空")

    private Integer type;



    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "显示顺序不能为空")

    private Integer sort;



    @Schema(description = "父菜单 ID", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "父菜单 ID 不能为空")

    private Long parentId;



    @Schema(description = "路由地址")

    @Size(max = 200, message = "路由地址不能超过200个字符")

    private String path;



    @Schema(description = "菜单图标")

    private String icon;



    @Schema(description = "菜单样式编号")

    private Long styleId;



    @Schema(description = "组件路径")

    @Size(max = 200, message = "组件路径不能超过200个字符")

    private String component;



    @Schema(description = "组件名称")

    private String componentName;



    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "状态不能为空")

    private Integer status;



    @Schema(description = "是否可见")

    private Boolean visible;



    @Schema(description = "是否缓存")

    private Boolean keepAlive;



    @Schema(description = "是否总是显示")

    private Boolean alwaysShow;



    @Schema(description = "菜单说明书文件地址")

    @Size(max = 1024, message = "菜单说明书地址不能超过1024个字符")

    private String manualUrl;



}

