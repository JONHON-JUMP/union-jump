package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 页面调用（绑定行）")
@Data
public class LeafApiBindingSceneRespVO {

    @Schema(description = "页面调用编码", example = "account_sync")
    private String scene;

    @Schema(description = "页面调用名称", example = "同步域账号/刷卡卡号")
    private String label;

    @Schema(description = "应对应的接口用途", example = "update")
    private String purpose;

    @Schema(description = "接口用途名称", example = "修改人员")
    private String purposeLabel;

}
