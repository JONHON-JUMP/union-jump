package cn.jonhon.jump.framework.recipechange.consumer.config;

import cn.jonhon.jump.framework.recipechange.consumer.client.RecipeChangeJUMPClient;
import cn.jonhon.jump.framework.recipechange.consumer.client.RecipeChangeJUMPHttpClient;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeDingTalkAlarmProcessor;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeMessageProcessor;
import cn.jonhon.jump.framework.recipechange.consumer.listener.RecipeChangeMessageListener;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 工艺变更消息消费者 Starter 自动装配
 * 当 MES 应用通过配置显式启用 Starter 后，本类负责创建专用 RabbitMQ 连接工厂、JSON 消息转换器、手动确认监听容器、JUMP HTTP 客户端和消费监听器
 * Starter 只使用 jonhonjump.recipe-change.consumer.rabbitmq 创建的专用连接，不读取或修改 MES 项目的 spring.rabbitmq 连接配置
 */
@AutoConfiguration
@ConditionalOnClass(SimpleRabbitListenerContainerFactory.class)
@ConditionalOnProperty(prefix = "jonhonjump.recipe-change.consumer", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RecipeChangeConsumerProperties.class)
public class RecipeChangeConsumerAutoConfiguration {

    /**
     * 显式注册工艺变更消费者监听的全部车间专属队列名称
     *
     * @param recipeChangeConsumerProperties Starter 配置，提供队列前缀和车间编码列表
     * @return 当前 MES 需要监听的全部 RabbitMQ 主队列名称
     */
    @Bean("recipeChangeConsumerQueueNames")
    public String[] recipeChangeConsumerQueueNames(RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        // 校验车间列表及拓扑参数，避免监听到错误队列或遗漏某个配置车间。
        recipeChangeConsumerProperties.validateRabbitInfrastructureProperties();
        // 通过显式命名 Bean 向 @RabbitListener 提供所有队列名；列表中每个车间均会被同一个 MES 实例消费。
        return recipeChangeConsumerProperties.getWorkshopCodes().stream()
                .map(recipeChangeConsumerProperties::getQueueName)
                .toArray(String[]::new);
    }

    /**
     * 创建工艺变更消费者专用 RabbitMQ 连接工厂
     *
     * @param recipeChangeConsumerProperties Starter 配置，包含专用 RabbitMQ 连接参数
     * @return 仅供工艺变更消费者使用的 RabbitMQ 连接工厂
     */
    @Bean("recipeChangeConnectionFactory")
    @ConditionalOnMissingBean(name = "recipeChangeConnectionFactory")
    public ConnectionFactory recipeChangeConnectionFactory(RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        // 启动前显式校验全部专用连接参数，避免遗漏配置时连接到错误的 RabbitMQ Broker
        recipeChangeConsumerProperties.validateRabbitMQProperties();
        // 读取 jonhonjump 下的专用连接配置，不使用 MES 全局 spring.rabbitmq 配置
        RecipeChangeConsumerProperties.RabbitMQProperties rabbitmqProperties = recipeChangeConsumerProperties.getRabbitmq();
        // 创建带连接复用能力的连接工厂，减少同一车间持续消费时重复建立 TCP 连接的开销
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        // 设置专用 RabbitMQ 服务地址
        connectionFactory.setHost(rabbitmqProperties.getHost());
        // 设置专用 RabbitMQ 服务端口
        connectionFactory.setPort(rabbitmqProperties.getPort());
        // 设置专用 RabbitMQ 登录用户名
        connectionFactory.setUsername(rabbitmqProperties.getUsername());
        // 设置专用 RabbitMQ 登录密码
        connectionFactory.setPassword(rabbitmqProperties.getPassword());
        // 设置专用 RabbitMQ 虚拟主机
        connectionFactory.setVirtualHost(rabbitmqProperties.getVirtualHost());
        // 返回命名连接工厂，供工艺变更监听容器通过限定名称注入
        return connectionFactory;
    }

