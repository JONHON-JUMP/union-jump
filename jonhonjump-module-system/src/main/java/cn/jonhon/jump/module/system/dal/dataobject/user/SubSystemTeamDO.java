package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("sub_system_team")
@KeySequence("sub_system_team_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemTeamDO extends BaseDO {

    @TableId
    private Long id;
    private Long deptId;
    private Long subSystemId;
    private String teamCode;
    private String teamName;
    private String description;
    private Long teamLeaderId;
    private String teamLeaderName;

}
