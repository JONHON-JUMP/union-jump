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
    /** 菜单样式编号，关联 system_menu_style.id */
    private Long styleId;
    private String componentName;
    private Integer alwaysShow;
    private String remark;

    /** 菜单说明书文件地址（可选） */
    private String manualUrl;

    /**
     * 通用菜单模板编号：非空表示本行是模板在各子系统的副本；
     * 模板本身以 subSystemId = 0 存储，字段变更时同步所有副本（位置/排序除外）
     */
    private Long sharedSourceId;

}
