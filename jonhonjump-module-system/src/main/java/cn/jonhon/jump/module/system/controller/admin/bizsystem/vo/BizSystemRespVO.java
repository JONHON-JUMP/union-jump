package cn.jonhon.jump.module.system.controller.admin.bizsystem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 系统明细 Response VO")
@Data
public class BizSystemRespVO {

    private Long id;
    private Integer sortNo;
    private String ip;
    private Integer port;
    private String systemName;
    private String backendDetail;
    private String systemType;
    private String framework;
    private Integer nginxPort;
    private LocalDateTime createTime;

}
