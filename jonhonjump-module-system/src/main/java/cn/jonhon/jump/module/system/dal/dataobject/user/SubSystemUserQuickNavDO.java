package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户外部子系统快捷导航配置 DO
 */
@TableName("sub_system_user_quick_nav")
@KeySequence("sub_system_user_quick_nav_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemUserQuickNavDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 主系统用户编号
     */
    private Long userId;
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
