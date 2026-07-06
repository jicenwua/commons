package com.xcz.member.security.config;

import com.xcz.member.core.security.ReactiveSecurityChainFilter;
import com.xcz.member.security.config.properties.IgnoreProperties;
import com.xcz.member.security.exception.GlobalReactiveExceptionHandler;
import com.xcz.member.security.interceptor.HeadReactAuthenticationFilter;
import com.xcz.member.security.service.TokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.List;

/**
 * Reactive 环境 Security 自动配置（Gateway 等）。
 */
@AutoConfiguration(after = SecurityCoreAutoConfiguration.class)
@EnableWebFluxSecurity
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class SecurityReactConfig {

    @Bean
    public HeadReactAuthenticationFilter headReactAuthenticationFilter(
            TokenService tokenService,
            IgnoreProperties ignoreProperties) {
        return new HeadReactAuthenticationFilter(tokenService, ignoreProperties);
    }

    @Bean
    public GlobalReactiveExceptionHandler globalReactiveExceptionHandler() {
        return new GlobalReactiveExceptionHandler();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @RefreshScope
    public SecurityWebFilterChain filterChain(
            ServerHttpSecurity httpSecurity,
            IgnoreProperties ignoreProperties,
            HeadReactAuthenticationFilter headReactAuthenticationFilter,
            List<ReactiveSecurityChainFilter> reactiveSecurityChainFilters) throws Exception {
        httpSecurity
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(request -> {
                    request.pathMatchers(HttpMethod.OPTIONS).permitAll();
                    List<String> ignoreUrls = ignoreProperties.getUrls();
                    if (ignoreUrls != null && !ignoreUrls.isEmpty()) {
                        request.pathMatchers(ignoreUrls.toArray(new String[0])).permitAll();
                    }
                    request.anyExchange().authenticated();
                });
        reactiveSecurityChainFilters.forEach(chainFilter ->
                httpSecurity.addFilterBefore(chainFilter.filter(), SecurityWebFiltersOrder.AUTHENTICATION));
        httpSecurity.addFilterBefore(headReactAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return httpSecurity.build();
    }
}
