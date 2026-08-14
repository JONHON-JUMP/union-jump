package cn.jonhon.jump.module.system.service.user;



import cn.jonhon.jump.module.system.controller.admin.user.vo.portal.UserPortalDefaultRespVO;



/**

 * 用户门户默认打开系统 Service

 */

public interface UserPortalDefaultService {



    /**

     * 获得当前用户的门户默认打开系统配置

     */

    UserPortalDefaultRespVO getUserPortalDefault(Long userId);



    /**

     * 保存当前用户的门户默认打开系统配置

     *

     * @param subSystemId 外部子系统编号，null 表示统一门户主页

     */

    void saveUserPortalDefault(Long userId, Long subSystemId);



    /**

     * 清除当前用户的门户默认打开系统配置

     */

    void clearUserPortalDefault(Long userId);



}

