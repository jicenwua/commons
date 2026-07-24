package com.xcz.commons.protobuf.properties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Protobuf 运行时扫描与路由配置。
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "protobuf")
public class ProtobufProperties {

    /** 自动扫描 @ProtobufMessage 实体的包名列表 */
    List<String> scanPackages = new ArrayList<>();

    /** 显式注册的实体类全名列表 */
    List<String> scanClasses = new ArrayList<>();

    /**
     * 路由 → 实体类全名，例如 kafka-test → com.mq.kafka.config.Message。
     * 也可在实体 {@code @ProtobufMessage(topic)} 上声明。
     */
    Map<String, String> routeMapping = new LinkedHashMap<>();
}
