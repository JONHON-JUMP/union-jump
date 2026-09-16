package cn.jonhon.jump.framework.recipechange.consumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工艺变更 RabbitMQ 消费者 Starter 的配置属性
 */
@ConfigurationProperties(prefix = "jonhonjump.recipe-change.consumer")
public class RecipeChangeConsumerProperties {

    /**
     * 是否启用工艺变更消息消费功能
     */
    private boolean enabled = false;
    /**
     * 当前 MES 需要消费的车间编码列表；配置几个车间，就声明并监听几个车间的专属队列。
     */
    private List<String> workshopCodes = new ArrayList<>();
    /**
     * 车间专属队列名称前缀，完整队列名为此前缀加车间编码
     */
    private String queueNamePrefix;
    /**
     * 工艺变更主直连交换机名称
     */
    private String exchange;
    /**
     * 按车间编码生成主队列路由键时使用的前缀
     */
    private String routingKeyPrefix;
    /**
     * 工艺变更延迟重试直连交换机名称
     */
    private String retryExchange;
    /**
     * 按车间编码生成延迟重试路由键时使用的前缀
     */
    private String retryRoutingKeyPrefix;
    /**
     * 按车间编码生成延迟重试队列名称时使用的前缀
     */
    private String retryQueueNamePrefix;
    /**
     * 延迟重试队列中消息的存活时长，单位毫秒。
     * Starter 声明队列时将其写入 x-message-ttl，消息到期后再死信回主队列。
     */
    private long retryDelayMillis;
    /**
     * 按车间覆盖延迟重试队列前缀和 TTL；未配置的车间使用全局 retryQueueNamePrefix 和 retryDelayMillis。
     */
    private Map<String, RetryQueueOverride> retryQueueOverrides = new HashMap<>();
    /**
     * JUMP 服务地址，不包含末尾斜杠
     */
    private String jumpBaseUrl;
    /**
     * 调用 JUMP 接口的连接超时时间，单位毫秒
     */
    private int connectTimeoutMillis = 10000;
    /**
     * 调用 JUMP 接口的读取超时时间，单位毫秒
     */
    private int readTimeoutMillis = 20000;
    /**
     * 工艺变更消费者专用 RabbitMQ 连接配置
     *
     * 该配置不读取 spring.rabbitmq，避免影响或误用 MES 项目其他 RabbitMQ 业务的连接配置
     */
    private RabbitMQProperties rabbitmq;

    /** 获取指定车间需要监听的 RabbitMQ 主队列名称。 */
    public String getQueueName(String workshopCode) {
        return queueNamePrefix + workshopCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getWorkshopCodes() {
        return workshopCodes;
    }

    public void setWorkshopCodes(List<String> workshopCodes) {
        this.workshopCodes = workshopCodes;
    }

    public String getQueueNamePrefix() {
        return queueNamePrefix;
    }

    public void setQueueNamePrefix(String queueNamePrefix) {
        this.queueNamePrefix = queueNamePrefix;
    }

    /**
     * 获取工艺变更主直连交换机名称
     *
     * @return 主直连交换机名称
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * 设置工艺变更主直连交换机名称
     *
     * @param exchange 主直连交换机名称
     */
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    /**
     * 获取主队列路由键前缀
     *
     * @return 主队列路由键前缀
     */
    public String getRoutingKeyPrefix() {
        return routingKeyPrefix;
    }

    /**
     * 设置主队列路由键前缀
     *
     * @param routingKeyPrefix 主队列路由键前缀
     */
    public void setRoutingKeyPrefix(String routingKeyPrefix) {
        this.routingKeyPrefix = routingKeyPrefix;
    }

    /**
     * 获取延迟重试直连交换机名称
     *
     * @return 延迟重试直连交换机名称
     */
    public String getRetryExchange() {
        return retryExchange;
    }

    /**
     * 设置延迟重试直连交换机名称
     *
     * @param retryExchange 延迟重试直连交换机名称
     */
    public void setRetryExchange(String retryExchange) {
        this.retryExchange = retryExchange;
    }

    /**
     * 获取延迟重试路由键前缀
     *
     * @return 延迟重试路由键前缀
     */
    public String getRetryRoutingKeyPrefix() {
        return retryRoutingKeyPrefix;
    }

    /**
     * 设置延迟重试路由键前缀
     *
     * @param retryRoutingKeyPrefix 延迟重试路由键前缀
     */
    public void setRetryRoutingKeyPrefix(String retryRoutingKeyPrefix) {
        this.retryRoutingKeyPrefix = retryRoutingKeyPrefix;
    }

    /**
     * 获取延迟重试队列名称前缀
     *
     * @return 延迟重试队列名称前缀
     */
    public String getRetryQueueNamePrefix() {
        return retryQueueNamePrefix;
    }

    /**
     * 设置延迟重试队列名称前缀
     *
     * @param retryQueueNamePrefix 延迟重试队列名称前缀
     */
    public void setRetryQueueNamePrefix(String retryQueueNamePrefix) {
        this.retryQueueNamePrefix = retryQueueNamePrefix;
    }

    /**
     * 获取延迟重试队列消息存活时长。
     *
     * @return 延迟时长，单位毫秒
     */
    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }

