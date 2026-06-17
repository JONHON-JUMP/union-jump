package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;



import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



import java.time.LocalDateTime;



@Schema(description = "管理后台 - 外部系统岗位 Response VO")

@Data

public class SubSystemPostRespVO {



    @Schema(description = "岗位编号")

    private Long id;



    @Schema(description = "外部系统 ID")

    private Long subSystemId;



    @Schema(description = "外部系统名称")

    private String clientName;



    @Schema(description = "岗位名称")

    private String name;



    @Schema(description = "岗位编码")

    private String code;



    @Schema(description = "显示顺序")

    private Integer sort;



    @Schema(description = "状态")

    private Integer status;



    @Schema(description = "创建时间")

    private LocalDateTime createTime;



}

