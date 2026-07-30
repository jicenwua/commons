package com.xcz.commons.protobuf.generator;

import com.xcz.commons.protobuf.annotation.ProtobufField;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Java 字段类型与 Protobuf 字段类型的映射工具。
 */
public final class ProtoTypeMapper {

    private ProtoTypeMapper() {
    }

    /**
     * 将 Java 字段名转为 proto 字段名（支持 @ProtobufField.name 覆盖）。
     */
    public static String toProtoFieldName(Field field) {
        ProtobufField annotation = field.getAnnotation(ProtobufField.class);
        if (annotation != null && !annotation.name().isBlank()) {
            return annotation.name();
        }
        return camelToSnake(field.getName());
    }

    static String toMessageName(Class<?> type) {
        return type.getSimpleName();
    }

    /**
     * 分析 Java 字段，返回 proto 字段名与类型描述（如 repeated string、map<string, int32>）。
     */
    public static ProtoFieldDescriptor describeField(Field field) {
        if (field.getAnnotation(ProtobufField.class) != null
                && field.getAnnotation(ProtobufField.class).ignore()) {
            return null;
        }

        Class<?> rawType = field.getType();
        if (Collection.class.isAssignableFrom(rawType)) {
            Class<?> elementType = resolveGenericArg(field.getGenericType(), 0);
            String protoType = toProtoScalarOrMessage(elementType);
            return new ProtoFieldDescriptor(toProtoFieldName(field), "repeated " + protoType, field);
        }
        if (Map.class.isAssignableFrom(rawType)) {
            Class<?> keyType = resolveGenericArg(field.getGenericType(), 0);
            Class<?> valueType = resolveGenericArg(field.getGenericType(), 1);
            if (!String.class.equals(keyType)) {
                throw new IllegalArgumentException("Map key 仅支持 String: " + field);
            }
            return new ProtoFieldDescriptor(
                    toProtoFieldName(field),
                    "map<string, " + toProtoScalarOrMessage(valueType) + ">",
                    field);
        }
        if (rawType.isEnum()) {
            return new ProtoFieldDescriptor(toProtoFieldName(field), toMessageName(rawType), field);
        }
        return new ProtoFieldDescriptor(toProtoFieldName(field), toProtoScalarOrMessage(rawType), field);
    }

    /** 将 Java 类型映射为 proto 标量或 message 类型名 */
    private static String toProtoScalarOrMessage(Class<?> type) {
        if (type == String.class || type == BigDecimal.class
                || type == LocalDateTime.class || type == LocalDate.class || type == Date.class) {
            return "string";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "bool";
        }
        if (type == int.class || type == Integer.class) {
            return "int32";
        }
        if (type == long.class || type == Long.class
                || type == Instant.class || type == LocalDateTime.class) {
            return type == LocalDateTime.class ? "string" : "int64";
        }
        if (type == float.class || type == Float.class) {
            return "float";
        }
        if (type == double.class || type == Double.class) {
            return "double";
        }
        if (type == byte[].class || type == Byte[].class) {
            return "bytes";
        }
        if (type.isEnum()) {
            return toMessageName(type);
        }
        if (type.getPackageName().startsWith("java.")) {
            throw new IllegalArgumentException("不支持的 Java 类型: " + type.getName());
        }
        return toMessageName(type);
    }

    static Object toProtoValue(Object value, Field field) {
        if (value == null) {
            return null;
        }
        Class<?> type = field.getType();
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toString();
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof Date date) {
            return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (type.isEnum()) {
            return ((Enum<?>) value).name();
        }
        return value;
    }

    static Object fromProtoValue(Object protoValue, Class<?> targetType, Field field) {
        if (protoValue == null) {
            return null;
        }
        if (targetType == String.class) {
            return protoValue.toString();
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(protoValue.toString());
        }
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(protoValue.toString());
        }
        if (targetType == LocalDate.class) {
            return LocalDate.parse(protoValue.toString());
        }
        if (targetType == Date.class) {
            LocalDateTime dateTime = LocalDateTime.parse(protoValue.toString());
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (targetType == Long.class || targetType == long.class) {
            return ((Number) protoValue).longValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return ((Number) protoValue).intValue();
        }
        if (targetType == Double.class || targetType == double.class) {
            return ((Number) protoValue).doubleValue();
        }
        if (targetType == Float.class || targetType == float.class) {
            return ((Number) protoValue).floatValue();
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(protoValue.toString());
        }
        if (targetType.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = Enum.valueOf((Class<Enum>) targetType, protoValue.toString());
            return enumValue;
        }
        return protoValue;
    }

    private static Class<?> resolveGenericArg(Type genericType, int index) {
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            throw new IllegalArgumentException("无法解析泛型: " + genericType);
        }
        Type arg = parameterizedType.getActualTypeArguments()[index];
        if (arg instanceof Class<?> clazz) {
            return clazz;
        }
        throw new IllegalArgumentException("无法解析泛型参数: " + arg);
    }

    /**
     * camelCase → snake_case。
     * 例如：orderId → order_id，userID → user_id，HTTPResponse → http_response。
     */
    private static String camelToSnake(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char current = name.charAt(i);
            if (Character.isUpperCase(current)) {
                boolean boundaryBefore = i > 0 && (
                        Character.isLowerCase(name.charAt(i - 1))
                                || (i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1))));
                if (boundaryBefore && result.charAt(result.length() - 1) != '_') {
                    result.append('_');
                }
                result.append(Character.toLowerCase(current));
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    /**
     * proto 字段描述：字段名、proto 类型字符串、原始 Java 字段。
     */
    public record ProtoFieldDescriptor(String protoName, String protoType, Field javaField) {
    }
}
