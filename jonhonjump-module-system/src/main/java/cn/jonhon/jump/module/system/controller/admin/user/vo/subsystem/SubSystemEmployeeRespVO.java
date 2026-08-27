package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 子系统人员 Response VO")
@Data
public class SubSystemEmployeeRespVO {

    @Schema(description = "工号")
    private String userCode;
    @Schema(description = "姓名")
    private String userName;
    @Schema(description = "车间编码")
    private String workshopCode;
    @Schema(description = "车间名称")
    private String workshopName;
    @Schema(description = "班组编码")
    private String teamCode;
    @Schema(description = "班组名称")
    private String teamName;
    @Schema(description = "域账号")
    private String domainName;
    @Schema(description = "ERP 号")
    private String erpNo;
    @Schema(description = "刷卡卡号")
    private String cardNo;
    @Schema(description = "在职状态：1 在职 0 离职")
    private String onDuty;

}
