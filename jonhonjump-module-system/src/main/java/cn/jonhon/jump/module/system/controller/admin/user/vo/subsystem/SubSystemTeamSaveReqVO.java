package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 外部系统班组创建/更新 Request VO")
@Data
public class SubSystemTeamSaveReqVO {

    @Schema(description = "主键编号")
    private Long id;

    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "业务系统不能为空")
    private Long subSystemId;

    @Schema(description = "JUMP 部门 ID（可选）", example = "100")
    private Long deptId;

    @Schema(description = "车间编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4200")
    @NotBlank(message = "车间编号不能为空")
    @Size(max = 50, message = "车间编号长度不能超过 50 个字符")
    private String workshopCode;

    @Schema(description = "班组编码。新增同步时由接口查回，不由页面填写", example = "甲班")
    @Size(max = 100, message = "班组编码长度不能超过 100 个字符")
    private String teamCode;

    @Schema(description = "班组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "一车间甲班")
    @NotBlank(message = "班组名称不能为空")
    @Size(max = 100, message = "班组名称长度不能超过 100 个字符")
    private String teamName;

    @Schema(description = "班组描述", example = "负责一车间 A 线生产")
    @Size(max = 200, message = "班组描述长度不能超过 200 个字符")
    private String description;

    @Schema(description = "班组长人员 ID", example = "1")
    private Long teamLeaderId;

    @Schema(description = "班组长姓名", example = "张三")
    @Size(max = 20, message = "班组长姓名长度不能超过 20 个字符")
    private String teamLeaderName;

    @Schema(description = "新增班组时是否同步到业务系统（需已配置班组新增接口）", example = "true")
    private Boolean syncToExternal;

}
