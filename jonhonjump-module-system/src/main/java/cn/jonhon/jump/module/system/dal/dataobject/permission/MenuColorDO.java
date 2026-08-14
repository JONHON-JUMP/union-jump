package cn.jonhon.jump.module.system.dal.dataobject.permission;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单样式 DO（颜色 + 形状）
 */
@TableName("system_menu_style")
@KeySequence("system_menu_style_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class MenuColorDO extends BaseDO {

    @TableId
    private Long id;
    /** 样式名称 */
    private String name;
    /** 图标形状：rounded / square / circle / pill */
    private String shape;
    /** 主色 HEX */
    private String color;
    /** MES 大类编码 */
    private String mesCategory;
    /** 适用场景说明 */
    private String remark;
    /** 显示排序 */
    private Integer sort;
    /**
     * 状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

}
