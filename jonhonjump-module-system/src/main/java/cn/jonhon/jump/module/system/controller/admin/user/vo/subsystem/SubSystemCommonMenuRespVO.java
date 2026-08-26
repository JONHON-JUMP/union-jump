package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 通用菜单（模板）Response VO")
@Data
public class SubSystemCommonMenuRespVO {

    @Schema(description = "模板编号")
    private Long id;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "菜单类型（1目录 2菜单 3按钮）")
    private Integer type;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态（0开启 1关闭）")
    private Integer status;

    @Schema(description = "菜单说明书文件地址")
    private String manualUrl;

    @Schema(description = "已挂载子系统编号列表")
    private List<Long> subSystemIds;

    @Schema(description = "已挂载子系统名称列表")
    private List<String> subSystemNames;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
