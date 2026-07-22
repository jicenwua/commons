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
 * 生成 traceId，并按需缓存请求体。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogFilter extends OncePerRequestFilter {

    /**
     * 设置 MDC，并按 Content-Type 决定是否缓存 body
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = TraceIdGenerator.nextTraceId();
        MDC.put("traceId", traceId);
        MDC.put("threadName", Thread.currentThread().getName());

        try {
            if (shouldCacheBody(request)) {
                CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
                String body = wrappedRequest.getBody();
                if (body != null && !body.isBlank()) {
                    wrappedRequest.setAttribute(RequestLogAttributes.REQUEST_BODY, body);
                }
                filterChain.doFilter(wrappedRequest, response);
            } else {
                filterChain.doFilter(request, response);
            }
        } finally {
            MDC.clear();
        }
    }

    /**
     * 是否需要缓存请求体（multipart 跳过）。
     */
    private boolean shouldCacheBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType == null || !contentType.toLowerCase().startsWith("multipart/");
    }
}
