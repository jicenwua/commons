package com.xcz.member.log.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.member.core.log.RequestLogAttributes;
import com.xcz.member.log.config.RequestLogProperties;
import com.xcz.member.log.support.HandlerMethodLinkResolver;
import com.xcz.member.log.support.LogContext;
import com.xcz.member.log.support.RequestLogSupport;
import com.xcz.member.log.support.RequestLogUserIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Servlet 请求日志拦截器。
 * <p>
 * 在请求完全结束后统一打印：用户、入参（query + form + body）、HTTP 状态、
 * 响应体、耗时及 traceId。错误请求使用 {@code log.error} 并附带异常栈。
 * <p>
 * 打印行为由 {@code request.log.enabled} 控制。
 */
@Slf4j
@RequiredArgsConstructor
public class RequestLogInterceptor implements HandlerInterceptor {

    private final RequestLogProperties properties;
    private final ObjectMapper objectMapper;
    private final RequestLogUserIdResolver userIdResolver;
    private final String appName;


    /**
     * 请求进入 Controller 前记录开始时间。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (properties.isEnabled()) {
            LogContext.setStartTime();
        }
        return true;
    }

    /**
     * 请求完全结束后（含全局异常处理）打印调试日志。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            if (!properties.isEnabled()) {
                return;
            }

            long cost = calculateCost();
            String traceId = MDC.get("traceId");
            String threadName = MDC.get("threadName");
            String uri = request.getRequestURI();
            String method = request.getMethod();

            // 合并 query / form / body，覆盖 GET 与 POST/PUT 等所有常见场景
            String params = RequestLogSupport.buildRequestParams(objectMapper, properties,
                    request.getParameterMap(), request.getContentType(), resolveRequestBody(request));

            // 读取 ResponseBodyAdvice 捕获的响应体，并解析本次异常（若有）
            Object respBody = ResponseRecorder.getResponseBody();
            Throwable error = ExceptionRecorder.resolve(request, ex);
            String responseStr = RequestLogSupport.buildResponseStr(objectMapper, properties, respBody, error);

            Long userId = userIdResolver.resolveUserId();
            int status = response.getStatus();
            boolean isError = RequestLogSupport.isErrorResponse(status, respBody, error);

            // 解析 Controller 方法，生成 IDE 可点击跳转链接（堆栈格式）
            HandlerMethodLinkResolver.ControllerMethodLink methodLink = HandlerMethodLinkResolver.resolve(handler);
            String info = RequestLogSupport.buildLogMessage(appName, methodLink.navigationLink(), method, uri,
                    userId, params, status, cost, responseStr, threadName, traceId);

            if (isError) {
                // 全局异常处理器已记录完整栈时，此处仅打印请求摘要，避免重复刷屏
                boolean loggedByGlobalHandler = request.getAttribute(RequestLogAttributes.EXCEPTION) != null;
                if (error != null && !loggedByGlobalHandler) {
                    log.error(info, error);
                } else {
                    log.error(info);
                }
            } else {
                log.info(info);
            }
        } catch (Exception e) {
            log.error("日志拦截器打印异常", e);
        } finally {
            LogContext.clear();
            ResponseRecorder.clear();
        }
    }

    /**
     * 计算请求耗时（毫秒）。
     */
    private long calculateCost() {
        Long start = LogContext.getStartTime();
        return (start != null) ? (System.currentTimeMillis() - start) : 0L;
    }

    /**
     * 读取缓存的请求体：优先 request attribute，再沿 Wrapper 链查找。
     *
     * @param request 当前请求
     * @return 请求体字符串；无 body 时返回 {@code null}
     */
    private String resolveRequestBody(HttpServletRequest request) {
        Object cached = request.getAttribute(RequestLogAttributes.REQUEST_BODY);
        if (cached instanceof String body && StringUtils.hasText(body)) {
            return body;
        }
        CachedBodyHttpServletRequest wrapped = CachedBodyHttpServletRequest.resolve(request);
        return wrapped == null ? null : wrapped.getBody();
    }
}
