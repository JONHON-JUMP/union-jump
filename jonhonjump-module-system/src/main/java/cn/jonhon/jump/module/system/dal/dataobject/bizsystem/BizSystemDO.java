package cn.jonhon.jump.module.system.dal.dataobject.bizsystem;

import cn.jonhon.jump.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务系统访问明细，对应「系统明细」表。
 */
@TableName("system_biz_system")
@KeySequence("system_biz_system_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class BizSystemDO extends TenantBaseDO {

    @TableId
    private Long id;
    /** 序号 */
    private Integer sortNo;
    private String ip;
    private Integer port;
    /** 系统名称 */
    private String systemName;
    /** 后端明细 */
    private String backendDetail;
    /** 系统类型：前后端分离 / 前后端一体 */
    private String systemType;
    /** 框架：若依 / 芋道 / C# */
    private String framework;
    /** Nginx 映射端口 */
    private Integer nginxPort;

}
