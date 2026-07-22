package com.xcz.commons.log.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.log.RequestLogAttributes;
import com.xcz.commons.log.config.RequestLogProperties;
import com.xcz.commons.log.support.HandlerMethodLinkResolver;
import com.xcz.commons.log.support.LogContext;
import com.xcz.commons.log.support.RequestLogSupport;
import com.xcz.commons.log.support.RequestLogUserIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Servlet 请求日志拦截器，请求结束后统一打印。
 */
@Slf4j
@RequiredArgsConstructor
public class RequestLogInterceptor implements HandlerInterceptor {

    private final RequestLogProperties properties;
    private final ObjectMapper objectMapper;
    private final RequestLogUserIdResolver userIdResolver;
    private final String appName;


    /**
     * 记录请求开始时间
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (properties.isEnabled()) {
            LogContext.setStartTime();
        }
        return true;
    }

    /**
     * 请求结束后打印调试日志
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

            String params = RequestLogSupport.buildRequestParams(objectMapper, properties,
                    request.getParameterMap(), request.getContentType(), resolveRequestBody(request));

            Object respBody = ResponseRecorder.getResponseBody();
            Throwable error = ExceptionRecorder.resolve(request, ex);
            String responseStr = RequestLogSupport.buildResponseStr(objectMapper, properties, respBody, error);

            Long userId = userIdResolver.resolveUserId();
            int status = response.getStatus();
            boolean isError = RequestLogSupport.isErrorResponse(status, respBody, error);

            HandlerMethodLinkResolver.ControllerMethodLink methodLink = HandlerMethodLinkResolver.resolve(handler);
            String info = RequestLogSupport.buildLogMessage(appName, methodLink.navigationLink(), method, uri,
                    userId, params, status, cost, responseStr, threadName, traceId);

            if (isError) {
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
     * 计算请求耗时（毫秒）
     */
    private long calculateCost() {
        Long start = LogContext.getStartTime();
        return (start != null) ? (System.currentTimeMillis() - start) : 0L;
    }

    /**
     * 读取缓存的请求体。
     *
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
