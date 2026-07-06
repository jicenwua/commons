package com.xcz.commons.log.config;

import com.xcz.commons.log.support.RequestLogUserIdResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 安全模块集成：当 classpath 存在 {@code SecurityUtils} 时，自动解析当前登录用户 ID。
 * <p>
 * 使用反射调用，避免 {@code commons-log} 硬依赖 {@code commons-security}。
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.xcz.commons.security.utils.SecurityUtils")
public class SecurityRequestLogUserIdAutoConfiguration {

    /**
     * 通过 {@code SecurityUtils.getUserId()} 解析用户 ID；解析失败时返回 {@code 0L}。
     *
     * @return 基于 SecurityUtils 的用户 ID 解析器
     */
    @Bean
    @ConditionalOnMissingBean(RequestLogUserIdResolver.class)
    public RequestLogUserIdResolver securityRequestLogUserIdResolver() {
        return () -> {
            try {
                // 反射调用，避免 compile-time 依赖 commons-security
                Class<?> securityUtils = Class.forName("com.xcz.commons.security.utils.SecurityUtils");
                Object userId = securityUtils.getMethod("getUserId").invoke(null);
                return userId instanceof Long id ? id : 0L;
            } catch (Exception e) {
                return 0L;
            }
        };
    }
}
