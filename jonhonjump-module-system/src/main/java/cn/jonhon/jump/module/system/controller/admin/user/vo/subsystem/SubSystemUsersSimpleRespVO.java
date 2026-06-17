package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 外部系统用户精简 Response VO")
@Data
public class SubSystemUsersSimpleRespVO {

    @Schema(description = "外部系统用户编号")
    private Long id;

    @Schema(description = "用户昵称")
    private String nickname;

}
