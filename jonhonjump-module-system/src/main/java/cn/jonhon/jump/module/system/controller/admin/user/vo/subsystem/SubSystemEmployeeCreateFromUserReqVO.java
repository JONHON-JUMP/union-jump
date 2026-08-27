package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 从主系统用户同步到业务系统「新增人员」Request VO（用户管理页联动，支持多选）")
@Data
public class SubSystemEmployeeCreateFromUserReqVO {

    @Schema(description = "主系统用户 ID（system_users.id）", requiredMode = Schema.RequiredMode.REQUIRED, example = "146")
    @NotNull(message = "主系统用户不能为空")
    private Long userId;

    @Schema(description = "要同步的业务系统 ID 列表（来自接口管理中已启用「新增人员」的系统）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "[3,5]")
    @NotEmpty(message = "请至少选择一个业务系统")
    private List<Long> subSystemIds;

}
