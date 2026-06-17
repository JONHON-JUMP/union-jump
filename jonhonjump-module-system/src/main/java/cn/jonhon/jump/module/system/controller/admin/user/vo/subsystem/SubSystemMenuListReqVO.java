package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;



import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



@Schema(description = "管理后台 - 外部系统菜单列表 Request VO")

@Data

public class SubSystemMenuListReqVO {



    @Schema(description = "外部系统 ID", example = "1")

    private Long subSystemId;



    @Schema(description = "菜单名称，模糊匹配", example = "生产监控")

    private String name;



    @Schema(description = "状态", example = "0")

    private Integer status;



}

