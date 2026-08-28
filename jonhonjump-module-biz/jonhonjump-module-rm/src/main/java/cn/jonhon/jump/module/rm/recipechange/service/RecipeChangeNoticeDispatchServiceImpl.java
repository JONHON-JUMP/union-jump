package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.module.rm.recipechange.config.RecipeChangeRabbitMQProperties;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeNoticeDO;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeOperationLogDO;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeStatusLogDO;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeNoticeMapper;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeOperationLogMapper;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeStatusLogMapper;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeNoticeStatusEnum;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeOperationResultEnum;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeOperationTypeEnum;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeTriggerTypeEnum;
import cn.jonhon.jump.module.rm.recipechange.mq.message.RecipeChangeMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 工艺变更通知 RabbitMQ 分发服务实现
 */
@Service
public class RecipeChangeNoticeDispatchServiceImpl implements RecipeChangeNoticeDispatchService {

    private static final Logger log = LoggerFactory.getLogger(RecipeChangeNoticeDispatchServiceImpl.class);

    /**
     * JUMP 系统标识，用于分发链路创建的日志
     */
    private static final String JUMP = "JUMP";
    /**
     * 等待 RabbitMQ 发布确认的最长时间，单位为秒
     */
    private static final long CONFIRM_TIMEOUT_SECONDS = 10L;

    /**
     * 工艺变更通知主表数据访问对象
     */
    @Resource
    private RecipeChangeNoticeMapper recipeChangeNoticeMapper;
    /**
     * 工艺变更操作流水表数据访问对象
     */
    @Resource
    private RecipeChangeOperationLogMapper recipeChangeOperationLogMapper;
    /**
     * 工艺变更状态流水表数据访问对象
     */
    @Resource
    private RecipeChangeStatusLogMapper recipeChangeStatusLogMapper;
    /**
     * RabbitMQ 消息发送模板
     */
    @Resource
    private RabbitTemplate recipeChangeRabbitTemplate;

    /**
     * RabbitMQ 管理对象，用于声明 Exchange、队列和绑定
     */
    @Resource
    private RabbitAdmin recipeChangeAmqpAdmin;
    /**
     * 工艺变更直连交换机
     */
    @Resource
    private DirectExchange recipeChangeDirectExchange;
    /**
     * 工艺变更延迟重试直连交换机
     */
    @Resource
    private DirectExchange recipeChangeRetryDirectExchange;
    /**
     * 工艺变更 RabbitMQ 配置属性
     */
    @Resource
    private RecipeChangeRabbitMQProperties recipeChangeRabbitMQProperties;
    /**
     * JSON 序列化工具
     */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 将工艺变更通知发送到目标车间的 RabbitMQ 队列
     * 仅在 Broker 发布确认成功后标记为已发送 MQ，异常、未确认和无法路由均标记为发送失败
     *
     * @param noticeId 工艺变更通知主键
     */
    @Override
    public void dispatchRecipeChangeNotice(Long noticeId) {
        dispatchRecipeChangeNotice(noticeId, RecipeChangeOperationTypeEnum.MQ_SEND.getType(), RecipeChangeTriggerTypeEnum.SYSTEM.getType(), JUMP, false);
    }

