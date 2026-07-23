package cn.jonhon.jump.module.system.controller.admin.user.vo.user;

import cn.jonhon.jump.framework.excel.core.annotations.DictFormat;
import cn.jonhon.jump.framework.excel.core.convert.DictConvert;
import cn.jonhon.jump.module.system.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "管理后台 - 用户信息 Response VO")
@Data
@ExcelIgnoreUnannotated
public class UserRespVO{

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("用户编号")
    private Long id;

    @Schema(description = "跨系统唯一用户标识（U+年月日时分秒+三位流水）", example = "U20260720170405001")
    @ExcelProperty("用户UID")
    private String userUid;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "jonhonjump")
    @ExcelProperty("用户名称")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @ExcelProperty("用户昵称")
    private String nickname;

    @Schema(description = "备注", example = "我是一个用户")
    private String remark;

    @Schema(description = "部门ID", example = "我是一个用户")
    private Long deptId;
    @Schema(description = "部门名称", example = "IT 部")
    @ExcelProperty("部门名称")
    private String deptName;

    @Schema(description = "岗位编号数组", example = "1")
    private Set<Long> postIds;

    @Schema(description = "用户邮箱", example = "jonhonjump@iocoder.cn")
    @ExcelProperty("用户邮箱")
    private String email;

    @Schema(description = "手机号码", example = "15601691300")
    @ExcelProperty("手机号码")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    @ExcelProperty(value = "用户性别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.USER_SEX)
    private Integer sex;

    @Schema(description = "用户头像", example = "https://www.iocoder.cn/xxx.png")
    private String avatar;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "帐号状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "最后登录 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "192.168.1.1")
    @ExcelProperty("最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    @ExcelProperty("最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "工号", example = "E001234")
    @ExcelProperty("工号")
    private String employeeNo;

    @Schema(description = "刷卡卡号", example = "1234567890")
    private String cardNo;

    @Schema(description = "人员分类（01正式员工 02外包派遣）", example = "01")
    private String personClass;

    @Schema(description = "人员转正状态（01试用期 02转正）", example = "02")
    private String regularizationStatus;

    @Schema(description = "在职状态（01在职 02停职 03退二线 04已退休）", example = "01")
    private String employmentStatus;

    @Schema(description = "在岗状态（01在岗 02离岗 03请假 04出差）", example = "01")
    private String dutyStatus;

    @Schema(description = "ERP 账号数组")
    private Set<String> erpNos;

    @Schema(description = "域账号", example = "zhangsan")
    private String domainNo;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

    @Schema(description = "子系统关系数量", example = "2")
    private Long subSystemCount;

}
