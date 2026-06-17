package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 子系统用户角色关联 DO
 */
@TableName("sub_system_user_role")
@KeySequence("sub_system_user_role_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemUserRoleDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 子系统用户 ID，关联 {@link SubSystemUsersDO#getId()}
     */
    private Long userId;
    private Long roleId;

}
