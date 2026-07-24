# commons-protobuf

中间件无关的 Protobuf 工具库：用注解定义实体，运行时直接对 POJO 做序列化/反序列化，无需 protoc 生成 Java 类。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-protobuf</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Spring Boot 配置

```yaml
protobuf:
  enabled: true
  scan-packages:
    - com.example.message
```

## 包结构

| 包 | 说明 |
|---|---|
| `com.xcz.commons.protobuf.annotation` | `@ProtobufMessage`、`@ProtobufField` |
| `com.xcz.commons.protobuf.runtime` | `ProtobufRuntime` 运行时 API |
| `com.xcz.commons.protobuf.config` | 自动配置、Schema 生成配置 |
| `com.xcz.commons.protobuf.properties` | `protobuf.*` 配置属性 |
| `com.xcz.commons.protobuf.generator` | `.proto` 文件生成 |
| `com.xcz.commons.protobuf.codec` | 编解码器 |
| `com.xcz.commons.protobuf.mapper` | POJO 反射映射 |
| `com.xcz.commons.protobuf.descriptor` | 运行时 Descriptor 构建 |

## 核心 API

```java
ProtobufRuntime runtime = ProtobufRuntime.builder()
    .scanPackages("com.example.message")
    .build();

byte[] bytes = runtime.serialize(entity);
MyMessage msg = runtime.deserialize(bytes, MyMessage.class);
Object routed = runtime.deserializeByRoute("kafka-topic", bytes);
```

Kafka 等中间件适配层在业务模块中单独实现（如 `com.mq.kafka.protobuf`）。
