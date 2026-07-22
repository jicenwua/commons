package com.xcz.commons.log.servlet;

import com.xcz.commons.core.log.RequestLogAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 解析本次请求异常。
 */
public final class ExceptionRecorder {

    private ExceptionRecorder() {
    }

    /**
     * 按优先级从多处来源解析异常。
     *
     * @param request       当前请求
     * @param interceptorEx 拦截器传入的异常
     * @return 异常；无则返回 {@code null}
     */
    public static Throwable resolve(HttpServletRequest request, Exception interceptorEx) {
        if (interceptorEx != null) {
            return interceptorEx;
        }

        Throwable fromHandler = (Throwable) request.getAttribute(RequestLogAttributes.EXCEPTION);
        if (fromHandler != null) {
            return fromHandler;
        }

        Throwable servletError = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        if (servletError != null) {
            return servletError;
        }

        return (Throwable) request.getAttribute("org.springframework.web.servlet.DispatcherServlet.EXCEPTION");
    }
}
