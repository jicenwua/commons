# commons-mongodb

MongoDB 客户端模块，覆盖 Spring Boot 默认 Mongo 自动配置，支持多数据源，并提供连接池、Socket、副本集与读偏好等细粒度控制。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-mongodb</artifactId>
</dependency>
```

本模块不依赖其他 commons 子模块。

## 激活条件

必须配置 `mongo.primary` 与 `mongo.datasources`，否则自动配置不生效。

## 配置

前缀：`mongo`

### 多数据源

```yaml
mongo:
  primary: primary
  datasources:
    primary:
      database: mobi_blog
      username: root                   # 可选
      password: secret                 # 配置 username 时必填
      authentication-database: admin   # 认证库，默认等于 database
      mode: standalone                 # standalone | replica-set
      hosts:
        - 127.0.0.1
      ports:
        - 27017
      connection-pool:
        max-size: 100
        min-size: 0
        max-wait-time: 120000
        max-connection-idle-time: 60000
        max-connection-life-time: 0
      socket:
        connect-timeout: 10000
        read-timeout: 0
    analytics:
      database: mobi_analytics
      hosts:
        - 10.0.0.2
      ports:
        - 27017
```

`mongo.primary` 指定主数据源名称，主库会额外注册无 qualifier 的 `mongoClient`、`mongoTemplate` 等 Bean（带 `@Primary`）。

每个数据源还会注册命名 Bean：

| Bean 名称 | 说明 |
|-----------|------|
| `{name}MongoClient` | MongoDB 客户端 |
| `{name}MongoDatabaseFactory` | 数据库工厂 |
| `{name}MongoTemplate` | 操作模板 |
| `{name}MongoTransactionManager` | 事务管理器（须显式指定，非默认） |
| `mongoDBTransactionManager` | 主库事务管理器（使用 `@MongoDBTransactional` 或显式指定） |

例如主库：`primaryMongoTemplate`；分析库：`analyticsMongoTemplate`。

`@Transactional` 默认仍使用 JDBC 的 `transactionManager`；Mongo 事务仅在副本集环境下通过 `@MongoDBTransactional` 按需启用。

### 副本集模式

```yaml
mongo:
  primary: primary
  datasources:
    primary:
      database: mobi_blog
      mode: replica-set
      hosts:
        - mongo1
        - mongo2
        - mongo3
      ports:
        - 27017
        - 27017
        - 27017
      cluster:
        replica-set: rs0               # 必填
        server-selection-timeout: 30000
        read-preference: secondaryPreferred
        # primary / primaryPreferred / secondary / secondaryPreferred / nearest
        local-threshold: 15
        retry-writes: true
        retry-reads: true
        heartbeat-frequency: 10000
```

启动时会校验配置完整性并打印 ASCII Banner。

## 使用方式

主数据源可直接注入默认 Bean：

```java
@Autowired
private MongoTemplate mongoTemplate;
```

其他数据源通过 `@Qualifier` 注入：

```java
@Qualifier("analyticsMongoTemplate")
private MongoTemplate analyticsMongoTemplate;
```

副本集环境下，MongoDB 写操作需要事务时使用 `@MongoDBTransactional`（勿在单机 MongoDB 上使用）：

```java
import com.xcz.commons.mongodb.transaction.MongoDBTransactional;

@MongoDBTransactional
public void saveArticleWithTags(Article article, List<Tag> tags) {
    articleRepository.save(article);
    tagRepository.saveAll(tags);
}
```

纯 MySQL 操作继续使用 Spring 自带的 `@Transactional` 即可，默认走 JDBC 的 `transactionManager`。

配合 Spring Data 的 `MongoRepository` 时，需为不同包指定 `mongoTemplateRef`：

```java
@Configuration
@EnableMongoRepositories(
    basePackages = "com.xcz.blog.repository",
    mongoTemplateRef = "primaryMongoTemplate"
)
public class PrimaryMongoRepositoryConfig {}

@Configuration
@EnableMongoRepositories(
    basePackages = "com.xcz.analytics.repository",
    mongoTemplateRef = "analyticsMongoTemplate"
)
public class AnalyticsMongoRepositoryConfig {}
```

## 主要类

| 类 | 职责 |
|----|------|
| `MongoConfig` | 多数据源自动装配入口 |
| `MongoDataSourceBeanDefinitionRegistrar` | 动态注册各数据源 Bean |
| `MongoDBTransactional` | 绑定 `mongoDBTransactionManager` 的便捷注解 |
| `MongoClientFactory` | 创建 `MongoClient` |
| `MongoSettingsProperties` | `mongo.*` 顶层配置 |
| `MongoDataSourceProperties` | 单数据源 `mongo.datasources.<name>.*` 配置 |

## 与 Spring Boot 默认配置的区别

| 方面 | 本模块 | Spring Boot 默认 |
|------|--------|-----------------|
| 配置前缀 | `mongo.*` | `spring.data.mongodb.*` |
| 多数据源 | `mongo.datasources` | 需手动配置 |
| 连接池 | 可细粒度配置 | 使用驱动默认值 |
| 副本集 | 显式 `mode` + `cluster.*` | URI 字符串 |
| 激活条件 | `mongo.primary` 必填 | `spring.data.mongodb.uri` |

引入本模块后，请使用 `mongo.*` 前缀配置，勿混用 `spring.data.mongodb.uri`。

## 注意事项

1. `hosts` 与 `ports` 列表长度需一致，按索引一一对应
2. 副本集模式下 `cluster.replica-set` 为必填项
3. 若同时引入 `spring-boot-starter-data-mongodb`，本模块会接管客户端创建
4. Mongo 事务管理器 Bean 名为 `mongoDBTransactionManager`，非默认 Primary，不会覆盖 JDBC 事务；跨库操作不支持分布式事务

[← 返回总览](../README.md)
