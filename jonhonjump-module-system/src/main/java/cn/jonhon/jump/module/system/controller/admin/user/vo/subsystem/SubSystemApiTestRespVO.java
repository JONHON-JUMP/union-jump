package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 子系统人员接口本页测试 Response VO")
@Data
public class SubSystemApiTestRespVO {

    @Schema(description = "完整 URL")
    private String url;

    @Schema(description = "HTTP 方法")
    private String method;

    @Schema(description = "实际发出的请求体")
    private String requestBody;

    @Schema(description = "响应原文")
    private String responseBody;

    @Schema(description = "是否成功")
    private Boolean success;

}
