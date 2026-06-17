package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 外部系统班组 Response VO")
@Data
public class SubSystemTeamRespVO {

    @Schema(description = "班组编号")
    private Long id;

    @Schema(description = "外部系统 ID")
    private Long subSystemId;

    @Schema(description = "外部系统名称")
    private String clientName;

    @Schema(description = "班组编码")
    private String teamCode;

    @Schema(description = "班组名称")
    private String teamName;

    @Schema(description = "班组描述")
    private String description;

    @Schema(description = "班组长人员 ID")
    private Long teamLeaderId;

    @Schema(description = "班组长姓名")
    private String teamLeaderName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
