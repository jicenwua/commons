package com.xcz.commons.protobuf.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

final class ScalarConverter {

    private ScalarConverter() {
    }

    static Object toProtoScalar(Object value, Class<?> targetType) {
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
        return value;
    }

    static Object fromProtoScalar(Object protoValue, Class<?> targetType) {
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
}
