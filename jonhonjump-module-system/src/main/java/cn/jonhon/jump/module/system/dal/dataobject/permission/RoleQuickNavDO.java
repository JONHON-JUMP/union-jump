package cn.jonhon.jump.module.system.dal.dataobject.permission;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色默认快捷导航配置 DO（主系统）
 */
@TableName("system_role_quick_nav")
@KeySequence("system_role_quick_nav_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQuickNavDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 角色编号
     *
     * 关联 {@link RoleDO#getId()}
     */
    private Long roleId;
    /**
     * 菜单编号
     *
     * 关联 {@link MenuDO#getId()}
     */
    private Long menuId;
    /**
     * 显示顺序
     */
    private Integer sort;

}