    /**
     * 设置延迟重试队列消息存活时长。
     * 修改该值后，必须删除并由应用重新声明对应延迟重试队列，RabbitMQ 才会接受新的 x-message-ttl。
     *
     * @param retryDelayMillis 延迟时长，单位毫秒
     */
    public void setRetryDelayMillis(long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
    }

    /** 获取指定车间当前生效的延迟重试队列名称。 */
    public String getRetryQueueName(String workshopCode) {
        RetryQueueOverride override = getRetryQueueOverride(workshopCode);
        String queueNamePrefix = override != null ? override.getQueueNamePrefix() : retryQueueNamePrefix;
        return queueNamePrefix + workshopCode;
    }

    /** 获取指定车间当前生效的延迟重试时长，单位毫秒。 */
    public long getRetryDelayMillis(String workshopCode) {
        RetryQueueOverride override = getRetryQueueOverride(workshopCode);
        return override != null ? override.getDelayMillis() : retryDelayMillis;
    }

    public Map<String, RetryQueueOverride> getRetryQueueOverrides() {
        return retryQueueOverrides;
    }

    public void setRetryQueueOverrides(Map<String, RetryQueueOverride> retryQueueOverrides) {
        this.retryQueueOverrides = retryQueueOverrides;
    }

    public String getJumpBaseUrl() {
        return jumpBaseUrl;
    }

