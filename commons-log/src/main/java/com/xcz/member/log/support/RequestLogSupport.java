package com.xcz.member.log.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.member.core.utils.LogSanitizer;
import com.xcz.member.core.web.vo.params.AjaxResult;
import com.xcz.member.log.config.RequestLogProperties;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet / Reactive 共用的请求日志格式化与判定逻辑。
 * <p>
 * 抽取两套栈重复的参数合并、响应序列化、错误判定与日志模板组装，保持输出格式一致。
 */
public final class RequestLogSupport {

    private RequestLogSupport() {
    }

    /**
     * 截断过长字符串，防止日志刷屏。
     *
     * @param content   原始内容
     * @param maxLength 最大保留长度
     * @return 截断后的字符串；{@code content} 为 {@code null} 或不超过限制时原样返回
     */
    public static String truncate(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...(已截断，共" + content.length() + "字符)";
    }

    /**
     * 判断 Content-Type 是否为 JSON 类型。
     *
     * @param contentType 请求 Content-Type
     * @return {@code true} 表示包含 json 标识
     */
    public static boolean isJsonContentType(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    /**
     * 通过内容前缀启发式判断是否为 JSON 文本（Content-Type 缺失时的兜底）。
     *
     * @param body 请求体字符串
     * @return {@code true} 表示以 {@code {}或[]} 开头
     */
    public static boolean looksLikeJson(String body) {
        if (body == null) {
            return false;
        }
        String trimmed = body.stripLeading();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    /**
     * 将请求体解析为日志友好格式：JSON 反序列化为对象便于阅读，否则截断原始文本。
     *
     * @param objectMapper JSON 工具
     * @param body         原始请求体
     * @param contentType  Content-Type
     * @param maxLength    非 JSON 时的截断长度
     * @return 用于日志输出的对象或字符串
     */
    public static Object parseBodyForLog(ObjectMapper objectMapper, String body, String contentType, int maxLength) {
        if (isJsonContentType(contentType) || looksLikeJson(body)) {
            try {
                return objectMapper.readValue(body, Object.class);
            } catch (Exception ignored) {
                // 非合法 JSON 时回退为原始文本
            }
        }
        return truncate(body, maxLength);
    }

    /**
     * 合并 query / form 参数与请求体，输出脱敏后的 JSON 字符串。
     * <p>
     * multipart 请求不读取 body，仅作占位说明，避免内存膨胀并破坏文件解析。
     *
     * @param objectMapper  JSON 工具
     * @param properties    日志配置
     * @param parameterMap  query 或 form 参数
     * @param contentType   Content-Type
     * @param body          缓存的请求体
     * @return 脱敏后的参数字符串；无参数时返回 {@code {}}
     */
    public static String buildRequestParams(ObjectMapper objectMapper, RequestLogProperties properties,
                                            Map<String, String[]> parameterMap, String contentType, String body)
            throws JsonProcessingException {
        Map<String, Object> all = new LinkedHashMap<>();

        // query 参数 + application/x-www-form-urlencoded 表单参数
        if (parameterMap != null && !parameterMap.isEmpty()) {
            all.put("params", parameterMap);
        }

        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            // 文件上传：不缓存 body，此处仅作占位说明
            all.put("body", "[multipart/form-data 已跳过]");
        } else if (StringUtils.hasText(body)) {
            all.put("body", parseBodyForLog(objectMapper, body, contentType, properties.getMaxBodyLength()));
        }

        return all.isEmpty() ? "{}" : LogSanitizer.sanitizeJson(objectMapper.writeValueAsString(all), objectMapper);
    }

    /**
     * 构建响应日志字段：优先使用 Advice / Filter 捕获的 body，无 body 时回退到异常摘要。
     *
     * @param objectMapper JSON 工具
     * @param properties   日志配置
     * @param respBody     响应体对象
     * @param error        本次请求异常（若有）
     * @return 用于日志输出的响应摘要字符串
     */
    public static String buildResponseStr(ObjectMapper objectMapper, RequestLogProperties properties,
                                          Object respBody, Throwable error) throws JsonProcessingException {
        if (respBody != null) {
            Object sanitized = LogSanitizer.sanitizeObject(respBody, objectMapper);
            String bodyStr = truncate(objectMapper.writeValueAsString(sanitized), properties.getMaxBodyLength());
            // 全局异常处理器已返回错误 JSON 时，追加异常摘要便于快速定位
            if (error != null) {
                return error.toString();
            }
            return bodyStr;
        }
        if (error != null) {
            return error.toString();
        }
        return "无响应体(或未触发 ResponseBodyAdvice)";
    }

    /**
     * 判断是否为错误响应：HTTP 状态码、业务 code 或存在未处理异常。
     *
     * @param status   HTTP 状态码
     * @param respBody 响应体（可为 {@link AjaxResult} 对应的 Map）
     * @param error    未处理异常
     * @return {@code true} 表示应使用 {@code log.error} 打印
     */
    public static boolean isErrorResponse(int status, Object respBody, Throwable error) {
        if (error != null || status >= HttpStatus.BAD_REQUEST.value()) {
            return true;
        }
        if (respBody instanceof Map<?, ?> map) {
            Object code = map.get(AjaxResult.CODE_TAG);
            return code instanceof Number number && number.intValue() != HttpStatus.OK.value();
        }
        return false;
    }

    /**
     * 组装格式化的多行调试日志。
     * <p>
     * 首行使用短格式 {@code 类名.方法名(类名.java:行号)} 或 {@code METHOD /uri}，
     * IDE 可点击跳转（Servlet 栈）。
     *
     * @param appName      应用名（{@code spring.application.name}）
     * @param handlerDesc  处理器描述（Controller 链接或路由）
     * @param method       HTTP 方法
     * @param uri          请求 URI
     * @param userId       当前用户 ID
     * @param params       入参 JSON
     * @param status       HTTP 状态码
     * @param cost         耗时（毫秒）
     * @param responseStr  响应摘要
     * @param threadName   线程名
     * @param traceId      链路 traceId
     * @return 多行格式化日志文本
     */
    public static String buildLogMessage(String appName, String handlerDesc, String method, String uri,
                                         Long userId, String params, int status, long cost, String responseStr,
                                         String threadName, String traceId) {
        return "\n" +
                "┏━━━━━ " + handlerDesc + " ━━━━━ [" + method + " " + uri + "] ━━━━━ [" + appName + "] ━━━━━━━━━\n" +
                "┣ 用户Id: " + userId + "\n" +
                "┣ 参数: " + params + "\n" +
                "┣ 耗时: " + cost + " ms\n" +
                "┣ 响应: " + responseStr + "\n" +
                "┗━━━━━━━━━━ [线程:" + threadName + "] ━━━━━ [traceId:" + traceId + "] ━━━━━━━━━━  [" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                "]  ━━━━━━━━━━━━━━━━━━━";
    }
}
