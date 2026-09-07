package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeNoticeDO;

/**
 * 工艺变更通知 MQ 分发权领取服务。
 *
 * 领取实现必须以独立新事务提交状态 8，确保 RabbitMQ 消息发出前 MES 查询已能读取该状态。
 */
public interface RecipeChangeDispatchClaimService {

    /**
     * 领取一条可发送的轻量 Outbox 通知。
     *
     * @param noticeId 通知主键
     * @param operationType 本次触发对应的操作类型，仅用于日志定位来源
     * @param triggerType 本次状态流转触发类型，用于写入状态流水
     * @param operator 当前执行分发的系统或人工操作人
     * @param recordScheduledRetryAttempt 是否记录一次已实际领取的定时重试尝试
     * @return 已提交状态 8 且当前调用方取得发送权的通知；不能领取时返回 {@code null}
     */
    RecipeChangeNoticeDO claimNoticeForDispatch(Long noticeId, Integer operationType, Integer triggerType, String operator, boolean recordScheduledRetryAttempt);
}