    public void setJumpBaseUrl(String jumpBaseUrl) {
        this.jumpBaseUrl = jumpBaseUrl;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    /**
     * 获取工艺变更消费者专用 RabbitMQ 连接配置
     *
     * @return RabbitMQ 连接配置
     */
    public RabbitMQProperties getRabbitmq() {
        return rabbitmq;
    }

    /**
     * 设置工艺变更消费者专用 RabbitMQ 连接配置
     *
     * @param rabbitmq RabbitMQ 连接配置
     */
    public void setRabbitmq(RabbitMQProperties rabbitmq) {
        this.rabbitmq = rabbitmq;
    }

    /**
     * 校验启用工艺变更消费者时必填的专用 RabbitMQ 连接参数
     *
     * 连接参数不提供默认值，避免 MES 因遗漏配置而连接到非预期 Broker
     */
    public void validateRabbitMQProperties() {
        if (rabbitmq == null || isBlank(rabbitmq.getHost()) || rabbitmq.getPort() == null
                || rabbitmq.getPort() <= 0 || isBlank(rabbitmq.getUsername())
                || isBlank(rabbitmq.getPassword()) || isBlank(rabbitmq.getVirtualHost())) {
            throw new IllegalStateException("启用工艺变更消费者时必须配置 jonhonjump.recipe-change.consumer.rabbitmq 的 host、port、username、password 和 virtual-host");
        }
    }

    /**
     * 校验启动监听器和声明 RabbitMQ 基础设施所需的工艺变更拓扑参数
     */
    public void validateRabbitInfrastructureProperties() {
        normalizeAndValidateWorkshopCodes();
        if (isBlank(queueNamePrefix) || isBlank(exchange) || isBlank(routingKeyPrefix)
                || isBlank(retryExchange) || isBlank(retryRoutingKeyPrefix) || isBlank(retryQueueNamePrefix)
                || retryDelayMillis < 0) {
            throw new IllegalStateException("启用工艺变更消费者时必须配置 workshop-codes、queue-name-prefix、exchange、routing-key-prefix、retry-exchange、retry-routing-key-prefix、retry-queue-name-prefix，且 retry-delay-millis 不能小于 0");
        }
        validateRetryQueueOverrides();
    }

    /** 规范化车间列表，拒绝空编码并去重，避免同一队列被重复声明和监听。 */
    private void normalizeAndValidateWorkshopCodes() {
        if (workshopCodes == null || workshopCodes.isEmpty()) {
            throw new IllegalStateException("启用工艺变更消费者时必须至少配置一个 workshop-codes");
        }
        Set<String> normalizedWorkshopCodes = new LinkedHashSet<>();
        for (String workshopCode : workshopCodes) {
            if (isBlank(workshopCode)) {
                throw new IllegalStateException("workshop-codes 中不能包含空车间编码");
            }
            normalizedWorkshopCodes.add(workshopCode.trim());
        }
        workshopCodes = new ArrayList<>(normalizedWorkshopCodes);
    }

    /** 校验按车间配置的延迟队列覆盖项，避免拼写错误或无效 TTL 导致 RabbitMQ 拓扑不一致。 */
    private void validateRetryQueueOverrides() {
        if (retryQueueOverrides == null) {
            retryQueueOverrides = new HashMap<>();
            return;
        }
        for (Map.Entry<String, RetryQueueOverride> entry : retryQueueOverrides.entrySet()) {
            String workshopCode = entry.getKey();
            RetryQueueOverride override = entry.getValue();
            if (isBlank(workshopCode) || !workshopCodes.contains(workshopCode) || override == null
                    || isBlank(override.getQueueNamePrefix()) || override.getDelayMillis() < 0) {
                throw new IllegalStateException("retry-queue-overrides 只能配置已监听车间，且每项必须提供 queue-name-prefix 和非负 delay-millis");
            }
        }
    }

    /** 获取指定车间的覆盖配置；未配置时返回空值并回退全局默认值。 */
    private RetryQueueOverride getRetryQueueOverride(String workshopCode) {
        return retryQueueOverrides == null ? null : retryQueueOverrides.get(workshopCode);
    }

    /**
     * 判断配置字符串是否为空或只包含空白字符
     *
     * @param value 待判断的配置值
     * @return true 表示配置值为空
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 单个车间的版本化延迟重试队列覆盖配置。 */
    public static class RetryQueueOverride {

        /** 当前车间延迟重试队列名称前缀。 */
        private String queueNamePrefix;
        /** 当前车间延迟重试队列 TTL，单位毫秒。 */
        private long delayMillis;

        public String getQueueNamePrefix() {
            return queueNamePrefix;
        }

        public void setQueueNamePrefix(String queueNamePrefix) {
            this.queueNamePrefix = queueNamePrefix;
        }

        public long getDelayMillis() {
            return delayMillis;
        }

        public void setDelayMillis(long delayMillis) {
            this.delayMillis = delayMillis;
        }
    }

    /**
     * 工艺变更消费者专用 RabbitMQ 连接参数
     */
    public static class RabbitMQProperties {

        /**
         * RabbitMQ 服务地址
         */
        private String host;
        /**
         * RabbitMQ 服务端口
         */
        private Integer port;
        /**
         * RabbitMQ 登录用户名
         */
        private String username;
        /**
         * RabbitMQ 登录密码
         */
        private String password;
        /**
         * RabbitMQ 虚拟主机
         */
        private String virtualHost;

        /**
         * 获取 RabbitMQ 服务地址
         *
         * @return RabbitMQ 服务地址
         */
        public String getHost() {
            return host;
        }

        /**
         * 设置 RabbitMQ 服务地址
         *
         * @param host RabbitMQ 服务地址
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * 获取 RabbitMQ 服务端口
         *
         * @return RabbitMQ 服务端口
         */
        public Integer getPort() {
            return port;
        }

        /**
         * 设置 RabbitMQ 服务端口
         *
         * @param port RabbitMQ 服务端口
         */
        public void setPort(Integer port) {
            this.port = port;
        }

        /**
         * 获取 RabbitMQ 登录用户名
         *
         * @return RabbitMQ 登录用户名
         */
        public String getUsername() {
            return username;
        }

        /**
         * 设置 RabbitMQ 登录用户名
         *
         * @param username RabbitMQ 登录用户名
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * 获取 RabbitMQ 登录密码
         *
         * @return RabbitMQ 登录密码
         */
        public String getPassword() {
            return password;
        }

        /**
         * 设置 RabbitMQ 登录密码
         *
         * @param password RabbitMQ 登录密码
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * 获取 RabbitMQ 虚拟主机
         *
         * @return RabbitMQ 虚拟主机
         */
        public String getVirtualHost() {
            return virtualHost;
        }

        /**
         * 设置 RabbitMQ 虚拟主机
         *
         * @param virtualHost RabbitMQ 虚拟主机
         */
        public void setVirtualHost(String virtualHost) {
            this.virtualHost = virtualHost;
        }

    }

}
