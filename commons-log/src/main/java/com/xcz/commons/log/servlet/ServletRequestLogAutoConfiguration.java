package com.xcz.commons.log.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.log.config.RequestLogProperties;
import com.xcz.commons.log.support.DefaultRequestLogUserIdResolver;
import com.xcz.commons.log.support.RequestLogUserIdResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Servlet MVC 请求日志自动配置。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = {
        "org.springframework.web.servlet.DispatcherServlet",
        "org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice"
})
@EnableConfigurationProperties(RequestLogProperties.class)
public class ServletRequestLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestLogUserIdResolver requestLogUserIdResolver() {
        return new DefaultRequestLogUserIdResolver();
    }

    @Bean
    @ConditionalOnMissingBean(name = "logFilter")
    public LogFilter logFilter() {
        return new LogFilter();
    }

    /**
     * 注册响应体记录器（需带 {@link ControllerAdvice} 才会生效）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "responseRecorder")
    public ResponseRecorder responseRecorder() {
        return new ResponseRecorder();
    }


    @Bean
    @ConditionalOnMissingBean(name = "requestLogInterceptor")
    public RequestLogInterceptor requestLogInterceptor(RequestLogProperties properties,
                                                       ObjectMapper objectMapper,
                                                       RequestLogUserIdResolver userIdResolver,
                                                       @Value("${spring.application.name:app}") String appName) {
        return new RequestLogInterceptor(properties, objectMapper, userIdResolver, appName);
    }

    /**
     * 注册日志拦截器，拦截全部路径。
     */
    @Bean
    public WebMvcConfigurer requestLogWebMvcConfigurer(RequestLogInterceptor requestLogInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(requestLogInterceptor).addPathPatterns("/**");
            }
        };
    }
}
