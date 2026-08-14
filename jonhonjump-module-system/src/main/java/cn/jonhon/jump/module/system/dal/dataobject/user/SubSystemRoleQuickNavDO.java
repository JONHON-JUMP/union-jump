package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色默认快捷导航配置 DO（外部子系统）
 */
@TableName("sub_system_role_quick_nav")
@KeySequence("sub_system_role_quick_nav_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemRoleQuickNavDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 外部子系统角色编号
     */
    private Long roleId;
    /**
     * 外部子系统编号
     */
    private Long subSystemId;
    /**
     * 子系统菜单编号
     */
    private Long menuId;
    /**
     * 显示顺序
     */
    private Integer sort;

}
