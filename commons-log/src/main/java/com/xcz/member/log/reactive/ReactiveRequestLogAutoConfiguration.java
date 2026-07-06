package com.xcz.member.log.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.member.log.config.RequestLogProperties;
import com.xcz.member.log.support.DefaultRequestLogUserIdResolver;
import com.xcz.member.log.support.RequestLogUserIdResolver;
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
 * <p>
 * 在 Reactive 环境下自动注册 {@link ReactiveLogFilter}，
 * 引入 {@code commons-log} 依赖即可生效，无需额外 Java 配置。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(WebFilter.class)
@EnableConfigurationProperties(RequestLogProperties.class)
public class ReactiveRequestLogAutoConfiguration {

    /**
     * 默认用户 ID 解析器（可被 {@link com.xcz.member.log.config.SecurityRequestLogUserIdAutoConfiguration} 覆盖）。
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestLogUserIdResolver reactiveRequestLogUserIdResolver() {
        return new DefaultRequestLogUserIdResolver();
    }

    /**
     * 响应式请求日志 WebFilter。
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
