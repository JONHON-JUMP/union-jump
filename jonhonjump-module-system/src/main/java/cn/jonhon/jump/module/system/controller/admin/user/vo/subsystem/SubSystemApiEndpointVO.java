package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 子系统接口配置 VO（一行一接口）")
@Data
public class SubSystemApiEndpointVO {

    @Schema(description = "主键编号（新增时可空）", example = "1")
    private Long id;

    @Schema(description = "用途：query/create/update/delete/team_combo/role_query/role_create/role_delete；空串=未指定用途", example = "create")
    private String purpose;

    @Schema(description = "前端分组：person=人员接口 / role=角色接口 / other=其他（保存时按 purpose 自动推导）", example = "person")
    private String groupCode;

    @Schema(description = "接口名称", example = "新增人员")
    private String name;

    @Schema(description = "完整地址或相对路径（相对时拼主表 base_url）", example = "http://192.168.240.125:8888/BasicData/Employee/addOrUpdateUser")
    private String url;

    @Schema(description = "HTTP 方法：GET/POST/...", example = "POST")
    private String method;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "是否携带系统会话 Cookie", example = "true")
    private Boolean withSession;

    @Schema(description = "组内排序", example = "10")
    private Integer sort;

}
