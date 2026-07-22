package com.xcz.commons.log.config;

import com.xcz.commons.log.support.RequestLogUserIdResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 存在 SecurityUtils 时，自动用其解析当前用户 ID。
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.xcz.commons.security.utils.SecurityUtils")
public class SecurityRequestLogUserIdAutoConfiguration {

    /**
     * 基于 SecurityUtils 的用户 ID 解析器。
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
