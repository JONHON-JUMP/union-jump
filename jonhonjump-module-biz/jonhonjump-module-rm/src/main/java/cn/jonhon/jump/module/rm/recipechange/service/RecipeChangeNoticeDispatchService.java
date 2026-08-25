package cn.jonhon.jump.module.rm.recipechange.service;

/**
 * 工艺变更通知分发服务
 * <p>
 * RabbitMQ 分发模块完成后，将在事务提交后消费接收事件并调用该接口
 */
public interface RecipeChangeNoticeDispatchService {

    /**
     * 将指定工艺变更通知分发到目标车间对应的 RabbitMQ 队列
     *
     * @param noticeId 已落库的工艺变更通知主键
     */
    void dispatchRecipeChangeNotice(Long noticeId);

    /**
     * 按指定重试场景分发工艺变更通知
     *
     * @param noticeId           通知主键
     * @param operationType      操作日志类型
     * @param triggerType        状态流转触发类型
     * @param operator           操作人或系统标识
     * @param increaseRetryCount 发送失败时是否增加自动重试次数
     */
    void dispatchRecipeChangeNotice(Long noticeId, Integer operationType, Integer triggerType, String operator, boolean increaseRetryCount);

}
