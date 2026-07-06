package com.xcz.commons.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 请求日志配置项，前缀 {@code request.log}。
 * <p>
 * 引入 {@code commons-log} 后可通过配置文件控制日志打印行为，无需额外 Java 配置。
 */
@Data
@ConfigurationProperties(prefix = "log.request")
public class RequestLogProperties {

    /**
     * 是否打印请求/响应调试日志。
     * <p>
     * 默认 {@code false}：仅生成 traceId 写入 MDC，不输出详细请求摘要。
     */
    private boolean enabled = false;

    /**
     * 请求体 / 响应体日志最大长度，超出部分截断，避免超大 payload 刷屏或 OOM。
     */
    private int maxBodyLength = 4096;
}
