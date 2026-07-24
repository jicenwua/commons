package com.xcz.commons.protobuf.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProtobufMapperRegistry {

    private final Map<Class<?>, ReflectionProtobufMapper> entityMappers = new LinkedHashMap<>();

    public void register(Class<?> entityClass) {
        entityMappers.put(entityClass, new ReflectionProtobufMapper(entityClass, this));
    }

    public boolean containsEntity(Class<?> entityClass) {
        return entityMappers.containsKey(entityClass);
    }

    public ReflectionProtobufMapper getMapper(Class<?> entityClass) {
        ReflectionProtobufMapper mapper = entityMappers.get(entityClass);
        if (mapper == null) {
            throw new IllegalArgumentException("未注册的实体类型: " + entityClass.getName());
        }
        return mapper;
    }
}
