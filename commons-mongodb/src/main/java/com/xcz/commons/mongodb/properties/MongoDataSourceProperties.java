package com.xcz.commons.mongodb.properties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个 MongoDB 数据源连接配置。
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MongoDataSourceProperties {

    /** 业务数据库名 */
    String database;

    /** 认证用户名，为空时不启用认证 */
    String username;

    /** 认证密码 */
    String password;

    /** 认证库，管理员账号通常为 admin */
    String authenticationDatabase;

    /** 部署模式：STANDALONE / REPLICA_SET */
    DeployMode mode = DeployMode.STANDALONE;

    /** 服务器地址列表，与 ports 一一对应 */
    List<String> hosts = new ArrayList<>();

    /** 服务器端口列表，与 hosts 一一对应 */
    List<Integer> ports = new ArrayList<>();

    /** 连接池配置 */
    ConnectionPool connectionPool = new ConnectionPool();

    /** Socket 超时配置 */
    Socket socket = new Socket();

    /** 集群 / 副本集配置 */
    Cluster cluster = new Cluster();

    public enum DeployMode {
        /** 单机部署 */
        STANDALONE,
        /** 副本集部署 */
        REPLICA_SET
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ConnectionPool {

        /** 每台服务器最大连接数 */
        int maxSize = 100;

        /** 连接池最小空闲连接数 */
        int minSize = 0;

        /** 获取连接最大等待时间（毫秒） */
        int maxWaitTime = 120_000;

        /** 连接最大空闲时间（毫秒），0 表示不限制 */
        int maxConnectionIdleTime = 60_000;

        /** 连接最大存活时间（毫秒），0 表示不限制 */
        int maxConnectionLifeTime = 0;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Socket {

        /** TCP 连接建立超时（毫秒） */
        int connectTimeout = 10_000;

        /** Socket 读取超时（毫秒），0 表示不设置 */
        int readTimeout = 0;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Cluster {

        /** 副本集名称，副本集模式下必填 */
        String replicaSet;

        /** 选择可用服务器的最大等待时间（毫秒） */
        int serverSelectionTimeout = 30_000;

        /** 读偏好：primary / primaryPreferred / secondary / secondaryPreferred / nearest */
        String readPreference = "primary";

        /** 本地阈值（毫秒） */
        int localThreshold = 15;

        /** 是否自动重试写操作 */
        boolean retryWrites = true;

        /** 是否自动重试读操作 */
        boolean retryReads = true;

        /** 心跳检测间隔（毫秒） */
        int heartbeatFrequency = 10_000;
    }

    /**
     * 校验当前数据源配置。
     *
     * @param dataSourceName 数据源名称
     */
    public void validate(String dataSourceName) {
        String prefix = "mongo.datasources." + dataSourceName;

        if (hosts == null || hosts.isEmpty()) {
            throw new IllegalStateException(prefix + ".hosts 不能为空");
        }
        if (ports == null || ports.size() != hosts.size()) {
            throw new IllegalStateException(prefix + ".hosts 与 " + prefix + ".ports 数量必须一致");
        }
        if (!StringUtils.hasText(database)) {
            throw new IllegalStateException(prefix + ".database 不能为空");
        }

        if (mode == DeployMode.STANDALONE && hosts.size() > 1) {
            throw new IllegalStateException(prefix + " 单机模式（mode=standalone）仅允许配置一个 host");
        }

        if (mode == DeployMode.REPLICA_SET && !StringUtils.hasText(cluster.getReplicaSet())) {
            throw new IllegalStateException(prefix + " 副本集模式（mode=replica-set）必须配置 cluster.replica-set");
        }

        if (StringUtils.hasText(username) && password == null) {
            throw new IllegalStateException(prefix + " 已配置 username 时，password 不能为空");
        }
    }
}
