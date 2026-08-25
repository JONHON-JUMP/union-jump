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
 * 工艺变更通知主记录，对应 {@code recipe_change_notice} 表
 */
@TenantIgnore
@TableName(value = "recipe_change_notice", autoResultMap = true)
@KeySequence("recipe_change_notice_id_seq")
@Data
public class RecipeChangeNoticeDO extends BaseDO {

    /**
     * 主键，由 PostgreSQL 序列 {@code recipe_change_notice_id_seq} 生成
     */
    @TableId
    private Long id;
    /**
     * MPM 通知唯一标识，也是通知幂等键
     */
    private String notifyId;
    /**
     * 目标车间编码，用于后续按车间进行消息分发
     */
    private String workshopCode;
    /**
     * 工艺变更业务内容，以 JSONB 格式存储
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode changeContent;
    /**
     * 通知当前生命周期状态，取值见 {@code RecipeChangeNoticeStatusEnum}
     */
    private Integer status;
    /**
     * 当前已执行的重试次数
     */
    private Integer retryCount;
    /**
     * 允许执行的最大重试次数
     */
    private Integer maxRetry;
    /**
     * 最近一次处理失败的错误信息
     */
    private String errorMsg;
    /**
     * 通知成功投递至 RabbitMQ 的时间
     */
    private LocalDateTime mqSendTime;
    /**
     * 当前 MES 消费者持有的处理令牌，用于拒绝过期消费者的回调
     */
    private String processingToken;
    /**
     * 当前处理令牌的租约到期时间，消费者异常退出后允许消息重新领取
     */
    private LocalDateTime processingLeaseUntil;
    /**
     * 预留备注
     */
    private String remark;

}
