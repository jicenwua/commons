package com.xcz.commons.mongodb.properties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB 连接配置，对应 Nacos / application 中 {@code mongo.*} 前缀项。
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "mongo")
public class MongoSettingsProperties {

    /*** 业务数据库名 **/
    String database;

    /*** 认证用户名，为空时不启用认证 **/
    String username;

    /*** 认证密码 **/
    String password;

    /**
     * 认证库。
     * 使用 root 等管理员账号时通常为 {@code admin}，而非业务库名。
     */
    String authenticationDatabase;

    /**
     * 部署模式：
     * <ul>
     *   <li>{@link DeployMode#STANDALONE} — 单机，仅使用第一个 host</li>
     *   <li>{@link DeployMode#REPLICA_SET} — 副本集，hosts 作为种子节点列表</li>
     * </ul>
     */
    DeployMode mode = DeployMode.STANDALONE;

    /*** 服务器地址列表，与 {@link #ports} 一一对应 **/
    List<String> hosts = new ArrayList<>();

    /*** 服务器端口列表，与 {@link #hosts} 一一对应 **/
    List<Integer> ports = new ArrayList<>();

    /*** 连接池相关配置 **/
    ConnectionPool connectionPool = new ConnectionPool();

    /*** Socket 超时配置 **/
    Socket socket = new Socket();

    /*** 集群 / 副本集相关配置 **/
    Cluster cluster = new Cluster();

    public enum DeployMode {
        /*** 单机部署 **/
        STANDALONE,
        /*** 副本集部署 **/
        REPLICA_SET
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ConnectionPool {

        /*** 每台服务器允许的最大连接数 **/
        int maxSize = 100;

        /*** 连接池维持的最小空闲连接数 **/
        int minSize = 0;

        /*** 从连接池获取连接的最大等待时间（毫秒） **/
        int maxWaitTime = 120_000;

        /**
         * 连接最大空闲时间（毫秒）。
         * 超过该时间未使用的连接会被关闭；0 表示不限制。
         */
        int maxConnectionIdleTime = 60_000;

        /**
         * 连接最大存活时间（毫秒）。
         * 超过该时间的连接会被关闭并重建；0 表示不限制。
         */
        int maxConnectionLifeTime = 0;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Socket {

        /*** TCP 连接建立超时（毫秒） **/
        int connectTimeout = 10_000;

        /**
         * Socket 读取超时（毫秒）。
         * 0 表示不设置读取超时。
         */
        int readTimeout = 0;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Cluster {

        /*** 副本集名称，{@link DeployMode#REPLICA_SET} 模式下必填 **/
        String replicaSet;

        /*** 选择可用服务器的最大等待时间（毫秒） **/
        int serverSelectionTimeout = 30_000;

        /**
         * 读偏好，副本集模式下生效。
         * 可选值：primary / primaryPreferred / secondary / secondaryPreferred / nearest
         */
        String readPreference = "primary";

        /*** 本地阈值（毫秒），配合 nearest 等读偏好使用 **/
        int localThreshold = 15;

        /*** 是否自动重试写操作（副本集推荐开启） **/
        boolean retryWrites = true;

        /*** 是否自动重试读操作 **/
        boolean retryReads = true;

        /*** 心跳检测间隔（毫秒），用于监测节点可用性 **/
        int heartbeatFrequency = 10_000;
    }
}
