package com.xcz.commons.security.config;

import com.xcz.commons.core.security.SecurityChainFilter;
import com.xcz.commons.security.aspect.InnerAuthAspect;
import com.xcz.commons.security.config.properties.IgnoreProperties;
import com.xcz.commons.security.exception.GlobalExceptionHandler;
import com.xcz.commons.security.interceptor.HeaderAuthenticationFilter;
import com.xcz.commons.security.service.TokenService;
import com.xcz.commons.security.support.ReleasePathCollector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Servlet 环境 Security 自动配置。
 */
@AutoConfiguration(after = SecurityCoreAutoConfiguration.class)
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
public class SecurityConfig {

    /**
     * 扫描 @Release 免认证路径。
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

    /**
     * 创建请求登录验证拦截
     */
    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter(
            IgnoreProperties ignoreProperties,
            TokenService tokenService,
            ReleasePathCollector releasePathCollector) {
        return new HeaderAuthenticationFilter(ignoreProperties, releasePathCollector, tokenService);
    }

    /**
     * 创建切片
     */
    @Bean
    public InnerAuthAspect innerAuthAspect() {
        return new InnerAuthAspect();
    }

    /**
     * 创建全局拦截器
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 创建密码加密
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * security拦截链路
     */
    @Bean
    @RefreshScope
    public SecurityFilterChain filterChain(
            HttpSecurity httpSecurity,
            IgnoreProperties ignoreProperties,
            ReleasePathCollector releasePathCollector,
            HeaderAuthenticationFilter headerAuthenticationFilter,
            List<SecurityChainFilter> securityChainFilters) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> {
                    request.requestMatchers(HttpMethod.OPTIONS).permitAll(); //允许所有预加载
                    //获取所有忽略路径，对忽略路径进行放行
                    List<String> ignoreUrls = ReleasePathCollector.mergeIgnoreUrls(ignoreProperties, releasePathCollector);
                    if (!ignoreUrls.isEmpty()) {
                        request.requestMatchers(ignoreUrls.toArray(new String[0])).permitAll(); //忽略的路径放行不进行权限校验
                    }
                    request.anyRequest().authenticated();
                });
        securityChainFilters.forEach(chainFilter ->
                httpSecurity.addFilterBefore(chainFilter.filter(), UsernamePasswordAuthenticationFilter.class));
        httpSecurity.addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
