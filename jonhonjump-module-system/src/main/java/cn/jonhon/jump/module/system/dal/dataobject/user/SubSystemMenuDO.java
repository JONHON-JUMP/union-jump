package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("sub_system_menu")
@KeySequence("sub_system_menu_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemMenuDO extends BaseDO {

    @TableId
    private Long id;
    private Long deptId;
    private Long subSystemId;
    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String path;
    private String component;
    private String query;
    private Integer isCache;
    private Integer isFrame;
    private String type;
    private Integer visible;
    private Integer status;
    private String perms;
    private String icon;
    private String componentName;
    private Integer alwaysShow;
    private String remark;

}
