package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户快捷导航配置 DO（主系统）
 */
@TableName("system_user_quick_nav")
@KeySequence("system_user_quick_nav_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuickNavDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 用户编号
     *
     * 关联 {@link AdminUserDO#getId()}
     */
    private Long userId;
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