    /**
     * 按调用场景发送通知并记录对应的状态和操作日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchRecipeChangeNotice(Long noticeId, Integer operationType, Integer triggerType, String operator, boolean increaseRetryCount) {
        // 从通知主表读取需要发送的原始内容、目标车间和当前状态
        RecipeChangeNoticeDO notice = recipeChangeNoticeMapper.selectById(noticeId);
        // 通知可能被人工删除或不存在，此时没有可分发的数据，直接结束
        if (notice == null) {
            return;
        }
        // 将数据库记录转换为 MES 约定的 MQ 消息体，不直接暴露数据库字段
        RecipeChangeMessage message = buildRecipeChangeMessage(notice);
        try {
            // CorrelationData 为本次发送创建唯一关联对象，用于等待 Broker 的发布确认
            CorrelationData correlationData = new CorrelationData(String.valueOf(noticeId));
            // 直连交换机通过精确匹配路由键定位目标车间已绑定的队列
            String routingKey = buildRoutingKey(notice.getWorkshopCode());
            // 发送前确保目标车间的 durable 队列和路由绑定已存在
            ensureQueueAndBinding(notice.getWorkshopCode(), routingKey);
            // 将消息发布到 Exchange，mandatory 配置会让无法匹配队列的消息返回发送端
            recipeChangeRabbitTemplate.convertAndSend(recipeChangeDirectExchange.getName(), routingKey, message, correlationData);
            // 阻塞等待本条消息的 Broker 确认，超过配置时限会抛出超时异常并进入失败处理
            CorrelationData.Confirm confirm = correlationData.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            // ack 为 false 代表 Broker 未接收消息或消息被退回，不能标记为已发送
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ 发布确认失败: " + confirm.getReason());
            }
            // 仅在收到确认后更新主状态及全链路日志
            recordSendSuccess(notice, message, routingKey, operationType, triggerType, operator);
        } catch (Exception exception) {
            log.error("工艺变更 RabbitMQ 分发失败，noticeId={}, notifyId={}, workshopCode={}, operationType={}, triggerType={}, retryCount={}",
                    notice.getId(), notice.getNotifyId(), notice.getWorkshopCode(), operationType, triggerType,
                    notice.getRetryCount(), exception);
            // 发送异常、超时、未确认和无法路由都会在此统一落为发送失败
            recordSendFailure(notice, message, exception, operationType, triggerType, operator, increaseRetryCount);
        }
    }

    /**
     * 按车间编码构造 RabbitMQ 路由键
     *
     * @param workshopCode 目标车间编码
     * @return 目标车间对应的路由键
     */
    private String buildRoutingKey(String workshopCode) {
        // 例如前缀 RECIPE_CHANGE_ROUTE_ 与车间 5600 拼接为 RECIPE_CHANGE_ROUTE_5600
        return recipeChangeRabbitMQProperties.getRoutingKeyPrefix() + workshopCode;
    }

    /**
     * 确保目标车间主队列、延迟重试队列及其 Exchange 路由绑定存在
     *
     * RabbitMQ 重复声明同名称且属性一致的队列和绑定是幂等操作
     * 因此每次发送前调用无需先查询，也能覆盖首次接入车间的场景
     *
     * @param workshopCode 目标车间编码
     * @param routingKey   目标车间对应的路由键
     */
    private void ensureQueueAndBinding(String workshopCode, String routingKey) {
        // 根据队列前缀和车间编码生成车间专属队列名称
        String queueName = recipeChangeRabbitMQProperties.getQueueNamePrefix() + workshopCode;
        // 根据车间版本覆盖配置生成当前生效的延迟重试队列名称；未覆盖时仍为默认 V1 队列名称。
        String retryQueueName = recipeChangeRabbitMQProperties.getRetryQueueName(workshopCode);
        // 根据延迟重试路由键前缀和车间编码生成主队列死信时使用的路由键
        String retryRoutingKey = recipeChangeRabbitMQProperties.getRetryRoutingKeyPrefix() + workshopCode;
        // 首次为该车间创建队列前，确保正常交换机和延迟重试交换机已在专用 RabbitMQ Broker 中声明
        recipeChangeAmqpAdmin.declareExchange(recipeChangeDirectExchange);
        recipeChangeAmqpAdmin.declareExchange(recipeChangeRetryDirectExchange);
        // 主队列拒绝失败消息时，将消息死信至延迟重试交换机和本车间延迟路由键
        Queue queue = QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", recipeChangeRetryDirectExchange.getName())
                .withArgument("x-dead-letter-routing-key", retryRoutingKey)
                .build();
        // 延迟重试队列中的消息等待指定时长后，死信回正常交换机和本车间主队列路由键
        Queue retryQueue = QueueBuilder.durable(retryQueueName)
                .withArgument("x-message-ttl", recipeChangeRabbitMQProperties.getRetryDelayMillis(workshopCode))
                .withArgument("x-dead-letter-exchange", recipeChangeDirectExchange.getName())
                .withArgument("x-dead-letter-routing-key", routingKey)
                .build();
        // 声明主队列，队列首次不存在时创建，已存在且属性一致时不产生副作用
        recipeChangeAmqpAdmin.declareQueue(queue);
        // 声明当前生效版本的延迟重试队列，消息按该车间配置的 TTL 到期后回流主队列。
        recipeChangeAmqpAdmin.declareQueue(retryQueue);
        // 绑定将队列、直连 Exchange 和精确路由键关联起来
        // 只有 routingKey 完全匹配时，Exchange 才会将消息路由至该车间队列
        recipeChangeAmqpAdmin.declareBinding(BindingBuilder.bind(queue).to(recipeChangeDirectExchange).with(routingKey));
        // 每个车间的重试路由键只绑定当前生效版本的一个延迟队列，避免失败消息被复制到多个版本队列。
        recipeChangeAmqpAdmin.declareBinding(BindingBuilder.bind(retryQueue).to(recipeChangeRetryDirectExchange).with(retryRoutingKey));
    }

