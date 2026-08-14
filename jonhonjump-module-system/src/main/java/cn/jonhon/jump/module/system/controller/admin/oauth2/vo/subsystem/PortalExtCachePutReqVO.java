package cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "子系统扩展缓存写入 Body")
@Data
public class PortalExtCachePutReqVO {

    @Schema(description = "缓存值（任意 JSON）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Object value;

    @Schema(description = "TTL 秒，默认 3600")
    private Long ttlSeconds;

}
