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
 * Servlet 环境：扫描 {@link com.xcz.commons.security.annotation.Release} 免认证路径。
 * <p>
 * 独立配置类，避免在 Gateway 等无 spring-webmvc 的应用中加载 Servlet 类型。
 * </p>
 */
@AutoConfiguration(before = SecurityConfig.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RequestMappingHandlerMapping.class)
public class ServletReleasePathCollectorAutoConfiguration {

    /**
     * 按 Bean 名称获取 WebMVC 主映射器（避免与 Actuator 的 handlerMapping 冲突）。
     * IDE 可能提示无法自动装配：本类仅在 Servlet 应用启动时生效，{@code requestMappingHandlerMapping}
     * 由 {@code WebMvcAutoConfiguration} 在运行时注册，静态分析无法感知。
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
