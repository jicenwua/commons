# commons-oss

阿里云 OSS 文件存储模块，封装上传、预签名 URL、分片断点续传与删除等能力。使用 **V4 签名**。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-oss</artifactId>
</dependency>
```

本模块不依赖其他 commons 子模块。

## 激活条件

同时满足：

1. classpath 存在阿里云 OSS SDK
2. 配置了 `aliyun.oss.endpoint`

## 配置

前缀：`aliyun.oss`

```yaml
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com   # 必填
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    region: cn-hangzhou
    bucket-name: my-bucket
    expire-time: 300000   # 预签名 URL 有效期（毫秒），默认 5 分钟
```

## 使用方式

注入 `UploadService` 即可：

```java
@Autowired
private UploadService uploadService;
```

### 简单上传

```java
// MultipartFile，path 为空时自动生成 UUID 路径
String objectKey = uploadService.simpleUpload(file, null);

// InputStream
String objectKey = uploadService.simpleUpload(inputStream, "photo.jpg", "images/");
```

### 带进度上传

```java
uploadService.uploadWithProgress(file, null, (written, total) -> {
    log.info("上传进度: {}/{}", written, total);
});
```

### 分片上传（大文件 / 断点续传）

```java
String objectKey = uploadService.uploadChunked(file, "videos/");

// 指定是否覆盖已存在文件
String objectKey = uploadService.uploadChunked(file, "videos/", false);
```

### 获取访问 URL

```java
// 预签名临时 URL（有过期时间）
String tempUrl = uploadService.getUrl(objectKey);

// 永久访问 URL
String permanentUrl = uploadService.getEnteralUrl(objectKey);
```

### 文件管理

```java
boolean exists = uploadService.exists(objectKey);
boolean deleted = uploadService.delete(objectKey);  // 支持 URL 或 objectKey
```

## 路径规则

`OssPathUtil` 负责生成 objectKey：

- 未指定 path 时，按日期目录 + UUID 自动生成
- 同名冲突时自动追加 `_1`、`_2` 等后缀

## 主要类

| 类 | 职责 |
|----|------|
| `OssClientConfig` | 注册 `OSS` 客户端与 `UploadService` |
| `OssProperties` | 配置属性绑定 |
| `UploadService` | 上传服务接口 |
| `UploadServiceImpl` | 上传实现 |
| `OssPathUtil` | objectKey 路径生成 |
| `PutObjectProgressListener` | 上传进度监听 |

## 注意事项

1. `access-key-id` / `access-key-secret` 属于敏感信息，请通过配置中心或环境变量注入，勿提交到代码仓库
2. 预签名 URL 过期后需重新调用 `getUrl()` 生成
3. 模块依赖 `spring-boot-starter-web`（`MultipartFile` 支持），纯非 Web 场景需改用 `InputStream` 重载方法

[← 返回总览](../README.md)
