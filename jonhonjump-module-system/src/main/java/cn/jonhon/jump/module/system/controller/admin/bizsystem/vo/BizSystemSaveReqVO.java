package cn.jonhon.jump.module.system.controller.admin.bizsystem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 系统明细新增/修改 Request VO")
@Data
public class BizSystemSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "序号")
    @NotNull(message = "序号不能为空")
    private Integer sortNo;

    @Schema(description = "IP")
    @NotBlank(message = "IP不能为空")
    @Size(max = 64, message = "IP长度不能超过 64")
    private String ip;

    @Schema(description = "端口")
    @NotNull(message = "端口不能为空")
    private Integer port;

    @Schema(description = "系统名称")
    @NotBlank(message = "系统名称不能为空")
    @Size(max = 100, message = "系统名称长度不能超过 100")
    private String systemName;

    @Schema(description = "后端明细")
    @Size(max = 255, message = "后端明细长度不能超过 255")
    private String backendDetail;

    @Schema(description = "系统类型")
    @Size(max = 32, message = "系统类型长度不能超过 32")
    private String systemType;

    @Schema(description = "框架")
    @Size(max = 32, message = "框架长度不能超过 32")
    private String framework;

    @Schema(description = "Nginx 映射端口")
    private Integer nginxPort;

}
