package com.xcz.commons.mongodb.properties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MongoDB 多数据源顶层配置。
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "mongo")
public class MongoSettingsProperties {

    /** 主数据源名称，对应 datasources 中的 key */
    String primary = "primary";

    /** 命名数据源集合，key 为数据源名称 */
    Map<String, MongoDataSourceProperties> datasources = new LinkedHashMap<>();

    /**
     * 校验多数据源配置完整性。
     */
    public void validate() {
        if (datasources == null || datasources.isEmpty()) {
            throw new IllegalStateException("mongo.datasources 不能为空");
        }
        if (!StringUtils.hasText(primary)) {
            throw new IllegalStateException("mongo.primary 不能为空");
        }
        if (!datasources.containsKey(primary)) {
            throw new IllegalStateException("mongo.primary 指向的数据源不存在: " + primary);
        }
        datasources.forEach((name, properties) -> properties.validate(name));
    }
}
