package com.xcz.commons.core.log;

/**
 * 请求日志相关属性名常量。
 */
public final class RequestLogAttributes {

    /** 全局异常处理器捕获的异常 attribute key */
    public static final String EXCEPTION = "xcz.request.exception";

    /** LogFilter 缓存的原始请求体 attribute key */
    public static final String REQUEST_BODY = "xcz.request.body";

    private RequestLogAttributes() {
    }
}
