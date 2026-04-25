package cn.jonhon.jump.module.mes.framework.web.config;

import cn.jonhon.jump.framework.swagger.config.JonhonjumpSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mes 模块的 web 组件的 Configuration
 *
 * @author 中航光电
 */
@Configuration(proxyBeanMethods = false)
public class MesWebConfiguration {

    /**
     * mes 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi mesGroupedOpenApi() {
        return JonhonjumpSwaggerAutoConfiguration.buildGroupedOpenApi("mes");
    }

}
