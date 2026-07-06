package com.xcz.commons.redis.properties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConditionalOnProperty(name = "redis.cluster.nodes")
@ConfigurationProperties(prefix = "redis.cluster")
public class ClusterRedisProperties {

    // 集群节点列表（必须配置）
    List<String> nodes;

    // 认证配置
    String password;
    String username;

    // 超时配置（毫秒）
    Integer timeout = 3000;
    Integer connectTimeout = 10000;
    Integer idleConnectionTimeout = 10000;

    // 重试配置
    Integer retryAttempts = 3;
    Integer retryDelay = 1500;

    // TCP 配置
    boolean keepAlive = true;
    boolean tcpNoDelay = true;

    // DNS 监控间隔（毫秒）
    Integer dnsMonitoringInterval = 5000;

    // 客户端名称
    String clientName;

    // 读取模式
    String readMode = "SLAVE";  // MASTER, SLAVE, MASTER_SLAVE

    // 订阅模式
    String subscriptionMode = "MASTER";  // MASTER, SLAVE

    // 连接池配置
    Connection connection = new Connection();

    // 订阅配置
    Subscription subscription = new Subscription();

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Connection {
        Integer poolSize = 64;
        Integer minimumIdleSize = 24;
        Integer masterConnectionPoolSize = 64;
        Integer masterConnectionMinimumIdleSize = 24;
        Integer slaveConnectionPoolSize = 64;
        Integer slaveConnectionMinimumIdleSize = 24;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Subscription {
        Integer subscriptionsPerConnection = 5;
        Integer connectionMinimumIdleSize = 1;
        Integer connectionPoolSize = 50;
    }
}
