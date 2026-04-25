package cn.jonhon.jump.framework.encrypt.config;

import cn.jonhon.jump.framework.common.enums.WebFilterOrderEnum;
import cn.jonhon.jump.framework.encrypt.core.filter.ApiEncryptFilter;
import cn.jonhon.jump.framework.web.config.WebProperties;
import cn.jonhon.jump.framework.web.core.handler.GlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static cn.jonhon.jump.framework.web.config.JonhonjumpWebAutoConfiguration.createFilterBean;

@AutoConfiguration
@Slf4j
@EnableConfigurationProperties(ApiEncryptProperties.class)
@ConditionalOnProperty(prefix = "jonhonjump.api-encrypt", name = "enable", havingValue = "true")
public class JonhonjumpApiEncryptAutoConfiguration {

    @Bean
    public FilterRegistrationBean<ApiEncryptFilter> apiEncryptFilter(WebProperties webProperties,
                                                                     ApiEncryptProperties apiEncryptProperties,
                                                                     RequestMappingHandlerMapping requestMappingHandlerMapping,
                                                                     GlobalExceptionHandler globalExceptionHandler) {
        ApiEncryptFilter filter = new ApiEncryptFilter(webProperties, apiEncryptProperties,
                requestMappingHandlerMapping, globalExceptionHandler);
        return createFilterBean(filter, WebFilterOrderEnum.API_ENCRYPT_FILTER);

    }

}
