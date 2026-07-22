package com.xcz.commons.log.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.log.config.RequestLogProperties;
import com.xcz.commons.log.support.DefaultRequestLogUserIdResolver;
import com.xcz.commons.log.support.RequestLogUserIdResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.WebFilter;

/**
 * WebFlux / Gateway 请求日志自动配置。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(WebFilter.class)
@EnableConfigurationProperties(RequestLogProperties.class)
public class ReactiveRequestLogAutoConfiguration {

    /**
     * 默认用户 ID 解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestLogUserIdResolver reactiveRequestLogUserIdResolver() {
        return new DefaultRequestLogUserIdResolver();
    }

    /**
     * 注册响应式请求日志 Filter
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveLogFilter.class)
    public ReactiveLogFilter reactiveLogFilter(RequestLogProperties properties,
                                               ObjectMapper objectMapper,
                                               RequestLogUserIdResolver userIdResolver,
                                               @Value("${spring.application.name:app}") String appName) {
        return new ReactiveLogFilter(properties, objectMapper, userIdResolver, appName);
    }
}
