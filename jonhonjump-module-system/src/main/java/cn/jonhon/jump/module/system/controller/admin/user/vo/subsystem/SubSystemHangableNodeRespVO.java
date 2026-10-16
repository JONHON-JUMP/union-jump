package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 人员可挂外部节点（仅叶子可选）")
@Data
public class SubSystemHangableNodeRespVO {

    @Schema(description = "节点类型：group=MES父节点不可选；leaf=可挂叶子（系统本身或车间）", example = "leaf")
    private String nodeType;

    @Schema(description = "展示名")
    private String label;

    @Schema(description = "业务系统 ID")
    private Long subSystemId;

    @Schema(description = "系统 clientId", example = "mes5500")
    private String clientId;

    @Schema(description = "业务系统名称")
    private String subSystemName;

    @Schema(description = "车间编号；系统级叶子为空", example = "5501")
    private String workshopCode;

    @Schema(description = "车间名称")
    private String workshopName;

    @Schema(description = "是否启用车间维度（父节点或系统叶子）")
    private Boolean workshopScopeEnabled;

    @Schema(description = "子节点（仅 group 有；子节点均为 leaf）")
    private List<SubSystemHangableNodeRespVO> children;

}
