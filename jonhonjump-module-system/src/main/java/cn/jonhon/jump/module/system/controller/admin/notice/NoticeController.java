package cn.jonhon.jump.module.system.controller.admin.notice;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.notice.vo.NoticePageReqVO;
import cn.jonhon.jump.module.system.controller.admin.notice.vo.NoticeRespVO;
import cn.jonhon.jump.module.system.controller.admin.notice.vo.NoticeSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.notice.NoticeDO;
import cn.jonhon.jump.module.system.enums.notice.NoticeStatusEnum;
import cn.jonhon.jump.module.system.service.notice.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 通知公告")
@RestController
@RequestMapping("/system/notice")
@Validated
public class NoticeController {

    @Resource
    private NoticeService noticeService;

    @PostMapping("/create")
    @Operation(summary = "创建通知公告")
    @PreAuthorize("@ss.hasPermission('system:notice:create')")
    public CommonResult<Long> createNotice(@Valid @RequestBody NoticeSaveReqVO createReqVO) {
        return success(noticeService.createNotice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改通知公告")
    @PreAuthorize("@ss.hasPermission('system:notice:update')")
    public CommonResult<Boolean> updateNotice(@Valid @RequestBody NoticeSaveReqVO updateReqVO) {
        noticeService.updateNotice(updateReqVO);
        return success(true);
    }

    @PutMapping("/publish")
    @Operation(summary = "发布通知公告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notice:update')")
    public CommonResult<Boolean> publishNotice(@RequestParam("id") Long id) {
        noticeService.publishNotice(id);
        return success(true);
    }

    @PutMapping("/revoke")
    @Operation(summary = "撤回通知公告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notice:update')")
    public CommonResult<Boolean> revokeNotice(@RequestParam("id") Long id) {
        noticeService.revokeNotice(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除通知公告（业务软删除，状态=已删除）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notice:delete')")
    public CommonResult<Boolean> deleteNotice(@RequestParam("id") Long id) {
        noticeService.deleteNotice(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除通知公告（业务软删除）")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:notice:delete')")
    public CommonResult<Boolean> deleteNoticeList(@RequestParam("ids") List<Long> ids) {
        noticeService.deleteNoticeList(ids);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取通知公告列表（管理员，可按草稿/已发布/已删除筛选）")
    @PreAuthorize("@ss.hasPermission('system:notice:query')")
    public CommonResult<PageResult<NoticeRespVO>> getNoticePage(@Validated NoticePageReqVO pageReqVO) {
        PageResult<NoticeDO> pageResult = noticeService.getNoticePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, NoticeRespVO.class));
    }

    @GetMapping("/workbench-page")
    @Operation(summary = "获得工作台通知分页", description = "普通用户仅可见已发布通知")
    public CommonResult<PageResult<NoticeRespVO>> getNoticeWorkbenchPage(@Validated NoticePageReqVO pageReqVO) {
        pageReqVO.setStatus(NoticeStatusEnum.PUBLISHED.getStatus());
        PageResult<NoticeDO> pageResult = noticeService.getNoticePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, NoticeRespVO.class));
    }

    @GetMapping("/app-get")
    @Operation(summary = "获得通知详情", description = "供工作台、我的通知查看已发布通知")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<NoticeRespVO> getAppNotice(@RequestParam("id") Long id) {
        NoticeDO notice = noticeService.getPublishedNotice(id);
        return success(BeanUtils.toBean(notice, NoticeRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得通知公告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notice:query')")
    public CommonResult<NoticeRespVO> getNotice(@RequestParam("id") Long id) {
        NoticeDO notice = noticeService.getNotice(id);
        return success(BeanUtils.toBean(notice, NoticeRespVO.class));
    }

}
