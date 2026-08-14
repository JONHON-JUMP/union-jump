package cn.jonhon.jump.module.system.service.faq;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.faq.vo.FaqPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.faq.vo.FaqSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.faq.FaqDO;

import java.util.List;

public interface FaqService {

    Long createFaq(FaqSaveReqVO createReqVO);

    void updateFaq(FaqSaveReqVO updateReqVO);

    /** 业务软删除：状态改为已删除 */
    void deleteFaq(Long id);

    void deleteFaqList(List<Long> ids);

    void publishFaq(Long id);

    void revokeFaq(Long id);

    PageResult<FaqDO> getFaqPage(FaqPageReqVO reqVO);

    FaqDO getFaq(Long id);

    /** 已发布的常见 QA（工作台 / 普通用户） */
    FaqDO getPublishedFaq(Long id);

}
