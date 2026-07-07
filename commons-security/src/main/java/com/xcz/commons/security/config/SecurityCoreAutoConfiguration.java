package com.xcz.commons.security.config;

import com.xcz.commons.security.config.properties.IgnoreProperties;
import com.xcz.commons.security.service.TokenService;
import com.xcz.commons.security.support.ReleasePathCollector;
import com.xcz.commons.security.utils.JwtUtils;
import com.xcz.commons.security.utils.PermissionUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

/**
 * Security 共用 Bean 自动配置（Servlet / Reactive 均依赖）。
 */
@AutoConfiguration
@EnableConfigurationProperties({
        IgnoreProperties.class,
        JwtUtils.class
})
public class SecurityCoreAutoConfiguration {

    @Bean
    public PermissionUtils permissionUtils() {
        return new PermissionUtils();
    }

    @Bean
    @Lazy
    public TokenService tokenService(JwtUtils jwtUtils) {
        if (jwtUtils.getSecret() == null || jwtUtils.getSecret().isBlank()
                || jwtUtils.getExpiration() == null) {
            throw new IllegalStateException(
                    "security.jwt.secret / expiration 未配置，请在 Nacos 或 application.yml 中设置");
        }
        return new TokenService(jwtUtils);
    }

    @Bean("ss")
    public PermissionExpression permissionExpression() {
        return new PermissionExpression();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping")
    public ReleasePathCollector releasePathCollector(
            org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping handlerMapping) {
        return ReleasePathCollector.fromServlet(handlerMapping);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(name = "org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping")
    public ReleasePathCollector releasePathCollector(
            org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping handlerMapping) {
        return ReleasePathCollector.fromReactive(handlerMapping);
    }
}
