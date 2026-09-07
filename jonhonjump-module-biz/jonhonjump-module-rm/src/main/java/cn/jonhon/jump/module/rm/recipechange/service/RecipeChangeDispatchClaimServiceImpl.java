package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeNoticeDO;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeStatusLogDO;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeNoticeMapper;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeStatusLogMapper;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeNoticeStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 工艺变更通知 MQ 分发权领取服务。
 *
 * 必须独立于 MPM 接收事务的 AFTER_COMMIT 监听器运行：监听器执行期间原事务资源仍可能绑定在线程上，
 * 若使用默认 REQUIRED 传播行为，状态 8 的更新可能不会作为新的事务提交，导致 MES 收到 MQ 消息时仍读到状态 5。
 */
@Service
public class RecipeChangeDispatchClaimServiceImpl implements RecipeChangeDispatchClaimService {

    private static final Logger log = LoggerFactory.getLogger(RecipeChangeDispatchClaimService.class);

    /** 工艺变更通知主表数据访问对象。 */
    @Resource
    private RecipeChangeNoticeMapper recipeChangeNoticeMapper;

    /** 工艺变更通知状态流水表数据访问对象。 */
    @Resource
    private RecipeChangeStatusLogMapper recipeChangeStatusLogMapper;

    /**
     * 领取一条可发送的轻量 Outbox 通知，并在返回前真实提交状态 8。
     *
     * 总体流程：查询通知、以原状态进行条件更新领取分发权、首次领取时记录状态流水，最后提交独立事务。
     * 返回后调用方才能发送 RabbitMQ，因此 MES 即使立即消费，也只能读到状态 8 或其后的状态，不能读到状态 5。
     *
     * 状态 8 的一分钟是分发发送租约而非消息延迟：RabbitMQ confirm 最长等待十秒，预留六倍时间给网络、
     * 队列声明和数据库调度；超时通常表示发送线程在发布或确认前宕机/卡死，才允许定时任务重新领取。
     *
     * @param noticeId 通知主键
     * @param operationType 本次触发对应的操作类型，仅用于日志定位来源
     * @param triggerType 本次状态流转触发类型，用于写入状态流水
     * @param operator 当前执行分发的系统或人工操作人
     * @return 已提交状态 8 且当前调用方取得发送权的通知；不能领取时返回 {@code null}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @Override
    public RecipeChangeNoticeDO claimNoticeForDispatch(Long noticeId, Integer operationType, Integer triggerType, String operator,
                                                        boolean recordScheduledRetryAttempt) {
        // 查询完整通知内容，后续 RabbitMQ 消息需要其中的车间编码和 changeContent。
        RecipeChangeNoticeDO notice = recipeChangeNoticeMapper.selectById(noticeId);
        // 通知可能已被逻辑删除或不存在，此时不能生成 MQ 消息。
        if (notice == null) {
            // 保留调用上下文，便于排查延迟事件或定时任务扫描到已删除数据的情况。
            log.warn("[claimNoticeForDispatch][通知不存在，放弃分发] noticeId={}, operationType={}, triggerType={}, operator={}",
                    noticeId, operationType, triggerType, operator);
            // 返回空值，调用方不会继续发送 RabbitMQ。
            return null;
        }
        // 保存查询时的原状态，条件更新和状态流水的 fromStatus 均以它为准。
        Integer originalStatus = notice.getStatus();
        // 保存条件更新影响行数；只有更新一行才表示当前线程取得分发权。
        int updatedRows;
        // 状态 8 表示已有线程正在发送，只处理超过一分钟发送租约的异常遗留记录。
        if (RecipeChangeNoticeStatusEnum.MQ_DISPATCHING.getStatus().equals(originalStatus)) {
            // 条件更新会续期 update_time；未超时或已被其他线程回收时返回 0。
            updatedRows = recipeChangeNoticeMapper.tryReclaimStaleMqDispatch(noticeId,
                    RecipeChangeNoticeStatusEnum.MQ_DISPATCHING.getStatus(), operator);
        } else {
            // MES 处理中、成功和人工终止等状态不得被重新分发，避免覆盖正常业务结果。
            if (!isDispatchStartAllowed(originalStatus)) {
                // 记录拒绝原因，便于区分重复事件、并发回调和人工操作造成的跳过。
                log.info("[claimNoticeForDispatch][当前状态不允许分发] noticeId={}, notifyId={}, workshopCode={}, status={}, operationType={}, triggerType={}, operator={}",
                        notice.getId(), notice.getNotifyId(), notice.getWorkshopCode(), originalStatus,
                        operationType, triggerType, operator);
                // 无发送权，调用方停止本次分发。
                return null;
            }
            // 使用主键和原状态原子更新为状态 8，避免多个提交后事件或定时任务重复发送。
            updatedRows = recipeChangeNoticeMapper.tryStartMqDispatch(noticeId, originalStatus,
                    RecipeChangeNoticeStatusEnum.MQ_DISPATCHING.getStatus(), operator);
        }
        // 更新失败代表并发线程已抢先领取，或当前状态已不再满足领取条件。
        if (updatedRows == 0) {
            // 输出完整上下文，便于分析并发竞争和状态不符合恢复条件两类情况。
            log.info("[claimNoticeForDispatch][条件领取未命中，可能已被其他分发流程领取] noticeId={}, notifyId={}, workshopCode={}, initialStatus={}, operationType={}, triggerType={}, operator={}",
                    notice.getId(), notice.getNotifyId(), notice.getWorkshopCode(), originalStatus,
                    operationType, triggerType, operator);
            // 当前线程没有发送权，不向 MQ 投递重复消息。
            return null;
        }
        // 定时任务只有在本线程实际领取成功后才计数；状态 5、8、15 的一次真实补发均统一计为一次重试尝试。
        if (recordScheduledRetryAttempt) {
            // 状态 8 是刚刚由本事务领取得到的状态，条件更新失败意味着异常并发，必须回滚本次领取避免账实不一致。
            if (recipeChangeNoticeMapper.increaseRetryCount(noticeId, RecipeChangeNoticeStatusEnum.MQ_DISPATCHING.getStatus()) != 1) {
                throw new IllegalStateException("工艺变更通知已领取分发权但未能记录定时重试次数，通知主键：" + noticeId);
            }
            // 同步更新内存快照，后续 MQ 操作日志和异常日志中的重试次数与数据库一致。
            notice.setRetryCount(notice.getRetryCount() + 1);
        }
        // 仅首次进入状态 8 才写状态流转；超时状态 8 的回收只是续期，不重复制造状态流水。
        if (!RecipeChangeNoticeStatusEnum.MQ_DISPATCHING.getStatus().equals(originalStatus)) {
            // 状态流水与状态 8 更新在同一 REQUIRES_NEW 事务中，任一失败都会一起回滚。
            recipeChangeStatusLogMapper.insertStatusLog(buildDispatchingStatusLog(notice, triggerType, operator));
        } else {
            // 标记该次领取是异常恢复，便于排查进程宕机或发送线程卡死。
            log.warn("[claimNoticeForDispatch][恢复超时分发任务] noticeId={}, notifyId={}, workshopCode={}, status={}, operationType={}, triggerType={}, operator={}",
                    notice.getId(), notice.getNotifyId(), notice.getWorkshopCode(), originalStatus,
                    operationType, triggerType, operator);
        }
        // 调用方后续记录 MQ confirm 结果时必须以状态 8 为条件，不能覆盖 MES 已抢先领取的状态 18。
        notice.setStatus(RecipeChangeNoticeStatusEnum.MQ_DISPATCHING.getStatus());
        // 方法返回前 Spring 将提交 REQUIRES_NEW 事务；返回后的调用方才允许发送 RabbitMQ。
        return notice;
    }

    /** 判断当前状态是否允许开始一次新的 MQ 分发。 */
    private boolean isDispatchStartAllowed(Integer status) {
        return RecipeChangeNoticeStatusEnum.RECEIVED_SUCCESS.getStatus().equals(status)
                || RecipeChangeNoticeStatusEnum.SEND_FAILED.getStatus().equals(status)
                || RecipeChangeNoticeStatusEnum.MES_PROCESS_FAILED.getStatus().equals(status)
                || RecipeChangeNoticeStatusEnum.PENDING_MANUAL.getStatus().equals(status);
    }

    /** 创建首次进入 MQ 分发中状态时的状态流水。 */
    private RecipeChangeStatusLogDO buildDispatchingStatusLog(RecipeChangeNoticeDO notice, Integer triggerType, String operator) {
        // 创建新流水，避免修改历史状态记录。
        RecipeChangeStatusLogDO statusLog = new RecipeChangeStatusLogDO();
        // 关联内部主键，支持按通知追溯完整状态轨迹。
        statusLog.setNoticeId(notice.getId());
        // 冗余外部通知标识，便于不关联主表直接检索。
        statusLog.setNotifyId(notice.getNotifyId());
        // 原状态来自领取前查询结果。
        statusLog.setFromStatus(notice.getStatus());
        // 目标状态固定为 MQ 分发中。
        statusLog.setToStatus(RecipeChangeNoticeStatusEnum.MQ_DISPATCHING.getStatus());
        // 记录本次状态变更发生时间。
        statusLog.setChangeTime(LocalDateTime.now());
        // 保存本次分发由系统、定时任务或人工操作触发的来源。
        statusLog.setTriggerType(triggerType);
        // 保存执行本次领取的系统或人工操作人。
        statusLog.setCreator(operator);
        return statusLog;
    }
}
