# Commons 公共基础模块

`commons` 是 Mobi 系列微服务项目的 **Maven 多模块公共库**，为业务服务提供统一的基础能力：通用工具、请求日志、认证授权、Redis、关系型数据库、对象存储与 MongoDB 等。

| 属性 | 值 |
|------|-----|
| GroupId | `com.xcz.commons` |
| Version | `0.0.1-SNAPSHOT` |
| Java | 21 |
| Spring Boot | 3.2.0 |
| Spring Cloud | 2023.0.0 |

## 模块一览

| 模块 | 说明 | 文档 |
|------|------|------|
| [commons-core](./commons-core/README.md) | 核心工具、统一响应、异常体系、Excel/IP/文件等 | 基础层，几乎所有模块都依赖它 |
| [commons-log](./commons-log/README.md) | 请求日志与 traceId，兼容 Servlet MVC 与 WebFlux | 引入即用 |
| [commons-redis](./commons-redis/README.md) | 基于 Redisson 的 Redis 客户端封装 | 需 `redis.enabled=true` |
| [commons-security](./commons-security/README.md) | JWT + Redis 会话、权限校验、白名单 | 依赖 commons-redis |
| [commons-database](./commons-database/README.md) | MyBatis-Plus + Druid + 动态数据源 | 关系型数据库访问 |
| [commons-oss](./commons-oss/README.md) | 阿里云 OSS 文件上传 | 按需引入 |
| [commons-mongodb](./commons-mongodb/README.md) | MongoDB 客户端与连接池配置 | 按需引入 |
| [commons-protobuf](./commons-protobuf/README.md) | 注解驱动 Protobuf 运行时序列化与 Schema 生成 | 按需引入 |

## 模块依赖关系

```
commons-core          ← 最底层
    ↑
commons-log ──────────┐
commons-redis ────────┤
    ↑                 │
commons-security      │
                      │
commons-database      （独立）
commons-oss           （独立）
commons-mongodb       （独立）
commons-protobuf      （独立）
```

## 快速开始

### 1. 引入父工程（版本统一管理）

在业务项目的 `pom.xml` 中声明父工程或 `dependencyManagement`：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.xcz.commons</groupId>
            <artifactId>commons</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 按需添加子模块

典型微服务（Servlet MVC）常用组合：

```xml
<dependencies>
    <dependency>
        <groupId>com.xcz.commons</groupId>
        <artifactId>commons-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xcz.commons</groupId>
        <artifactId>commons-log</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xcz.commons</groupId>
        <artifactId>commons-security</artifactId>
    </dependency>
    <dependency>
        <groupId>com.xcz.commons</groupId>
        <artifactId>commons-database</artifactId>
    </dependency>
</dependencies>
```

### 3. 最小配置示例

```yaml
spring:
  application:
    name: member-service

security:
  jwt:
    secret: your-256-bit-secret-key-at-least-32-chars
    expiration: 7200000
  ignore:
    urls:
      - /actuator/**

redis:
  enabled: true
  single:
    host: localhost
    port: 6379

log:
  request:
    enabled: true
```

更多配置项见各子模块文档。

## 自动配置机制

本项目使用 **Spring Boot 3** 标准自动配置，入口文件为各模块下的：

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

引入对应 Maven 依赖后，相关 Bean 会自动注册，无需手动 `@Import`。

## 典型应用场景

| 场景 | 推荐模块 |
|------|----------|
| 管理后台 / 会员服务（Servlet） | core + log + security + database |
| API 网关（WebFlux） | core + log + security |
| 博客 / 内容服务（MongoDB） | core + log + security + mongodb |
| 文件上传服务 | core + oss |
| 仅工具类（无 Web） | core |

## 构建

```bash
mvn clean install
```

安装到本地仓库后，业务项目即可通过 `com.xcz.commons` 坐标引用各子模块。

## 注意事项

1. **commons-security 依赖 Redis**：`TokenService`、`PermissionUtils` 等需要 `redis.enabled=true`，否则启动或运行时会失败。
2. **Servlet 与 Reactive 隔离**：log、security 模块通过 `@ConditionalOnWebApplication` 区分，Gateway（Reactive）与业务服务（Servlet）可共存于同一依赖树。
3. **IP 归属地**：`commons-core` 的 `IpUtils` 需要在 classpath 放置 `ip2region/ip2region.xdb` 数据文件。
4. **Mapper 扫描路径**：`commons-database` 默认扫描 `com.xcz.**.mapper`，业务 Mapper 需放在该包路径下。

## 子模块文档

- [commons-core](./commons-core/README.md)
- [commons-log](./commons-log/README.md)
- [commons-redis](./commons-redis/README.md)
- [commons-security](./commons-security/README.md)
- [commons-database](./commons-database/README.md)
- [commons-oss](./commons-oss/README.md)
- [commons-mongodb](./commons-mongodb/README.md)
