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
    // 默认5分钟 (5 * 60 * 1000)
    private long expireTime = 300000;

}
