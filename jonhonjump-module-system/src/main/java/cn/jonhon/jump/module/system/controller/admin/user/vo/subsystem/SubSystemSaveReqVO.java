package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 外部系统创建/更新 Request VO")
@Data
public class SubSystemSaveReqVO {

    @Schema(description = "系统编号")
    private Long id;

    @Schema(description = "OAuth2 客户端编号（system_oauth2_client.id）", requiredMode = Schema.RequiredMode.REQUIRED, example = "41")
    @NotNull(message = "OAuth2 客户端不能为空")
    private Long oauth2ClientId;

    @Schema(description = "系统名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "SCADA 生产监控系统")
    @NotBlank(message = "系统名称不能为空")
    @Size(max = 100, message = "系统名称长度不能超过 100 个字符")
    private String systemName;

    @Schema(description = "系统描述", example = "车间 SCADA 系统")
    @Size(max = 255, message = "系统描述长度不能超过 255 个字符")
    private String description;

    @Schema(description = "系统访问地址", example = "http://10.1.19.34:28080")
    @Size(max = 255, message = "系统访问地址长度不能超过 255 个字符")
    private String systemUrl;

    @Schema(description = "系统图标", example = "https://www.example.com/icon.png")
    @Size(max = 255, message = "系统图标长度不能超过 255 个字符")
    private String systemIcon;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
