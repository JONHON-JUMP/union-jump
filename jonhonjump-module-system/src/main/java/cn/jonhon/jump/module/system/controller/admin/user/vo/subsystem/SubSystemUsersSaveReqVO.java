package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 外部系统用户新增/修改 Request VO")
@Data
public class SubSystemUsersSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "外部系统不能为空")
    private Long subSystemId;

    @Schema(description = "主数据人员 ID（可选，挂接门户用户后可访问）", example = "1")
    private Long mainUserId;

    @Schema(description = "子系统登录用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64 个字符")
    private String username;

    @Schema(description = "用户姓名", example = "张三")
    @Size(max = 64, message = "用户姓名长度不能超过 64 个字符")
    private String nickname;

    @Schema(description = "车间编号")
    private String workshopId;

    @Schema(description = "班组编码（对应班组 teamCode，非主键 id）")
    private String teamId;

    @Schema(description = "主页面菜单 ID")
    private Long homeMenuId;

    @Schema(description = "状态（0正常 1禁用）", example = "0")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "角色编号列表")
    private List<Long> roleIds;

    @Schema(description = "岗位编号列表")
    private List<Long> postIds;

}
