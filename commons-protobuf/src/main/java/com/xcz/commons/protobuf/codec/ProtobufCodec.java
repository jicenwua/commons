package com.xcz.commons.protobuf.codec;

import com.xcz.commons.protobuf.mapper.ProtobufMapperRegistry;
import com.xcz.commons.protobuf.mapper.ReflectionProtobufMapper;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

/**
 * 单个实体类型的 Protobuf 编解码器。
 * 负责 Java 对象与二进制字节之间的转换。
 */
public class ProtobufCodec<T> {

    private final Class<T> entityClass;
    private final ReflectionProtobufMapper mapper;

    /**
     * 为指定实体类创建编解码器，并注册到全局 Mapper 注册表。
     */
    public ProtobufCodec(Class<T> entityClass, ProtobufMapperRegistry registry) {
        this.entityClass = entityClass;
        registry.register(entityClass);
        this.mapper = registry.getMapper(entityClass);
    }

    /**
     * 返回绑定的实体类型。
     */
    public Class<T> entityClass() {
        return entityClass;
    }

    /**
     * 将 Java 实体序列化为 Protobuf 二进制。
     */
    public byte[] encode(T entity) {
        Message message = mapper.toProto(entity);
        return message.toByteArray();
    }

    /**
     * 将 Protobuf 二进制反序列化为 Java 实体。
     */
    @SuppressWarnings("unchecked")
    public T decode(byte[] data) {
        try {
            // 按运行时 Descriptor 解析，无需 protoc 生成的 Java 类
            Message message = DynamicMessage.parseFrom(mapper.descriptor(), data);
            return (T) mapper.fromProto(message);
        } catch (com.google.protobuf.InvalidProtocolBufferException ex) {
            throw new IllegalStateException("Protobuf 反序列化失败: " + entityClass.getName(), ex);
        }
    }
}
