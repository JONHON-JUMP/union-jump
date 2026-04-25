package cn.jonhon.jump.module.iot.framework.web.config;

import cn.jonhon.jump.framework.swagger.config.JonhonjumpSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * iot 模块的 web 组件的 Configuration
 *
 * @author ahh
 */
@Configuration(proxyBeanMethods = false)
public class IotWebConfiguration {

    /**
     * iot 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi iotGroupedOpenApi() {
        return JonhonjumpSwaggerAutoConfiguration.buildGroupedOpenApi("iot");
    }

}
