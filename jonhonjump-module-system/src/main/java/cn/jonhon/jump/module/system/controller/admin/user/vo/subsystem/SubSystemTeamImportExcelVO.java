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
public class SubSystemTeamImportExcelVO {

    @ExcelProperty("班组编码")
    private String teamCode;

    @ExcelProperty("班组名称")
    private String teamName;

    @ExcelProperty("班组描述")
    private String description;

    @ExcelProperty("班组长主用户UID")
    private String leaderUserUid;

    @ExcelProperty("班组长登录账号")
    private String leaderUsername;

}
