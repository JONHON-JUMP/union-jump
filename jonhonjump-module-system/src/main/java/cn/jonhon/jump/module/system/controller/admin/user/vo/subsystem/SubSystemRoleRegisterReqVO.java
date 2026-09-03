package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 外部系统角色补注册 Request VO")
@Data
public class SubSystemRoleRegisterReqVO {

    @Schema(description = "接口目标系统 ID（调哪个系统的角色新增接口；与本地角色所属系统可不同）", example = "9")
    private Long apiSubSystemId;

    @Schema(description = "车间编号（角色名无「车间_」前缀时必填）", example = "4200")
    @Size(max = 32, message = "车间编号长度不能超过 32 个字符")
    private String workshopCode;

}
