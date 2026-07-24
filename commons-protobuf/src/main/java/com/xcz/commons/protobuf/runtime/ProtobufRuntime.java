package com.xcz.commons.protobuf.runtime;

import com.xcz.commons.protobuf.annotation.ProtobufMessage;
import com.xcz.commons.protobuf.codec.ProtobufCodec;
import com.xcz.commons.protobuf.generator.ClassScanner;
import com.xcz.commons.protobuf.mapper.ProtobufMapperRegistry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用 Protobuf 运行时：注册实体并提供序列化/反序列化，与 Kafka、Netty、RabbitMQ 等中间件无关。
 */
public final class ProtobufRuntime {

    public static final String SCAN_PACKAGES_KEY = "protobuf.scan.packages";
    public static final String SCAN_CLASSES_KEY = "protobuf.scan.classes";
    public static final String REGISTRY_KEY = "protobuf.registry";
    public static final String ROUTE_MAPPING_KEY = "protobuf.route.mapping";
    public static final String TOPIC_MAPPING_KEY = "protobuf.topic.mapping";
    public static final String ENTITY_CLASS_KEY = "protobuf.entity.class";

    private final ProtobufMapperRegistry mapperRegistry = new ProtobufMapperRegistry();
    private final Map<Class<?>, ProtobufCodec<?>> codecByEntity = new LinkedHashMap<>();
    private final Map<String, Class<?>> entityByRoute = new HashMap<>();

    private ProtobufRuntime() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ProtobufRuntime fromConfig(Map<String, ?> configs) {
        Builder builder = builder();
        Object packages = configs.get(SCAN_PACKAGES_KEY);
        if (packages != null) {
            builder.scanPackages(parseLines(packages.toString()));
        }
        Object classes = configs.get(SCAN_CLASSES_KEY);
        if (classes != null) {
            builder.scanClasses(parseLines(classes.toString()));
        }
        Object registry = configs.get(REGISTRY_KEY);
        if (registry != null) {
            parseLines(registry.toString()).forEach(line -> {
                String entityClassName = line.contains("=") ? line.split("=", 2)[0].trim() : line.trim();
                builder.registerClassName(entityClassName);
            });
        }
        Object entity = configs.get(ENTITY_CLASS_KEY);
        if (entity != null) {
            builder.registerClassName(entity.toString());
        }
        Object routeMapping = configs.get(ROUTE_MAPPING_KEY);
        if (routeMapping == null) {
            routeMapping = configs.get(TOPIC_MAPPING_KEY);
        }
        if (routeMapping != null) {
            parseLines(routeMapping.toString()).forEach(line -> {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("protobuf.route.mapping 格式错误: " + line);
                }
                builder.route(parts[0].trim(), parts[1].trim());
            });
        }
        ProtobufRuntime runtime = builder.build();
        if (runtime.codecByEntity.isEmpty()) {
            throw new IllegalArgumentException(
                    "未配置 Protobuf 实体，请设置 protobuf.scan.packages / protobuf.scan.classes / protobuf.registry");
        }
        return runtime;
    }

    @SuppressWarnings("unchecked")
    public byte[] serialize(Object entity) {
        if (entity == null) {
            return null;
        }
        ProtobufCodec<Object> codec = (ProtobufCodec<Object>) codec(entity.getClass());
        return codec.encode(entity);
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> entityClass) {
        if (data == null) {
            return null;
        }
        return (T) codec(entityClass).decode(data);
    }

    public Object deserializeByRoute(String route, byte[] data) {
        if (data == null) {
            return null;
        }
        Class<?> entityClass = entityByRoute.get(route);
        if (entityClass == null) {
            throw new IllegalArgumentException(
                    "路由 [" + route + "] 未绑定实体，请在 @ProtobufMessage(topic) 或 protobuf.route.mapping 中配置");
        }
        return deserialize(data, entityClass);
    }

    @SuppressWarnings("unchecked")
    public <T> ProtobufCodec<T> codec(Class<T> entityClass) {
        ProtobufCodec<T> codec = (ProtobufCodec<T>) codecByEntity.get(entityClass);
        if (codec == null) {
            throw new IllegalArgumentException("未注册的实体类型: " + entityClass.getName());
        }
        return codec;
    }

    public boolean isRegistered(Class<?> entityClass) {
        return codecByEntity.containsKey(entityClass);
    }

    private void registerEntity(Class<?> entityClass) {
        if (codecByEntity.containsKey(entityClass)) {
            bindRouteFromAnnotation(entityClass);
            return;
        }
        ProtobufCodec<?> codec = new ProtobufCodec<>(entityClass, mapperRegistry);
        codecByEntity.put(entityClass, codec);
        bindRouteFromAnnotation(entityClass);
    }

    private void bindRouteFromAnnotation(Class<?> entityClass) {
        ProtobufMessage annotation = entityClass.getAnnotation(ProtobufMessage.class);
        if (annotation != null && !annotation.topic().isBlank()) {
            entityByRoute.put(annotation.topic(), entityClass);
        }
    }

    private void bindRoute(String route, String entityClassName) {
        try {
            Class<?> entityClass = Class.forName(entityClassName);
            registerEntity(entityClass);
            entityByRoute.put(route, entityClass);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("实体类不存在: " + entityClassName, ex);
        }
    }

    private static List<String> parseLines(String text) {
        return Arrays.stream(text.split("[\\r\\n,;]+"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    public static final class Builder {

        private final ProtobufRuntime runtime = new ProtobufRuntime();

        public Builder scanPackages(String... packages) {
            return scanPackages(List.of(packages));
        }

        public Builder scanPackages(List<String> packages) {
            Set<Class<?>> classes = ClassScanner.scanPackages(packages);
            classes.stream()
                    .filter(clazz -> !clazz.isEnum())
                    .forEach(runtime::registerEntity);
            return this;
        }

        public Builder scanClasses(String... classNames) {
            return scanClasses(List.of(classNames));
        }

        public Builder scanClasses(List<String> classNames) {
            classNames.forEach(this::registerClassName);
            return this;
        }

        public Builder register(Class<?> entityClass) {
            runtime.registerEntity(entityClass);
            return this;
        }

        public Builder registerClassName(String entityClassName) {
            try {
                return register(Class.forName(entityClassName));
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException("实体类不存在: " + entityClassName, ex);
            }
        }

        public Builder route(String route, String entityClassName) {
            runtime.bindRoute(route, entityClassName);
            return this;
        }

        public ProtobufRuntime build() {
            if (runtime.codecByEntity.isEmpty()) {
                throw new IllegalStateException("未注册任何 @ProtobufMessage 实体");
            }
            return runtime;
        }
    }
}
