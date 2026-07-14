# commons-database

关系型数据库访问模块，集成 **MyBatis-Plus**、**Druid** 连接池与 **dynamic-datasource** 动态数据源。引入后自动配置分页、乐观锁、防全表更新删除及 `createTime` / `updateTime` 自动填充。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xcz.commons</groupId>
    <artifactId>commons-database</artifactId>
</dependency>
```

本模块不依赖其他 commons 子模块，可独立使用。

## 自动配置

`MybatisConfig` 注册以下内容：

| 能力 | 说明 |
|------|------|
| `@MapperScan("com.xcz.**.mapper")` | 扫描 Mapper 接口 |
| 分页插件 | MySQL 方言，可配置溢出与最大条数 |
| 乐观锁插件 | `@Version` 字段 |
| 防全表更新删除 | 阻止无 WHERE 的 UPDATE/DELETE |
| `MetaObjectHandler` | 自动填充 `createTime`、`updateTime` |

## 配置

### 模块专属

```yaml
mybatis-plus:
  page:
    overflow: false    # 超过最大页是否回到首页
    max-limit: 500     # 单页最大条数
```

### 数据源（业务应用配置）

数据源由引入的 starter 提供，本模块不定义专属前缀。示例：

**Druid 单数据源：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mobi?useUnicode=true&characterEncoding=utf8
    username: root
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**dynamic-datasource 多数据源：**

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/mobi
          username: root
          password: secret
        slave:
          url: jdbc:mysql://localhost:3306/mobi_slave
          username: root
          password: secret
```

**MyBatis-Plus 常用配置：**

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
```

## 使用方式

### 定义 Mapper

Mapper 接口需放在 `com.xcz.**.mapper` 包下：

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

### 实体自动填充

```java
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createTime;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;
```

### 分页查询

```java
Page<User> page = new Page<>(pageNum, pageSize);
userMapper.selectPage(page, queryWrapper);
```

## 代码生成器（开发期）

`GeneraCodeUtil` 基于 MyBatis-Plus Generator + Freemarker 模板，在开发阶段生成 entity / mapper / service / controller：

```java
new GeneraCodeUtil(jdbcUrl, username, password)
    .generate(List.of("sys_user", "sys_role"));
```

生成器依赖已包含在模块中，仅开发时使用，不会传递给运行时业务逻辑。

## 主要类

| 类 | 职责 |
|----|------|
| `MybatisConfig` | MyBatis-Plus 拦截器与自动填充 |
| `GeneraCodeUtil` | 代码生成工具 |

## 依赖说明

| 依赖 | 用途 |
|------|------|
| mybatis-plus-spring-boot3-starter | ORM 框架 |
| druid-spring-boot-3-starter | 连接池与监控 |
| dynamic-datasource-spring-boot3-starter | 多数据源切换 |
| mysql-connector-j | MySQL 驱动（runtime） |
| mybatis-plus-generator + freemarker | 代码生成 |

## 注意事项

1. Mapper 包路径必须为 `com.xcz.**.mapper`，否则无法被扫描
2. 自动填充字段名固定为 `createTime`、`updateTime`，实体需使用相同命名
3. 多数据源切换使用 `@DS("slave")` 等注解（dynamic-datasource 提供）

[← 返回总览](../README.md)
