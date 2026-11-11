package cn.jonhon.jump.module.system.controller.admin.user.vo.user;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JUMP 主用户和业务账号同一行导入。
 * 部门在页面上选择，不在表里。业务系统由车间编号对应的叶子确定。
 * 状态默认启用。业务用户名原样保存。班组、岗位不在本表导入。
 * 最后一列备注只出现在模板里提示填表人，导入逻辑不读取。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBizImportExcelVO {

    @ExcelProperty("用户名（必填）")
    private String username;

    @ExcelProperty("用户姓名（必填）")
    private String nickname;

    @ExcelProperty("工号（非必填）")
    private String employeeNo;

    @ExcelProperty("域账号（非必填）")
    private String domainNo;

    @ExcelProperty("车间编号（必填）")
    private String workshopId;

    @ExcelProperty("业务用户名（必填）")
    private String bizUsername;

    @ExcelProperty("角色标识（必填）")
    private String roleCodes;

    @ExcelProperty("刷卡卡号（非必填）")
    private String cardNo;

    @ExcelProperty("ERP号（非必填）")
    private String erpNo;

    /** 模板提示列。导入时不读取、不入库。 */
    @ExcelProperty("备注（仅提示，不导入）")
    private String remark;

}
