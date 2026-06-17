package cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 用户快捷导航 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuickNavRespVO {

    @Schema(description = "已保存的菜单编号列表（按顺序）")
    private List<Long> menuIds;

    @Schema(description = "是否已保存过个人配置（false 表示默认空，尚未自定义）")
    private Boolean configured;

}
