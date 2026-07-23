package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * 子系统角色 DO
 */
@TableName(value = "sub_system_role", autoResultMap = true)
@KeySequence("sub_system_role_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemRoleDO extends BaseDO {

    @TableId
    private Long id;
    private Long subSystemId;
    private String name;
    private String code;
    private Integer sort;
    private Integer dataScope;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Set<Long> dataScopeDeptIds;
    private Integer menuCheckStrictly;
    private Integer deptCheckStrictly;
    private Integer status;
    private Integer type;

}
