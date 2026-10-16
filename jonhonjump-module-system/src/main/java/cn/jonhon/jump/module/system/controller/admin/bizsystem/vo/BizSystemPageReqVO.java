package cn.jonhon.jump.module.system.controller.admin.bizsystem.vo;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 系统明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class BizSystemPageReqVO extends PageParam {

    @Schema(description = "系统名称")
    private String systemName;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "端口")
    private Integer port;

    @Schema(description = "框架")
    private String framework;

}
