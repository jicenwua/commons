package com.xcz.commons.security.config;

import com.xcz.commons.core.security.ReactiveSecurityChainFilter;
import com.xcz.commons.security.aspect.InnerAuthAspect;
import com.xcz.commons.security.config.properties.IgnoreProperties;
import com.xcz.commons.security.exception.GlobalReactiveExceptionHandler;
import com.xcz.commons.security.interceptor.HeadReactAuthenticationFilter;
import com.xcz.commons.security.service.TokenService;
import com.xcz.commons.security.support.ReleasePathCollector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Reactive 环境 Security 自动配置（Gateway 等）。
 */
@AutoConfiguration(after = SecurityCoreAutoConfiguration.class)
@EnableWebFluxSecurity
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class SecurityReactConfig {

    /**
     * 扫描 @Release 免认证路径。
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

    @Bean
    public HeadReactAuthenticationFilter headReactAuthenticationFilter(
            TokenService tokenService,
            IgnoreProperties ignoreProperties,
            ReleasePathCollector releasePathCollector) {
        return new HeadReactAuthenticationFilter(tokenService, ignoreProperties, releasePathCollector);
    }

    /**
     * 创建切片
     */
    @Bean
    public InnerAuthAspect innerAuthAspect() {
        return new InnerAuthAspect();
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
            ReleasePathCollector releasePathCollector,
            HeadReactAuthenticationFilter headReactAuthenticationFilter,
            List<ReactiveSecurityChainFilter> reactiveSecurityChainFilters) throws Exception {
        httpSecurity
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(request -> {
                    request.pathMatchers(HttpMethod.OPTIONS).permitAll();
                    List<String> ignoreUrls = ReleasePathCollector.mergeIgnoreUrls(ignoreProperties, releasePathCollector);
                    if (!ignoreUrls.isEmpty()) {
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
