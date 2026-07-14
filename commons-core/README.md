# commons-core

核心基础模块，提供通用工具类、统一 API 响应模型、异常体系、常量定义及安全链扩展点。其他 commons 子模块大多直接或间接依赖本模块。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-core</artifactId>
</dependency>
```

## 包结构

```
com.xcz.commons.core
├── annotation/          # @Excel、@Excels 等注解
├── config/              # 自动配置（日期格式等）
├── constant/            # 全局常量
├── domain/              # ResponseEntity 统一响应体
├── event/               # 领域事件
├── exception/           # 异常体系
├── log/                 # 请求日志 attribute 键
├── security/            # Security 链扩展接口
├── text/                # 类型转换、字符集
├── utils/               # 通用工具
│   ├── file/            # 文件、图片、MIME
│   ├── html/            # HTML 转义、XSS 过滤
│   ├── ip/              # IP 解析与归属地
│   ├── poi/             # Excel 导入导出
│   ├── reflect/         # 反射
│   ├── response/        # 响应构建
│   ├── sign/            # Base64
│   └── uuid/            # ID / UUID 生成
├── web.vo.params/       # AjaxResult
└── xss/                 # @Xss 校验注解
```

## 自动配置

| 类 | 作用 |
|----|------|
| `SpringUtils` | 注册为 `BeanFactoryPostProcessor`，提供静态 `getBean()` |
| `DateFormatConfig` | 统一 `LocalDateTime` 序列化格式为 `yyyy-MM-dd HH:mm:ss` |

## 核心能力

### 统一响应

```java
// ResponseEntity 风格
ResponseEntityUtils.ok(data);
ResponseEntityUtils.okPage(list, total, "查询成功");

// AjaxResult 风格（HashMap）
AjaxResult.success(data);
AjaxResult.error("操作失败");
```

### 异常体系

| 异常类 | 用途 |
|--------|------|
| `ServiceException` | 通用业务异常 |
| `NotLoginException` | 未登录 |
| `NotPermissionException` | 无权限 |
| `NotRoleException` | 无角色 |
| `InnerAuthException` | 内部调用鉴权失败 |
| `CaptchaException` | 验证码错误 |
| `FileSizeLimitExceededException` | 文件超限 |

### 常用工具

```java
StringUtils.isEmpty(str);
DateUtils.parseDate(str);
IpUtils.getIpAddr(request);
IpUtils.getFriendlyIpLocation(ip);   // 需 ip2region.xdb
PageUtils.slice(list, pageNum, pageSize);
SpringUtils.getBean(SomeService.class);
ServletUtils.getRequest();
ExceptionUtil.getExceptionMessage(e);
LogSanitizer.sanitize(body);
```

### Excel 导出

在实体字段上使用 `@Excel` 注解，配合 `ExcelUtil` 完成导入导出：

```java
@Excel(name = "用户名", sort = 1)
private String username;
```

### XSS 防护

```java
@Xss
private String content;  // JSR-303 校验，拒绝含脚本的输入
```

### 安全链扩展点

供 `commons-security` 或业务服务插入自定义 Filter：

```java
@Bean
public SecurityChainFilter myFilter() {
    return () -> new MyAuthenticationFilter();
}
```

Reactive 环境使用 `ReactiveSecurityChainFilter`。

## 常量

| 类 | 内容 |
|----|------|
| `SecurityConstants` | 认证头、内部调用头、Feign 标记、白名单路径 |
| `TokenConstants` | Token 相关字段名 |
| `UserConstants` | 用户状态、默认值 |
| `Constants` | HTTP 状态、分页默认值等 |

## 配置

本模块无专属 `@ConfigurationProperties`。`DateFormatConfig` 自动生效，无需额外配置。

若使用 IP 归属地，需在 `resources/ip2region/` 下放置 `ip2region.xdb` 文件。

## 依赖说明

- `spring-boot-starter-web` / `webflux` 为 **optional**，纯工具场景可不引入 Web 依赖
- 主要第三方库：ip2region、UserAgentUtils、Apache POI、Fastjson2、Commons IO

## 相关模块

- 被 [commons-log](../commons-log/README.md)、[commons-redis](../commons-redis/README.md) 直接依赖
- [commons-security](../commons-security/README.md) 通过 commons-redis 间接依赖

[← 返回总览](../README.md)
