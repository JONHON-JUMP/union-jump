package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 外部系统用户新增/修改 Request VO")
@Data
public class SubSystemUsersSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "外部系统不能为空")
    private Long subSystemId;

    @Schema(description = "主数据人员 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "主数据用户不能为空")
    private Long mainUserId;

    @Schema(description = "车间编号")
    private String workshopId;

    @Schema(description = "班组编号")
    private String teamId;

    @Schema(description = "主页面菜单 ID")
    private Long homeMenuId;

    @Schema(description = "状态（0正常 1禁用）", example = "0")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "角色编号列表")
    private List<Long> roleIds;

    @Schema(description = "岗位编号列表")
    private List<Long> postIds;

}
