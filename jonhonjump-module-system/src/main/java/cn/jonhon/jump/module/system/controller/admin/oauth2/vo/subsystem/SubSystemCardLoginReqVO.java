package cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 子系统刷卡校验 Request VO")
@Data
public class SubSystemCardLoginReqVO {

    @Schema(description = "OAuth2 客户端编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "cabinet")
    @NotBlank(message = "clientId 不能为空")
    private String clientId;

    @Schema(description = "OAuth2 客户端密钥（可用 HTTP Basic 代替）", example = "Cabinet@2026")
    private String clientSecret;

    @Schema(description = "子系统用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    @NotBlank(message = "username 不能为空")
    private String username;

}
