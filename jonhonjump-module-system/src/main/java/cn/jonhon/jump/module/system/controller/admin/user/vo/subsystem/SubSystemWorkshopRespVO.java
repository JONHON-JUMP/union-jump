package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 业务系统子表叶子 Response VO")
@Data
public class SubSystemWorkshopRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "叶子编号", example = "5501")
    private String workshopCode;

    @Schema(description = "叶子名称", example = "环控管路交付中心")
    private String workshopName;

    @Schema(description = "产品线", example = "环控")
    private String productLine;

    @Schema(description = "所属系统 client_id", example = "mes5500")
    private String clientId;

    @Schema(description = "所属业务系统 ID（sub_system.id）")
    private Long subSystemId;

    @Schema(description = "业务系统名称")
    private String subSystemName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态（0正常 1停用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "该叶子下业务用户数")
    private Long userCount;

    @Schema(description = "该叶子下业务角色数")
    private Long roleCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
