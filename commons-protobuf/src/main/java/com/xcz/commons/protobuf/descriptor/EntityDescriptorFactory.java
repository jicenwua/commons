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
 * <p>
 * 流程：收集嵌套类型 → 构建 FileDescriptorProto → 编译为运行时 Descriptor。
 */
public final class EntityDescriptorFactory {

    private EntityDescriptorFactory() {
    }

    /**
     * 为指定实体类构建 Protobuf Descriptor，供 DynamicMessage 编解码使用。
     *
     * @param entityClass 带 @ProtobufMessage 的实体类
     * @return 该实体对应的 message Descriptor
     */
    public static Descriptors.Descriptor buildDescriptor(Class<?> entityClass) {
        try {
            // 递归收集根实体及所有嵌套 message / enum
            Set<Class<?>> messageTypes = collectMessageTypes(entityClass);
            List<Class<?>> enums = messageTypes.stream().filter(Class::isEnum).sorted(Comparator.comparing(Class::getSimpleName)).toList();
            List<Class<?>> messages = messageTypes.stream().filter(clazz -> !clazz.isEnum()).sorted(Comparator.comparing(Class::getSimpleName)).toList();

            // 拼装虚拟 .proto 文件描述
            DescriptorProtos.FileDescriptorProto.Builder fileBuilder = DescriptorProtos.FileDescriptorProto.newBuilder()
                    .setName(resolveFileName(entityClass));

            for (Class<?> enumType : enums) {
                fileBuilder.addEnumType(buildEnumDescriptor(enumType));
            }
            for (Class<?> messageType : messages) {
                fileBuilder.addMessageType(buildMessageDescriptor(messageType));
            }

            // 编译为运行时 FileDescriptor，再取出目标 message 的 Descriptor
            Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(
                    fileBuilder.build(), new Descriptors.FileDescriptor[0]);
            return fileDescriptor.findMessageTypeByName(resolveMessageName(entityClass));
        } catch (Descriptors.DescriptorValidationException ex) {
            throw new IllegalStateException("构建 Protobuf Descriptor 失败: " + entityClass.getName(), ex);
        }
    }

    /**
     * 从根实体开始，递归收集所有需要纳入同一 proto 文件的 message / enum 类型。
     */
    private static Set<Class<?>> collectMessageTypes(Class<?> root) {
        Set<Class<?>> result = new LinkedHashSet<>();
        collectMessageTypes(root, result);
        return result;
    }

