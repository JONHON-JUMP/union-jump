package cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 用户外部子系统快捷导航保存 Request VO")
@Data
public class SubSystemUserQuickNavSaveReqVO {

    @Schema(description = "外部子系统编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "外部子系统编号不能为空")
    private Long subSystemId;

    @Schema(description = "子系统菜单编号列表（按显示顺序）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜单编号列表不能为空")
    private List<Long> menuIds;

}
