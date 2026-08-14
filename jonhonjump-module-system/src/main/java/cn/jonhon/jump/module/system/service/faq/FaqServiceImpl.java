package cn.jonhon.jump.module.system.service.faq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.faq.vo.FaqPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.faq.vo.FaqSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.dept.DeptDO;
import cn.jonhon.jump.module.system.dal.dataobject.faq.FaqDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.mysql.faq.FaqMapper;
import cn.jonhon.jump.module.system.enums.faq.FaqStatusEnum;
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
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.FAQ_CANNOT_EDIT_DELETED;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.FAQ_DELETE_FAIL;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.FAQ_NOT_FOUND;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.FAQ_PUBLISH_FAIL;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.FAQ_REVOKE_FAIL;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.FAQ_STATUS_INVALID;

@Service
@Validated
public class FaqServiceImpl implements FaqService {

    @Resource
    private FaqMapper faqMapper;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private DeptService deptService;

    @Override
    public Long createFaq(FaqSaveReqVO createReqVO) {
        FaqDO faq = BeanUtils.toBean(createReqVO, FaqDO.class);
        if (faq.getSort() == null) {
            faq.setSort(0);
        }
        Integer status = normalizeEditableStatus(createReqVO.getStatus());
        faq.setStatus(status);
        if (FaqStatusEnum.isPublished(status)) {
            fillPublisherInfo(faq);
        }
        faqMapper.insert(faq);
        return faq.getId();
    }

    @Override
    public void updateFaq(FaqSaveReqVO updateReqVO) {
        FaqDO existing = validateFaqExists(updateReqVO.getId());
        if (FaqStatusEnum.isDeleted(existing.getStatus())) {
            throw exception(FAQ_CANNOT_EDIT_DELETED);
        }
        Integer status = normalizeEditableStatus(updateReqVO.getStatus());
        FaqDO updateObj = BeanUtils.toBean(updateReqVO, FaqDO.class);
        updateObj.setStatus(status);
        if (FaqStatusEnum.isPublished(status)) {
            fillPublisherInfo(updateObj);
        }
        faqMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFaq(Long id) {
        FaqDO existing = validateFaqExists(id);
        if (FaqStatusEnum.isDeleted(existing.getStatus())) {
            return;
        }
        int rows = faqMapper.update(null, new LambdaUpdateWrapper<FaqDO>()
                .eq(FaqDO::getId, id)
                .set(FaqDO::getStatus, FaqStatusEnum.DELETED.getStatus()));
        if (rows <= 0) {
            throw exception(FAQ_DELETE_FAIL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFaqList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteFaq(id);
        }
    }

    @Override
    public void publishFaq(Long id) {
        FaqDO existing = validateFaqExists(id);
        if (!FaqStatusEnum.isDraft(existing.getStatus())) {
            throw exception(FAQ_PUBLISH_FAIL);
        }
        FaqDO update = new FaqDO();
        update.setId(id);
        update.setStatus(FaqStatusEnum.PUBLISHED.getStatus());
        fillPublisherInfo(update);
        faqMapper.updateById(update);
    }

    @Override
    public void revokeFaq(Long id) {
        FaqDO existing = validateFaqExists(id);
        if (!FaqStatusEnum.isPublished(existing.getStatus())) {
            throw exception(FAQ_REVOKE_FAIL);
        }
        faqMapper.update(null, new LambdaUpdateWrapper<FaqDO>()
                .eq(FaqDO::getId, id)
                .set(FaqDO::getStatus, FaqStatusEnum.DRAFT.getStatus()));
    }

    @Override
    public PageResult<FaqDO> getFaqPage(FaqPageReqVO reqVO) {
        return faqMapper.selectPage(reqVO);
    }

    @Override
    public FaqDO getFaq(Long id) {
        return faqMapper.selectById(id);
    }

    @Override
    public FaqDO getPublishedFaq(Long id) {
        FaqDO faq = getFaq(id);
        if (faq == null || !FaqStatusEnum.isPublished(faq.getStatus())) {
            throw exception(FAQ_NOT_FOUND);
        }
        return faq;
    }

    @VisibleForTesting
    public FaqDO validateFaqExists(Long id) {
        if (id == null) {
            throw exception(FAQ_NOT_FOUND);
        }
        FaqDO faq = faqMapper.selectById(id);
        if (faq == null) {
            throw exception(FAQ_NOT_FOUND);
        }
        return faq;
    }

    private Integer normalizeEditableStatus(Integer status) {
        if (status == null) {
            return FaqStatusEnum.DRAFT.getStatus();
        }
        if (FaqStatusEnum.isDraft(status) || FaqStatusEnum.isPublished(status)) {
            return status;
        }
        throw exception(FAQ_STATUS_INVALID);
    }

    private void fillPublisherInfo(FaqDO faq) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return;
        }
        AdminUserDO user = adminUserService.getUser(userId);
        if (user == null) {
            return;
        }
        if (StrUtil.isBlank(faq.getPublisherName())) {
            faq.setPublisherName(user.getNickname());
        }
        if (StrUtil.isBlank(faq.getDeptName()) && user.getDeptId() != null) {
            DeptDO dept = deptService.getDept(user.getDeptId());
            if (dept != null) {
                faq.setDeptName(dept.getName());
            }
        }
    }

}
