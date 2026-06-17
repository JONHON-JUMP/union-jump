package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;



import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



import java.util.List;



@Schema(description = "管理后台 - 外部系统菜单树 Response VO")

@Data

public class SubSystemMenuTreeRespVO {



    @Schema(description = "菜单编号")

    private Long id;



    @Schema(description = "菜单名称")

    private String name;



    @Schema(description = "父菜单编号")

    private Long parentId;



    @Schema(description = "菜单类型（M目录 C菜单 F按钮）")

    private String type;



    @Schema(description = "显示顺序")

    private Integer orderNum;



    @Schema(description = "子菜单")

    private List<SubSystemMenuTreeRespVO> children;



}

