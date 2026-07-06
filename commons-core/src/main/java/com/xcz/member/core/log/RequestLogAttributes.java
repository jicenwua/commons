package com.xcz.member.core.log;

/**
 * 请求日志相关属性名常量。
 * <p>
 * 供全局异常处理器写入、日志拦截器读取，避免模块间硬编码字符串。
 */
public final class RequestLogAttributes {

    /**
     * 全局异常处理器捕获的异常，存入 {@link jakarta.servlet.http.HttpServletRequest} 的 attribute key。
     */
    public static final String EXCEPTION = "xcz.request.exception";

    /**
     * LogFilter 缓存的原始请求体 attribute key，
     * 供日志拦截器读取（避免外层 HttpServletRequestWrapper 导致 body 丢失）。
     */
    public static final String REQUEST_BODY = "xcz.request.body";

    private RequestLogAttributes() {
    }
}
