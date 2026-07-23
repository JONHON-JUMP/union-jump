package cn.jonhon.jump.module.system.service.notify;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.notify.vo.message.NotifyMessageDetailRespVO;
import cn.jonhon.jump.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.notice.NoticeDO;
import cn.jonhon.jump.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.jonhon.jump.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.jonhon.jump.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.jonhon.jump.module.system.dal.mysql.notice.NoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.NOTIFY_MESSAGE_NOT_EXISTS;

/**
 * 站内信 Service 实现类
 *
 * @author xrcoder
 */
@Service
@Validated
public class NotifyMessageServiceImpl implements NotifyMessageService {

    @Resource
    private NotifyMessageMapper notifyMessageMapper;
    @Resource
    private NoticeMapper noticeMapper;

    @Override
    public Long createNotifyMessage(Long userId, Integer userType,
                                    NotifyTemplateDO template, String templateContent, Map<String, Object> templateParams) {
        NotifyMessageDO message = new NotifyMessageDO().setUserId(userId).setUserType(userType)
                .setTemplateId(template.getId()).setTemplateCode(template.getCode())
                .setTemplateType(template.getType()).setTemplateNickname(template.getNickname())
                .setTemplateContent(templateContent).setTemplateParams(templateParams).setReadStatus(false);
        notifyMessageMapper.insert(message);
        return message.getId();
    }

    @Override
    public PageResult<NotifyMessageDO> getNotifyMessagePage(NotifyMessagePageReqVO pageReqVO) {
        return notifyMessageMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<NotifyMessageDO> getMyMyNotifyMessagePage(NotifyMessageMyPageReqVO pageReqVO, Long userId, Integer userType) {
        return notifyMessageMapper.selectPage(pageReqVO, userId, userType);
    }

    @Override
    public NotifyMessageDO getNotifyMessage(Long id) {
        return notifyMessageMapper.selectById(id);
    }

    @Override
    public NotifyMessageDetailRespVO getMyNotifyMessageDetail(Long id, Long userId, Integer userType) {
        NotifyMessageDO message = notifyMessageMapper.selectByIdAndUserIdAndUserType(id, userId, userType);
        if (message == null) {
            throw exception(NOTIFY_MESSAGE_NOT_EXISTS);
        }
        if (Boolean.FALSE.equals(message.getReadStatus())) {
            updateNotifyMessageRead(Collections.singletonList(id), userId, userType);
            message.setReadStatus(true);
        }
        return buildDetailRespVO(message);
    }

    @Override
    public List<NotifyMessageDO> getUnreadNotifyMessageList(Long userId, Integer userType, Integer size) {
        return notifyMessageMapper.selectUnreadListByUserIdAndUserType(userId, userType, size);
    }

    @Override
    public Long getUnreadNotifyMessageCount(Long userId, Integer userType) {
        return notifyMessageMapper.selectUnreadCountByUserIdAndUserType(userId, userType);
    }

    @Override
    public int updateNotifyMessageRead(Collection<Long> ids, Long userId, Integer userType) {
        return notifyMessageMapper.updateListRead(ids, userId, userType);
    }

    @Override
    public int updateAllNotifyMessageRead(Long userId, Integer userType) {
        return notifyMessageMapper.updateListRead(userId, userType);
    }

    private NotifyMessageDetailRespVO buildDetailRespVO(NotifyMessageDO message) {
        NotifyMessageDetailRespVO detail = new NotifyMessageDetailRespVO();
        detail.setId(message.getId());
        detail.setTemplateType(message.getTemplateType());
        detail.setReadStatus(message.getReadStatus());
        detail.setCreateTime(message.getCreateTime());
        detail.setTitle(message.getTemplateContent());
        detail.setContent(message.getTemplateContent());

        NoticeDO notice = getNoticeFromTemplateParams(message);
        if (notice != null) {
            detail.setTitle(notice.getTitle());
            detail.setContent(notice.getContent());
            detail.setPublisherName(notice.getPublisherName());
            detail.setDeptName(notice.getDeptName());
            detail.setAttachments(notice.getAttachments());
        }
        return detail;
    }

    private NoticeDO getNoticeFromTemplateParams(NotifyMessageDO message) {
        if (message.getTemplateParams() == null) {
            return null;
        }
        Object noticeId = message.getTemplateParams().get("noticeId");
        if (noticeId == null) {
            return null;
        }
        return noticeMapper.selectById(Long.valueOf(noticeId.toString()));
    }

}
