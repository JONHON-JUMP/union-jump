package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 外部系统精简 Response VO")
@Data
public class SubSystemClientSimpleRespVO {

    @Schema(description = "外部系统编号")
    private Long id;

    @Schema(description = "客户端编号")
    private String clientId;

    @Schema(description = "外部系统名称")
    private String name;

    @Schema(description = "客户端 Logo")
    private String logo;

    @Schema(description = "外部系统用户数量")
    private Long userCount;

    @Schema(description = "外部系统角色数量")
    private Long roleCount;

    @Schema(description = "外部系统菜单数量")
    private Long menuCount;

    @Schema(description = "外部系统岗位数量")
    private Long postCount;

    @Schema(description = "外部系统班组数量")
    private Long teamCount;

    @Schema(description = "是否 JUMP 门户业务系统（已绑定 OAuth）。false=仅接口目标，不出现在外部用户管理等业务系统列表")
    private Boolean portalBound;

}
