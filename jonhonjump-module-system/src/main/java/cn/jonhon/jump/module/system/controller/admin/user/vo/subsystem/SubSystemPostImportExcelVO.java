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
public class SubSystemPostImportExcelVO {

    @ExcelProperty("岗位名称")
    private String name;

    @ExcelProperty("岗位编码")
    private String code;

    @ExcelProperty("显示顺序")
    private Integer sort;

    @ExcelProperty("状态(0正常1停用)")
    private Integer status;

}
