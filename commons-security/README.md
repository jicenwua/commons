# commons-security

认证与授权模块，提供 **JWT + Redis 会话**、方法级权限校验、白名单、`@Release` 免登录、`@InnerAuth` 内部调用保护。同时支持 **Servlet MVC** 与 **WebFlux**。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-security</artifactId>
</dependency>
```

依赖 [commons-redis](../commons-redis/README.md)（间接依赖 [commons-core](../commons-core/README.md)）。

## 架构概览

```
请求
  │
  ├─ HeaderAuthenticationFilter (Servlet)
  │  或 HeadReactAuthenticationFilter (Reactive)
  │      │
  │      ├─ 白名单 / @Release 路径 → 放行
  │      ├─ 解析 JWT → TokenService 续签 / 校验
  │      └─ 权限版本变更 → 自动刷新 token，回写响应头
  │
  └─ @PreAuthorize("@ss.hasPermi('xxx')") → PermissionExpression
```

## 配置

### 必填项

```yaml
security:
  jwt:
    secret: your-256-bit-secret-key-at-least-32-chars   # HS512 签名密钥
    expiration: 7200000                                  # 过期时间（毫秒）
```

启动时 `TokenService` 会校验上述两项，缺失则启动失败。

### 可选项

```yaml
security:
  jwt:
    is-refresh: false       # 是否动态续期 Redis 会话 TTL
  ignore:
    urls:                   # Ant 风格白名单，与 @Release 扫描结果合并
      - /public/**
      - /actuator/**
```

### Redis（必填）

```yaml
redis:
  enabled: true
  single:
    host: localhost
    port: 6379
```

`security.jwt` 与 `security.ignore` 支持 `@RefreshScope`（Nacos 动态刷新）。

## 注解与 API

### 免登录接口 `@Release`

标注在类或方法上，启动时由 `ReleasePathCollector` 扫描并加入白名单：

```java
@Release
@GetMapping("/public/info")
public AjaxResult info() { ... }
```

### 方法级权限

```java
@PreAuthorize("@ss.hasPermi('system:user:list')")
public AjaxResult list() { ... }

@PreAuthorize("@ss.hasRole('admin')")
public void adminOnly() { ... }
```

表达式 Bean 名为 `ss`（`PermissionExpression`）。

### 内部调用 `@InnerAuth`

```java
@InnerAuth
@PostMapping("/internal/sync")
public void sync() { ... }
```

切面校验：请求头 `authorization` 必须存在，且 `from-source` 头必须为空。

### 编程式权限

```java
PermissionUtils.hasPermi("order:create");
PermissionUtils.checkRole("shop_admin");

// 管理端修改权限后广播
PermissionUtils.publishRoleVersion("admin", version);
PermissionUtils.addRoleChange(userId, List.of("admin"));
```

### Token 与用户

```java
@Autowired TokenService tokenService;

// 登录后发 token
String token = tokenService.createToken(loginUser);

// 获取当前用户
LoginUser user = SecurityUtils.getLoginUser();
Long userId = SecurityUtils.getUserId();
```

## Redis 键约定

| 键模式 | 用途 |
|--------|------|
| `login:body:{token}` | 登录用户缓存 |
| `login:refresh` | 续期 Set 缓存 |
| `sys:role:permission` | 角色权限 Hash |
| `sys:role:user:{userId}` | 用户角色变更标记 |
| `version:topic` | 权限版本 Pub/Sub |

## 自动配置

| 类 | 条件 | 关键 Bean |
|----|------|----------|
| `SecurityCoreAutoConfiguration` | 始终 | `JwtUtils`、`TokenService`、`PermissionUtils`、`PermissionExpression` |
| `ServletReleasePathCollectorAutoConfiguration` | Servlet | `ReleasePathCollector` |
| `ReactiveReleasePathCollectorAutoConfiguration` | Reactive | `ReleasePathCollector` |
| `SecurityConfig` | Servlet | `HeaderAuthenticationFilter`、`SecurityFilterChain`、`GlobalExceptionHandler` |
| `SecurityReactConfig` | Reactive | `HeadReactAuthenticationFilter`、`SecurityWebFilterChain` |

## 主要类

| 类 | 职责 |
|----|------|
| `LoginUser` | 实现 `UserDetails` 的登录用户模型 |
| `JwtUtils` | JWT 签发与解析 |
| `TokenService` | Token 创建、续签、登出、Redis 会话 |
| `PermissionUtils` | 权限/角色校验、版本广播 |
| `AuthenticationSessionSupport` | Servlet/Reactive 共用认证逻辑 |
| `GlobalExceptionHandler` | Servlet 全局异常处理 |
| `GlobalReactiveExceptionHandler` | Reactive 全局异常处理 |
| `InnerAuthAspect` | `@InnerAuth` AOP 切面 |

## 扩展 Filter 链

实现 [commons-core](../commons-core/README.md) 中的 `SecurityChainFilter` 或 `ReactiveSecurityChainFilter` 并注册为 Bean，会自动插入 Security 链：

```java
@Bean
public SecurityChainFilter feignAuthFilter() {
    return () -> new InternalServiceAuthFilter();
}
```

## 注意事项

1. **JWT secret** 生产环境务必使用足够长度的随机密钥，并通过配置中心管理
2. 单独使用 security 中的 core 工具类时，建议显式引入 `commons-core`
3. 与 [commons-log](../commons-log/README.md) 集成后，请求日志可自动解析当前用户 ID

[← 返回总览](../README.md)
