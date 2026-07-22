package com.xcz.commons.log.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.utils.LogSanitizer;
import com.xcz.commons.core.web.vo.params.AjaxResult;
import com.xcz.commons.log.config.RequestLogProperties;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 请求日志格式化与判定（Servlet / Reactive 共用）。
 */
public final class RequestLogSupport {

    private RequestLogSupport() {
    }

    /**
     * 截断过长字符串。
     *
     * @param content   原始内容
     * @param maxLength 最大保留长度
     * @return 截断后的字符串；未超限时原样返回
     */
    public static String truncate(String content, int maxLength) {
        if (content == null || content.length() <= maxLength || maxLength <= 0) {
            return content;
        }
        return content.substring(0, maxLength) + "...(已截断，共" + content.length() + "字符)";
    }

    /**
     * 判断 Content-Type 是否为 JSON。
     */
    public static boolean isJsonContentType(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    /**
     * 按内容前缀判断是否像 JSON（Content-Type 缺失时兜底）。
     */
    public static boolean looksLikeJson(String body) {
        if (body == null) {
            return false;
        }
        String trimmed = body.stripLeading();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    /**
     * 将请求体转为日志友好格式：JSON 反序列化，否则截断原文。
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
     * 合并 query/form 与请求体，返回脱敏后的 JSON。
     *
     * @return 脱敏参数字符串；无参数时返回 {@code {}}
     */
    public static String buildRequestParams(ObjectMapper objectMapper, RequestLogProperties properties,
                                            Map<String, String[]> parameterMap, String contentType, String body)
            throws JsonProcessingException {
        Map<String, Object> all = new LinkedHashMap<>();

        if (parameterMap != null && !parameterMap.isEmpty()) {
            all.put("params", parameterMap);
        }

        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            all.put("body", "[multipart/form-data 已跳过]");
        } else if (StringUtils.hasText(body)) {
            all.put("body", parseBodyForLog(objectMapper, body, contentType, properties.getMaxBodyLength()));
        }

        return all.isEmpty() ? "{}" : LogSanitizer.sanitizeJson(objectMapper.writeValueAsString(all), objectMapper);
    }

    /**
     * 构建响应日志字段；无 body 时回退为异常摘要。
     */
    public static String buildResponseStr(ObjectMapper objectMapper, RequestLogProperties properties,
                                          Object respBody, Throwable error) throws JsonProcessingException {
        if (respBody != null) {
            Object sanitized = LogSanitizer.sanitizeObject(respBody, objectMapper);
            String bodyStr = truncate(objectMapper.writeValueAsString(sanitized), properties.getMaxBodyLength());
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
     * 判断是否为错误响应（HTTP 状态、业务 code 或未处理异常）。
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
     * 组装多行调试日志。
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
