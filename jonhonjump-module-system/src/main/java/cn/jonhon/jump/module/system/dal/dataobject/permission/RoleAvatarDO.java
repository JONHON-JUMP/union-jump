package cn.jonhon.jump.module.system.dal.dataobject.permission;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色系统头像 DO
 */
@TableName("system_role_avatar")
@KeySequence("system_role_avatar_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class RoleAvatarDO extends BaseDO {

    @TableId
    private Long id;
    /** 角色标识，对应 system_role.code */
    private String roleCode;
    /** 头像 URL */
    private String avatarUrl;
    /** 显示排序（越小越靠前，多角色默认头像优先级） */
    private Integer sort;
    /**
     * 状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /** 备注 */
    private String remark;

}
