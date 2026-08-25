package cn.jonhon.jump.module.rm.recipechange.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工艺变更通知操作类型定义
 */
@Getter
@AllArgsConstructor
public enum RecipeChangeOperationTypeEnum {

    /**
     * MPM 系统调用工艺变更通知接收接口
     */
    MPM_CALL(10, "MPM下发"),
    /**
     * JUMP 向 RabbitMQ 发送工艺变更消息
     */
    MQ_SEND(20, "JUMP消息发送"),
    /**
     * MES 回调工艺变更消息处理结果
     */
    MES_CALLBACK(30, "MES上报处理结果"),
    /**
     * JUMP 手动重发
     */
    MANUAL_RETRY(40, "手动重发"),
    /**
     * JUMP 定时重试
     */
    SCHEDULED_RETRY(50, "JUMP定时重试"),
    /**
     * 管理员对异常通知执行批量标记完成
     */
    BATCH_PROCESS(60, "批量标记完成"),
    /**
     * MES 调用钉钉告警处理器后的发送结果
     */
    DINGTALK_ALARM(70, "MES钉钉告警");

    /**
     * 操作类型编码，持久化至操作流水表
     */
    private final Integer type;
    /**
     * 操作类型中文名称，用于展示和日志说明
     */
    private final String name;

}
