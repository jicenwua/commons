package com.xcz.commons.log.servlet;

import com.xcz.commons.core.log.RequestLogAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Servlet 栈异常记录读取工具。
 * <p>
 * 全局异常处理器通过 {@link RequestLogAttributes#EXCEPTION} 写入异常，
 * 本类在日志拦截器阶段统一解析，兼容未进入 Advice 的异常场景。
 */
public final class ExceptionRecorder {

    private ExceptionRecorder() {
    }

    /**
     * 从多个来源解析本次请求发生的异常（按优先级依次尝试）。
     *
     * @param request       当前 HTTP 请求
     * @param interceptorEx 拦截器 {@code afterCompletion} 传入的异常（未处理异常时有值）
     * @return 解析到的异常；无异常时返回 {@code null}
     */
    public static Throwable resolve(HttpServletRequest request, Exception interceptorEx) {
        // 1. 拦截器直接传入的未处理异常
        if (interceptorEx != null) {
            return interceptorEx;
        }

        // 2. GlobalExceptionHandler 写入的异常（已被 Advice 处理，ex 通常为 null）
        Throwable fromHandler = (Throwable) request.getAttribute(RequestLogAttributes.EXCEPTION);
        if (fromHandler != null) {
            return fromHandler;
        }

        // 3. Spring Boot 转发 /error 前的 Servlet 异常
        Throwable servletError = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        if (servletError != null) {
            return servletError;
        }

        // 4. DispatcherServlet 记录的异常
        return (Throwable) request.getAttribute("org.springframework.web.servlet.DispatcherServlet.EXCEPTION");
    }
}
