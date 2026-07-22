package com.xcz.commons.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 请求日志配置，前缀 {@code log.request}。
 */
@Data
@ConfigurationProperties(prefix = "log.request")
public class RequestLogProperties {

    /**
     * 是否打印请求/响应调试日志，默认关闭
     */
    private boolean enabled = false;

    /**
     * 请求体/响应体最大长度，≤0 表示不截断
     */
    private int maxBodyLength = -1;
}
