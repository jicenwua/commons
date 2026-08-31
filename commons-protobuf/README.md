# commons-protobuf

中间件无关的 **Protobuf 注解驱动**工具库：在 Java POJO 上用注解描述消息结构，运行时直接完成二进制序列化/反序列化，**无需 protoc 生成 Java 类**。

同时支持从实体**反向生成 `.proto` 文件**，便于跨语言协作或作为协议文档分发。

## 适用场景

| 场景 | 说明 |
|------|------|
| Kafka / RabbitMQ 消息体 | 业务实体即消息体，统一编解码 |
| 高性能 RPC / 自定义协议 | 比 JSON 更小的体积与更快的解析 |
| 多语言协作 | 从 Java 实体导出 `.proto` 给 Go、Python 等使用 |
| 非 Spring 环境 | 通过 `ProtobufRuntime.fromConfig()` 手动构建 |

Kafka、Netty 等中间件的 Serializer/Deserializer 适配层建议在业务模块中单独实现；本库只提供**运行时与 Schema 生成**能力。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-protobuf</artifactId>
</dependency>
```

底层依赖 `protobuf-java` 与 `reflections`（包扫描）。Spring Boot 项目引入后会自动装配 `ProtobufRuntime`（可关闭）。

---

## 快速开始

### 1. 定义消息实体

在业务包中创建 POJO，类上标注 `@ProtobufMessage`，字段按需标注 `@ProtobufField`：

```java
@ProtobufMessage(
    comment = "订单创建消息",
    topic = "order-created"   // 可选：绑定 Kafka Topic / 路由键
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    @ProtobufField(number = 1, comment = "订单主键")
    private Long orderId;

    private Long userId;          // 字段名自动转为 order_id
    private BigDecimal amount;    // 映射为 proto string
    private List<String> tags;    // 映射为 repeated string
    private Map<String, String> attributes;  // 映射为 map<string, string>
}
```

### 2. Spring Boot 配置

```yaml
protobuf:
  enabled: true                          # 默认 true；设为 false 可关闭自动配置
  scan-packages:
    - com.example.message                # 扫描带 @ProtobufMessage 的类
  scan-classes: []                         # 或显式指定类全名
  route-mapping:                         # 可选：路由 → 实体类全名
    order-updated: com.example.message.OrderUpdatedEvent
```

`scan-packages` 与 `scan-classes` **至少配置一项**，否则启动报错。

### 3. 序列化 / 反序列化

```java
@Autowired
private ProtobufRuntime protobufRuntime;

// 序列化
byte[] bytes = protobufRuntime.serialize(event);

// 反序列化
OrderCreatedEvent decoded = protobufRuntime.deserialize(bytes, OrderCreatedEvent.class);

// 按路由反序列化（Topic / routingKey）
Object routed = protobufRuntime.deserializeByRoute("order-created", bytes);
```

---

## 注解说明

### `@ProtobufMessage`（类 / 枚举）

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `name` | 覆盖 proto message / enum 名称 | 类名 |
| `comment` | 写入 `.proto` 文件的注释 | 空 |
| `topic` | 路由键（如 Kafka Topic），用于 `deserializeByRoute` | 空 |

只有带此注解的类才会被扫描注册。

### `@ProtobufField`（字段）

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `number` | proto 字段编号；为 0 时按声明顺序自动分配 | `0` |
| `ignore` | 是否忽略该字段 | `false` |
| `name` | 覆盖 proto 字段名 | camelCase → snake_case |
| `comment` | 写入 `.proto` 的字段注释（Java 源码注释无法反射获取，需显式声明） | 空 |

**命名规则示例：**

| Java 字段 | proto 字段 |
|-----------|------------|
| `orderId` | `order_id` |
| `userID` | `user_id` |
| `HTTPResponse` | `http_response` |

---

## Java 类型映射

| Java 类型 | proto 类型 | 备注 |
|-----------|------------|------|
| `String` | `string` | |
| `boolean` / `Boolean` | `bool` | |
| `int` / `Integer` | `int32` | |
| `long` / `Long` | `int64` | |
| `float` / `Float` | `float` | |
| `double` / `Double` | `double` | |
| `byte[]` | `bytes` | |
| `BigDecimal` | `string` | 序列化为 plain string |
| `LocalDateTime` / `LocalDate` / `Date` | `string` | ISO 字符串 |
| `Instant` | `int64` | 时间戳 |
| `Enum`（带 `@ProtobufMessage`） | 同名 enum | 自动生成 `UNSPECIFIED = 0` |
| 嵌套 POJO（带 `@ProtobufMessage`） | 同名 message | 递归收集 |
| `List<T>` / `Set<T>` | `repeated T` | |
| `Map<String, V>` | `map<string, V>` | **key 仅支持 String** |

`java.*` 包下的其他类型不支持，会抛出 `IllegalArgumentException`。

---

## 运行时 API

### `ProtobufRuntime`

核心入口，负责实体注册与编解码调度。

```java
// 编程式构建（非 Spring 场景）
ProtobufRuntime runtime = ProtobufRuntime.builder()
    .scanPackages("com.example.message")
    .scanClasses("com.example.message.LegacyEvent")  // 可选：显式注册
    .route("custom-route", "com.example.message.CustomEvent")
    .register(OrderCreatedEvent.class)
    .build();

