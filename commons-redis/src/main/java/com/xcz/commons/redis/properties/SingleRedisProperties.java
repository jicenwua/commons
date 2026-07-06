package com.xcz.commons.redis.properties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConditionalOnProperty("redis.single.host")
@ConfigurationProperties(prefix = "redis.single")
public class SingleRedisProperties {
    //必须会有的配置
    String host;
    Integer port;

    String clientName;
    String password;
    String username;
    Integer timeout = 3000;
    Integer idleConnectionTimeout = 10000;
    Integer retryAttempts = 3;
    Integer retryDelay = 1500;
    boolean keepAlieve = true;
    boolean tcpNoDelay = true;
    Integer dnsMonitoringInterval = 1000;
    Connection connection = new Connection();
    Subscription subscription = new Subscription();

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Connection{
        Integer poolSize = 50;
        Integer minimumSize = 10;
        Integer timeout = 10000;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Subscription{
        Integer connection = 5;
        Integer minimumSize = 1;
        Integer poolSize = 50;
    }
}
