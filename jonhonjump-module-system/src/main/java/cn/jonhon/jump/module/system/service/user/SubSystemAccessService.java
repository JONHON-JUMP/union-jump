package cn.jonhon.jump.module.system.service.user;

import java.util.Set;

/**
 * 业务系统管理页面的"可管系统"范围 Service
 *
 * 依据主系统角色的 sub_system_ids（可管业务系统集合）计算当前登录人的数据范围：
 * 角色留空 = 该角色不参与限制；用户所有启用角色都留空（含超级管理员）= 不限；
 * 只要有角色勾了可管系统，就只取这些勾选的并集，留空的角色不会把范围放大成全部系统。
 */
public interface SubSystemAccessService {

    /**
     * 计算指定用户可管理的业务系统编号集合
     *
     * @param userId 用户编号；为空（内部调用，无登录上下文）时不受限
     * @return 可管理的业务系统编号集合；null 表示不受限，空集合表示无任何可管系统
     */
    Set<Long> getAllowedSubSystemIds(Long userId);

    /**
     * 校验业务系统在当前登录人可管范围内，越权抛 {@code SUB_SYSTEM_NO_PERMISSION}
     */
    void checkAccessible(Long subSystemId);

    /**
     * 当前登录人是否不受限（内部调用、超级管理员，或所有启用角色都未配置可管系统）
     */
    boolean isUnrestricted();

}
