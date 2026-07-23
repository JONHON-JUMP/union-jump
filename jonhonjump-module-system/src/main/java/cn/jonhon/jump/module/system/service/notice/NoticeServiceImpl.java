package cn.jonhon.jump.module.system.service.notice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.notice.vo.NoticePageReqVO;
import cn.jonhon.jump.module.system.controller.admin.notice.vo.NoticeSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.dept.DeptDO;
import cn.jonhon.jump.module.system.dal.dataobject.notice.NoticeDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.mysql.notice.NoticeMapper;
import cn.jonhon.jump.module.system.enums.notice.NoticeStatusEnum;
import cn.jonhon.jump.module.system.service.dept.DeptService;
import cn.jonhon.jump.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.NOTICE_CANNOT_EDIT_DELETED;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.NOTICE_DELETE_FAIL;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.NOTICE_NOT_FOUND;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.NOTICE_PUBLISH_FAIL;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.NOTICE_REVOKE_FAIL;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.NOTICE_STATUS_INVALID;

/**
 * 通知公告 Service 实现类
 */
@Service
@Validated
public class NoticeServiceImpl implements NoticeService {

    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private DeptService deptService;

    @Override
    public Long createNotice(NoticeSaveReqVO createReqVO) {
        NoticeDO notice = BeanUtils.toBean(createReqVO, NoticeDO.class);
        Integer status = normalizeEditableStatus(createReqVO.getStatus());
        notice.setStatus(status);
        if (NoticeStatusEnum.isPublished(status)) {
            fillPublisherInfo(notice);
        }
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Override
    public void updateNotice(NoticeSaveReqVO updateReqVO) {
        NoticeDO existing = validateNoticeExists(updateReqVO.getId());
        if (NoticeStatusEnum.isDeleted(existing.getStatus())) {
            throw exception(NOTICE_CANNOT_EDIT_DELETED);
        }
        Integer status = normalizeEditableStatus(updateReqVO.getStatus());
        NoticeDO updateObj = BeanUtils.toBean(updateReqVO, NoticeDO.class);
        updateObj.setStatus(status);
        if (NoticeStatusEnum.isPublished(status)) {
            fillPublisherInfo(updateObj);
        }
        noticeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        NoticeDO existing = validateNoticeExists(id);
        if (NoticeStatusEnum.isDeleted(existing.getStatus())) {
            return;
        }
        int rows = noticeMapper.update(null, new LambdaUpdateWrapper<NoticeDO>()
                .eq(NoticeDO::getId, id)
                .set(NoticeDO::getStatus, NoticeStatusEnum.DELETED.getStatus()));
        if (rows <= 0) {
            throw exception(NOTICE_DELETE_FAIL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNoticeList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteNotice(id);
        }
    }

    @Override
    public void publishNotice(Long id) {
        NoticeDO existing = validateNoticeExists(id);
        if (!NoticeStatusEnum.isDraft(existing.getStatus())) {
            throw exception(NOTICE_PUBLISH_FAIL);
        }
        NoticeDO update = new NoticeDO();
        update.setId(id);
        update.setStatus(NoticeStatusEnum.PUBLISHED.getStatus());
        fillPublisherInfo(update);
        noticeMapper.updateById(update);
    }

    @Override
    public void revokeNotice(Long id) {
        NoticeDO existing = validateNoticeExists(id);
        if (!NoticeStatusEnum.isPublished(existing.getStatus())) {
            throw exception(NOTICE_REVOKE_FAIL);
        }
        noticeMapper.update(null, new LambdaUpdateWrapper<NoticeDO>()
                .eq(NoticeDO::getId, id)
                .set(NoticeDO::getStatus, NoticeStatusEnum.DRAFT.getStatus()));
    }

    @Override
    public PageResult<NoticeDO> getNoticePage(NoticePageReqVO reqVO) {
        PageResult<NoticeDO> pageResult = noticeMapper.selectPage(reqVO);
        if (pageResult.getList() != null) {
            pageResult.getList().forEach(this::fillPublisherNameIfAbsent);
        }
        return pageResult;
    }

    @Override
    public NoticeDO getNotice(Long id) {
        NoticeDO notice = noticeMapper.selectById(id);
        fillPublisherNameIfAbsent(notice);
        return notice;
    }

    @Override
    public NoticeDO getPublishedNotice(Long id) {
        NoticeDO notice = getNotice(id);
        if (notice == null || !NoticeStatusEnum.isPublished(notice.getStatus())) {
            throw exception(NOTICE_NOT_FOUND);
        }
        return notice;
    }

    @VisibleForTesting
    public NoticeDO validateNoticeExists(Long id) {
        if (id == null) {
            throw exception(NOTICE_NOT_FOUND);
        }
        NoticeDO notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw exception(NOTICE_NOT_FOUND);
        }
        return notice;
    }

    /**
     * 表单只允许草稿/已发布，已删除只能走删除接口
     */
    private Integer normalizeEditableStatus(Integer status) {
        if (status == null) {
            return NoticeStatusEnum.DRAFT.getStatus();
        }
        if (NoticeStatusEnum.isDraft(status) || NoticeStatusEnum.isPublished(status)) {
            return status;
        }
        throw exception(NOTICE_STATUS_INVALID);
    }

    private void fillPublisherInfo(NoticeDO notice) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return;
        }
        AdminUserDO user = adminUserService.getUser(userId);
        if (user == null) {
            return;
        }
        if (StrUtil.isBlank(notice.getPublisherName())) {
            notice.setPublisherName(user.getNickname());
        }
        if (StrUtil.isBlank(notice.getDeptName()) && user.getDeptId() != null) {
            DeptDO dept = deptService.getDept(user.getDeptId());
            if (dept != null) {
                notice.setDeptName(dept.getName());
            }
        }
    }

    private void fillPublisherNameIfAbsent(NoticeDO notice) {
        if (notice == null || StrUtil.isNotBlank(notice.getPublisherName())) {
            return;
        }
        String creator = notice.getCreator();
        if (StrUtil.isBlank(creator) || !NumberUtil.isLong(creator)) {
            return;
        }
        AdminUserDO user = adminUserService.getUser(Long.valueOf(creator));
        if (user != null && StrUtil.isNotBlank(user.getNickname())) {
            notice.setPublisherName(user.getNickname());
        }
    }

}
