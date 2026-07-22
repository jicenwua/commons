package com.xcz.commons.security.config;

import com.xcz.commons.security.support.ReleasePathCollector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Servlet 环境：扫描 @Release 免认证路径。
 */
@AutoConfiguration(before = SecurityConfig.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RequestMappingHandlerMapping.class)
public class ServletReleasePathCollectorAutoConfiguration {

    /**
     * 按 Bean 名称获取 WebMVC 主映射器并收集免认证路径。
     */
    @Bean
    public ReleasePathCollector releasePathCollector(ApplicationContext applicationContext) {
        RequestMappingHandlerMapping handlerMapping = applicationContext.getBean(
                "requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        ReleasePathCollector collector = new ReleasePathCollector();
        collector.collect(handlerMapping.getHandlerMethods(),
                info -> ((RequestMappingInfo) info).getPatternValues());
        return collector;
    }
}
