package com.xcz.commons.log.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.log.RequestLogAttributes;
import com.xcz.commons.log.config.RequestLogProperties;
import com.xcz.commons.log.support.RequestLogSupport;
import com.xcz.commons.log.support.RequestLogUserIdResolver;
import com.xcz.commons.log.support.TraceIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebFlux / Gateway 请求日志 Filter。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReactiveLogFilter implements WebFilter {

    /**
     * exchange attribute：traceId
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * exchange attribute：请求开始时间戳
     */
    public static final String START_TIME_KEY = "xcz.request.startTime";

    private final RequestLogProperties properties;
    private final ObjectMapper objectMapper;
    private final RequestLogUserIdResolver userIdResolver;
    private final String appName;

    public ReactiveLogFilter(RequestLogProperties properties, ObjectMapper objectMapper,
                             RequestLogUserIdResolver userIdResolver, String appName) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.userIdResolver = userIdResolver;
        this.appName = appName;
    }

    /**
     * 生成 traceId，缓存请求/响应体，结束后打印日志
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = TraceIdGenerator.nextTraceId();
        exchange.getAttributes().put(TRACE_ID_KEY, traceId);
        exchange.getAttributes().put(START_TIME_KEY, System.currentTimeMillis());

        ServerHttpRequest request = exchange.getRequest();
        String contentType = request.getHeaders().getFirst("Content-Type");

        if (!shouldCacheBody(contentType)) {
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            return chain.filter(exchange)
                    .doOnEach(signal -> bindMdc(traceId))
                    .doOnError(errorRef::set)
                    .doFinally(signalType -> finishLog(exchange, traceId, null, null, errorRef.get()));
        }

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(exchange.getResponse().bufferFactory().allocateBuffer(0))
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    if (bytes.length > 0) {
                        dataBuffer.read(bytes);
                    }
                    DataBufferUtils.release(dataBuffer);

                    String requestBody = bytes.length > 0 ? new String(bytes, StandardCharsets.UTF_8) : null;
                    if (StringUtils.hasText(requestBody)) {
                        exchange.getAttributes().put(RequestLogAttributes.REQUEST_BODY, requestBody);
                    }

                    ServerHttpRequest decoratedRequest = decorateRequest(exchange, bytes);
                    AtomicReference<String> responseBodyRef = new AtomicReference<>();
                    ServerHttpResponse decoratedResponse = decorateResponse(exchange, responseBodyRef);

                    return chain.filter(exchange.mutate()
                                    .request(decoratedRequest)
                                    .response(decoratedResponse)
                                    .build())
                            .doOnEach(signal -> bindMdc(traceId))
                            .doOnError(errorRef::set)
                            .doFinally(signalType -> finishLog(exchange, traceId, requestBody, responseBodyRef.get(), errorRef.get()));
                });
    }

    /**
     * 链路结束后组装并输出日志。
     */
    private void finishLog(ServerWebExchange exchange, String traceId, String requestBody,
                           String responseBody, Throwable chainError) {
        try {
            bindMdc(traceId);
            if (!properties.isEnabled()) {
                return;
            }

            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            String method = request.getMethod().name();
            String uri = request.getURI().getPath();
            String contentType = request.getHeaders().getFirst("Content-Type");

            if (requestBody == null) {
                Object cached = exchange.getAttribute(RequestLogAttributes.REQUEST_BODY);
                if (cached instanceof String body) {
                    requestBody = body;
                }
            }

            String params = RequestLogSupport.buildRequestParams(objectMapper, properties,
                    toParameterMap(request.getQueryParams()), contentType, requestBody);

            Throwable error = chainError != null ? chainError : resolveException(exchange);
            Object respBody = parseResponseBody(responseBody);
            int status = response.getStatusCode() != null ? response.getStatusCode().value() : -1;
            String responseStr = RequestLogSupport.buildResponseStr(objectMapper, properties, respBody, error);

            Long userId = userIdResolver.resolveUserId();
            long cost = calculateCost(exchange);
            boolean isError = RequestLogSupport.isErrorResponse(status, respBody, error);
            String handlerDesc = method + " " + uri;

            String info = RequestLogSupport.buildLogMessage(appName, handlerDesc, method, uri,
                    userId, params, status, cost, responseStr, Thread.currentThread().getName(), traceId);

            if (isError) {
                boolean loggedByGlobalHandler = exchange.getAttribute(RequestLogAttributes.EXCEPTION) != null;
                if (error != null && !loggedByGlobalHandler) {
                    log.error(info, error);
                } else {
                    log.error(info);
                }
            } else {
                log.info(info);
            }
        } catch (Exception e) {
            log.error("响应式日志 Filter 打印异常", e);
        } finally {
            MDC.clear();
        }
    }

    /**
     * 将 traceId 写入 MDC
     */
    private static void bindMdc(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put("threadName", Thread.currentThread().getName());
    }

    /**
     * 计算请求耗时（毫秒）
     */
    private static long calculateCost(ServerWebExchange exchange) {
        Object start = exchange.getAttribute(START_TIME_KEY);
        if (start instanceof Long startTime) {
            return System.currentTimeMillis() - startTime;
        }
        return 0L;
    }

    /**
     * 读取全局异常处理器写入的异常
     */
    private static Throwable resolveException(ServerWebExchange exchange) {
        Object fromHandler = exchange.getAttribute(RequestLogAttributes.EXCEPTION);
        if (fromHandler instanceof Throwable throwable) {
            return throwable;
        }
        return null;
    }

    /**
     * 解析响应体为 JSON 对象；失败则截断原文
     */
    private Object parseResponseBody(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, Object.class);
        } catch (Exception ignored) {
            return RequestLogSupport.truncate(responseBody, properties.getMaxBodyLength());
        }
    }

    /**
     * 将 query 参数转为 Servlet 风格 Map
     */
    private static Map<String, String[]> toParameterMap(org.springframework.util.MultiValueMap<String, String> queryParams) {
        Map<String, String[]> map = new LinkedHashMap<>();
        queryParams.forEach((key, values) -> map.put(key, values.toArray(new String[0])));
        return map;
    }

    /**
     * 是否需要缓存请求体（multipart 跳过）
     */
    private static boolean shouldCacheBody(String contentType) {
        return contentType == null || !contentType.toLowerCase().startsWith("multipart/");
    }

    /**
     * 用缓存 byte[] 重新构造请求 body
     */
    private static ServerHttpRequest decorateRequest(ServerWebExchange exchange, byte[] bytes) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                if (bytes.length == 0) {
                    return Flux.empty();
                }
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                return Flux.just(buffer);
            }
        };
    }

    /**
     * 捕获响应体内容供日志打印
     */
    private static ServerHttpResponse decorateResponse(ServerWebExchange exchange, AtomicReference<String> responseBodyRef) {
        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return super.writeWith(Flux.from(body).map(dataBuffer -> {
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    DataBufferUtils.release(dataBuffer);

                    String chunk = new String(content, StandardCharsets.UTF_8);
                    responseBodyRef.updateAndGet(existing -> existing == null ? chunk : existing + chunk);
                    return bufferFactory().wrap(content);
                }));
            }
        };
    }
}
