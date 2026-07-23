package cn.jonhon.jump.module.system.service.notice;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.notice.vo.NoticePageReqVO;
import cn.jonhon.jump.module.system.controller.admin.notice.vo.NoticeSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.notice.NoticeDO;

import java.util.List;

/**
 * 通知公告 Service 接口
 */
public interface NoticeService {

    Long createNotice(NoticeSaveReqVO createReqVO);

    void updateNotice(NoticeSaveReqVO reqVO);

    /**
     * 业务软删除：状态改为已删除（仍可在管理端按状态筛选查看）
     */
    void deleteNotice(Long id);

    void deleteNoticeList(List<Long> ids);

    /**
     * 发布：草稿 → 已发布
     */
    void publishNotice(Long id);

    /**
     * 撤回：已发布 → 草稿
     */
    void revokeNotice(Long id);

    PageResult<NoticeDO> getNoticePage(NoticePageReqVO reqVO);

    NoticeDO getNotice(Long id);

    /**
     * 获得已发布的通知（工作台 / 普通用户）
     */
    NoticeDO getPublishedNotice(Long id);

}
