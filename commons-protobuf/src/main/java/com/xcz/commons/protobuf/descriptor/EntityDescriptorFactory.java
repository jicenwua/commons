package com.xcz.commons.protobuf.descriptor;

import com.xcz.commons.protobuf.annotation.ProtobufField;
import com.xcz.commons.protobuf.annotation.ProtobufMessage;
import com.xcz.commons.protobuf.generator.ProtoTypeMapper;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从 {@code @ProtobufMessage} 实体反射构建 Protobuf Descriptor，无需 protoc 生成的 Java 类。
 */
public final class EntityDescriptorFactory {

    private EntityDescriptorFactory() {
    }

    public static Descriptors.Descriptor buildDescriptor(Class<?> entityClass) {
        try {
            Set<Class<?>> messageTypes = collectMessageTypes(entityClass);
            List<Class<?>> enums = messageTypes.stream().filter(Class::isEnum).sorted(Comparator.comparing(Class::getSimpleName)).toList();
            List<Class<?>> messages = messageTypes.stream().filter(clazz -> !clazz.isEnum()).sorted(Comparator.comparing(Class::getSimpleName)).toList();

            DescriptorProtos.FileDescriptorProto.Builder fileBuilder = DescriptorProtos.FileDescriptorProto.newBuilder()
                    .setName(resolveFileName(entityClass));

            for (Class<?> enumType : enums) {
                fileBuilder.addEnumType(buildEnumDescriptor(enumType));
            }
            for (Class<?> messageType : messages) {
                fileBuilder.addMessageType(buildMessageDescriptor(messageType));
            }

            Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(
                    fileBuilder.build(), new Descriptors.FileDescriptor[0]);
            return fileDescriptor.findMessageTypeByName(resolveMessageName(entityClass));
        } catch (Descriptors.DescriptorValidationException ex) {
            throw new IllegalStateException("构建 Protobuf Descriptor 失败: " + entityClass.getName(), ex);
        }
    }

    private static Set<Class<?>> collectMessageTypes(Class<?> root) {
        Set<Class<?>> result = new LinkedHashSet<>();
        collectMessageTypes(root, result);
        return result;
    }

    private static void collectMessageTypes(Class<?> type, Set<Class<?>> result) {
        if (type == null || type == Object.class || !type.isAnnotationPresent(ProtobufMessage.class)) {
            return;
        }
        if (!result.add(type)) {
            return;
        }
        for (Field field : allFields(type)) {
            ProtoTypeMapper.ProtoFieldDescriptor descriptor = ProtoTypeMapper.describeField(field);
            if (descriptor == null) {
                continue;
            }
            Class<?> fieldType = resolveReferencedType(field, descriptor.protoType());
            if (fieldType != null && fieldType.isAnnotationPresent(ProtobufMessage.class)) {
                collectMessageTypes(fieldType, result);
            }
        }
    }

    private static Class<?> resolveReferencedType(Field field, String protoType) {
        String normalized = protoType;
        if (normalized.startsWith("repeated ")) {
            normalized = normalized.substring("repeated ".length());
        }
        if (normalized.startsWith("map<string, ")) {
            normalized = normalized.substring("map<string, ".length(), normalized.length() - 1).trim();
        }
        return switch (normalized) {
            case "string", "bool", "int32", "int64", "float", "double", "bytes" -> resolveMapOrCollectionValueClass(field);
            default -> field.getType().isEnum() ? field.getType()
                    : isNestedEntity(field.getType()) ? field.getType()
                    : resolveCollectionElementType(field, normalized);
        };
    }

    private static Class<?> resolveMapOrCollectionValueClass(Field field) {
        if (Map.class.isAssignableFrom(field.getType())) {
            return resolveMapValueClass(field);
        }
        if (Collection.class.isAssignableFrom(field.getType())) {
            return resolveCollectionElementClass(field);
        }
        return null;
    }

