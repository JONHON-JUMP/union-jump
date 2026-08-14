package cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "快捷导航应用项（可直接渲染，不必等 my-menus 全树）")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuickNavAppItemVO {

    private Long menuId;
    private String name;
    /** 门户路由，如 /portal/scada/xxx */
    private String path;
    private String icon;
    private String color;
    private String shape;
    private String manualUrl;

}
