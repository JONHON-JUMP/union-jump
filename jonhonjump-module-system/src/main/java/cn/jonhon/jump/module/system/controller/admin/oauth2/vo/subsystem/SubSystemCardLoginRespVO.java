package cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 子系统刷卡校验 Response VO")
@Data
public class SubSystemCardLoginRespVO {

    @Schema(description = "外部系统编号", example = "2")
    private Long subSystemId;

    @Schema(description = "OAuth2 客户端编号", example = "cabinet")
    private String clientId;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "用户姓名", example = "张三")
    private String nickname;

    @Schema(description = "状态（0正常 1停用）", example = "0")
    private String status;

    @Schema(description = "车间编号", example = "WS01")
    private String workshopId;

    @Schema(description = "班组编码", example = "T01")
    private String teamId;

    @Schema(description = "关联的主系统用户编号（可空）", example = "1")
    private Long mainUserId;

    @Schema(description = "角色标识列表", example = "[\"common\"]")
    private List<String> roleCodes;

}
