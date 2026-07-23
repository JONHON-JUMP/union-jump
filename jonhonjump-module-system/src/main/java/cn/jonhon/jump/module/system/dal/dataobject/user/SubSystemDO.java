package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 外部系统基本信息 DO
 */
@TableName("sub_system")
@KeySequence("sub_system_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * OAuth2 客户端编号
     *
     * 关联 {@link OAuth2ClientDO#getId()}
     */
    private Long oauth2ClientId;
    /**
     * 外部系统名称
     */
    private String systemName;
    /**
     * 系统描述
     */
    private String description;
    /**
     * 外部系统访问地址
     */
    private String systemUrl;
    /**
     * 系统图标
     */
    private String systemIcon;
    /**
     * 系统状态（0正常 1停用）
     */
    private Integer status;

}
