package com.xcz.commons.security.config;

import com.xcz.commons.security.support.ReleasePathCollector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.stream.Collectors;

/**
 * Reactive 环境（Gateway 等）：扫描 {@link com.xcz.commons.security.annotation.Release} 免认证路径。
 */
@AutoConfiguration(before = SecurityReactConfig.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(RequestMappingHandlerMapping.class)
public class ReactiveReleasePathCollectorAutoConfiguration {

    /**
     * 按 Bean 名称获取 WebFlux 主映射器（排除 Actuator 的 controllerEndpointHandlerMapping）。
     */
    @Bean
    public ReleasePathCollector releasePathCollector(ApplicationContext applicationContext) {
        RequestMappingHandlerMapping handlerMapping = applicationContext.getBean(
                "requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        ReleasePathCollector collector = new ReleasePathCollector();
        collector.collect(handlerMapping.getHandlerMethods(), info -> {
            RequestMappingInfo mappingInfo = (RequestMappingInfo) info;
            return mappingInfo.getPatternsCondition().getPatterns().stream()
                    .map(PathPattern::getPatternString)
                    .collect(Collectors.toSet());
        });
        return collector;
    }
}