    /**
     * 创建仅用于工艺变更 Exchange、队列和绑定声明的 RabbitAdmin
     *
     * @param recipeChangeConnectionFactory 工艺变更消费者专用 RabbitMQ 连接工厂
     * @return 工艺变更专用 RabbitMQ 管理对象
     */
    @Bean("recipeChangeAmqpAdmin")
    public RabbitAdmin recipeChangeAmqpAdmin(@Qualifier("recipeChangeConnectionFactory") ConnectionFactory recipeChangeConnectionFactory) {
        // 使用工艺变更专用连接声明消息基础设施，不会在 MES 其他业务 RabbitMQ Broker 上创建资源
        return new RabbitAdmin(recipeChangeConnectionFactory);
    }

    /**
     * 在 MES 启动阶段显式声明工艺变更主队列、延迟重试队列、交换机及绑定
     *
     * @param recipeChangeAmqpAdmin 工艺变更专用 RabbitMQ 管理对象
     * @param recipeChangeConsumerProperties Starter 配置，提供车间和消息拓扑参数
     * @return Spring 初始化回调，确保监听容器启动前 RabbitMQ 基础设施已存在
     */
    @Bean
    public InitializingBean recipeChangeRabbitInfrastructureInitializer(@Qualifier("recipeChangeAmqpAdmin") RabbitAdmin recipeChangeAmqpAdmin, RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        return () -> declareRabbitInfrastructure(recipeChangeAmqpAdmin, recipeChangeConsumerProperties);
    }

    /**
     * 按全部配置车间声明与 JUMP 完全一致的 RabbitMQ 主消费和延迟重试拓扑
     *
     * @param recipeChangeAmqpAdmin 工艺变更专用 RabbitMQ 管理对象
     * @param recipeChangeConsumerProperties Starter 配置，提供交换机、队列和延迟参数
     */
    private void declareRabbitInfrastructure(RabbitAdmin recipeChangeAmqpAdmin, RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        // 声明前校验所有基础设施参数，遗漏配置时阻止监听器在错误拓扑上启动
        recipeChangeConsumerProperties.validateRabbitInfrastructureProperties();
        // 按配置创建持久化主交换机，JUMP 向该交换机发布工艺变更消息
        DirectExchange recipeChangeExchange = new DirectExchange(recipeChangeConsumerProperties.getExchange(), true, false);
        // 按配置创建持久化延迟重试交换机，主队列失败消息通过死信投递到该交换机
        DirectExchange recipeChangeRetryExchange = new DirectExchange(recipeChangeConsumerProperties.getRetryExchange(), true, false);
        // 先声明两个共用交换机，使 MES 可在 JUMP 首次分发前安全启动。
        recipeChangeAmqpAdmin.declareExchange(recipeChangeExchange);
        recipeChangeAmqpAdmin.declareExchange(recipeChangeRetryExchange);
        // 每个车间拥有独立主队列、延迟队列及路由键，消息失败后只会回流本车间主队列。
        for (String workshopCode : recipeChangeConsumerProperties.getWorkshopCodes()) {
            // 拼接当前车间主消费队列名称。
            String queueName = recipeChangeConsumerProperties.getQueueName(workshopCode);
            // 按车间覆盖配置取得当前生效的版本化延迟重试队列名称。
            String retryQueueName = recipeChangeConsumerProperties.getRetryQueueName(workshopCode);
            // 拼接主交换机投递至当前车间主队列的路由键。
            String routingKey = recipeChangeConsumerProperties.getRoutingKeyPrefix() + workshopCode;
            // 拼接主队列死信投递至当前车间延迟重试队列的路由键。
            String retryRoutingKey = recipeChangeConsumerProperties.getRetryRoutingKeyPrefix() + workshopCode;
            // 主队列拒绝消息时，Broker 将消息转入本车间延迟重试交换机而非立即重新投递。
            Queue queue = QueueBuilder.durable(queueName)
                    .withArgument("x-dead-letter-exchange", recipeChangeRetryExchange.getName())
                    .withArgument("x-dead-letter-routing-key", retryRoutingKey)
                    .build();
            // 将本车间生效 TTL 写入延迟队列；JUMP 与 Starter 对同一车间的名称和 TTL 必须一致。
            Queue retryQueue = QueueBuilder.durable(retryQueueName)
                    .withArgument("x-dead-letter-exchange", recipeChangeExchange.getName())
                    .withArgument("x-dead-letter-routing-key", routingKey)
                    .withArgument("x-message-ttl", recipeChangeConsumerProperties.getRetryDelayMillis(workshopCode))
                    .build();
            // 显式声明本车间主队列和延迟重试队列，重复声明且属性一致时是幂等操作。
            recipeChangeAmqpAdmin.declareQueue(queue);
            recipeChangeAmqpAdmin.declareQueue(retryQueue);
            // 绑定主队列，确保 JUMP 主交换机的本车间消息可以路由到 MES 监听队列。
            recipeChangeAmqpAdmin.declareBinding(BindingBuilder.bind(queue).to(recipeChangeExchange).with(routingKey));
            // 每个重试路由键只绑定当前生效的一个版本化延迟队列，避免同一失败消息被重复延迟。
            recipeChangeAmqpAdmin.declareBinding(BindingBuilder.bind(retryQueue).to(recipeChangeRetryExchange).with(retryRoutingKey));
        }
    }

