package cn.jonhon.jump.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 主系统进程启动标识 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthBootIdRespVO {

    @Schema(description = "进程启动标识；主系统重新发布后会变化，供门户探测发版并自动重连子系统")
    private String bootId;

}
