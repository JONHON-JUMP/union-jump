package cn.jonhon.jump.module.member.framework.web.config;

import cn.jonhon.jump.framework.swagger.config.JonhonjumpSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * member 模块的 web 组件的 Configuration
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
public class MemberWebConfiguration {

    /**
     * member 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi memberGroupedOpenApi() {
        return JonhonjumpSwaggerAutoConfiguration.buildGroupedOpenApi("member");
    }

}
