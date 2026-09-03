package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "管理后台 - 外部系统角色 Response VO")
@Data
public class SubSystemRoleRespVO {

    @Schema(description = "角色编号")
    private Long id;

    @Schema(description = "外部系统 ID")
    private Long subSystemId;

    @Schema(description = "客户端编号")
    private String clientId;

    @Schema(description = "外部系统名称")
    private String clientName;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色标识")
    private String code;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "角色类型")
    private Integer type;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "角色接口注册状态（0未注册 1已注册）")
    private String roleRegistered;

    @Schema(description = "数据范围")
    private Integer dataScope;

    @Schema(description = "数据范围（指定部门）")
    private Set<Long> dataScopeDeptIds;

    @Schema(description = "部门树选择项是否关联显示")
    private Integer deptCheckStrictly;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
