package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubSystemMenuImportExcelVO {

    @ExcelProperty("父菜单名称")
    private String parentName;

    @ExcelProperty("菜单名称")
    private String name;

    @ExcelProperty("菜单类型(1目录2菜单3按钮)")
    private Integer type;

    @ExcelProperty("显示顺序")
    private Integer sort;

    @ExcelProperty("路由地址")
    private String path;

    @ExcelProperty("组件路径")
    private String component;

    @ExcelProperty("组件名称")
    private String componentName;

    @ExcelProperty("权限标识")
    private String permission;

    @ExcelProperty("图标")
    private String icon;

    @ExcelProperty("状态(0正常1停用)")
    private Integer status;

    @ExcelProperty("是否可见(true/false)")
    private Boolean visible;

}
