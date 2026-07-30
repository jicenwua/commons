# commons-excel

基于注解的 Excel 配置读写与统一配置中心模块，适用于游戏策划表、运营配置表等场景。

支持：

- 通过 Java 类 + 注解定义 Excel 结构，自动生成模板文件
- 从 Excel 反序列化为 Java 对象（List / Map / 嵌套对象）
- Spring Boot 启动时统一加载配置到内存
- 运行时热更新（增删改配置项）

---

## 依赖引入

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-excel</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

底层依赖：Hutool POI、Apache POI、FastJson2。

---

## 模块结构

```
commons-excel
├── database/              # 数据模型与注解
│   ├── annotation/        # @ExcelBase / @ExcelSheet / @ExcelField
│   ├── Config.java        # 配置项元数据
│   └── DataType.java      # 配置数据类型枚举
├── support/               # 核心读写
│   ├── ExcelSupport.java  # Excel 创建与读取
│   ├── ConfigResolver.java# 配置值解析（含 JSON / Excel）
│   └── CellValueConverter.java # 单元格类型转换
├── config/                # Spring 自动配置
│   ├── DataProvider.java  # 配置注册抽象类（业务继承）
│   └── DataInitializing.java # 启动加载与热更新
└── client/                # 运行时访问
    ├── DataClient.java    # 内存配置读取
    └── DataEvent.java     # 热更新入口
```

---

## 核心概念

### 1. 配置类（Excel 总类）

一个 `@ExcelBase` 类对应一个 Excel 文件，文件命名规则：

```
{name}-{version}.xlsx
```

例如 `GameConfig` + version `1.0.0` → `GameConfig-1.0.0.xlsx`

### 2. Sheet 字段

配置类中的字段用 `@ExcelSheet` 标记，映射到 Excel 中的 Sheet 页：

- `List<T>`：按列或按行读取对象列表
- `Map<K, V>`：两列 key-value 映射（K、V 须为基本类型）

### 3. 行对象字段

`List` 的元素类型若是对象，其字段用 `@ExcelField` 标记列映射。

### 4. Excel 文件布局

每个业务 Sheet 的布局如下：

| 行号 | 内容 |
|------|------|
| 第 0 行 | 分组标题（合并单元格） |
| 第 1 行 | 列表头 |
| 第 2 行起 | 数据行 |

另有一个固定 Sheet **「版本」**，记录版本号、注释、更新时间，并继承上一版本的历史记录。

---

## 注解说明

### @ExcelBase（类级别）

标记 Excel 配置总类。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `version` | 版本号 | `0.0.1` |
| `name` | 文件前缀 | 类名 |
| `comment` | 版本注释 | 空 |

### @ExcelSheet（字段级别）

将字段映射到指定 Sheet。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `sheet` | Sheet 名称 | 空 |
| `startList` | 起始列索引（从 0 开始） | `0` |
| `comment` | 分组标题（第 0 行） | 空 |
| `type` | `LIST` 或 `Map` | `LIST` |
| `mapKey` | Map 类型 key 列名 | 空 |
| `mapValue` | Map 类型 value 列名 | 空 |

同一 Sheet 可映射多个字段，通过 `startList` 指定不同起始列。例如「场次配置」Sheet 中，列 0~4 读 `List<RoomConfig>`，列 5 读 `List<String>`。

### @ExcelField（字段级别）

标记行对象中的列映射。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `label` | 列标题 | 字段名 |
| `index` | 列序号（从 1 开始） | `1` |
| `comment` | 列批注 | 空 |
| `value` | 单元格为空时的默认值 | 空 |

---

## 快速开始

### 第一步：定义配置类

```java
@Data
@ExcelBase(version = "1.0.0", comment = "游戏主配置")
public class GameConfig {

    @ExcelSheet(sheet = "场次配置", startList = 0, comment = "场次配置")
    private List<RoomConfig> roomConfigs;

    @ExcelSheet(sheet = "表情配置", type = ExcelType.Map, startList = 0,
            comment = "表情配置", mapKey = "编号", mapValue = "表情名称")
    private Map<Long, String> emojiConfigs;
}
```

```java
@Data
public class RoomConfig {

    @ExcelField(label = "场次名", index = 1, comment = "游戏场次的名称", value = "默认房间")
    private String roomName;

    @ExcelField(label = "桌费", index = 2, value = "0")
    private BigDecimal deskCoin;

    @ExcelField(label = "对局人数", index = 3, value = "4")
    private int playerNum;

    @ExcelField(label = "奖励列表", index = 4)
    private List<String> jiangli;

    @ExcelField(label = "牌堆", index = 5)
    private Map<Long, String> poker;
}
```

### 第二步：创建 / 读取 Excel（无 Spring）

```java
String dir = "config/excel";

// 不存在则自动创建模板（会继承同目录最新版本的历史数据）
ExcelSupport.createExcel(dir, GameConfig.class);

// 读取并映射为 Java 对象
GameConfig config = ExcelSupport.readExcel(dir, GameConfig.class);

List<RoomConfig> rooms = config.getRoomConfigs();
Map<Long, String> emojis = config.getEmojiConfigs();
```

### 第三步：Spring Boot 集成

实现 `DataProvider` 并注册为 Bean：

```java
@Component
public class GameDataProvider extends DataProvider {

    @Override
    public void onInit() {
        // 注册基础类型
        dataString("server.name", "服务器名称", "测试服");
        dataInt("server.port", "端口", 8080);

        // 注册 JSON 配置
        dataJson("game.rule", "规则配置", "{\"maxRound\":10}", RuleConfig.class);

        // 注册 Excel 配置
        dataExcel("game.config", "游戏配置", "config/excel", GameConfig.class);
    }

    @Override
    public void onComplete() {
        // 全量加载完成后的回调
    }

    @Override
    public void onChange(List<Config> configs) {
        // 热更新成功后的回调
    }
}
```

