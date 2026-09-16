package cn.jonhon.jump.framework.recipechange.consumer.listener;

import cn.jonhon.jump.framework.recipechange.consumer.client.RecipeChangeJUMPClient;
import cn.jonhon.jump.framework.recipechange.consumer.client.RecipeChangeProcessingAcquireResult;
import cn.jonhon.jump.framework.recipechange.consumer.config.RecipeChangeConsumerProperties;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeConsumptionAlarmDTO;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeDingTalkAlarmProcessor;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeDingTalkAlarmLogDTO;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeMessageDTO;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeMessageProcessor;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 工艺变更 RabbitMQ 消息监听器
 */
public class RecipeChangeMessageListener {

    /**
     * 当前监听器共用的日志记录器
     *
     * 使用 static final 保证所有监听器实例复用同一个日志记录器，运行期间不会被重新替换
     * 用于记录工艺变更消息处理和 JUMP 回调等关键异常信息
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeChangeMessageListener.class);
    /**
     * MES 工艺变更业务处理器
     *
     * 使用 final 在监听器创建时固定业务处理实现，避免运行中的消息被切换到不同处理器
     * 处理成功是当前消息确认 ACK 的唯一业务判断依据
     */
    private final RecipeChangeMessageProcessor recipeChangeMessageProcessor;

    /**
     * JUMP 服务调用客户端
     *
     * 使用 final 固定同一套 JUMP 通信配置和调用实现
     * 用于原子领取消息处理权，以及向 JUMP 回调携带令牌的 MES 处理结果
     */
    private final RecipeChangeJUMPClient recipeChangeJUMPClient;

    /**
     * MES 提供的钉钉告警处理器
     *
     * 使用 final 在监听器创建时固定钉钉发送实现
     * Starter 启动时已校验该实现存在，MES 工艺变更处理失败时直接调用此服务发送告警
     */
    private final RecipeChangeDingTalkAlarmProcessor recipeChangeDingTalkAlarmProcessor;

    /**
     * 工艺变更消费者配置
     *
     * 使用 final 固定当前监听器使用的配置对象引用
     * 用于读取队列名称、告警抑制时长等消费行为配置，避免运行期间被替换为其他配置对象
     */
    private final RecipeChangeConsumerProperties recipeChangeConsumerProperties;

    /**
     * 创建工艺变更 RabbitMQ 消息监听器
     *
     * @param recipeChangeMessageProcessor MES 工艺变更业务处理器，处理成功后才确认消息
     * @param recipeChangeJUMPClient JUMP 服务调用客户端，用于领取处理权和结果回调
     * @param recipeChangeDingTalkAlarmProcessor MES 提供的钉钉告警处理器，启动前已校验必须存在
     * @param recipeChangeConsumerProperties 消费端配置，提供队列和告警抑制等参数
     */
    public RecipeChangeMessageListener(RecipeChangeMessageProcessor recipeChangeMessageProcessor,
                                       RecipeChangeJUMPClient recipeChangeJUMPClient,
                                       RecipeChangeDingTalkAlarmProcessor recipeChangeDingTalkAlarmProcessor,
                                       RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        // 固定 MES 业务处理器，保证监听器生命周期内使用一致的处理逻辑
        this.recipeChangeMessageProcessor = recipeChangeMessageProcessor;
        // 固定 JUMP 客户端，保证状态查询和回调使用一致的通信实现
        this.recipeChangeJUMPClient = recipeChangeJUMPClient;
        // 固定钉钉告警处理器，车间处理失败时直接调用该 MES 实现
        this.recipeChangeDingTalkAlarmProcessor = recipeChangeDingTalkAlarmProcessor;
        // 固定消费者配置引用，供消息消费和告警抑制逻辑读取
        this.recipeChangeConsumerProperties = recipeChangeConsumerProperties;
    }

