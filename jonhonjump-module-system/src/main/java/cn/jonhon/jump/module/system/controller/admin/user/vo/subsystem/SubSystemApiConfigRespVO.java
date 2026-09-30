package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 子系统人员接口配置 Response VO")
@Data
public class SubSystemApiConfigRespVO {

    @Schema(description = "主键编号")
    private Long id;

    @Schema(description = "外部系统 ID")
    private Long subSystemId;

    @Schema(description = "外部系统名称")
    private String clientName;

    @Schema(description = "适配器类型：camstar / http")
    private String apiType;

    @Schema(description = "接口基地址")
    private String baseUrl;

    @Schema(description = "鉴权方式")
    private String authType;

    @Schema(description = "鉴权配置 JSON")
    private String authConfig;

    @Schema(description = "接口列表（一行一接口）")
    private List<SubSystemApiEndpointVO> endpoints;

    @Schema(description = "参数映射 JSON")
    private String paramMapping;

    @Schema(description = "响应映射 JSON")
    private String responseMapping;

    @Schema(description = "删除二次确认提示语")
    private String deleteTip;

    @Schema(description = "连接超时（毫秒）")
    private Long connectTimeoutMs;

    @Schema(description = "读取超时（毫秒）")
    private Long readTimeoutMs;

    @Schema(description = "状态：0启用 1停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
