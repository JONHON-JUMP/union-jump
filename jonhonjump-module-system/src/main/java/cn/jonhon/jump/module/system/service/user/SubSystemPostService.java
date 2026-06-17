package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPostPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPostRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPostSaveReqVO;

public interface SubSystemPostService {

    PageResult<SubSystemPostRespVO> getSubSystemPostPage(SubSystemPostPageReqVO pageReqVO);

    SubSystemPostRespVO getSubSystemPost(Long id);

    Long createSubSystemPost(SubSystemPostSaveReqVO createReqVO);

    void updateSubSystemPost(SubSystemPostSaveReqVO updateReqVO);

    void deleteSubSystemPost(Long id);

    void deleteSubSystemPostList(java.util.List<Long> ids);

}