byte[] payload = runtime.serialize(entity);
OrderCreatedEvent msg = runtime.deserialize(payload, OrderCreatedEvent.class);
```

| 方法 | 说明 |
|------|------|
| `serialize(Object)` | Java 实体 → `byte[]` |
| `deserialize(byte[], Class<T>)` | `byte[]` → 指定类型 |
| `deserializeByRoute(String, byte[])` | 按路由键反序列化 |
| `codec(Class<T>)` | 获取单实体编解码器 |
| `isRegistered(Class<?>)` | 是否已注册 |

### `ProtobufCodec<T>`

单个实体类型的编解码器，由 Runtime 内部创建：

```java
ProtobufCodec<OrderCreatedEvent> codec = runtime.codec(OrderCreatedEvent.class);
byte[] bytes = codec.encode(event);
OrderCreatedEvent decoded = codec.decode(bytes);
```

内部使用 `DynamicMessage` + 运行时构建的 `Descriptor`，**不依赖 protoc 生成的 Java 类**。

### `ProtobufRuntimeHolder`

供 Kafka Serializer 等非 Spring 管理的组件获取全局 Runtime：

```java
// Spring 启动后由自动配置绑定
ProtobufRuntime runtime = ProtobufRuntimeHolder.require();

// Kafka 等中间件：优先用 Holder，否则按 configs 临时创建
ProtobufRuntime runtime = ProtobufRuntimeHolder.getOrCreate(configs);
```

### `ProtobufRuntime.fromConfig(Map)`

适用于 Kafka `Serializer` / `Deserializer` 的 `configure()` 方法：

```java
Map<String, Object> configs = Map.of(
    "protobuf.scan.packages", "com.example.message",
    "protobuf.route.mapping", "order-created=com.example.message.OrderCreatedEvent"
);
ProtobufRuntime runtime = ProtobufRuntime.fromConfig(configs);
```

支持的配置键：

| 键 | 说明 |
|----|------|
| `protobuf.scan.packages` | 逗号/分号/换行分隔的包名 |
| `protobuf.scan.classes` | 逗号分隔的类全名 |
| `protobuf.registry` | 同 scan.classes，支持 `route=class` 格式 |
| `protobuf.entity.class` | 注册单个实体类 |
| `protobuf.route.mapping` / `protobuf.topic.mapping` | `route=全限定类名`，多行或逗号分隔 |

---

## 路由绑定

消费者按 Topic / routingKey 反序列化时，有两种绑定方式（二选一或混用）：

**方式一：注解声明**

```java
@ProtobufMessage(topic = "order-created")
public class OrderCreatedEvent { ... }
```

**方式二：配置文件**

```yaml
protobuf:
  route-mapping:
    order-created: com.example.message.OrderCreatedEvent
```

调用：

```java
Object event = protobufRuntime.deserializeByRoute("order-created", bytes);
```

未绑定的路由会抛出 `IllegalArgumentException`。

---

## 生成 `.proto` 文件

运行时编解码**不需要**生成 `.proto`；若需给其它语言或作为协议文档，可主动生成。

### 方式一：在业务模块写 main 方法（推荐）

```java
public final class OrderProtoGeneratorMain {
    public static void main(String[] args) throws Exception {
        ProtobufGenerator.generate(
            List.of("com.example.message"),
            "target/generated-proto",
            "com.example.message.proto"
        );
    }
}
```

### 方式二：命令行

```bash
java com.xcz.commons.protobuf.generator.ProtoGeneratorMain \
  com.example.message \
  target/generated-proto \
  com.example.message.proto
