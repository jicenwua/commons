package com.xcz.member.log.servlet;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Servlet 响应体记录器。
 * <p>
 * 利用 {@link ResponseBodyAdvice}，在响应写回客户端前将 body 存入 ThreadLocal，
 * 供 {@link RequestLogInterceptor} 在 {@code afterCompletion} 阶段读取。
 * <p>
 * 由 {@link ServletRequestLogAutoConfiguration} 以带 {@code @ControllerAdvice} 的内部类 Bean 注册；
 * 顶层类<b>不可</b>直接标注 {@code @ControllerAdvice}，否则 Gateway 组件扫描时会因缺少 spring-webmvc 而启动失败。
 * <p>
 * 全局异常处理器返回的 {@code AjaxResult} 同样会经过此 Advice，因此错误响应可被记录。
 */
public class ResponseRecorder implements ResponseBodyAdvice<Object> {

    /** 当前线程的响应体缓存 */
    private static final ThreadLocal<Object> RESPONSE_BODY = new ThreadLocal<>();

    /**
     * 对所有返回值生效（含 Controller 与 {@code @ExceptionHandler} 的返回值）。
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Class converterType) {
        return true;
    }

    /**
     * 响应序列化前缓存 body，原样返回不影响业务逻辑。
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
     * @return 响应对象；无 body（如 void 返回）时返回 {@code null}
     */
    public static Object getResponseBody() {
        return RESPONSE_BODY.get();
    }

    /**
     * 清理当前线程的响应体缓存，防止线程池复用导致数据串扰。
     */
    public static void clear() {
        RESPONSE_BODY.remove();
    }
}