    /**
     * 将通知主记录转换为发送给 MES 的消息体
     *
     * @param notice 工艺变更通知主记录
     * @return 发送给 MES 的工艺变更消息
     */
    private RecipeChangeMessage buildRecipeChangeMessage(RecipeChangeNoticeDO notice) {
        // notifyId 供 MES 幂等和回调关联，workshopCode 供 MES 识别自身车间，changeContent 为工艺变更正文
        return new RecipeChangeMessage(notice.getNotifyId(), notice.getWorkshopCode(), notice.getChangeContent());
    }

    /**
     * 记录 RabbitMQ 发送成功后的主状态、状态流水和操作流水
     *
     * @param notice     工艺变更通知主记录
     * @param message    已发送的工艺变更消息
     * @param routingKey 本次发送使用的路由键
     */
    private void recordSendSuccess(RecipeChangeNoticeDO notice, RecipeChangeMessage message, String routingKey, Integer operationType, Integer triggerType, String operator) {
        // 用原状态作为更新条件，避免并发分发或人工操作覆盖已经变化的状态
        int updatedRows = recipeChangeNoticeMapper.updateSendSuccess(notice.getId(), notice.getStatus(), RecipeChangeNoticeStatusEnum.SENT_MQ.getStatus(), operator);
        // 未更新说明状态已被其他流程改变，本次不重复写入成功流水
        if (updatedRows == 0) {
            return;
        }
        // 统一使用同一个时间点，确保状态流水和操作流水的时间可对应
        LocalDateTime now = LocalDateTime.now();
        // 记录从发送前状态到已发送 MQ 状态的系统自动流转
        recipeChangeStatusLogMapper.insertStatusLog(buildStatusLog(notice, RecipeChangeNoticeStatusEnum.SENT_MQ, now, triggerType, operator));
        // 记录发送的消息体、路由键和成功结果，便于排查消息投递过程
        recipeChangeOperationLogMapper.insertOperationLog(buildOperationLog(notice, message, routingKey, RecipeChangeOperationResultEnum.SUCCESS, null, now, operationType, operator));
    }

    /**
     * 记录 RabbitMQ 发送失败后的主状态、状态流水和操作流水
     *
     * @param notice    工艺变更通知主记录
     * @param message   未成功发送的工艺变更消息
     * @param exception 发送过程中抛出的异常
     */
    private void recordSendFailure(RecipeChangeNoticeDO notice, RecipeChangeMessage message, Exception exception, Integer operationType, Integer triggerType, String operator, boolean increaseRetryCount) {
        // 将异常转换为可保存、可查询的失败原因
        String errorMsg = getErrorMessage(exception);
        // 用原状态作为更新条件，避免覆盖其他流程已经处理完成的通知
        int updatedRows = recipeChangeNoticeMapper.updateSendFailure(notice.getId(), notice.getStatus(),
                RecipeChangeNoticeStatusEnum.SEND_FAILED.getStatus(), errorMsg, increaseRetryCount, operator);
        // 未更新说明状态已被其他流程改变，本次不重复写入失败流水
        if (updatedRows == 0) {
            return;
        }

        // 统一使用同一个时间点，确保失败状态和失败操作记录可关联
        LocalDateTime now = LocalDateTime.now();
        // 记录从发送前状态到发送失败状态的系统自动流转
        recipeChangeStatusLogMapper.insertStatusLog(buildStatusLog(notice, RecipeChangeNoticeStatusEnum.SEND_FAILED, now, triggerType, operator));
        // 失败操作日志仍保存原始消息和目标路由键，供后续重试和问题排查使用
        recipeChangeOperationLogMapper.insertOperationLog(buildOperationLog(notice, message, buildRoutingKey(notice.getWorkshopCode()), RecipeChangeOperationResultEnum.FAILURE, errorMsg, now, operationType, operator));
    }