    /**
     * 消费单条工艺变更消息
     * 成功或人工终止的消息直接确认；正在被其他消费者处理的重复消息进入延迟队列
     * MES 业务成功后立即确认消息，JUMP 成功回调失败仅记录日志且不影响确认结果
     * 仅 MES 业务处理失败才回调失败、发送告警并重新入队
     *
     * @param message     JUMP 发布的工艺变更消息
     * @param channel     RabbitMQ 通道，用于手动确认或重新入队
     * @param deliveryTag 本条消息的投递标识
     * @throws IOException 手动确认或重新入队失败时抛出
     */
    @RabbitListener(queues = "#{@recipeChangeConsumerQueueNames}", containerFactory = "recipeChangeRabbitListenerContainerFactory")
    public void consume(RecipeChangeMessageDTO message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        RecipeChangeProcessingAcquireResult processingAcquireResult;
        try {
            // 领取处理权，确保人工重发和延迟回流的重复消息不会并发进入 MES 业务
            processingAcquireResult = recipeChangeJUMPClient.acquireProcessing(message.getNotifyId(), message.getWorkshopCode());
        } catch (Exception exception) {
            // 状态查询失败时无法安全判断是否应跳过业务，requeue=false 使 RabbitMQ 按主队列死信配置将消息转入延迟重试队列，而非立即重新投递，但不作为车间处理失败告警
            channel.basicNack(deliveryTag, false, false);
            LOGGER.error("工艺变更消息处理权领取失败，消息重新入队，通知ID：{}，车间：{}", message.getNotifyId(), message.getWorkshopCode(), exception);
            return;
        }
        if (processingAcquireResult.isProcessingNotRequired()) {
            // 已成功或已标记完成的消息无需 MES 处理，直接确认以从队列移除该消息
            channel.basicAck(deliveryTag, false);
            LOGGER.warn("工艺变更消息无需处理，直接确认，通知ID：{}，车间：{}", message.getNotifyId(), message.getWorkshopCode());
            return;
        }
        if (!processingAcquireResult.isAcquired()) {
            // 已有消费者持有处理权；当前重复消息进入延迟队列，不能执行 MES 业务或触发失败告警
            channel.basicNack(deliveryTag, false, false);
            LOGGER.info("工艺变更消息正在由其他消费者处理，延迟重试，通知ID：{}，车间：{}", message.getNotifyId(), message.getWorkshopCode());
            return;
        }
        // 令牌必须贯穿成功和失败回调，确保只有已领取的消费者能变更 JUMP 状态
        String processingToken = processingAcquireResult.getProcessingToken();
        try {
            // 交由 MES 项目实现的业务扩展点执行本地工艺变更处理
            recipeChangeMessageProcessor.process(message);
        } catch (Exception exception) {
            // 仅此分支代表真实的车间处理失败，需要写失败回调、发送告警并转入延迟重试队列
            callbackFailureSafely(message, processingToken, exception);
            try {
                // 钉钉告警发送异常不能阻止失败消息转入延迟重试队列
                sendAlarm(message, exception);
            } catch (Exception alarmException) {
                LOGGER.error("工艺变更消息消费失败钉钉告警发送异常，通知ID：{}，车间：{}", message.getNotifyId(), message.getWorkshopCode(), alarmException);
            }
            // requeue=false 使 RabbitMQ 按主队列死信配置将消息转入延迟重试队列，而非立即重新投递
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        // 是否确认消息只取决于 MES 本地业务是否成功，成功后立即确认消息
        channel.basicAck(deliveryTag, false);
        try {
            // 确认后异步状态同步失败不能影响已成功的 MES 业务；令牌校验可阻止旧回调覆盖新处理
            recipeChangeJUMPClient.callbackProcessResult(message, processingToken, true, null);
        } catch (Exception exception) {
            LOGGER.error("工艺变更消息处理成功回调 JUMP 失败，通知ID：{}，车间：{}", message.getNotifyId(), message.getWorkshopCode(), exception);
        }
    }

    /**
     * 尝试回调 MES 处理失败结果，回调异常不影响原消息重新入队
     */
    private void callbackFailureSafely(RecipeChangeMessageDTO message, String processingToken, Exception exception) {
        try {
            recipeChangeJUMPClient.callbackProcessResult(message, processingToken, false, getErrorMessage(exception));
        } catch (Exception ignored) {
            // 保留原始消费异常作为告警内容，回调失败本身不能导致消息被确认
        }
    }

    /**
     * 发送本次车间处理失败的钉钉告警，并调用 MES 项目提供的钉钉发送实现
     *
     * 每次车间处理失败都是独立失败事件，不进行告警抑制
     * 消息经过延迟队列再次投递后若仍失败，也会再次发送钉钉告警
     */
    private void sendAlarm(RecipeChangeMessageDTO message, Exception exception) {
        RecipeChangeConsumptionAlarmDTO alarm = buildAlarm(message, exception);
        boolean sendSuccess = false;
        String sendErrorMsg = null;
        try {
            // 启动阶段已确保处理器存在，调用 MES 实现将完整告警内容发送至钉钉
            recipeChangeDingTalkAlarmProcessor.sendConsumptionFailureAlarm(alarm);
            sendSuccess = true;
        } catch (Exception alarmException) {
            // 保存钉钉发送异常并继续上报日志，随后将原异常抛给调用方记录本地错误日志
            sendErrorMsg = getErrorMessage(alarmException);
            throw alarmException;
        } finally {
            // 无论钉钉发送成功或失败，都向 JUMP 上报完整请求和发送结果用于操作日志审计
            recordDingTalkAlarmResultSafely(alarm, sendSuccess, sendErrorMsg);
        }
    }

    /**
     * 组装交给钉钉实现类的完整消费失败告警内容
     */
    private RecipeChangeConsumptionAlarmDTO buildAlarm(RecipeChangeMessageDTO message, Exception exception) {
        RecipeChangeConsumptionAlarmDTO alarm = new RecipeChangeConsumptionAlarmDTO();
        alarm.setTitle("工艺变更消息消费失败告警");
        alarm.setNotifyId(message.getNotifyId());
        alarm.setWorkshopCode(message.getWorkshopCode());
        // 按消息所属车间计算实际消费队列，避免多车间 MES 的告警始终显示某个默认队列。
        alarm.setQueueName(recipeChangeConsumerProperties.getQueueName(message.getWorkshopCode()));
        // 将当前失败时间转为 ISO-8601 字符串，保证告警 DTO 可以直接参与 JSON 传输
        alarm.setFailureTime(LocalDateTime.now().toString());
        alarm.setErrorMsg(getErrorMessage(exception));
        // 组装钉钉处理器实际使用的完整告警文本，确保后续操作日志可还原当时的告警内容
        alarm.setAlarmContent(buildAlarmContent(alarm));
        return alarm;
    }

    /**
     * 按统一模板组装实际发送给钉钉的完整告警文本
     */
    private String buildAlarmContent(RecipeChangeConsumptionAlarmDTO alarm) {
        return alarm.getTitle() + "\n"
                + "MPM通知ID：" + alarm.getNotifyId() + "\n"
                + "车间：" + alarm.getWorkshopCode() + "\n"
                + "队列：" + alarm.getQueueName() + "\n"
                + "失败时间：" + alarm.getFailureTime() + "\n"
                + "失败原因：" + alarm.getErrorMsg();
    }

    /**
     * 将钉钉告警发送结果上报至 JUMP，日志上报失败不得影响消息进入延迟重试队列
     */
    private void recordDingTalkAlarmResultSafely(RecipeChangeConsumptionAlarmDTO alarm, boolean sendSuccess, String sendErrorMsg) {
        RecipeChangeDingTalkAlarmLogDTO dingTalkAlarmLog = new RecipeChangeDingTalkAlarmLogDTO();
        dingTalkAlarmLog.setNotifyId(alarm.getNotifyId());
        dingTalkAlarmLog.setWorkshopCode(alarm.getWorkshopCode());
        dingTalkAlarmLog.setTitle(alarm.getTitle());
        dingTalkAlarmLog.setAlarmContent(alarm.getAlarmContent());
        dingTalkAlarmLog.setQueueName(alarm.getQueueName());
        // 告警 DTO 已保存 ISO-8601 字符串，直接透传以保留钉钉告警中的原始失败时间
        dingTalkAlarmLog.setFailureTime(alarm.getFailureTime());
        dingTalkAlarmLog.setProcessErrorMsg(alarm.getErrorMsg());
        dingTalkAlarmLog.setSendSuccess(sendSuccess);
        dingTalkAlarmLog.setSendErrorMsg(sendErrorMsg);
        try {
            recipeChangeJUMPClient.recordDingTalkAlarmResult(dingTalkAlarmLog);
        } catch (Exception exception) {
            LOGGER.error("工艺变更钉钉告警发送结果写入 JUMP 失败，通知ID：{}，车间：{}", alarm.getNotifyId(), alarm.getWorkshopCode(), exception);
        }
    }

    /**
     * 获取适合写入回调和告警的异常说明
     */
    private String getErrorMessage(Exception exception) {
        return exception.getMessage() != null ? exception.getMessage() : exception.getClass().getName();
    }

}
