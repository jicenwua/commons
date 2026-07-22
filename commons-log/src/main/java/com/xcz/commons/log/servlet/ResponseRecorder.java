package com.xcz.commons.log.servlet;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 缓存响应体，供日志拦截器读取（ThreadLocal）。
 */
@ControllerAdvice
public class ResponseRecorder implements ResponseBodyAdvice<Object> {

    /**
     * 当前线程的响应体缓存
     */
    private static final ThreadLocal<Object> RESPONSE_BODY = new ThreadLocal<>();

    /**
     * 对所有返回值生效
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Class converterType) {
        return true;
    }

    /**
     * 序列化前缓存 body，原样返回
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        RESPONSE_BODY.set(body);
        return body;
    }

    /**
     * 获取当前线程缓存的响应体。
     *
     * @return 响应对象；无 body 时返回 {@code null}
     */
    public static Object getResponseBody() {
        return RESPONSE_BODY.get();
    }

    /**
     * 清理当前线程响应体缓存
     */
    public static void clear() {
        RESPONSE_BODY.remove();
    }
}