    private static Class<?> resolveCollectionElementType(Field field, String protoType) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            Class<?> element = resolveCollectionElementClass(field);
            if (element != null && element.isAnnotationPresent(ProtobufMessage.class)) {
                return element;
            }
        }
        if (!protoType.isBlank() && field.getType().isAnnotationPresent(ProtobufMessage.class)) {
            return field.getType();
        }
        return null;
    }

    private static boolean isNestedEntity(Class<?> type) {
        return type.isAnnotationPresent(ProtobufMessage.class) && !type.isEnum();
    }

    private static DescriptorProtos.EnumDescriptorProto buildEnumDescriptor(Class<?> enumType) {
        DescriptorProtos.EnumDescriptorProto.Builder builder = DescriptorProtos.EnumDescriptorProto.newBuilder()
                .setName(resolveMessageName(enumType))
                .addValue(DescriptorProtos.EnumValueDescriptorProto.newBuilder().setName("UNSPECIFIED").setNumber(0));
        Object[] constants = enumType.getEnumConstants();
        for (int i = 0; i < constants.length; i++) {
            Enum<?> constant = (Enum<?>) constants[i];
            builder.addValue(DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                    .setName(constant.name())
                    .setNumber(i + 1));
        }
        return builder.build();
    }

    private static DescriptorProtos.DescriptorProto buildMessageDescriptor(Class<?> messageType) {
        String messageName = resolveMessageName(messageType);
        DescriptorProtos.DescriptorProto.Builder builder = DescriptorProtos.DescriptorProto.newBuilder()
                .setName(messageName);

        int autoNumber = 1;
        for (Field field : allFields(messageType)) {
            ProtoTypeMapper.ProtoFieldDescriptor descriptor = ProtoTypeMapper.describeField(field);
            if (descriptor == null) {
                continue;
            }
            int fieldNumber = resolveFieldNumber(field, autoNumber++);
            String protoType = descriptor.protoType();
            if (protoType.startsWith("map<string, ")) {
                String valueType = protoType.substring("map<string, ".length(), protoType.length() - 1).trim();
                String entryName = mapEntryTypeName(descriptor.protoName());
                builder.addNestedType(buildMapEntryDescriptor(entryName, valueType, field));
                builder.addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                        .setName(descriptor.protoName())
                        .setNumber(fieldNumber)
                        .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED)
                        .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                        .setTypeName("." + messageName + "." + entryName));
                continue;
            }
            DescriptorProtos.FieldDescriptorProto.Builder fieldBuilder = DescriptorProtos.FieldDescriptorProto.newBuilder()
                    .setName(descriptor.protoName())
                    .setNumber(fieldNumber);
            applyProtoType(fieldBuilder, protoType, field);
            builder.addField(fieldBuilder.build());
        }
        return builder.build();
    }

    private static DescriptorProtos.DescriptorProto buildMapEntryDescriptor(
            String entryName,
            String valueProtoType,
            Field mapField) {
        DescriptorProtos.DescriptorProto.Builder entryBuilder = DescriptorProtos.DescriptorProto.newBuilder()
                .setName(entryName)
                .setOptions(DescriptorProtos.MessageOptions.newBuilder().setMapEntry(true))
                .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                        .setName("key")
                        .setNumber(1)
                        .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
                        .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING));

        DescriptorProtos.FieldDescriptorProto.Builder valueBuilder = DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("value")
                .setNumber(2)
                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL);
        applyMapValueType(valueBuilder, valueProtoType, mapField);
        entryBuilder.addField(valueBuilder.build());
        return entryBuilder.build();
    }

    private static void applyMapValueType(
            DescriptorProtos.FieldDescriptorProto.Builder builder,
            String valueProtoType,
            Field mapField) {
        Class<?> valueClass = resolveMapValueClass(mapField);
        if (valueClass != null && valueClass.isEnum()) {
            builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM)
                    .setTypeName("." + resolveMessageName(valueClass));
            return;
        }
        if (valueClass != null && valueClass.isAnnotationPresent(ProtobufMessage.class)) {
            builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName("." + resolveMessageName(valueClass));
            return;
        }
        applyScalarType(builder, valueProtoType);
    }

    private static void applyProtoType(
            DescriptorProtos.FieldDescriptorProto.Builder builder,
            String protoType,
            Field field) {
        if (protoType.startsWith("repeated ")) {
            builder.setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
            applyScalarOrMessage(builder, protoType.substring("repeated ".length()), field);
            return;
        }
        builder.setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL);
        applyScalarOrMessage(builder, protoType, field);
    }

    private static void applyScalarOrMessage(
            DescriptorProtos.FieldDescriptorProto.Builder builder,
            String protoType,
            Field field) {
        if (field.getType().isEnum()) {
            builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM)
                    .setTypeName("." + resolveMessageName(field.getType()));
            return;
        }
        if (isNestedEntity(field.getType())) {
            builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName("." + resolveMessageName(field.getType()));
            return;
        }
        if (!isScalarProtoType(protoType)) {
            builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName("." + protoType);
            return;
        }
        applyScalarType(builder, protoType);
    }

    private static void applyScalarType(DescriptorProtos.FieldDescriptorProto.Builder builder, String protoType) {
        switch (protoType) {
            case "string" -> builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING);
            case "bool" -> builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL);
            case "int32" -> builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
            case "int64" -> builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64);
            case "float" -> builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT);
            case "double" -> builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE);
            case "bytes" -> builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES);
            default -> throw new IllegalArgumentException("不支持的 proto 标量类型: " + protoType);
        }
    }

    private static boolean isScalarProtoType(String protoType) {
        return switch (protoType) {
            case "string", "bool", "int32", "int64", "float", "double", "bytes" -> true;
            default -> false;
        };
    }

    private static String mapEntryTypeName(String protoFieldName) {
        return toPascalCase(protoFieldName) + "Entry";
    }

    private static String toPascalCase(String snakeOrCamel) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : snakeOrCamel.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Class<?> resolveCollectionElementClass(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type arg = parameterizedType.getActualTypeArguments()[0];
            if (arg instanceof Class<?> clazz) {
                return clazz;
            }
        }
        return null;
    }

    private static Class<?> resolveMapValueClass(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type arg = parameterizedType.getActualTypeArguments()[1];
            if (arg instanceof Class<?> clazz) {
                return clazz;
            }
        }
        return null;
    }

    private static int resolveFieldNumber(Field field, int autoNumber) {
        ProtobufField annotation = field.getAnnotation(ProtobufField.class);
        if (annotation != null && annotation.number() > 0) {
            return annotation.number();
        }
        return autoNumber;
    }

    private static String resolveMessageName(Class<?> type) {
        ProtobufMessage annotation = type.getAnnotation(ProtobufMessage.class);
        if (annotation != null && !annotation.name().isBlank()) {
            return annotation.name();
        }
        return type.getSimpleName();
    }

    private static String resolveFileName(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase() + ".proto";
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }
}
