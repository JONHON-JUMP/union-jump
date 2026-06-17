package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.jonhon.jump.framework.common.validation.InEnum;
import cn.jonhon.jump.module.system.enums.permission.DataScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Set;

@Schema(description = "管理后台 - 外部系统角色分配数据权限 Request VO")
@Data
public class SubSystemRoleAssignDataScopeReqVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @Schema(description = "数据范围", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "数据范围不能为空")
    @InEnum(value = DataScopeEnum.class, message = "数据范围必须是 {value}")
    private Integer dataScope;

    @Schema(description = "部门编号列表")
    private Set<Long> dataScopeDeptIds = Collections.emptySet();

}
