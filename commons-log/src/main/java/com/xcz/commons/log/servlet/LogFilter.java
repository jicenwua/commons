package com.xcz.commons.log.servlet;

import com.xcz.commons.core.log.RequestLogAttributes;
import com.xcz.commons.log.support.TraceIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet 请求日志 Filter：生成 traceId，并按需包装请求体以便后续日志打印。
 * <p>
 * 需在 Filter 链靠前执行（{@link Ordered#HIGHEST_PRECEDENCE}），
 * 确保后续 DispatcherServlet 收到的是 {@link CachedBodyHttpServletRequest}。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogFilter extends OncePerRequestFilter {

    /**
     * 为每个请求设置 MDC 上下文，并按 Content-Type 决定是否缓存 body。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 生成全局唯一的 traceId，便于链路追踪
        String traceId = TraceIdGenerator.nextTraceId();
        MDC.put("traceId", traceId);
        MDC.put("threadName", Thread.currentThread().getName());

        try {
            //判断是否需要缓存请求体
            if (shouldCacheBody(request)) {
                // JSON / form 等请求：包装后 body 可被日志拦截器与 Controller 重复读取
                CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
                String body = wrappedRequest.getBody();
                if (body != null && !body.isBlank()) {
                    wrappedRequest.setAttribute(RequestLogAttributes.REQUEST_BODY, body);
                }
                filterChain.doFilter(wrappedRequest, response);
            } else {
                // multipart 文件上传：不缓存 body，避免内存膨胀并破坏文件解析
                filterChain.doFilter(request, response);
            }
        } finally {
            MDC.clear();
        }
    }

    /**
     * 判断是否需要缓存请求体。
     * <p>
     * multipart/form-data 跳过缓存，其余类型（含 JSON、urlencoded、无 Content-Type 的 GET）均包装。
     *
     * @param request 当前请求
     * @return {@code true} 表示需要缓存并包装
     */
    private boolean shouldCacheBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType == null || !contentType.toLowerCase().startsWith("multipart/");
    }
}
