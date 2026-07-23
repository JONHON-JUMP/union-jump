package cn.jonhon.jump.module.system.controller.admin.user.vo.user;

import cn.idev.excel.annotation.ExcelProperty;
import cn.jonhon.jump.framework.excel.core.annotations.DictFormat;
import cn.jonhon.jump.framework.excel.core.convert.DictConvert;
import cn.jonhon.jump.module.system.enums.DictTypeConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户 Excel 导入 VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserImportExcelVO {

    @ExcelProperty("登录名称")
    private String username;

    @ExcelProperty("用户名称")
    private String nickname;

    @ExcelProperty("部门编号")
    private Long deptId;

    @ExcelProperty("用户邮箱")
    private String email;

    @ExcelProperty("手机号码")
    private String mobile;

    @ExcelProperty("工号")
    private String employeeNo;

    @ExcelProperty("域账号")
    private String domainNo;

    @ExcelProperty("刷卡卡号")
    private String cardNo;

    /**
     * ERP 账号，多个用英文逗号/中文逗号/顿号分隔，例如：erp001,erp002
     */
    @ExcelProperty("ERP账号")
    private String erpNos;

    @ExcelProperty(value = "用户性别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.USER_SEX)
    private Integer sex;

    @ExcelProperty(value = "账号状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

}
