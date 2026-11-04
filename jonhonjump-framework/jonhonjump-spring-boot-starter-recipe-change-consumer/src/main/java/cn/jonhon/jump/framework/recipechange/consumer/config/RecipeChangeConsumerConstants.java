package cn.jonhon.jump.framework.recipechange.consumer.config;

/**
 * 工艺变更消费者 Starter 的内置基础设施参数。
 *
 * 这些参数随 Starter jar 发布；RabbitMQ 或 JUMP 基础设施变更时，需要重新发布 Starter。
 */
public final class RecipeChangeConsumerConstants {

    public static final String JUMP_BASE_URL = "http://192.168.240.129:8777";
    public static final int CONNECT_TIMEOUT_MILLIS = 10000;
    public static final int READ_TIMEOUT_MILLIS = 20000;

    public static final String RABBITMQ_HOST = "192.168.240.160";
    public static final int RABBITMQ_PORT = 5672;
    public static final String RABBITMQ_USERNAME = "admin";
    public static final String RABBITMQ_PASSWORD = "admin*963.";

//    public static final String JUMP_BASE_URL = "http://127.0.0.1:48080";
//    public static final int CONNECT_TIMEOUT_MILLIS = 20000;
//    public static final int READ_TIMEOUT_MILLIS = 40000;
//
//    public static final String RABBITMQ_HOST = "127.0.0.1";
//    public static final int RABBITMQ_PORT = 5672;
//    public static final String RABBITMQ_USERNAME = "admin";
//    public static final String RABBITMQ_PASSWORD = "admin123";

    public static final String RABBITMQ_VIRTUAL_HOST = "/";

    public static final String QUEUE_NAME_PREFIX = "QUEUE_RECIPE_CHANGE_";

    private RecipeChangeConsumerConstants() {
    }

}
