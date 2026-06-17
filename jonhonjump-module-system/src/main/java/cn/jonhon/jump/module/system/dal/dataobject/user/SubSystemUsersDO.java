package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员子系统关系 DO
 */
@TableName("sub_system_users")
@KeySequence("sub_system_users_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemUsersDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 主数据人员 ID
     *
     * 关联 {@link AdminUserDO#getId()}
     */
    private Long mainUserId;
    /**
     * 外部系统 ID
     *
     * 关联 {@link SubSystemDO#getId()}
     */
    private Long subSystemId;
    /**
     * 车间编号
     */
    private String workshopId;
    /**
     * 班组编号
     */
    private String teamId;
    /**
     * 主页面菜单 ID
     *
     * 关联 {@link SubSystemMenuDO#getId()}，type=C 的菜单页面
     */
    private Long homeMenuId;
    /**
     * 状态（0正常 1禁用）
     */
    private String status;
    /**
     * 备注
     */
    private String remark;

}
