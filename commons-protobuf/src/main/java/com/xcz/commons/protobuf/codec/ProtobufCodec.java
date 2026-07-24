package com.xcz.commons.protobuf.codec;

import com.xcz.commons.protobuf.mapper.ProtobufMapperRegistry;
import com.xcz.commons.protobuf.mapper.ReflectionProtobufMapper;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

public class ProtobufCodec<T> {

    private final Class<T> entityClass;
    private final ReflectionProtobufMapper mapper;

    public ProtobufCodec(Class<T> entityClass, ProtobufMapperRegistry registry) {
        this.entityClass = entityClass;
        registry.register(entityClass);
        this.mapper = registry.getMapper(entityClass);
    }

    public Class<T> entityClass() {
        return entityClass;
    }

    public byte[] encode(T entity) {
        Message message = mapper.toProto(entity);
        return message.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public T decode(byte[] data) {
        try {
            Message message = DynamicMessage.parseFrom(mapper.descriptor(), data);
            return (T) mapper.fromProto(message);
        } catch (com.google.protobuf.InvalidProtocolBufferException ex) {
            throw new IllegalStateException("Protobuf 反序列化失败: " + entityClass.getName(), ex);
        }
    }
}
