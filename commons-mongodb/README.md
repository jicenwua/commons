# commons-mongodb

MongoDB 客户端模块，覆盖 Spring Boot 默认 Mongo 自动配置，提供更细粒度的连接池、Socket、副本集与读偏好控制。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-mongodb</artifactId>
</dependency>
```

本模块不依赖其他 commons 子模块。

## 激活条件

必须配置 `mongo.database`，否则自动配置不生效。

## 配置

前缀：`mongo`

### 单机模式

```yaml
mongo:
  database: mobi_blog              # 必填，业务库名
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
```

### 副本集模式

```yaml
mongo:
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

自动注册 `MongoClient`、`MongoDatabaseFactory`、`MongoTemplate`：

```java
@Autowired
private MongoTemplate mongoTemplate;

// 保存文档
mongoTemplate.save(document);

// 查询
List<Blog> blogs = mongoTemplate.find(query, Blog.class);
```

也可配合 Spring Data 的 `MongoRepository`（业务侧需添加 `@EnableMongoRepositories`）：

```java
@EnableMongoRepositories(basePackages = "com.xcz.blog.repository")
@SpringBootApplication
public class BlogApplication { }
```

## 主要类

| 类 | 职责 |
|----|------|
| `MongoConfig` | 自定义 `MongoClient` 与 `MongoTemplate` |
| `MongoSettingsProperties` | `mongo.*` 配置绑定 |

## 与 Spring Boot 默认配置的区别

| 方面 | 本模块 | Spring Boot 默认 |
|------|--------|-----------------|
| 配置前缀 | `mongo.*` | `spring.data.mongodb.*` |
| 连接池 | 可细粒度配置 | 使用驱动默认值 |
| 副本集 | 显式 `mode` + `cluster.*` | URI 字符串 |
| 激活条件 | `mongo.database` 必填 | `spring.data.mongodb.uri` |

引入本模块后，请使用 `mongo.*` 前缀配置，勿混用 `spring.data.mongodb.uri`。

## 注意事项

1. `hosts` 与 `ports` 列表长度需一致，按索引一一对应
2. 副本集模式下 `cluster.replica-set` 为必填项
3. 若同时引入 `spring-boot-starter-data-mongodb`，本模块的 `MongoConfig` 会接管客户端创建

[← 返回总览](../README.md)
