package cn.jonhon.jump.module.rm.recipechange.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工艺变更通知生命周期状态定义
 */
@Getter
@AllArgsConstructor
public enum RecipeChangeNoticeStatusEnum {

    /**
     * 已完成 MPM 请求接收、通知主记录及初始日志落库
     */
    RECEIVED_SUCCESS(5, "接收成功"),
    /**
     * RabbitMQ 已确认接收消息，等待 MES 消费
     */
    SENT_MQ(10, "已发送MQ"),
    /**
     * RabbitMQ 发送异常、未确认或无法路由
     */
    SEND_FAILED(15, "MQ发送失败"),
    /**
     * MES 已原子领取处理权，当前仅允许持有令牌的消费者回调处理结果
     */
    MES_PROCESSING(18, "MES处理中"),
    /**
     * MES 已成功完成工艺变更处理
     */
    MES_PROCESS_SUCCESS(20, "MES处理成功"),
    /**
     * 车间处理失败
     */
    MES_PROCESS_FAILED(25, "MES处理失败"),
    /**
     * 待人工处理
     */
    PENDING_MANUAL(30, "待人工处理"),
    /**
     * 管理员确认终止后续处理和重试
     */
    MARKED_COMPLETED(35, "已标记完成");

    /**
     * 状态编码，持久化至通知主表和状态流水表
     */
    private final Integer status;
    /**
     * 状态中文名称，用于展示和日志说明
     */
    private final String name;

}
