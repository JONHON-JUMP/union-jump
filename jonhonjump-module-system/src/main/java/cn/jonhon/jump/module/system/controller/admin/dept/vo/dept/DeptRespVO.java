package cn.jonhon.jump.module.system.controller.admin.dept.vo.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 部门信息 Response VO")
@Data
public class DeptRespVO {

    @Schema(description = "部门编号", example = "1024")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋道")
    private String name;

    @Schema(description = "父部门 ID", example = "1024")
    private Long parentId;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Integer sort;

    @Schema(description = "负责人的用户编号", example = "2048")
    private Long leaderUserId;

    @Schema(description = "联系电话", example = "15601691000")
    private String phone;

    @Schema(description = "邮箱", example = "jonhonjump@iocoder.cn")
    private String email;

    @Schema(description = "状态,见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "组织编码", example = "GS-1")
    private String organizationCode;

    @Schema(description = "英文组织名称", example = "JONHON")
    private String engName;

    @Schema(description = "描述说明")
    private String description;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "ERP 系统中的编码")
    private String erpCode;

    @Schema(description = "PDM 系统中的编码")
    private String pdmCode;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

}
