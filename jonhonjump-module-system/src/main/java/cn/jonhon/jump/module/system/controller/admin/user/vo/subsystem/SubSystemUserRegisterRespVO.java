package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 花名册人员接口注册结果 Response VO（逐项）")
@Data
@Accessors(chain = true)
public class SubSystemUserRegisterRespVO {

    @Schema(description = "花名册行编号", example = "1")
    private Long id;

    @Schema(description = "子系统用户名", example = "zhangsan")
    private String username;

    @Schema(description = "用户姓名", example = "张三")
    private String nickname;

    @Schema(description = "是否成功（已注册跳过视为成功）", example = "true")
    private Boolean success;

    @Schema(description = "结果说明（失败原因 / 已注册跳过）", example = "对方接口返回：工号已存在")
    private String message;

}
