package com.xcz.commons.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String region;
    private String bucketName;
    /** CDN / 自定义访问域名（如 cdn.example.com），仅用于拼永久 URL；上传删除仍走 OSS */
    private String domain;
    // 默认5分钟 (5 * 60 * 1000)
    private long expireTime = 300000;

}
