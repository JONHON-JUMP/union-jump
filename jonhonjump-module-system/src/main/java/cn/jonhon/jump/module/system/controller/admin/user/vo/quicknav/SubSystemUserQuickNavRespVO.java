package cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 用户外部子系统快捷导航 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubSystemUserQuickNavRespVO {

    @Schema(description = "已保存的子系统菜单编号列表（按顺序）")
    private List<Long> menuIds;

    @Schema(description = "是否已保存过个人配置")
    private Boolean configured;

}
