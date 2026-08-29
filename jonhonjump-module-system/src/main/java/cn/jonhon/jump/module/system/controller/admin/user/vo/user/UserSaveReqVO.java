package cn.jonhon.jump.module.system.controller.admin.user.vo.user;

import cn.hutool.core.util.ObjectUtil;
import cn.jonhon.jump.framework.common.validation.Mobile;
import cn.jonhon.jump.module.system.framework.operatelog.core.DeptParseFunction;
import cn.jonhon.jump.module.system.framework.operatelog.core.PostParseFunction;
import cn.jonhon.jump.module.system.framework.operatelog.core.SexParseFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;
import java.util.List;
import java.util.Set;

@Schema(description = "管理后台 - 用户创建/修改 Request VO")
@Data
public class UserSaveReqVO {

    @Schema(description = "用户编号", example = "1024")
    private Long id;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "jonhonjump")
    @NotBlank(message = "用户账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "用户账号由 数字、字母 组成")
    @Size(min = 4, max = 30, message = "用户账号长度为 4-30 个字符")
    @DiffLogField(name = "用户账号")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @Size(max = 30, message = "用户昵称长度不能超过30个字符")
    @DiffLogField(name = "用户昵称")
    private String nickname;

    @Schema(description = "备注", example = "我是一个用户")
    @DiffLogField(name = "备注")
    private String remark;

    @Schema(description = "部门编号", example = "我是一个用户")
    @DiffLogField(name = "部门", function = DeptParseFunction.NAME)
    private Long deptId;

    @Schema(description = "岗位编号数组", example = "1")
    @DiffLogField(name = "岗位", function = PostParseFunction.NAME)
    private Set<Long> postIds;

    @Schema(description = "用户邮箱", example = "jonhonjump@iocoder.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    @DiffLogField(name = "用户邮箱")
    private String email;

    @Schema(description = "手机号码", example = "15601691300")
    @Mobile
    @DiffLogField(name = "手机号码")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    @DiffLogField(name = "用户性别", function = SexParseFunction.NAME)
    private Integer sex;

    @Schema(description = "用户头像", example = "https://www.iocoder.cn/xxx.png")
    @DiffLogField(name = "用户头像")
    private String avatar;

    @Schema(description = "工号", example = "E001234")
    @Size(max = 20, message = "工号长度不能超过 20 个字符")
    @DiffLogField(name = "工号")
    private String employeeNo;

    @Schema(description = "刷卡卡号", example = "1234567890")
    @Size(max = 20, message = "刷卡卡号长度不能超过 20 个字符")
    @DiffLogField(name = "刷卡卡号")
    private String cardNo;

    @Schema(description = "人员分类（01正式员工 02外包派遣）", example = "01")
    @Size(max = 10, message = "人员分类长度不能超过 10 个字符")
    @DiffLogField(name = "人员分类")
    private String personClass;

    @Schema(description = "人员转正状态（01试用期 02转正）", example = "02")
    @Size(max = 20, message = "转正状态长度不能超过 20 个字符")
    @DiffLogField(name = "转正状态")
    private String regularizationStatus;

    @Schema(description = "在职状态（01在职 02停职 03退二线 04已退休）", example = "01")
    @Size(max = 20, message = "在职状态长度不能超过 20 个字符")
    @DiffLogField(name = "在职状态")
    private String employmentStatus;

    @Schema(description = "在岗状态（01在岗 02离岗 03请假 04出差）", example = "01")
    @Size(max = 20, message = "在岗状态长度不能超过 20 个字符")
    @DiffLogField(name = "在岗状态")
    private String dutyStatus;

    @Schema(description = "ERP 账号数组", example = "[\"erp001\",\"erp002\"]")
    @DiffLogField(name = "ERP账号")
    private Set<String> erpNos;

    @Schema(description = "域账号", example = "zhangsan")
    @Size(max = 32, message = "域账号长度不能超过 32 个字符")
    @DiffLogField(name = "域账号")
    private String domainNo;

    // ========== 仅【创建】时，需要传递的字段 ==========

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @Schema(description = "登记子系统编号数组（仅新增时生效；本地花名册登记，不调外部接口）", example = "[1,2]")
    private List<Long> subSystemIds;

    @AssertTrue(message = "密码不能为空")
    @JsonIgnore
    public boolean isPasswordValid() {
        return id != null // 修改时，不需要传递
                || (ObjectUtil.isAllNotEmpty(password)); // 新增时，必须都传递 password
    }

}