    /**
     * 深度优先遍历字段，发现嵌套的 @ProtobufMessage 类型并加入集合。
     */
    private static void collectMessageTypes(Class<?> type, Set<Class<?>> result) {
        if (type == null || type == Object.class || !type.isAnnotationPresent(ProtobufMessage.class)) {
            return;
        }
        if (!result.add(type)) {
            return; // 已处理过，避免循环引用
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

    /**
     * 根据 proto 类型字符串，解析字段引用的嵌套 Java 类型（用于递归收集）。
     */
    private static Class<?> resolveReferencedType(Field field, String protoType) {
        String normalized = protoType;
        if (normalized.startsWith("repeated ")) {
            normalized = normalized.substring("repeated ".length());
        }
        if (normalized.startsWith("map<string, ")) {
            normalized = normalized.substring("map<string, ".length(), normalized.length() - 1).trim();
        }
        return switch (normalized) {
            // 标量类型：尝试从 List/Map 泛型中取嵌套实体
            case "string", "bool", "int32", "int64", "float", "double", "bytes" -> resolveMapOrCollectionValueClass(field);
            default -> field.getType().isEnum() ? field.getType()
                    : isNestedEntity(field.getType()) ? field.getType()
                    : resolveCollectionElementType(field, normalized);
        };
    }

    /**
     * 标量 proto 类型时，从 Map 或 Collection 泛型中解析嵌套实体类。
     */
    private static Class<?> resolveMapOrCollectionValueClass(Field field) {
        if (Map.class.isAssignableFrom(field.getType())) {
            return resolveMapValueClass(field);
        }
        if (Collection.class.isAssignableFrom(field.getType())) {
            return resolveCollectionElementClass(field);
        }
        return null;
    }

    /**
     * 从 repeated / message 类型中解析嵌套实体类。
     */
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

    /**
     * 判断是否为嵌套 message 实体（非 enum）。
     */
    private static boolean isNestedEntity(Class<?> type) {
        return type.isAnnotationPresent(ProtobufMessage.class) && !type.isEnum();
    }

    /**
     * 构建 enum 的 DescriptorProto（含 UNSPECIFIED = 0 默认值）。
     */
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

    /**
     * 构建 message 的 DescriptorProto，遍历字段生成 FieldDescriptorProto。
     */
    private static DescriptorProtos.DescriptorProto buildMessageDescriptor(Class<?> messageType) {
        String messageName = resolveMessageName(messageType);
        DescriptorProtos.DescriptorProto.Builder builder = DescriptorProtos.DescriptorProto.newBuilder()
                .setName(messageName);

        int autoNumber = 1;
        for (Field field : allFields(messageType)) {
            ProtoTypeMapper.ProtoFieldDescriptor descriptor = ProtoTypeMapper.describeField(field);
            if (descriptor == null) {
                continue; // @ProtobufField(ignore=true) 等被忽略的字段
            }
            int fieldNumber = resolveFieldNumber(field, autoNumber++);
            String protoType = descriptor.protoType();
            // map 字段需生成嵌套 Entry message
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

    /**
     * 构建 map 字段的 Entry 嵌套 message（key=string, value=指定类型）。
     */
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

    /**
     * 为 map Entry 的 value 字段设置 proto 类型。
     */
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

    /**
     * 设置字段 label（repeated / optional）并应用具体类型。
     */
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

    /**
     * 按 Java 字段类型或 proto 类型名，设置 enum / message / 标量类型。
     */
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
            // 非标量：按 proto 类型名引用 message
            builder.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName("." + protoType);
            return;
        }
        applyScalarType(builder, protoType);
    }

    /**
     * 将 proto 标量类型名映射为 FieldDescriptorProto.Type。
     */
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

    /**
     * 判断是否为 proto3 标量类型名。
     */
    private static boolean isScalarProtoType(String protoType) {
        return switch (protoType) {
            case "string", "bool", "int32", "int64", "float", "double", "bytes" -> true;
            default -> false;
        };
    }

    /**
     * 生成 map Entry 嵌套类型名，如 user_map → UserMapEntry。
     */
    private static String mapEntryTypeName(String protoFieldName) {
        return toPascalCase(protoFieldName) + "Entry";
    }

    /**
     * snake_case / camelCase 转 PascalCase。
     */
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

    /**
     * 从 List / Set 等 Collection 泛型中解析元素类型。
     */
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

    /**
     * 从 Map 泛型中解析 value 类型（key 固定为 String）。
     */
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

    /**
     * 解析字段编号：优先 @ProtobufField(number)，否则按声明顺序自动分配。
     */
    private static int resolveFieldNumber(Field field, int autoNumber) {
        ProtobufField annotation = field.getAnnotation(ProtobufField.class);
        if (annotation != null && annotation.number() > 0) {
            return annotation.number();
        }
        return autoNumber;
    }

    /**
     * 解析 message / enum 名称：优先 @ProtobufMessage(name)，否则用类名。
     */
    private static String resolveMessageName(Class<?> type) {
        ProtobufMessage annotation = type.getAnnotation(ProtobufMessage.class);
        if (annotation != null && !annotation.name().isBlank()) {
            return annotation.name();
        }
        return type.getSimpleName();
    }

    /**
     * 生成虚拟 proto 文件名（仅用于 FileDescriptorProto，不影响实际文件）。
     */
    private static String resolveFileName(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase() + ".proto";
    }

    /**
     * 收集类及其父类的所有非 static、非 transient 字段。
     */
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
