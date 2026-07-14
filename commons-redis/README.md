# commons-redis

基于 **Redisson** 的 Redis 客户端封装，支持单机与集群模式，预注册 16 个逻辑库（database0 ~ database15）对应的 `RedissonClient` Bean。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-redis</artifactId>
</dependency>
```

依赖 [commons-core](../commons-core/README.md)。被 [commons-security](../commons-security/README.md) 用于 Token 会话与权限缓存。

## 激活条件

**必须**配置 `redis.enabled=true`，否则整个自动配置不生效。

```yaml
redis:
  enabled: true
```

## 配置

### 单机模式

需配置 `redis.single.host`：

```yaml
redis:
  enabled: true
  single:
    host: 127.0.0.1
    port: 6379
    username:              # 可选
    password:              # 可选
    client-name:           # 默认 spring.application.name
    timeout: 3000
    idle-connection-timeout: 10000
    retry-attempts: 3
    retry-delay: 1500
    keep-alieve: true
    tcp-no-delay: true
    connection:
      pool-size: 50
      minimum-size: 10
      timeout: 10000
    subscription:
      connection: 5
      minimum-size: 1
      pool-size: 50
```

### 集群模式

需配置 `redis.cluster.nodes`（与单机二选一）：

```yaml
redis:
  enabled: true
  cluster:
    nodes:
      - redis://host1:6379
      - redis://host2:6379
    password: ""
    read-mode: SLAVE              # MASTER / SLAVE / MASTER_SLAVE
    subscription-mode: MASTER     # MASTER / SLAVE
    connection:
      master-connection-pool-size: 64
      slave-connection-pool-size: 64
```

## 使用方式

### 推荐：通过枚举获取客户端

```java
RedissonClient client = RedisUtil.getRedisson(DatabaseEnum.DATABASE_0);
RBucket<String> bucket = client.getBucket("key");
bucket.set("value");
```

### 直接注入 Bean

```java
@Autowired
@Qualifier("database1")
private RedissonClient redis1;
```

`database0` 标注了 `@Primary`，可省略 `@Qualifier`。

## 逻辑库划分

| 枚举 | Bean 名 | 典型用途（业务约定） |
|------|---------|---------------------|
| `DATABASE_0` | `database0` | 通用缓存（默认） |
| `DATABASE_1` ~ `DATABASE_15` | `database1` ~ `database15` | 按业务隔离 |

具体库号分配由业务项目自行约定；security 模块使用 `DATABASE_0` 存储登录会话与权限数据。

## 序列化

使用 `JsonJacksonCodec`，支持 Jackson 默认类型信息，对象可直接存取。

## 主要类

| 类 | 职责 |
|----|------|
| `RedisConfig` | Redisson 自动配置，注册 16 个客户端 |
| `SingleRedisProperties` | 单机配置绑定 |
| `ClusterRedisProperties` | 集群配置绑定 |
| `RedisUtil` | 按 `DatabaseEnum` 获取客户端 |
| `DatabaseEnum` | 逻辑库编号与 Bean 名映射 |

## 注意事项

1. security 模块的 `TokenService`、`PermissionUtils` **强依赖**本模块，未启用 Redis 时无法正常工作
2. 单机与集群通过 `@ConditionalOnProperty` 互斥激活，勿同时配置 `redis.single.host` 与 `redis.cluster.nodes`

[← 返回总览](../README.md)