启动后 `DataInitializing` 会自动加载所有配置到 `DataClient`。

### 第四步：业务中读取配置

```java
@RequiredArgsConstructor
public class GameService {

    private final DataClient dataClient;

    public void play() {
        String serverName = dataClient.getString("server.name");
        GameConfig gameConfig = dataClient.get("game.config");
        RoomConfig room = gameConfig.getRoomConfigs().getFirst();
    }
}
```

---

## 支持的数据类型

### Config 注册类型（DataProvider）

| DataType | 注册方法 | 说明 |
|----------|----------|------|
| `STRING` | `dataString` | 字符串 |
| `INT` | `dataInt` | 整数 |
| `LONG` | `dataLong` | 长整数 |
| `DOUBLE` | `dataDouble` | 浮点数 |
| `BOOLEAN` | `dataBool` | 布尔 |
| `BIG_DECIMAL` | `dataBigDecimal` | 高精度小数 |
| `JSON` | `dataJson` | JSON 反序列化为对象 |
| `EXCEL` | `dataExcel` | 读取 Excel 配置类 |

### Excel 单元格类型

基本类型：`String`、`int`、`long`、`double`、`float`、`boolean`、`BigDecimal`、`BigInteger`

日期类型：`Date`、`LocalDate`、`LocalDateTime`

### 单元格内集合格式

对象字段中的 `List` / `Set` / `Map` 可在**单个单元格**中用花括号表达：

| 类型 | 单元格示例 | 解析结果 |
|------|-----------|----------|
| `List<String>` | `{gold,silver,copper}` | `["gold","silver","copper"]` |
| `Set<Integer>` | `{1,2,3}` | `{1, 2, 3}` |
| `Map<Long,String>` | `{1:方片,2:梅花}` | `{1L:"方片", 2L:"梅花"}` |

Map 的键值对用 `:` 分隔，多项用 `,` 分隔。

---

## 热更新

通过 `DataEvent` 在运行时增删改配置：

```java
@RequiredArgsConstructor
public class ConfigAdminService {

    private final DataEvent dataEvent;

    // 更新配置
    public void reloadGameConfig() {
        Config config = Config.builder()
                .key("game.config")
                .title("游戏配置")
                .path("config/excel")
                .dataType(DataType.EXCEL)
                .clazz(GameConfig.class.getName())
                .build();
        dataEvent.updateConfig(config);
    }

    // 新增配置
    public void addConfig(Config config) {
        dataEvent.addConfig(config);
    }

    // 删除配置
    public void removeConfig(Config config) {
        dataEvent.deleteConfig(config);
    }
}
```

热更新流程：

1. `ConfigResolver` 重新解析配置值
2. `DataClient` 更新内存数据
3. `DataProvider.onChange` / `onAddConfig` / `onRemoveConfig` 回调通知业务

---

## ExcelSupport 常用 API

| 方法 | 说明 |
|------|------|
| `createExcel(path, configClass)` | 创建 Excel 模板（不存在时），继承最新版本历史 |
| `readExcel(path, configClass)` | 读取 Excel 并映射为配置对象 |
| `resolveExcelPath(path, configClass)` | 解析 Excel 文件完整路径 |
| `getLastVersionFile(dir, configClass)` | 获取目录下最新版本文件 |
| `versionComparator()` | 版本号比较器（如 `1.0.1` vs `1.0.0`） |

---

## DataClient 常用 API

| 方法 | 说明 |
|------|------|
| `get(key)` | 获取配置值（泛型） |
| `getString(key)` | 获取字符串 |
| `getInt(key)` | 获取整数 |
| `getLong(key)` | 获取长整数 |
| `getDouble(key)` | 获取浮点数 |
| `getBool(key)` | 获取布尔值 |
| `getBigDecimal(key)` | 获取 BigDecimal |

key 不存在时 `get` 抛出 `IllegalArgumentException`。

---

## 版本管理

- 文件名格式：`{name}-{version}.xlsx`
- 创建新版本时，自动从目录中查找同名前缀的最新版本文件，复制其「版本」Sheet 的历史记录
- 版本号按 `.` 分段数值比较，如 `1.0.10` > `1.0.9`

---

## 注意事项

1. **Map 的 key 和 value 必须为基本类型**，不支持嵌套对象作为 Map 的 key/value（Sheet 级 Map 同理）。
2. **数据从第 2 行开始读取**（第 0 行标题、第 1 行表头），遇到空行停止读取。
3. **同一 Sheet 多字段映射**时，用 `startList` 区分起始列，避免列重叠。
4. **配置类需要无参构造器**（Lombok `@Data` 默认满足）。
5. **Spring 集成**需要实现 `DataProvider` 并注册为 Bean，`ExcelAutoConfiguration` 会自动装配其余组件。
6. Excel 文件不存在时，`readExcel` 会先调用 `createExcel` 生成模板，再读取（此时数据为空结构）。

---

## 完整示例

参考模块内测试代码：

- `GameConfig.java` — Excel 配置总类
- `RoomConfig.java` — 行对象定义
- `CommonsExcelApplicationTests.java` — 无 Spring 读写测试
- `ExcelSupportCollectionTest.java` — 单元格内 List/Map 格式测试

运行测试：

```bash
cd commons-excel
mvn test
```
