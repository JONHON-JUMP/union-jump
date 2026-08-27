package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 子系统车间 DO（JUMP 部门与子系统车间的映射）
 *
 * 说明：JUMP 的"部门"与子系统的"车间"（如 Camstar 的 Factory）是同一组织实体的两套标识，
 * 本表维护两者对应关系；多个部门可映射同一车间；人员同步时按 deptId 在此换算 workshopCode。
 */
@TableName("sub_system_workshop")
@KeySequence("sub_system_workshop_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemWorkshopDO extends BaseDO {

    @TableId
    private Long id;
    /** 外部系统 ID（sub_system.id） */
    private Long subSystemId;
    /** JUMP 部门 ID（system_dept.id） */
    private Long deptId;
    /** 子系统车间编码（如 Camstar 的 4200） */
    private String workshopCode;
    /** 子系统车间名称 */
    private String workshopName;
    /** 描述 */
    private String description;

}
