package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 同步域账号/卡号/ERP号到业务系统 Request VO")
@Data
public class SubSystemUserAccountSyncReqVO {

    @Schema(description = "挂接记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "请选择已挂接的业务系统")
    private Long id;

    @Schema(description = "接口系统。为空时按该叶子「同步域账号/刷卡卡号」的绑定解析", example = "9")
    private Long apiSubSystemId;

    @Schema(description = "本次同步的 ERP 账号。不传则不同步 ERP，只同步域账号和刷卡卡号", example = "10086")
    private String erpNo;

}
