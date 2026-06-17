package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

    @Schema(description = "主数据用户账号")
    private String username;

    @Schema(description = "主数据用户昵称")
    private String nickname;

    @Schema(description = "工号")
    private String employeeNo;

    @Schema(description = "刷卡卡号")
    private String cardNo;

    @Schema(description = "ERP 账号数组")
    private Set<String> erpNos;

    @Schema(description = "域账号")
    private String domainNo;

    @Schema(description = "车间编号")
    private String workshopId;

    @Schema(description = "班组编号")
    private String teamId;

    @Schema(description = "班组名称")
    private String teamName;

    @Schema(description = "主页面菜单 ID")
    private Long homeMenuId;

    @Schema(description = "主页面名称")
    private String homeMenuName;

    @Schema(description = "状态（0正常 1禁用）")
    private String status;

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
