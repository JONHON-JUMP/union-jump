package cn.jonhon.jump.module.system.controller.admin.user;

import cn.hutool.core.collection.CollUtil;
import cn.jonhon.jump.framework.apilog.core.annotation.ApiAccessLog;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageParam;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.excel.core.util.ExcelUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.user.*;
import cn.jonhon.jump.module.system.convert.user.UserConvert;
import cn.jonhon.jump.module.system.dal.dataobject.dept.DeptDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.service.dept.DeptService;
import cn.jonhon.jump.module.system.service.user.AdminUserService;
import cn.jonhon.jump.module.system.service.user.SubSystemUsersService;
import cn.jonhon.jump.module.system.service.user.UserBizImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cn.jonhon.jump.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - 用户")
@RestController
@RequestMapping("/system/user")
@Validated
public class UserController {

    @Resource
    private AdminUserService userService;
    @Resource
    private DeptService deptService;
    @Resource
    private SubSystemUsersService subSystemUsersService;
    @Resource
    private UserBizImportService userBizImportService;

    @PostMapping("/create")
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> createUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        Long id = userService.createUser(reqVO);
        return success(id);
    }

    @PutMapping("update")
    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        userService.updateUser(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @Operation(summary = "批量删除用户")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUserList(@RequestParam("ids") List<Long> ids) {
        userService.deleteUserList(ids);
        return success(true);
    }

    @PutMapping("/update-password")
    @Operation(summary = "重置用户密码")
    @PreAuthorize("@ss.hasPermission('system:user:update-password')")
    public CommonResult<Boolean> updateUserPassword(@Valid @RequestBody UserUpdatePasswordReqVO reqVO) {
        userService.updateUserPassword(reqVO.getId(), reqVO.getPassword());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改用户状态")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUserStatus(@Valid @RequestBody UserUpdateStatusReqVO reqVO) {
        userService.updateUserStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得用户分页列表")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqVO pageReqVO) {
        // 获得用户分页列表
        PageResult<AdminUserDO> pageResult = userService.getUserPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(new PageResult<>(pageResult.getTotal()));
        }
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(pageResult.getList(), AdminUserDO::getDeptId));
        Map<Long, Long> subSystemCountMap = subSystemUsersService.getCountMapByMainUserIds(
                convertList(pageResult.getList(), AdminUserDO::getId));
        List<UserRespVO> list = UserConvert.INSTANCE.convertList(pageResult.getList(), deptMap);
        list.forEach(user -> user.setSubSystemCount(subSystemCountMap.getOrDefault(user.getId(), 0L)));
        return success(new PageResult<>(list, pageResult.getTotal()));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取用户精简信息列表", description = "只包含被开启的用户，主要用于前端的下拉选项")
    public CommonResult<List<UserSimpleRespVO>> getSimpleUserList() {
        List<AdminUserDO> list = userService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus());
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        return success(UserConvert.INSTANCE.convertSimpleList(list, deptMap));
    }

    @GetMapping("/get")
    @Operation(summary = "获得用户详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        AdminUserDO user = userService.getUser(id);
        if (user == null) {
            return success(null);
        }
        // 拼接数据
        DeptDO dept = deptService.getDept(user.getDeptId());
        return success(UserConvert.INSTANCE.convert(user, dept));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出用户")
    @PreAuthorize("@ss.hasPermission('system:user:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUserList(@Validated UserPageReqVO exportReqVO,
                               HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AdminUserDO> list = userService.getUserPage(exportReqVO).getList();
        // 输出 Excel
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        ExcelUtils.write(response, "用户数据.xls", "数据", UserRespVO.class,
                UserConvert.INSTANCE.convertList(list, deptMap));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入用户模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        // 第 1 行：一个人只挂一个车间。第 2、3 行：同一个人挂两个车间，用户名相同，业务用户名不同。
        List<UserBizImportExcelVO> list = Arrays.asList(
                UserBizImportExcelVO.builder().username("77176").nickname("张三")
                        .employeeNo("77176").domainNo("zhangsan")
                        .workshopId("4303").bizUsername("771764303")
                        .roleCodes("4303_装配工").cardNo("771764303").erpNo("430300001")
                        .remark("示例：一个人只挂一个车间。正式导入前删掉示例行。").build(),
                UserBizImportExcelVO.builder().username("HY122").nickname("李行行-WB")
                        .employeeNo("HY122").domainNo("lihang")
                        .workshopId("4306").bizUsername("HY1224306")
                        .roleCodes("4306_装配工").cardNo("HY1224306").erpNo("430613432")
                        .remark("示例：同一个人挂多个车间时，用户名和用户姓名保持一致，每个车间一行。").build(),
                UserBizImportExcelVO.builder().username("HY122").nickname("李行行-WB")
                        .employeeNo("HY122").domainNo("lihang")
                        .workshopId("4309").bizUsername("HY1224309")
                        .roleCodes("4309_装配工").cardNo("HY1224309").erpNo("430913432")
                        .remark("示例：与上一行是同一个人，车间和业务用户名不同。").build()
        );
        ExcelUtils.write(response, "用户导入模板.xls", "用户列表", UserBizImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入用户和业务账号")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "deptId", description = "新建主用户的部门", required = true),
            @Parameter(name = "syncToExternal", description = "是否把人员和角色推送到业务系统", example = "false")
    })
    @PreAuthorize("@ss.hasPermission('system:user:import')")
    public CommonResult<UserImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam("deptId") Long deptId,
                                                      @RequestParam(value = "syncToExternal", required = false, defaultValue = "false") Boolean syncToExternal) throws Exception {
        List<UserBizImportExcelVO> list = ExcelUtils.read(file, UserBizImportExcelVO.class);
        return success(userBizImportService.importUsers(deptId, Boolean.TRUE.equals(syncToExternal), list));
    }

}
