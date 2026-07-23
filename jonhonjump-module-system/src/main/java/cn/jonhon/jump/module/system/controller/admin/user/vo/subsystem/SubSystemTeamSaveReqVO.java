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
    @NotNull(message = "外部系统不能为空")
    private Long subSystemId;

    @Schema(description = "班组编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "WS01-T01")
    @NotBlank(message = "班组编码不能为空")
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

}
