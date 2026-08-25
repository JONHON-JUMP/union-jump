package cn.jonhon.jump.module.rm.recipechange.dal.dataobject;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工艺变更通知操作流水，对应 {@code recipe_change_operation_log} 表
 */
@TenantIgnore
@TableName(value = "recipe_change_operation_log", autoResultMap = true)
@KeySequence("recipe_change_operation_log_id_seq")
@Data
public class RecipeChangeOperationLogDO extends BaseDO {

    /**
     * 主键，由 PostgreSQL 序列 {@code recipe_change_operation_log_id_seq} 生成
     */
    @TableId
    private Long id;
    /**
     * 关联的工艺变更通知主键
     */
    private Long noticeId;
    /**
     * MPM 通知唯一标识，便于按外部业务标识检索
     */
    private String notifyId;
    /**
     * 本次操作对应的目标车间编码
     */
    private String workshopCode;
    /**
     * 操作类型，取值见 {@code RecipeChangeOperationTypeEnum}
     */
    private Integer operationType;
    /**
     * 操作发生时间
     */
    private LocalDateTime operationTime;
    /**
     * 操作执行者或来源系统标识
     */
    private String operator;
    /**
     * 操作结果，取值见 {@code RecipeChangeOperationResultEnum}
     */
    private Integer operationResult;
    /**
     * 操作失败时记录的错误信息
     */
    private String errorMsg;
    /**
     * 本次操作的请求参数快照，以 JSONB 格式存储
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode requestParams;
    /**
     * 本次操作的响应参数快照，以 JSONB 格式存储
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode responseParams;
    /**
     * 预留备注
     */
    private String remark;

}
