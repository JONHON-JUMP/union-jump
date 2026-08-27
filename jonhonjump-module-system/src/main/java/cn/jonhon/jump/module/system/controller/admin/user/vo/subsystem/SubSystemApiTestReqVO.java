package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 子系统人员接口本页测试 Request VO")
@Data
public class SubSystemApiTestReqVO {

    @Schema(description = "配置编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请先保存配置")
    private Long id;

    @Schema(description = "接口：query / create / update / delete", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择接口")
    private String apiKey;

    @Schema(description = "请求 JSON 体")
    private String requestBody;

}
