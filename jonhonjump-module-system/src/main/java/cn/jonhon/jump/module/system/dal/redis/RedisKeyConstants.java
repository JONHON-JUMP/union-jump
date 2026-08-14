package cn.jonhon.jump.module.system.dal.redis;

import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;

/**
 * System Redis Key 枚举类
 *
 * @author 中航光电
 */
public interface RedisKeyConstants {

    /**
     * 指定部门的所有子部门编号数组的缓存
     * <p>
     * KEY 格式：dept_children_ids:{id}
     * VALUE 数据类型：String 子部门编号集合
     */
    String DEPT_CHILDREN_ID_LIST = "dept_children_ids";

    /**
     * 角色的缓存
     * <p>
     * KEY 格式：role:{id}
     * VALUE 数据类型：String 角色信息
     */
    String ROLE = "role";

    /**
     * 用户拥有的角色编号的缓存
     * <p>
     * KEY 格式：user_role_ids:{userId}
     * VALUE 数据类型：String 角色编号集合
     */
    String USER_ROLE_ID_LIST = "user_role_ids";

    /**
     * 拥有指定菜单的角色编号的缓存
     * <p>
     * KEY 格式：menu_role_ids:{menuId}
     * VALUE 数据类型：String 角色编号集合
     */
    String MENU_ROLE_ID_LIST = "menu_role_ids";

    /**
     * 拥有权限对应的菜单编号数组的缓存
     * <p>
     * KEY 格式：permission_menu_ids:{permission}
     * VALUE 数据类型：String 菜单编号数组
     */
    String PERMISSION_MENU_ID_LIST = "permission_menu_ids";

    /**
     * OAuth2 客户端的缓存
     * <p>
     * KEY 格式：oauth_client:{id}
     * VALUE 数据类型：String 客户端信息
     */
    String OAUTH_CLIENT = "oauth_client";

    /**
     * 访问令牌的缓存
     * <p>
     * KEY 格式：oauth2_access_token:{token}
     * VALUE 数据类型：String 访问令牌信息 {@link OAuth2AccessTokenDO}
     * <p>
     * 由于动态过期时间，使用 RedisTemplate 操作
     */
    String OAUTH2_ACCESS_TOKEN = "oauth2_access_token:%s";

    /**
     * 站内信模版的缓存
     * <p>
     * KEY 格式：notify_template:{code}
     * VALUE 数据格式：String 模版信息
     */
    String NOTIFY_TEMPLATE = "notify_template";

    /**
     * 邮件账号的缓存
     * <p>
     * KEY 格式：mail_account:{id}
     * VALUE 数据格式：String 账号信息
     */
    String MAIL_ACCOUNT = "mail_account";

    /**
     * 邮件模版的缓存
     * <p>
     * KEY 格式：mail_template:{code}
     * VALUE 数据格式：String 模版信息
     */
    String MAIL_TEMPLATE = "mail_template";

    /**
     * 短信模版的缓存
     * <p>
     * KEY 格式：sms_template:{id}
     * VALUE 数据格式：String 模版信息
     */
    String SMS_TEMPLATE = "sms_template";

    /**
     * 小程序订阅模版的缓存
     *
     * KEY 格式：wxa_subscribe_template:{userType}
     * VALUE 数据格式 String, 模版信息
     */
    String WXA_SUBSCRIBE_TEMPLATE = "wxa_subscribe_template";

    /**
     * 用户主系统快捷导航的缓存
     * <p>
     * KEY 格式：user_quick_nav:{userId}
     * VALUE 数据格式：String {@link cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavRespVO}
     */
    String USER_QUICK_NAV = "user_quick_nav:%s";

    /**
     * 用户外部子系统快捷导航的缓存
     * <p>
     * KEY 格式：sub_system_user_quick_nav:{userId}:{subSystemId}
     * VALUE 数据格式：String {@link cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO}
     */
    String SUB_SYSTEM_USER_QUICK_NAV = "sub_system_user_quick_nav_v2:%s:%s";

    /**
     * 登录用户权限信息缓存
     * <p>
     * KEY 格式：user_permission_info:{userId}
     * VALUE 数据格式：String {@link cn.jonhon.jump.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO}
     * <p>
     * 无 TTL：菜单/角色变更时主动 delete；过期靠淘汰而非固定 10 分钟重建
     */
    String USER_PERMISSION_INFO = "user_permission_info:%s";

    /**
     * 主系统用户权限变更版本（菜单/角色/授权/数据权限变更时 INCR）
     * <p>
     * KEY 格式：user_permission_rbac_version:{userId}
     * VALUE：Long；前端登录时记下，有动作时比对，不一致则提示重登
     */
    String USER_PERMISSION_RBAC_VERSION = "user_permission_rbac_version:%s";

    /**
     * 门户 my-menus 缓存
     * <p>
     * KEY 格式：portal_my_menus:{userId}:{subSystemId}
     * VALUE：List&lt;SubSystemPortalMenuRespVO&gt; JSON；改子系统菜单/角色时主动失效
     */
    String PORTAL_MY_MENUS = "portal_my_menus:%s:%s";

    /**
     * 子系统权限包（主系统写、子系统只读）
     * <p>
     * KEY 格式：portal:perm:context:{username}:{clientId}
     * VALUE 数据格式：String {@link cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.PortalPermContextRespVO}
     */
    String PORTAL_PERM_CONTEXT = "portal:perm:context:%s:%s";

    /**
     * 子系统扩展业务缓存（主系统 HTTP 代写）
     * <p>
     * KEY 格式：portal:ext:{clientId}:{namespace}:{key}
     */
    String PORTAL_EXT_CACHE = "portal:ext:%s:%s:%s";

    /**
     * 子系统 RBAC 变更版本（菜单/角色/授权变更时 INCR）
     * <p>
     * KEY 格式：portal_rbac_version:{subSystemId}
     * VALUE：Long；门户进入子系统时轻量比对，有变化才重拉 my-menus
     */
    String PORTAL_RBAC_VERSION = "portal_rbac_version:%s";

    /**
     * 字典数据（按类型）缓存
     * <p>
     * KEY 格式：dict_data_type:{dictType}
     * VALUE 数据格式：String List&lt;DictDataDO&gt; JSON
     */
    String DICT_DATA_TYPE = "dict_data_type:%s";

    /**
     * 字典数据全量快照缓存
     * <p>
     * KEY 格式：dict_data_all
     * VALUE 数据格式：String List&lt;DictDataDO&gt; JSON
     */
    String DICT_DATA_ALL = "dict_data_all";

}
