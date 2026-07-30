package com.xcz.commons.protobuf.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Protobuf 映射器注册表，管理所有已注册实体与其 Mapper 的对应关系。
 */
public class ProtobufMapperRegistry {

    private final Map<Class<?>, ReflectionProtobufMapper> entityMappers = new LinkedHashMap<>();

    /**
     * 注册实体类并创建对应的反射映射器。
     */
    public void register(Class<?> entityClass) {
        entityMappers.put(entityClass, new ReflectionProtobufMapper(entityClass, this));
    }

    /**
     * 判断实体类型是否已注册。
     */
    public boolean containsEntity(Class<?> entityClass) {
        return entityMappers.containsKey(entityClass);
    }

    /**
     * 获取实体类型对应的映射器。
     */
    public ReflectionProtobufMapper getMapper(Class<?> entityClass) {
        ReflectionProtobufMapper mapper = entityMappers.get(entityClass);
        if (mapper == null) {
            throw new IllegalArgumentException("未注册的实体类型: " + entityClass.getName());
        }
        return mapper;
    }
}
