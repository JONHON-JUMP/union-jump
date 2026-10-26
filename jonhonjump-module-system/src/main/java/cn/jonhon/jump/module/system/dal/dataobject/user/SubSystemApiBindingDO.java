package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务叶子与外部接口的绑定。一个叶子可有多条：每条对应一次页面调用，指向一个接口系统和一条具体接口。
 */
@TableName("sub_system_api_binding")
@KeySequence("sub_system_api_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemApiBindingDO extends BaseDO {

    @TableId
    private Long id;
    /** 业务系统 ID */
    private Long bizSubSystemId;
    /** 叶子编号；无叶子时为空串 */
    private String workshopCode;
    /** 页面调用，见 {@link cn.jonhon.jump.module.system.enums.user.LeafApiSceneEnum} */
    private String scene;
    /** 接口所在系统 ID */
    private Long apiSubSystemId;
    /** 具体接口行 ID（sub_system_api_endpoint.id） */
    private Long endpointId;

}
