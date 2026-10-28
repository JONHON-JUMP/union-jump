package cn.jonhon.jump.framework.mpm.config;

import cn.jonhon.jump.framework.mpm.client.MpmClient;
import cn.jonhon.jump.framework.mpm.client.MpmRouteClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动配置类
 *
 */
@Configuration(proxyBeanMethods = false)
public class MpmAutoConfiguration {

    /**
     * 创建通过接口平台调用 MPM 的默认客户端。
     *
     * @param objectMapper MES Spring 容器中的 Jackson 映射器，用于解析 MPM 返回的 JSON
     * @return 可供业务代码注入的 {@code MpmClient}
     */
    @Bean
    @ConditionalOnMissingBean(MpmClient.class)
    public MpmClient mpmClient(ObjectMapper objectMapper) {
        return new MpmRouteClient(objectMapper);
    }
}
