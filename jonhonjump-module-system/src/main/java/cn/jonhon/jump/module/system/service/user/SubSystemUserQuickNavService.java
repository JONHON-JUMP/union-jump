package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavCandidateRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO;

import java.util.List;

/**
 * 用户外部子系统快捷导航 Service
 */
public interface SubSystemUserQuickNavService {

    SubSystemUserQuickNavRespVO getUserQuickNav(Long userId, Long subSystemId);

    List<SubSystemUserQuickNavCandidateRespVO> getCandidateList(Long userId, Long subSystemId);

    void saveUserQuickNav(Long userId, Long subSystemId, List<Long> menuIds);

    void deleteByMenuId(Long menuId);

    void deleteByMenuIds(List<Long> menuIds);

}
