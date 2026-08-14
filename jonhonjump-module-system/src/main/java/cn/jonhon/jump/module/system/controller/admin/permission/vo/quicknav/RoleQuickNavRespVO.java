package cn.jonhon.jump.module.system.controller.admin.permission.vo.quicknav;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 角色快捷导航 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleQuickNavRespVO {

    @Schema(description = "菜单编号列表（按显示顺序）")
    private List<Long> menuIds;

}
