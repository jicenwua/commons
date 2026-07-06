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
 * <p>
 * 在响应式环境下通过装饰请求/响应流实现与 Servlet 栈一致的日志能力：
 * traceId、入参、出参、耗时、异常摘要。Gateway 无 Controller，handler 描述为 {@code METHOD /path}。
 * <p>
 * 打印行为由 {@code request.log.enabled} 控制；traceId 始终写入 exchange attribute 与 MDC。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReactiveLogFilter implements WebFilter {

    /** exchange attribute：traceId */
    public static final String TRACE_ID_KEY = "traceId";

    /** exchange attribute：请求开始时间（毫秒时间戳） */
    public static final String START_TIME_KEY = "xcz.request.startTime";

    private final RequestLogProperties properties;
    private final ObjectMapper objectMapper;
    private final RequestLogUserIdResolver userIdResolver;
    private final String appName;

    /**
     * @param properties     日志配置
     * @param objectMapper   JSON 序列化工具
     * @param userIdResolver   用户 ID 解析器
     * @param appName        应用名（{@code spring.application.name}）
     */
    public ReactiveLogFilter(RequestLogProperties properties, ObjectMapper objectMapper,
                             RequestLogUserIdResolver userIdResolver, String appName) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.userIdResolver = userIdResolver;
        this.appName = appName;
    }

    /**
     * 请求入口：生成 traceId，按需缓存请求体并装饰响应流，在 {@code doFinally} 中统一打印日志。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = TraceIdGenerator.nextTraceId();
        exchange.getAttributes().put(TRACE_ID_KEY, traceId);
        exchange.getAttributes().put(START_TIME_KEY, System.currentTimeMillis());

        ServerHttpRequest request = exchange.getRequest();
        String contentType = request.getHeaders().getFirst("Content-Type");

        // multipart 等不缓存 body 的场景：直接放行，仅在结束时打印摘要
        if (!shouldCacheBody(contentType)) {
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            return chain.filter(exchange)
                    .doOnEach(signal -> bindMdc(traceId))
                    .doOnError(errorRef::set)
                    .doFinally(signalType -> finishLog(exchange, traceId, null, null, errorRef.get()));
        }

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        // 响应式 body 为 Flux<DataBuffer>，需 join 后缓存再重新供给下游
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
     * 请求链路结束后组装并输出日志（含异常与响应体摘要）。
     *
     * @param exchange     当前交换对象
     * @param traceId      链路 traceId
     * @param requestBody  已缓存的请求体（可为 {@code null}）7
     * @param responseBody 装饰器捕获的响应体字符串（可为 {@code null}）
     * @param chainError   Filter 链中抛出的异常（可为 {@code null}）
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

            // doFinally 回调中 requestBody 可能未传入，回退到 attribute
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
            // Gateway 无 HandlerMethod，用 METHOD + path 作为 handler 描述
            String handlerDesc = method + " " + uri;

            String info = RequestLogSupport.buildLogMessage(appName, handlerDesc, method, uri,
                    userId, params, status, cost, responseStr, Thread.currentThread().getName(), traceId);

            if (isError) {
                // 全局异常处理器已记录完整栈时，此处仅打印请求摘要
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
     * 将 traceId 绑定到 MDC，便于 Reactor 线程切换后日志仍能关联 traceId。
     */
    private static void bindMdc(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put("threadName", Thread.currentThread().getName());
    }

    /**
     * 根据 exchange 中记录的开始时间计算耗时（毫秒）。
     */
    private static long calculateCost(ServerWebExchange exchange) {
        Object start = exchange.getAttribute(START_TIME_KEY);
        if (start instanceof Long startTime) {
            return System.currentTimeMillis() - startTime;
        }
        return 0L;
    }

    /**
     * 从 exchange attribute 读取全局异常处理器写入的异常。
     */
    private static Throwable resolveException(ServerWebExchange exchange) {
        Object fromHandler = exchange.getAttribute(RequestLogAttributes.EXCEPTION);
        if (fromHandler instanceof Throwable throwable) {
            return throwable;
        }
        return null;
    }

    /**
     * 将捕获的响应体字符串解析为 JSON 对象；非 JSON 时截断返回原始文本。
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
     * 将 WebFlux query 参数转为 Servlet 风格的 {@code Map<String, String[]>}，复用共用格式化逻辑。
     */
    private static Map<String, String[]> toParameterMap(org.springframework.util.MultiValueMap<String, String> queryParams) {
        Map<String, String[]> map = new LinkedHashMap<>();
        queryParams.forEach((key, values) -> map.put(key, values.toArray(new String[0])));
        return map;
    }

    /**
     * 判断是否需要缓存请求体（multipart 跳过）。
     */
    private static boolean shouldCacheBody(String contentType) {
        return contentType == null || !contentType.toLowerCase().startsWith("multipart/");
    }

    /**
     * 装饰请求：用缓存的 byte[] 重新构造 body Flux，供下游 Filter / Handler 重复读取。
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
     * 装饰响应：在写回客户端时逐块捕获 body 内容，供 finishLog 打印。
     */
    private static ServerHttpResponse decorateResponse(ServerWebExchange exchange, AtomicReference<String> responseBodyRef) {
        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return super.writeWith(Flux.from(body).map(dataBuffer -> {
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    DataBufferUtils.release(dataBuffer);

                    // 流式响应可能分多次 writeWith，需拼接
                    String chunk = new String(content, StandardCharsets.UTF_8);
                    responseBodyRef.updateAndGet(existing -> existing == null ? chunk : existing + chunk);
                    return bufferFactory().wrap(content);
                }));
            }
        };
    }
}
