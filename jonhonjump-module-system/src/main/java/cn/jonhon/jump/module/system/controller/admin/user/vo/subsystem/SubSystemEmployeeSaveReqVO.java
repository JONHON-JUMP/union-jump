package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 子系统人员创建/更新 Request VO")
@Data
public class SubSystemEmployeeSaveReqVO {

    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "外部系统不能为空")
    private Long subSystemId;

    @Schema(description = "工号", requiredMode = Schema.RequiredMode.REQUIRED, example = "00078")
    @NotBlank(message = "工号不能为空")
    @Size(max = 64, message = "工号长度不能超过 64 个字符")
    private String userCode;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过 100 个字符")
    private String userName;

    @Schema(description = "车间编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "4200")
    @NotBlank(message = "车间不能为空")
    private String workshopCode;

    @Schema(description = "班组编码（可选）", example = "001b8480000006c5")
    private String teamCode;

    @Schema(description = "域账号", example = "zhangsan")
    private String domainName;

    @Schema(description = "ERP 号", example = "3508043")
    private String erpNo;

    @Schema(description = "刷卡卡号", example = "00558")
    private String cardNo;

}
