package cn.jonhon.jump.module.system.service.user;



import cn.jonhon.jump.framework.common.pojo.PageResult;

import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.SubSystemCardLoginRespVO;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.SubSystemUserPermissionRespVO;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.PortalPermContextRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;



import javax.validation.Valid;

import java.util.Collection;

import java.util.List;

import java.util.Map;

import java.util.Set;



/**

 * 人员子系统关系 Service

 */

public interface SubSystemUsersService {



    /**

     * 获得指定主数据用户的子系统关系列表

     */

    List<SubSystemUsersRespVO> getListByMainUserId(Long mainUserId);



    /**

     * 批量统计主数据用户的子系统关系数量

     */

    Map<Long, Long> getCountMapByMainUserIds(Collection<Long> mainUserIds);



    /**

     * 获得当前登录用户可访问的外部系统列表

     */

    List<UserExternalSystemRespVO> getMyExternalSystemList(Long userId);

    /**
     * 获得当前登录用户在指定外部系统下的门户菜单（Iframe 嵌入）
     */
    List<SubSystemPortalMenuRespVO> getMyPortalMenus(Long userId, Long subSystemId);

    /**
     * 获得用户在指定外部子系统下可选的快捷导航菜单编号（叶子菜单）
     */
    Set<Long> getAllowedQuickNavMenuIds(Long userId, Long subSystemId);

    /**
     * 仅校验候选菜单是否可作为快捷导航（冷路径用，避免每次拉该子系统全量菜单）
     */
    Set<Long> retainAllowedQuickNavMenuIds(Long userId, Long subSystemId, Collection<Long> candidateIds);

    /**

     * 获得外部系统精简列表
     *
     * @param portalOnly true=仅已绑定 OAuth 的 JUMP 门户业务系统（外部用户/角色/菜单等）；
     *                   false/null=全部（含仅接口目标，供接口管理页使用）
     */
    List<SubSystemClientSimpleRespVO> getClientSimpleList(Boolean portalOnly);



    /**

     * 获得外部系统用户分页

     */

    PageResult<SubSystemUsersRespVO> getSubSystemUserPage(SubSystemUsersPageReqVO pageReqVO);



    /**

     * 获得外部系统用户

     */

    SubSystemUsersRespVO getSubSystemUser(Long id);



    /**

     * 创建外部系统用户

     */

    Long createSubSystemUser(@Valid SubSystemUsersSaveReqVO createReqVO);

    /**
     * 按用户名查询外部系统用户
     */
    SubSystemUsersRespVO getBySubSystemIdAndUsername(Long subSystemId, String username);

    /**
     * 将主系统用户挂接到外部系统同名用户（main_user_id 可选关联）
     */
    Long bindMainUser(Long subSystemId, Long mainUserId);



    /**

     * 更新外部系统用户

     */

    void updateSubSystemUser(@Valid SubSystemUsersSaveReqVO updateReqVO);



    /**

     * 删除外部系统用户

     */

    void deleteSubSystemUser(Long id);



    /**

     * 批量删除外部系统用户

     */

    void deleteSubSystemUserList(List<Long> ids);



    /**

     * 修改外部系统用户状态

     */

    void updateSubSystemUserStatus(Long id, String status);



    /**

     * 获得外部系统角色精简列表

     */

    List<SubSystemRoleSimpleRespVO> getRoleSimpleList(Long subSystemId);

    List<SubSystemPostSimpleRespVO> getPostSimpleList(Long subSystemId);

    List<SubSystemTeamSimpleRespVO> getTeamSimpleList(Long subSystemId, Long deptId);

    List<SubSystemMenuTreeRespVO> getUserHomeMenuTree(Long subSystemId, List<Long> roleIds);

    void assignSubSystemUserRole(@Valid SubSystemUsersAssignRoleReqVO reqVO);



    /**
     * 获得外部系统用户角色编号列表
     */
    List<Long> getSubSystemUserRoleIds(Long id);

    /**
     * 子系统刷卡校验：按 clientId 定位外部系统，按 username 查花名册
     */
    SubSystemCardLoginRespVO cardLogin(String clientId, String clientSecret, String username);

    /**
     * 按 clientId + username 查询花名册用户的角色、菜单、权限
     */
    SubSystemUserPermissionRespVO getPermissionInfo(String clientId, String clientSecret, String username);

}
