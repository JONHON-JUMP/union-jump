package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务系统子表叶子：一行 = 父系统下的一套可挂角色作用域。
 * <ul>
 *   <li>主表 sub_system：菜单 / OAuth</li>
 *   <li>本表：多叶子共用父菜单；每个叶子一套角色</li>
 *   <li>一对一也落一条；人员只挂本表记录（workshopCode → users.workshop_id）</li>
 * </ul>
 */
@TableName("sub_system_workshop")
@KeySequence("sub_system_workshop_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemWorkshopDO extends BaseDO {

    @TableId
    private Long id;
    /** 叶子编号，如 5501、4200；对齐花名册/角色上的车间字段 */
    private String workshopCode;
    private String workshopName;
    /** 所属业务系统 ID（权威关联） */
    private Long subSystemId;
    /** 所属系统 client_id（冗余；以 subSystemId 为准） */
    private String clientId;
    /** 产品线分组名，如 环控 / 液冷 */
    private String productLine;
    private Integer sort;
    /** 0 正常 1 停用 */
    private Integer status;
    private String remark;

}
