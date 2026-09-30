package cn.jonhon.jump.module.system.framework.subsystemapi.dto;

import lombok.Data;

/**
 * 统一子系统人员 DTO（各适配器对外的标准契约）
 *
 * 字段语义与 Camstar EmployeeEntity 对齐；其他系统接入时在接口配置表里做参数名映射。
 */
@Data
public class SubSystemEmployeeDTO {

    /** 工号（匹配键） */
    private String userCode;
    /** 姓名 */
    private String userName;
    /** 车间编码 */
    private String workshopCode;
    /** 车间名称（查询回显用） */
    private String workshopName;
    /** 班组编码 */
    private String teamCode;
    /** 班组名称（查询回显用） */
    private String teamName;
    /** 域账号 */
    private String domainName;
    /** ERP 号 */
    private String erpNo;
    /** 刷卡卡号 */
    private String cardNo;
    /** 在职状态：1 在职 0 离职 */
    private String onDuty;
    /**
     * Camstar 角色 ID，逗号分隔（ROLEDEF.roleId，不是角色名）。
     * 含内置 Login：001bda8000000001
     */
    private String userRoleIdStr;
    /**
     * 为 true 时 Camstar 只改人员角色，不回写菜单、主页、工位、班组、密码。
     */
    private Boolean roleOnly;

}
