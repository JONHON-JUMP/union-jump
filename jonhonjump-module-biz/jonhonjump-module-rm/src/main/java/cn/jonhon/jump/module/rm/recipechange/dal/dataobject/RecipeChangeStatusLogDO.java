package cn.jonhon.jump.module.rm.recipechange.dal.dataobject;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工艺变更通知状态流转记录，对应 {@code recipe_change_status_log} 表
 */
@TenantIgnore
@TableName("recipe_change_status_log")
@KeySequence("recipe_change_status_log_id_seq")
@Data
public class RecipeChangeStatusLogDO extends BaseDO {

    /**
     * 主键，由 PostgreSQL 序列 {@code recipe_change_status_log_id_seq} 生成
     */
    @TableId
    private Long id;
    /**
     * 关联的工艺变更通知主键
     */
    private Long noticeId;
    /**
     * MPM 通知唯一标识，便于按外部业务标识追溯状态流转
     */
    private String notifyId;
    /**
     * 变更前的生命周期状态；首次接收时为空
     */
    private Integer fromStatus;
    /**
     * 变更后的生命周期状态
     */
    private Integer toStatus;
    /**
     * 状态发生变更的时间
     */
    private LocalDateTime changeTime;
    /**
     * 状态变更触发方式，取值见 {@code RecipeChangeTriggerTypeEnum}
     */
    private Integer triggerType;
    /**
     * 本次状态流转的补充说明
     */
    private String remark;

}