```

### 方式三：仅生成内容、不写磁盘

```java
ProtobufScanConfig config = new ProtobufScanConfig();
config.setScanPackages(List.of("com.example.message"));
config.setProtoJavaPackage("com.example.message.proto");

Map<String, String> files = new ProtoSchemaGenerator().generate(config);
// files: 文件名 → .proto 文本
```

### 生成结果示例

输入 `SampleOrder`（`orderId`、`userID`、`amount`）后，生成类似：

```protobuf
syntax = "proto3";

option java_package = "com.xcz.commons.protobuf.model.proto";
option java_multiple_files = true;

// 测试订单消息
message SampleOrder {
  // 订单主键
  int64 order_id = 1;
  int64 user_id = 2;
  string amount = 3;
}
```

输出路径规则：`{包路径}/{类名小写}.proto`，例如 `com/xcz/commons/protobuf/model/sampleorder.proto`。

---

## 包结构

```
com.xcz.commons.protobuf
├── annotation/          # @ProtobufMessage、@ProtobufField
├── codec/             # ProtobufCodec 单实体编解码
├── config/            # ProtobufAutoConfiguration、ProtobufScanConfig
├── descriptor/        # 运行时 Descriptor 构建（EntityDescriptorFactory）
├── generator/         # .proto 文件生成（ProtobufGenerator、ProtoSchemaGenerator）
├── mapper/            # POJO ↔ DynamicMessage 反射映射
├── properties/        # protobuf.* 配置属性
└── runtime/           # ProtobufRuntime、ProtobufRuntimeHolder
```

---

## 与中间件集成（示例思路）

本库不内置 Kafka 依赖，典型集成方式如下：

```java
public class KafkaProtobufSerializer implements Serializer<Object> {
    private ProtobufRuntime runtime;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        this.runtime = ProtobufRuntimeHolder.getOrCreate(configs);
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        return runtime.serialize(data);
    }
}
```

```java
public class KafkaProtobufDeserializer implements Deserializer<Object> {
    private ProtobufRuntime runtime;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        this.runtime = ProtobufRuntimeHolder.getOrCreate(configs);
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        return runtime.deserializeByRoute(topic, data);
    }
}
```

Kafka 生产者/消费者配置中传入相同的 `protobuf.scan.packages` 即可。

---

## 工作原理（简述）

```
@ProtobufMessage POJO
        │
        ▼
EntityDescriptorFactory（反射构建虚拟 FileDescriptor）
        │
        ▼
ReflectionProtobufMapper（POJO ↔ DynamicMessage）
        │
        ▼
ProtobufCodec.encode / decode → byte[]
```

- **序列化**：POJO → `DynamicMessage` → `byte[]`
- **反序列化**：`byte[]` → `DynamicMessage.parseFrom(descriptor, data)` → POJO
- **Schema 生成**：同一套类型映射规则，输出标准 proto3 文本

---

## 注意事项

1. **字段编号稳定性**：生产环境建议为字段显式指定 `@ProtobufField(number = …)`，避免增删字段导致编号漂移。
2. **Map 的 key**：仅支持 `Map<String, V>`，其它 key 类型会报错。
3. **枚举**：生成 proto 时自动添加 `UNSPECIFIED = 0`，业务枚举值从 `1` 起编。
4. **静态 / transient 字段**：自动忽略，不参与序列化。
5. **与 protoc 生成类的关系**：本库运行时**不依赖** protoc 生成的 Java 类；`.proto` 文件主要用于文档或给其他语言使用。若两端都用本库，以 Java 实体 + 注解为唯一事实来源即可。
6. **关闭自动配置**：`protobuf.enabled=false` 时不会创建 `ProtobufRuntime` Bean，需手动 `ProtobufRuntime.builder().build()`。

---

## 本模块测试示例

| 类 | 说明 |
|----|------|
| `com.xcz.commons.protobuf.model.SampleOrder` | 基础字段映射 |
| `com.xcz.commons.protobuf.model.SampleOrderWithMap` | Map 字段 |
| `com.xcz.commons.protobuf.example.ProtobufCodegenMain` | 生成 `.proto` 的 IDE 可运行示例 |
| `ProtobufRuntimeTest` | 序列化与路由反序列化单测 |

在 IDE 中运行 `ProtobufCodegenMain` 前，请先编译本模块，确保测试实体在 classpath 中。
