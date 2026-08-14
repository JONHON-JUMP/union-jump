package cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "子系统扩展缓存续期 Body")
@Data
public class PortalExtCacheExpireReqVO {

    @Schema(description = "TTL 秒", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long ttlSeconds;

}
