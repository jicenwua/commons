# commons-log

请求日志模块，兼容 **Servlet MVC** 与 **WebFlux / Gateway**。引入依赖后自动注册 Filter / Interceptor，为每个请求生成 `traceId` 并写入 MDC，可按开关输出详细请求/响应摘要。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-log</artifactId>
</dependency>
```

依赖 [commons-core](../commons-core/README.md)，compile 时不依赖 security，运行时通过反射解析用户 ID。

## 工作原理

```
请求进入
    │
    ├─ Servlet: LogFilter → 生成 traceId、缓存 body
    │           RequestLogInterceptor → 请求结束后打印摘要
    │           ResponseRecorder → 捕获响应体
    │
    └─ Reactive: ReactiveLogFilter → 一体化处理
```

- **traceId**：始终生成并写入 MDC（`%X{traceId}`），与 `log.request.enabled` 无关
- **详细日志**：`enabled=true` 时输出方法、URI、耗时、请求/响应体（超长截断）
- **用户 ID**：若 classpath 存在 `SecurityUtils`，自动从当前登录用户解析；否则为 `0`

## 配置

前缀：`log.request`

```yaml
log:
  request:
    enabled: false          # 是否打印详细请求/响应日志，默认 false
    max-body-length: 4096   # body 截断长度
```

配合标准属性：

```yaml
spring:
  application:
    name: member-service   # 日志中显示应用名
```

### Logback 引用 traceId

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{traceId}] %-5level %logger - %msg%n</pattern>
```

## 自动配置

| 类 | 条件 | 注册内容 |
|----|------|----------|
| `SecurityRequestLogUserIdAutoConfiguration` | 存在 `SecurityUtils` | `RequestLogUserIdResolver` |
| `ServletRequestLogAutoConfiguration` | Servlet + WebMVC | `LogFilter`、`RequestLogInterceptor`、`ResponseRecorder` |
| `ReactiveRequestLogAutoConfiguration` | Reactive + `WebFilter` | `ReactiveLogFilter` |

## 自定义用户 ID 解析

```java
@Bean
public RequestLogUserIdResolver requestLogUserIdResolver() {
    return () -> myCustomUserId;
}
```

## 主要类

| 类 | 职责 |
|----|------|
| `LogFilter` | Servlet 入口 Filter，生成 traceId |
| `CachedBodyHttpServletRequest` | 可重复读取的请求体包装 |
| `RequestLogInterceptor` | 请求完成后输出摘要 |
| `ResponseRecorder` | `ResponseBodyAdvice`，捕获响应体 |
| `ReactiveLogFilter` | WebFlux 请求日志 |
| `TraceIdGenerator` | traceId 生成策略 |
| `HandlerMethodLinkResolver` | 生成可跳转的 Controller 方法链接 |

## 注意事项

1. **multipart 请求**不缓存 body，避免大文件占用内存
2. 与 [commons-security](../commons-security/README.md) 的全局异常处理协作：读取 `RequestLogAttributes.EXCEPTION`，避免重复打印堆栈
3. Gateway 等 Reactive 应用只会激活 `ReactiveRequestLogAutoConfiguration`，不会加载 Servlet 相关 Bean

[← 返回总览](../README.md)
