package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavCandidateRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavRespVO;

import java.util.List;

/**
 * 用户快捷导航 Service（主系统）
 */
public interface UserQuickNavService {

    UserQuickNavRespVO getUserQuickNav(Long userId);

    List<UserQuickNavCandidateRespVO> getCandidateList(Long userId);

    void saveUserQuickNav(Long userId, List<Long> menuIds);

    void deleteByMenuId(Long menuId);

    void deleteByMenuIds(List<Long> menuIds);

}
