package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 子系统用户 Excel 导入行（独立花名册，不强制匹配主系统用户）。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubSystemUserImportExcelVO {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("用户姓名")
    private String nickname;

    @ExcelProperty("车间编号")
    private String workshopId;

    /** 对应班组表 team_code，不是班组主键 id */
    @ExcelProperty("班组编码")
    private String teamId;

    @ExcelProperty("角色标识(逗号分隔)")
    private String roleCodes;

    @ExcelProperty("状态(0正常1停用)")
    private String status;

    @ExcelProperty("备注")
    private String remark;

}
