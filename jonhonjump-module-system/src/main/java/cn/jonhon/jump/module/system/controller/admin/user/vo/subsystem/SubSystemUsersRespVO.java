package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 人员子系统关系 Response VO")
@Data
public class SubSystemUsersRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "主数据人员 ID")
    private Long mainUserId;

    @Schema(description = "外部系统 ID")
    private Long subSystemId;

    @Schema(description = "OAuth2 客户端编号")
    private String clientId;

    @Schema(description = "外部系统名称")
    private String clientName;

    @Schema(description = "子系统用户名")
    private String username;

    @Schema(description = "用户姓名")
    private String nickname;

    @Schema(description = "车间编号")
    private String workshopId;

    @Schema(description = "班组编码（对应班组 teamCode，非主键 id）")
    private String teamId;

    @Schema(description = "班组名称")
    private String teamName;

    @Schema(description = "主页面菜单 ID")
    private Long homeMenuId;

    @Schema(description = "主页面名称")
    private String homeMenuName;

    @Schema(description = "状态（0正常 1禁用）")
    private String status;

    @Schema(description = "人员接口注册状态（0未注册 1已注册）")
    private String employeeRegistered;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "角色名称，逗号分隔")
    private String roleNames;

    @Schema(description = "角色编号列表")
    private List<Long> roleIds;

    @Schema(description = "岗位名称，逗号分隔")
    private String postNames;

    @Schema(description = "岗位编号列表")
    private List<Long> postIds;

}
