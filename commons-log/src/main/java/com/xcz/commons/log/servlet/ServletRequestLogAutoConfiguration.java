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
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Servlet MVC 请求日志自动配置。
 * <p>
 * 使用 {@link AutoConfiguration} 注册（非组件扫描），仅在 Servlet 环境且 classpath 存在 webmvc 时生效。
 * Servlet 相关类<b>不可</b>标注 {@code @Configuration}/{@code @Component}/{@code @RestControllerAdvice}，
 * 否则 WebFlux / Gateway 扫描 {@code com.xcz.member} 时会因缺少 spring-webmvc 而启动失败。
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
     * 必须通过带 {@link ControllerAdvice} 的 Bean 注册，Spring MVC 才会调用 {@link ResponseBodyAdvice}。
     * 使用内部类 + 自动配置 {@link Bean}，避免在 Gateway 组件扫描时加载 Servlet 专用类。
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
     * 定义拦截的路径
     * @param requestLogInterceptor 日志拦截器
     * @return  这里拦截所有路径
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
