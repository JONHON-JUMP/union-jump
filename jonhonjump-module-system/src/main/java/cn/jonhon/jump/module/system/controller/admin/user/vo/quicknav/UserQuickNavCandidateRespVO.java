package cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 用户快捷导航候选菜单 Response VO")
@Data
public class UserQuickNavCandidateRespVO {

    @Schema(description = "菜单编号", example = "1024")
    private Long id;

    @Schema(description = "父菜单编号", example = "1")
    private Long parentId;

    @Schema(description = "菜单名称", example = "用户管理")
    private String name;

    @Schema(description = "菜单类型，参见 MenuTypeEnum", example = "2")
    private Integer type;

    @Schema(description = "菜单图标", example = "user")
    private String icon;

    @Schema(description = "子菜单")
    private List<UserQuickNavCandidateRespVO> children;

}
