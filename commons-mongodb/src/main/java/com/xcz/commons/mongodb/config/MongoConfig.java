package com.xcz.commons.mongodb.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.xcz.commons.mongodb.properties.MongoSettingsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB 客户端与 {@link MongoTemplate} 的自动装配。
 * <p>
 * 通过自定义 {@link MongoClientSettings} 覆盖 Spring Boot 默认自动配置，
 * 以支持连接池、超时、副本集读偏好等细粒度控制。
 */
@AutoConfiguration
@EnableConfigurationProperties(MongoSettingsProperties.class)
@ConditionalOnProperty(prefix = "mongo", name = "database")
public class MongoConfig {

    @Autowired
    private MongoSettingsProperties mongoSettingsProperties;

    @Autowired
    private Environment environment;

    /**
     * 创建 MongoDB 原生客户端。
     * 根据 {@link MongoSettingsProperties#getMode()} 区分单机与副本集两种部署方式。
     */
    @Bean
    public MongoClient mongoClient() {
        validateSettings();

        List<ServerAddress> serverAddresses = buildServerAddresses();
        MongoCredential credential = buildCredential();
        MongoSettingsProperties.ConnectionPool pool = mongoSettingsProperties.getConnectionPool();
        MongoSettingsProperties.Socket socket = mongoSettingsProperties.getSocket();
        MongoSettingsProperties.Cluster cluster = mongoSettingsProperties.getCluster();

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
                    if (mongoSettingsProperties.getMode() == MongoSettingsProperties.DeployMode.REPLICA_SET) {
                        clusterBuilder.requiredReplicaSetName(cluster.getReplicaSet());
                    }
                });

        if (credential != null) {
            builder.credential(credential);
        }

        return MongoClients.create(builder.build());
    }

    /**
     * 绑定默认业务库，供 Spring Data MongoDB 使用。
     */
    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(
                mongoClient, mongoSettingsProperties.getDatabase());
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }

    /**
     * 校验 hosts / ports 及部署模式相关配置，启动阶段快速失败。
     */
    private void validateSettings() {
        List<String> hosts = mongoSettingsProperties.getHosts();
        List<Integer> ports = mongoSettingsProperties.getPorts();

        if (hosts == null || hosts.isEmpty()) {
            throw new IllegalStateException("mongo.hosts 不能为空");
        }
        if (ports == null || ports.size() != hosts.size()) {
            throw new IllegalStateException("mongo.hosts 与 mongo.ports 数量必须一致");
        }
        if (!StringUtils.hasText(mongoSettingsProperties.getDatabase())) {
            throw new IllegalStateException("mongo.database 不能为空");
        }

        if (mongoSettingsProperties.getMode() == MongoSettingsProperties.DeployMode.STANDALONE
                && hosts.size() > 1) {
            throw new IllegalStateException("单机模式（mongo.mode=standalone）仅允许配置一个 host");
        }

        if (mongoSettingsProperties.getMode() == MongoSettingsProperties.DeployMode.REPLICA_SET
                && !StringUtils.hasText(mongoSettingsProperties.getCluster().getReplicaSet())) {
            throw new IllegalStateException("副本集模式（mongo.mode=replica-set）必须配置 mongo.cluster.replica-set");
        }

        if (StringUtils.hasText(mongoSettingsProperties.getUsername())
                && mongoSettingsProperties.getPassword() == null) {
            throw new IllegalStateException("已配置 mongo.username 时，mongo.password 不能为空");
        }
    }

    /**
     * 根据 hosts / ports 构建服务器地址列表。
     * 单机模式仅取第一个节点；副本集模式将全部节点作为种子列表。
     */
    private List<ServerAddress> buildServerAddresses() {
        List<String> hosts = mongoSettingsProperties.getHosts();
        List<Integer> ports = mongoSettingsProperties.getPorts();
        int limit = mongoSettingsProperties.getMode() == MongoSettingsProperties.DeployMode.STANDALONE
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
    private MongoCredential buildCredential() {
        if (!StringUtils.hasText(mongoSettingsProperties.getUsername())) {
            return null;
        }

        String authDatabase = StringUtils.hasText(mongoSettingsProperties.getAuthenticationDatabase())
                ? mongoSettingsProperties.getAuthenticationDatabase()
                : mongoSettingsProperties.getDatabase();

        return MongoCredential.createCredential(
                mongoSettingsProperties.getUsername(),
                authDatabase,
                mongoSettingsProperties.getPassword().toCharArray());
    }

    /**
     * 将配置字符串映射为 MongoDB 读偏好枚举。
     */
    private ReadPreference resolveReadPreference(String value) {
        if (!StringUtils.hasText(value)) {
            return ReadPreference.primary();
        }

        return switch (value.replace("_", "").toLowerCase()) {
            case "primary" -> ReadPreference.primary();
            case "primarypreferred" -> ReadPreference.primaryPreferred();
            case "secondary" -> ReadPreference.secondary();
            case "secondarypreferred" -> ReadPreference.secondaryPreferred();
            case "nearest" -> ReadPreference.nearest();
            default -> throw new IllegalArgumentException("不支持的 mongo.cluster.read-preference: " + value);
        };
    }
}