    /**
     * 创建工艺变更消费者专用 JSON 消息转换器
     *
     * @return Starter 默认 JSON 消息转换器
     */
    @Bean("recipeChangeMessageConverter")
    @ConditionalOnMissingBean(name = "recipeChangeMessageConverter")
    public MessageConverter recipeChangeMessageConverter() {
        // 创建命名 Jackson 转换器，避免使用 MES 其他 RabbitMQ 业务的全局消息转换器
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建专用监听容器工厂并启用手动确认模式
     * 失败消息由监听器显式 basicNack 重新入队，避免框架自动确认造成消息丢失
     *
     * @param recipeChangeConnectionFactory 工艺变更消费者专用 RabbitMQ 连接工厂
     * @param recipeChangeMessageConverter 工艺变更消费者专用 JSON 消息转换器
     * @return 仅供工艺变更消息监听器使用的容器工厂
     */
    @Bean("recipeChangeRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory recipeChangeRabbitListenerContainerFactory(
            @Qualifier("recipeChangeConnectionFactory") ConnectionFactory recipeChangeConnectionFactory,
            @Qualifier("recipeChangeMessageConverter") MessageConverter recipeChangeMessageConverter) {
        // 创建本 Starter 专用监听容器工厂，避免改变 MES 现有其他 RabbitMQ 监听器的确认策略
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // 注入专用连接工厂，监听器只会建立到工艺变更配置指定的 RabbitMQ Broker
        factory.setConnectionFactory(recipeChangeConnectionFactory);
        // 强制手动确认，只有监听器明确 basicAck 才会从队列移除消息
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // 未捕获异常时默认重新入队，作为监听器显式 nack 逻辑之外的最后保护
        factory.setDefaultRequeueRejected(true);
        // 注入专用 JSON 转换器，确保消息体稳定转换为 RecipeChangeMessageDTO
        factory.setMessageConverter(recipeChangeMessageConverter);
        // 返回配置完成的专用监听容器工厂
        return factory;
    }

    /**
     * 创建调用 JUMP 接口的 HTTP 客户端，MES 项目可通过自定义同类型 Bean 替换实现
     *
     * @param recipeChangeConsumerProperties Starter 配置，包含 JUMP 服务地址和超时参数
     * @param recipeChangeRestTemplate Starter 创建的 HTTP 调用模板
     * @return 默认 JUMP HTTP 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public RecipeChangeJUMPClient recipeChangeJUMPClient(RecipeChangeConsumerProperties recipeChangeConsumerProperties, RestTemplate recipeChangeRestTemplate) {
        // 组合 HTTP 调用模板与 Starter 配置，创建用于查询状态和上报回调的默认客户端
        return new RecipeChangeJUMPHttpClient(recipeChangeRestTemplate, recipeChangeConsumerProperties);
    }

    /**
     * 创建具备连接和读取超时控制的 JUMP HTTP 调用模板
     *
     * @param recipeChangeConsumerProperties Starter 配置，包含连接和读取超时时间
     * @return 仅供工艺变更 JUMP 客户端使用的 RestTemplate
     */
    @Bean
    @ConditionalOnMissingBean(name = "recipeChangeRestTemplate")
    public RestTemplate recipeChangeRestTemplate(RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        // 创建底层 HTTP 请求工厂，以便分别配置连接和读取超时
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 设置建立到 JUMP 服务 TCP 连接的最长等待时间
        requestFactory.setConnectTimeout(recipeChangeConsumerProperties.getConnectTimeoutMillis());
        // 设置连接建立后等待 JUMP 响应数据的最长时间
        requestFactory.setReadTimeout(recipeChangeConsumerProperties.getReadTimeoutMillis());
        // 使用上述请求工厂创建 RestTemplate，避免影响 MES 应用已有的 RestTemplate Bean
        return new RestTemplate(requestFactory);
    }

    /**
     * 注册消息监听器，MES 项目必须提供工艺变更处理器和钉钉告警服务
     *
     * @param recipeChangeMessageProcessorProvider MES 本地工艺变更业务处理器提供器
     * @param recipeChangeJUMPClient JUMP 状态查询和处理结果回调客户端
     * @param recipeChangeDingTalkAlarmProcessorProvider MES 钉钉告警处理器提供器
     * @param recipeChangeConsumerProperties Starter 运行配置
     * @return 用于监听车间专属工艺变更队列的消息监听器
     */
    @Bean
    public RecipeChangeMessageListener recipeChangeMessageListener(ObjectProvider<RecipeChangeMessageProcessor> recipeChangeMessageProcessorProvider,
                                                                   RecipeChangeJUMPClient recipeChangeJUMPClient,
                                                                   ObjectProvider<RecipeChangeDingTalkAlarmProcessor> recipeChangeDingTalkAlarmProcessorProvider,
                                                                   RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        // 延迟获取 MES 业务处理器，使 Starter 工程自身不会因缺少业务实现产生 IDEA 注入警告
        RecipeChangeMessageProcessor recipeChangeMessageProcessor = recipeChangeMessageProcessorProvider.getIfAvailable();
        // 消费功能启用后没有业务处理器时直接终止启动，防止消息被错误消费或确认
        if (recipeChangeMessageProcessor == null) {
            throw new IllegalStateException("启用工艺变更消费者时必须提供 RecipeChangeMessageProcessor 实现");
        }
        // 延迟获取 MES 钉钉告警实现，使 Starter 工程自身不会因缺少业务实现产生 IDEA 注入警告
        RecipeChangeDingTalkAlarmProcessor recipeChangeDingTalkAlarmProcessor = recipeChangeDingTalkAlarmProcessorProvider.getIfAvailable();
        // 消费功能启用后没有钉钉告警实现时终止启动，确保车间处理失败能够按要求触发钉钉告警
        if (recipeChangeDingTalkAlarmProcessor == null) {
            throw new IllegalStateException("启用工艺变更消费者时必须提供 RecipeChangeDingTalkAlarmProcessor 实现");
        }
        // 创建监听器并注入业务处理、JUMP 通信、必需告警实现和运行配置
        return new RecipeChangeMessageListener(recipeChangeMessageProcessor, recipeChangeJUMPClient, recipeChangeDingTalkAlarmProcessor, recipeChangeConsumerProperties);
    }

}