    /**
     * 创建 RabbitMQ 发送后的状态流转记录
     *
     * @param notice       工艺变更通知主记录
     * @param targetStatus 发送完成后的目标状态
     * @param changeTime   状态变更时间
     * @return 待写入的状态流水
     */
    private RecipeChangeStatusLogDO buildStatusLog(RecipeChangeNoticeDO notice, RecipeChangeNoticeStatusEnum targetStatus,
                                                   LocalDateTime changeTime, Integer triggerType, String creator) {
        // 新建一条独立流水，不修改历史流水
        RecipeChangeStatusLogDO statusLog = new RecipeChangeStatusLogDO();
        // 关联通知主键，供按通知查询全部状态轨迹
        statusLog.setNoticeId(notice.getId());
        // 冗余保存外部通知标识，便于不关联主表时直接检索
        statusLog.setNotifyId(notice.getNotifyId());
        // 保存发送前状态，形成完整的状态变更方向
        statusLog.setFromStatus(notice.getStatus());
        // 保存发送后的目标状态，例如已发送 MQ 或发送失败
        statusLog.setToStatus(targetStatus.getStatus());
        // 保存本次状态变更的统一时间点
        statusLog.setChangeTime(changeTime);
        // 标识该状态由系统自动发送流程触发，不是人工处理
        statusLog.setTriggerType(triggerType);
        // 分发流程由 JUMP 自身执行，因此创建者为 JUMP
        statusLog.setCreator(creator);
        return statusLog;
    }

    /**
     * 创建 RabbitMQ 发送操作流水
     *
     * @param notice        工艺变更通知主记录
     * @param message       本次发送的工艺变更消息
     * @param routingKey    本次发送使用的路由键
     * @param result        RabbitMQ 发送结果
     * @param errorMsg      RabbitMQ 发送失败原因
     * @param operationTime 操作发生时间
     * @return 待写入的操作流水
     */
    private RecipeChangeOperationLogDO buildOperationLog(RecipeChangeNoticeDO notice, RecipeChangeMessage message,
                                                         String routingKey, RecipeChangeOperationResultEnum result,
                                                         String errorMsg, LocalDateTime operationTime,
                                                         Integer operationType, String operator) {
        // 新建一条独立操作流水，记录一次 MQ 发送尝试
        RecipeChangeOperationLogDO operationLog = new RecipeChangeOperationLogDO();
        // 关联通知主键，供按通知查询全链路操作记录
        operationLog.setNoticeId(notice.getId());
        // 冗余保存外部通知标识，便于按 MPM 通知编号检索
        operationLog.setNotifyId(notice.getNotifyId());
        // 保存本次发送的目标车间，便于定位对应 MES
        operationLog.setWorkshopCode(notice.getWorkshopCode());
        // 标识操作类型为 JUMP 向 RabbitMQ 发送消息
        operationLog.setOperationType(operationType);
        // 保存本次 MQ 发送尝试的发生时间
        operationLog.setOperationTime(operationTime);
        // 操作执行者为 JUMP 系统，不是外部 MPM 或人工用户
        operationLog.setOperator(operator);
        // 分发流程创建该日志，因此创建者为 JUMP
        operationLog.setCreator(operator);
        // 保存成功或失败结果编码
        operationLog.setOperationResult(result.getResult());
        // 失败时保存异常原因，成功时为 null
        operationLog.setErrorMsg(errorMsg);
        // 将实际发送给 RabbitMQ 的消息转换为 JSON 快照
        operationLog.setRequestParams(objectMapper.valueToTree(message));
        // 将路由键、发送结果和失败原因转换为 JSON 快照
        operationLog.setResponseParams(buildResponseParams(result, routingKey, errorMsg));
        return operationLog;
    }

    /**
     * 构造 RabbitMQ 发送结果 JSON 快照
     *
     * @param result     RabbitMQ 发送结果
     * @param routingKey 本次发送使用的路由键
     * @param errorMsg   RabbitMQ 发送失败原因
     * @return 发送结果 JSON 快照
     */
    private JsonNode buildResponseParams(RecipeChangeOperationResultEnum result, String routingKey, String errorMsg) {
        // 创建 JSON 对象作为操作日志的响应快照
        com.fasterxml.jackson.databind.node.ObjectNode responseParams = objectMapper.createObjectNode();
        // 保存本次发送实际使用的路由键，用于确认消息应进入哪个车间队列
        responseParams.put("routingKey", routingKey);
        // 保存发送结果中文名称，便于管理页面直接展示
        responseParams.put("result", result.getName());
        // 仅失败时写入错误信息，成功记录保持精简
        if (errorMsg != null) {
            responseParams.put("errorMsg", errorMsg);
        }
        return responseParams;
    }

    /**
     * 获取用于持久化的发送失败原因
     *
     * @param exception RabbitMQ 发送过程中抛出的异常
     * @return 优先使用异常消息，异常消息为空时使用异常类名
     */
    private String getErrorMessage(Exception exception) {
        // 优先记录异常自带的业务原因，消息为空时退回到异常类名以保证错误信息不为空
        return exception.getMessage() != null ? exception.getMessage() : exception.getClass().getName();
    }

}
