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
     * 业务系统编号，门户路由 /portal/{clientId}。旧数据可从 OAuth2 客户端回填。
     */
    private String clientId;
    /**
     * OAuth2 客户端编号；人员接口专用系统可为空（不绑门户）
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

    /**
     * 门户路由用的系统编号：优先本表 client_id，否则用已绑定的 OAuth2 client_id。
     */
    public String resolvePortalClientId(String oauthClientId) {
        if (clientId != null && !clientId.trim().isEmpty()) {
            return clientId.trim();
        }
        return oauthClientId;
    }

    /**
     * 是否门户业务系统（有系统编号，或仍绑着旧 OAuth2 客户端）。仅接口目标为 false。
     */
    public boolean isPortalBound() {
        return (clientId != null && !clientId.trim().isEmpty()) || oauth2ClientId != null;
    }

}
