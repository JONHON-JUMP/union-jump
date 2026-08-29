package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 花名册人员手动调「新增人员」接口注册 Request VO")
@Data
public class SubSystemUserRegisterReqVO {

    @Schema(description = "新增人员接口目标系统 ID（接口管理中 create 用途已启用的系统，与花名册系统解耦）", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    @NotNull(message = "新增人员接口目标不能为空")
    private Long apiSubSystemId;

    @Schema(description = "花名册行编号列表（须同属一个花名册系统）", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1,2]")
    @NotEmpty(message = "请选择要注册的用户")
    private List<Long> ids;

}
