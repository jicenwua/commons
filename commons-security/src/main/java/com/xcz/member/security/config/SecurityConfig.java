package com.xcz.member.security.config;

import com.xcz.member.core.security.SecurityChainFilter;
import com.xcz.member.security.aspect.InnerAuthAspect;
import com.xcz.member.security.config.properties.IgnoreProperties;
import com.xcz.member.security.exception.GlobalExceptionHandler;
import com.xcz.member.security.interceptor.HeaderAuthenticationFilter;
import com.xcz.member.security.service.TokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
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
     * 创建请求登录验证拦截
     */
    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter(
            IgnoreProperties ignoreProperties,
            TokenService tokenService) {
        return new HeaderAuthenticationFilter(ignoreProperties, tokenService);
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
            HeaderAuthenticationFilter headerAuthenticationFilter,
            List<SecurityChainFilter> securityChainFilters) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> {
                    request.requestMatchers(HttpMethod.OPTIONS).permitAll(); //允许所有预加载
                    //获取所有忽略路径，对忽略路径进行放行
                    List<String> ignoreUrls = ignoreProperties.getUrls();
                    if (ignoreUrls != null && !ignoreUrls.isEmpty()) {
                        request.requestMatchers(ignoreUrls.toArray(new String[0])).permitAll();
                    }
                    request.anyRequest().authenticated();
                });
        securityChainFilters.forEach(chainFilter ->
                httpSecurity.addFilterBefore(chainFilter.filter(), UsernamePasswordAuthenticationFilter.class));
        httpSecurity.addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
