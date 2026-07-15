package com.xcz.commons.mongodb.factory;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.xcz.commons.mongodb.properties.MongoDataSourceProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 根据单数据源配置创建 {@link MongoClient}。
 * <p>
 * 封装 {@link MongoClientSettings} 的构建逻辑，供多数据源场景复用。
 */
public final class MongoClientFactory {

    private MongoClientFactory() {
    }

    /**
     * 创建 MongoDB 原生客户端。
     *
     * @param dataSourceName 数据源名称，仅用于校验错误提示
     * @param properties     单数据源连接配置
     * @param environment    Spring 环境，用于读取 {@code spring.application.name}
     * @return 已配置的 {@link MongoClient} 实例
     */
    public static MongoClient create(String dataSourceName, MongoDataSourceProperties properties,
                                     Environment environment) {
        properties.validate(dataSourceName);

        List<ServerAddress> serverAddresses = buildServerAddresses(properties);
        MongoCredential credential = buildCredential(properties);
        MongoDataSourceProperties.ConnectionPool pool = properties.getConnectionPool();
        MongoDataSourceProperties.Socket socket = properties.getSocket();
        MongoDataSourceProperties.Cluster cluster = properties.getCluster();

        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                // 便于在 MongoDB 服务端日志中识别当前应用
                .applicationName(environment.getProperty("spring.application.name", "spring-server"))
                // 写 / 读失败时的自动重试策略
                .retryWrites(cluster.isRetryWrites())
                .retryReads(cluster.isRetryReads())
                // 读偏好：副本集模式下决定从主节点还是从节点读取
                .readPreference(resolveReadPreference(cluster.getReadPreference()))
                // 连接池：控制并发连接数量与连接生命周期
                .applyToConnectionPoolSettings(poolBuilder -> poolBuilder
                        .maxSize(pool.getMaxSize())
                        .minSize(pool.getMinSize())
                        .maxWaitTime(pool.getMaxWaitTime(), TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(pool.getMaxConnectionIdleTime(), TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(pool.getMaxConnectionLifeTime(), TimeUnit.MILLISECONDS))
                // Socket：控制 TCP 连接与读取超时
                .applyToSocketSettings(socketBuilder -> socketBuilder
                        .connectTimeout(socket.getConnectTimeout(), TimeUnit.MILLISECONDS)
                        .readTimeout(socket.getReadTimeout(), TimeUnit.MILLISECONDS))
                // 节点心跳：副本集下用于及时发现故障节点
                .applyToServerSettings(serverBuilder -> serverBuilder
                        .heartbeatFrequency(cluster.getHeartbeatFrequency(), TimeUnit.MILLISECONDS))
                // 集群：种子节点、副本集名称、选主超时
                .applyToClusterSettings(clusterBuilder -> {
                    clusterBuilder
                            .hosts(serverAddresses)
                            .serverSelectionTimeout(cluster.getServerSelectionTimeout(), TimeUnit.MILLISECONDS)
                            .localThreshold(cluster.getLocalThreshold(), TimeUnit.MILLISECONDS);
                    if (properties.getMode() == MongoDataSourceProperties.DeployMode.REPLICA_SET) {
                        clusterBuilder.requiredReplicaSetName(cluster.getReplicaSet());
                    }
                });

        if (credential != null) {
            builder.credential(credential);
        }

        return MongoClients.create(builder.build());
    }

    /**
     * 格式化 hosts 摘要，用于启动日志输出。
     *
     * @param properties 单数据源配置
     * @return 形如 {@code 127.0.0.1:27017, 10.0.0.2:27017} 的端点列表
     */
    public static String formatHostsSummary(MongoDataSourceProperties properties) {
        List<String> hosts = properties.getHosts();
        List<Integer> ports = properties.getPorts();
        int limit = properties.getMode() == MongoDataSourceProperties.DeployMode.STANDALONE
                ? 1
                : hosts.size();

        List<String> endpoints = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            endpoints.add(hosts.get(i) + ":" + ports.get(i));
        }
        return String.join(", ", endpoints);
    }

    /**
     * 根据 hosts / ports 构建服务器地址列表。
     * 单机模式仅取第一个节点；副本集模式将全部节点作为种子列表。
     */
    private static List<ServerAddress> buildServerAddresses(MongoDataSourceProperties properties) {
        List<String> hosts = properties.getHosts();
        List<Integer> ports = properties.getPorts();
        int limit = properties.getMode() == MongoDataSourceProperties.DeployMode.STANDALONE
                ? 1
                : hosts.size();

        List<ServerAddress> serverAddresses = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            serverAddresses.add(new ServerAddress(hosts.get(i), ports.get(i)));
        }
        return serverAddresses;
    }

    /**
     * 构建 SCRAM 认证信息；未配置用户名时返回 null，表示匿名连接。
     */
    private static MongoCredential buildCredential(MongoDataSourceProperties properties) {
        if (!StringUtils.hasText(properties.getUsername())) {
            return null;
        }

        String authDatabase = StringUtils.hasText(properties.getAuthenticationDatabase())
                ? properties.getAuthenticationDatabase()
                : properties.getDatabase();

        return MongoCredential.createCredential(
                properties.getUsername(),
                authDatabase,
                properties.getPassword().toCharArray());
    }

    /**
     * 将配置字符串映射为 MongoDB 读偏好枚举。
     */
    private static ReadPreference resolveReadPreference(String value) {
        if (!StringUtils.hasText(value)) {
            return ReadPreference.primary();
        }

        return switch (value.replace("_", "").toLowerCase()) {
            case "primary" -> ReadPreference.primary();
            case "primarypreferred" -> ReadPreference.primaryPreferred();
            case "secondary" -> ReadPreference.secondary();
            case "secondarypreferred" -> ReadPreference.secondaryPreferred();
            case "nearest" -> ReadPreference.nearest();
            default -> throw new IllegalArgumentException("不支持的 cluster.read-preference: " + value);
        };
    }
}
