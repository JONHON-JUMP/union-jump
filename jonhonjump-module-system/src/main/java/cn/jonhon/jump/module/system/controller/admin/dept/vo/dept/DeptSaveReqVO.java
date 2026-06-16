package cn.jonhon.jump.module.system.controller.admin.dept.vo.dept;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 部门创建/修改 Request VO")
@Data
public class DeptSaveReqVO {

    @Schema(description = "部门编号", example = "1024")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋道")
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 256, message = "部门名称长度不能超过 256 个字符")
    private String name;

    @Schema(description = "父部门 ID", example = "1024")
    private Long parentId;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "显示顺序不能为空")
    private Integer sort;

    @Schema(description = "负责人的用户编号", example = "2048")
    private Long leaderUserId;

    @Schema(description = "联系电话", example = "15601691000")
    @Size(max = 11, message = "联系电话长度不能超过11个字符")
    private String phone;

    @Schema(description = "邮箱", example = "jonhonjump@iocoder.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    private String email;

    @Schema(description = "状态,见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "修改状态必须是 {value}")
    private Integer status;

    @Schema(description = "组织编码", example = "GS-1")
    @Size(max = 64, message = "组织编码长度不能超过 64 个字符")
    private String organizationCode;

    @Schema(description = "英文组织名称", example = "JONHON")
    @Size(max = 256, message = "英文组织名称长度不能超过 256 个字符")
    private String engName;

    @Schema(description = "描述说明")
    private String description;

    @Schema(description = "地址")
    @Size(max = 500, message = "地址长度不能超过 500 个字符")
    private String address;

    @Schema(description = "ERP 系统中的编码")
    @Size(max = 255, message = "ERP 编码长度不能超过 255 个字符")
    private String erpCode;

    @Schema(description = "PDM 系统中的编码")
    @Size(max = 255, message = "PDM 编码长度不能超过 255 个字符")
    private String pdmCode;

}
