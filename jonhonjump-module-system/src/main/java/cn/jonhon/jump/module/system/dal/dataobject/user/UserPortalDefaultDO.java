package cn.jonhon.jump.module.system.dal.dataobject.user;



import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户门户默认打开系统配置 DO
 */
@TableName("system_user_portal_default")
@KeySequence("system_user_portal_default_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPortalDefaultDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 用户编号
     */
    private Long userId;
    /**
     * 默认打开的外部子系统编号，null 表示统一门户主页。
     * 必须 ALWAYS：否则改回统一门户时 null 不会写入，库里仍保留原子系统 ID。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long subSystemId;

}

