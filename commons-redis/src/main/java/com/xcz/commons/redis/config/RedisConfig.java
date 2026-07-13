package com.xcz.commons.redis.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xcz.commons.redis.extend.DatabaseBeanName;
import com.xcz.commons.redis.extend.DatabaseEnum;
import com.xcz.commons.redis.properties.ClusterRedisProperties;
import com.xcz.commons.redis.properties.SingleRedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.ConstantDelay;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.redisson.connection.balancer.RoundRobinLoadBalancer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.TimeZone;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({SingleRedisProperties.class, ClusterRedisProperties.class})
@ConditionalOnProperty(prefix = "redis", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {
    @Autowired
    private Environment env;
    @Autowired(required = false)
    private SingleRedisProperties singleRedisProperties;
    @Autowired(required = false)
    private ClusterRedisProperties clusterRedisProperties;



    /**
     * 创建单体redis配置
     * @param singleRedisProperties 配置文件配置
     * @param database  使用数据库编号
     * @return  redissonClient
     */
    public RedissonClient redissonSingleClient(SingleRedisProperties singleRedisProperties, int database) {
        Config config = new Config();

        String servername = env.getProperty("spring.application.name","spring-server");
        SingleRedisProperties.Connection connection = singleRedisProperties.getConnection();
        SingleRedisProperties.Subscription subscription = singleRedisProperties.getSubscription();

        config.useSingleServer()
                .setClientName(singleRedisProperties.getClientName() == null ? servername : singleRedisProperties.getClientName())
                .setAddress("redis://" + singleRedisProperties.getHost() + ":" + singleRedisProperties.getPort())
                .setDatabase(database)
                .setUsername(singleRedisProperties.getUsername() == null ? null : singleRedisProperties.getUsername())
                .setPassword(singleRedisProperties.getPassword() == null ? null : singleRedisProperties.getPassword())

                // === 连接池配置 ===
                .setConnectionPoolSize(connection.getPoolSize())         // 最大连接数
                .setConnectionMinimumIdleSize(connection.getMinimumSize())  // 最小空闲连接数
                .setConnectTimeout(connection.getTimeout())          // 连接超时时间（毫秒），默认10000

                // === 超时配置 ===
                .setTimeout(singleRedisProperties.getTimeout())                  // 命令等待超时时间（毫秒），默认3000
                .setIdleConnectionTimeout(singleRedisProperties.getIdleConnectionTimeout())   // 空闲连接超时时间，超过后关闭（毫秒）

                // === 重试配置 ===
                .setRetryAttempts(3)               // 命令失败重试次数，默认3
                .setRetryDelay(
                        new ConstantDelay(Duration.ofMillis(singleRedisProperties.getRetryDelay()))
                ) // 重试间隔时间（毫秒），默认1500

                // === 订阅配置 ===
                .setSubscriptionsPerConnection(subscription.getConnection())  // 每个连接的订阅数量
                .setSubscriptionConnectionMinimumIdleSize(subscription.getMinimumSize())  // 订阅连接最小空闲数
                .setSubscriptionConnectionPoolSize(subscription.getPoolSize())         // 订阅连接池大小

                // === 其他配置 ===
                .setDnsMonitoringInterval(singleRedisProperties.getDnsMonitoringInterval())                // DNS监控间隔（毫秒）
                .setKeepAlive(singleRedisProperties.isKeepAlieve())                            // 是否启用TCP保活
                .setTcpNoDelay(singleRedisProperties.isTcpNoDelay());                          // 是否禁用Nagle算法

        config.setCodec(objectMapper());
        return Redisson.create(config);
    }


    public RedissonClient redissonClusterClient(ClusterRedisProperties clusterRedisProperties) {
        Config config = new Config();
        String serviceName = env.getProperty("spring.application.name","spring-server");
        String[] nodeAddresses = clusterRedisProperties.getNodes().toArray(new String[0]);

        ClusterRedisProperties.Connection connection = clusterRedisProperties.getConnection();
        ClusterRedisProperties.Subscription subscription = clusterRedisProperties.getSubscription();

        config.useClusterServers()
                .setClientName(clusterRedisProperties.getClientName() != null
                        ? clusterRedisProperties.getClientName() : serviceName)
                .addNodeAddress(nodeAddresses)
                .setPassword(StringUtils.hasText(clusterRedisProperties.getPassword())
                        ? clusterRedisProperties.getPassword() : null)
                .setUsername(StringUtils.hasText(clusterRedisProperties.getUsername())
                        ? clusterRedisProperties.getUsername() : null)

                .setReadMode(ReadMode.valueOf(clusterRedisProperties.getReadMode()))
                .setSubscriptionMode(SubscriptionMode.valueOf(clusterRedisProperties.getSubscriptionMode()))

                .setLoadBalancer(new RoundRobinLoadBalancer())

                .setMasterConnectionPoolSize(connection.getMasterConnectionPoolSize())
                .setMasterConnectionMinimumIdleSize(connection.getMasterConnectionMinimumIdleSize())
                .setSlaveConnectionPoolSize(connection.getSlaveConnectionPoolSize())
                .setSlaveConnectionMinimumIdleSize(connection.getSlaveConnectionMinimumIdleSize())

                .setTimeout(clusterRedisProperties.getTimeout())
                .setConnectTimeout(clusterRedisProperties.getConnectTimeout())
                .setIdleConnectionTimeout(clusterRedisProperties.getIdleConnectionTimeout())

                .setRetryAttempts(clusterRedisProperties.getRetryAttempts())
                .setRetryDelay(
                        new ConstantDelay(Duration.ofMillis(clusterRedisProperties.getRetryDelay()))
                )

                .setSubscriptionsPerConnection(subscription.getSubscriptionsPerConnection())
                .setSubscriptionConnectionMinimumIdleSize(subscription.getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(subscription.getConnectionPoolSize())

                .setDnsMonitoringInterval(clusterRedisProperties.getDnsMonitoringInterval())
                .setKeepAlive(clusterRedisProperties.isKeepAlive())
                .setTcpNoDelay(clusterRedisProperties.isTcpNoDelay())
                ;
        config.setCodec(objectMapper());
        return Redisson.create(config);
    }

    public JsonJacksonCodec objectMapper() {
        // JSON 序列化配置
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        // 时间相关配置
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        //设置时间时域
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        // 处理未知属性（避免反序列化时出错）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 处理空值（跳过序列化空值）
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        // 设置json中字段的输出顺序按照字母顺序输出
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        // 激活默认类型信息，用于多态类型反序列化
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );

        return new JsonJacksonCodec(mapper);
    }

    @Primary
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME0)
    public RedissonClient redisDatabase0(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_0);
    }

    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME1)
    public RedissonClient redisDatabase1(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_1);
    }

    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME2)
    public RedissonClient redisDatabase2(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_2);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME3)
    public RedissonClient redisDatabase3(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_3);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME4)
    public RedissonClient redisDatabase4(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_4);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME5)
    public RedissonClient redisDatabase5(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_5);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME6)
    public RedissonClient redisDatabase6(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_6);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME7)
    public RedissonClient redisDatabase7(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_7);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME8)
    public RedissonClient redisDatabase8(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_8);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME9)
    public RedissonClient redisDatabase9(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_9);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME10)
    public RedissonClient redisDatabase10(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_10);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME11)
    public RedissonClient redisDatabase11(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_11);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME12)
    public RedissonClient redisDatabase12(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_12);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME13)
    public RedissonClient redisDatabase13(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_13);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME14)
    public RedissonClient redisDatabase14(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_14);
    }
    @Lazy
    @Bean(DatabaseBeanName.DATABASE_BEAN_NAME15)
    public RedissonClient redisDatabase15(){
        return checkRedissonConfig(DatabaseEnum.DATABASE_15);
    }


    private RedissonClient checkRedissonConfig(DatabaseEnum databaseEnum){
        if(singleRedisProperties != null){
            return redissonSingleClient(singleRedisProperties,databaseEnum.getDatabase());
        }else if(clusterRedisProperties != null){
            return redissonClusterClient(clusterRedisProperties);
        }else {
            throw new IllegalArgumentException("请配置Redis");
        }
    }
}
