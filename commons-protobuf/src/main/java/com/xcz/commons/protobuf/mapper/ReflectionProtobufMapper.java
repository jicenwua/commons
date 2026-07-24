package com.xcz.commons.protobuf.mapper;

import com.xcz.commons.protobuf.descriptor.EntityDescriptorFactory;
import com.xcz.commons.protobuf.generator.ProtoTypeMapper;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在 Java 实体与 Protobuf 消息之间做反射映射（基于运行时 Descriptor，无需 protoc 生成类）。
 */
public class ReflectionProtobufMapper {

    private final Class<?> entityClass;
    private final Descriptors.Descriptor descriptor;
    private final ProtobufMapperRegistry registry;

    public ReflectionProtobufMapper(Class<?> entityClass, ProtobufMapperRegistry registry) {
        this.entityClass = entityClass;
        this.registry = registry;
        this.descriptor = EntityDescriptorFactory.buildDescriptor(entityClass);
    }

    public Class<?> entityClass() {
        return entityClass;
    }

    public Descriptors.Descriptor descriptor() {
        return descriptor;
    }

    public Message toProto(Object entity) {
        if (entity == null) {
            return null;
        }
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
        for (Field field : allFields(entityClass)) {
            ProtoTypeMapper.ProtoFieldDescriptor fieldDescriptor = ProtoTypeMapper.describeField(field);
            if (fieldDescriptor == null) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value == null) {
                    continue;
                }
                Descriptors.FieldDescriptor protoField = descriptor.findFieldByName(fieldDescriptor.protoName());
                if (protoField == null) {
                    continue;
                }
                setBuilderValue(builder, protoField, value, field);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("读取字段失败: " + field.getName(), ex);
            }
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    public <T> T fromProto(Message message) {
        if (message == null) {
            return null;
        }
        try {
            Constructor<T> constructor = (Constructor<T>) entityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T entity = constructor.newInstance();
            for (Field field : allFields(entityClass)) {
                ProtoTypeMapper.ProtoFieldDescriptor fieldDescriptor = ProtoTypeMapper.describeField(field);
                if (fieldDescriptor == null) {
                    continue;
                }
                Descriptors.FieldDescriptor protoField = descriptor.findFieldByName(fieldDescriptor.protoName());
                if (protoField == null || !hasProtoField(message, protoField)) {
                    continue;
                }
                Object protoValue = message.getField(protoField);
                Object javaValue = convertFromProtoValue(protoValue, field, protoField);
                field.setAccessible(true);
                field.set(entity, javaValue);
            }
            return entity;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("实体映射失败: " + entityClass.getName(), ex);
        }
    }

    private void setBuilderValue(
            DynamicMessage.Builder builder,
            Descriptors.FieldDescriptor protoField,
            Object value,
            Field field) {
        if (protoField.isMapField()) {
            Map<?, ?> mapValue = (Map<?, ?>) value;
            Class<?> valueClass = resolveMapValueClass(field);
            Descriptors.Descriptor entryDescriptor = protoField.getMessageType();
            Descriptors.FieldDescriptor keyField = entryDescriptor.findFieldByName("key");
            Descriptors.FieldDescriptor valueField = entryDescriptor.findFieldByName("value");
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                DynamicMessage.Builder entryBuilder = DynamicMessage.newBuilder(entryDescriptor);
                entryBuilder.setField(keyField, entry.getKey().toString());
                Object protoValue = convertToProtoElement(entry.getValue(), valueClass);
                if (protoValue != null) {
                    entryBuilder.setField(valueField, protoValue);
                }
                builder.addRepeatedField(protoField, entryBuilder.build());
            }
            return;
        }
        if (protoField.isRepeated()) {
            Collection<?> collection = (Collection<?>) value;
            List<Object> converted = new ArrayList<>();
            Class<?> elementType = resolveCollectionElementClass(field);
            for (Object element : collection) {
                converted.add(convertToProtoElement(element, elementType));
            }
            builder.setField(protoField, converted);
            return;
        }
        builder.setField(protoField, convertToProtoElement(value, field.getType()));
    }

    private Object convertToProtoElement(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (registry.containsEntity(targetType)) {
            return registry.getMapper(targetType).toProto(value);
        }
        return ScalarConverter.toProtoScalar(value, targetType);
    }

    private Object convertFromProtoValue(Object protoValue, Field field, Descriptors.FieldDescriptor protoField) {
        if (protoField.isMapField()) {
            Map<Object, Object> result = new LinkedHashMap<>();
            Class<?> valueClass = resolveMapValueClass(field);
            List<?> entries = (List<?>) protoValue;
            Descriptors.Descriptor entryDescriptor = protoField.getMessageType();
            Descriptors.FieldDescriptor keyField = entryDescriptor.findFieldByName("key");
            Descriptors.FieldDescriptor valueField = entryDescriptor.findFieldByName("value");
            for (Object entryObject : entries) {
                Message entryMessage = (Message) entryObject;
                Object key = entryMessage.getField(keyField);
                Object value = entryMessage.getField(valueField);
                result.put(key, convertFromProtoElement(value, valueClass));
            }
            return result;
        }
        if (protoField.isRepeated()) {
            List<Object> result = new ArrayList<>();
            Class<?> elementClass = resolveCollectionElementClass(field);
            for (Object element : (List<?>) protoValue) {
                result.add(convertFromProtoElement(element, elementClass));
            }
            return result;
        }
        return convertFromProtoElement(protoValue, field.getType());
    }

    private Object convertFromProtoElement(Object protoValue, Class<?> targetType) {
        if (protoValue instanceof Message message && registry.containsEntity(targetType)) {
            return registry.getMapper(targetType).fromProto(message);
        }
        if (protoValue instanceof Descriptors.EnumValueDescriptor enumValue) {
            return Enum.valueOf(asEnumType(targetType), enumValue.getName());
        }
        return ScalarConverter.fromProtoScalar(protoValue, targetType);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Enum> asEnumType(Class<?> targetType) {
        return (Class<? extends Enum>) targetType;
    }

    private Class<?> resolveCollectionElementClass(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getActualTypeArguments()[0];
        }
        return Object.class;
    }

    private Class<?> resolveMapValueClass(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getActualTypeArguments()[1];
        }
        return Object.class;
    }

    private boolean hasProtoField(Message message, Descriptors.FieldDescriptor protoField) {
        if (protoField.isRepeated()) {
            return message.getRepeatedFieldCount(protoField) > 0;
        }
        return message.hasField(protoField);
    }

    private List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        || java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }
}
