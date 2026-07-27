package com.xcz.commons.excel.support;

import cn.hutool.core.date.DateUtil;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Excel 单元格值转换器。
 */
public final class CellValueConverter {

    private CellValueConverter() {
    }

    /**
     * 判断值是否为空。
     *
     * @param value 单元格值
     * @return 是否为空
     */
    public static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    /**
     * 转换单元格值为目标类型。
     *
     * @param value      单元格值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    public static Object convertValue(Object value, Class<?> targetType) {
        if (isBlank(value)) {
            return getDefaultValue(targetType);
        }
        String text = String.valueOf(value).trim();
        if (targetType == String.class) {
            return text;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return value instanceof Number number ? number.intValue() : Integer.parseInt(text);
        }
        if (targetType == long.class || targetType == Long.class) {
            return value instanceof Number number ? number.longValue() : Long.parseLong(text);
        }
        if (targetType == double.class || targetType == Double.class) {
            return value instanceof Number number ? number.doubleValue() : Double.parseDouble(text);
        }
        if (targetType == float.class || targetType == Float.class) {
            return value instanceof Number number ? number.floatValue() : Float.parseFloat(text);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof Number number) {
                return number.intValue() != 0;
            }
            return Boolean.parseBoolean(text);
        }
        if (targetType == java.math.BigDecimal.class) {
            return value instanceof java.math.BigDecimal decimal ? decimal : new java.math.BigDecimal(text);
        }
        if (targetType == java.math.BigInteger.class) {
            return value instanceof java.math.BigInteger bigInteger ? bigInteger : new java.math.BigInteger(text);
        }
        if (targetType == java.util.Date.class) {
            return toUtilDate(value, text);
        }
        if (targetType == java.sql.Date.class) {
            if (value instanceof java.sql.Date sqlDate) {
                return sqlDate;
            }
            return new java.sql.Date(toUtilDate(value, text).getTime());
        }
        if (targetType == LocalDate.class) {
            return toLocalDateTime(value, text).toLocalDate();
        }
        if (targetType == LocalDateTime.class) {
            return toLocalDateTime(value, text);
        }
        return text;
    }

    /**
     * 按字段类型转换单元格值（含 List/Set/Map 集合格式）。
     *
     * @param value 单元格值
     * @param field 目标字段
     * @return 转换后的值
     */
    public static Object convertFieldValue(Object value, Field field) {
        Class<?> fieldType = field.getType();
        if (List.class.isAssignableFrom(fieldType)) {
            Class<?> elementType = ClassAnalyseSupport.analyse(field).getFirst();
            return parseListValue(value, elementType, fieldType);
        }
        if (Set.class.isAssignableFrom(fieldType)) {
            Class<?> elementType = ClassAnalyseSupport.analyse(field).getFirst();
            return parseSetValue(value, elementType, fieldType);
        }
        if (Map.class.isAssignableFrom(fieldType)) {
            List<Class<?>> types = ClassAnalyseSupport.analyse(field);
            return parseMapValue(value, types.get(0), types.get(1), fieldType);
        }
        return convertValue(value, fieldType);
    }

    private static Object parseListValue(Object value, Class<?> elementType, Class<?> fieldType) {
        if (isBlank(value)) {
            return createCollection(fieldType, CollectionKind.LIST);
        }
        String content = unwrapBraceContent(String.valueOf(value).trim());
        if (content.isEmpty()) {
            return createCollection(fieldType, CollectionKind.LIST);
        }
        List<Object> result = new ArrayList<>();
        for (String item : splitCollectionItems(content)) {
            result.add(convertValue(item, elementType));
        }
        return result;
    }

    private static Object parseSetValue(Object value, Class<?> elementType, Class<?> fieldType) {
        if (isBlank(value)) {
            return createCollection(fieldType, CollectionKind.SET);
        }
        String content = unwrapBraceContent(String.valueOf(value).trim());
        if (content.isEmpty()) {
            return createCollection(fieldType, CollectionKind.SET);
        }
        Set<Object> result = new LinkedHashSet<>();
        for (String item : splitCollectionItems(content)) {
            result.add(convertValue(item, elementType));
        }
        return result;
    }

    private static Object parseMapValue(Object value, Class<?> keyType, Class<?> valueType, Class<?> fieldType) {
        if (isBlank(value)) {
            return createCollection(fieldType, CollectionKind.MAP);
        }
        String content = unwrapBraceContent(String.valueOf(value).trim());
        if (content.isEmpty()) {
            return createCollection(fieldType, CollectionKind.MAP);
        }
        Map<Object, Object> result = new LinkedHashMap<>();
        for (String pair : splitMapPairs(content)) {
            String[] kv = pair.split(":", 2);
            if (kv.length != 2) {
                throw new IllegalArgumentException("Map 单元格格式错误，键值对需使用 ':' 分隔: " + pair);
            }
            Object key = convertValue(kv[0].trim(), keyType);
            Object mapValue = convertValue(kv[1].trim(), valueType);
            if (key != null) {
                result.put(key, mapValue);
            }
        }
        return result;
    }

    private static String unwrapBraceContent(String text) {
        if (text.startsWith("{") && text.endsWith("}")) {
            return text.substring(1, text.length() - 1).trim();
        }
        throw new IllegalArgumentException("集合单元格格式错误，需以 '{}' 包裹: " + text);
    }

    private static List<String> splitCollectionItems(String content) {
        return Arrays.stream(content.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static List<String> splitMapPairs(String content) {
        return Arrays.stream(content.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private enum CollectionKind {
        LIST, SET, MAP
    }

    private static Object createCollection(Class<?> fieldType, CollectionKind kind) {
        return switch (kind) {
            case LIST -> List.class.isAssignableFrom(fieldType) && !fieldType.isInterface()
                    ? createInstance(fieldType) : new ArrayList<>();
            case SET -> Set.class.isAssignableFrom(fieldType) && !fieldType.isInterface()
                    ? createInstance(fieldType) : new LinkedHashSet<>();
            case MAP -> Map.class.isAssignableFrom(fieldType) && !fieldType.isInterface()
                    ? createInstance(fieldType) : new LinkedHashMap<>();
        };
    }

    private static <T> T createInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法创建实例: " + clazz.getName(), e);
        }
    }

    private static java.util.Date toUtilDate(Object value, String text) {
        if (value instanceof java.util.Date date) {
            return date;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return DateUtil.date(localDateTime);
        }
        if (value instanceof LocalDate localDate) {
            return DateUtil.date(localDate);
        }
        if (value instanceof Number number) {
            return org.apache.poi.ss.usermodel.DateUtil.getJavaDate(number.doubleValue());
        }
        return DateUtil.parse(text);
    }

    private static LocalDateTime toLocalDateTime(Object value, String text) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (value instanceof java.util.Date date) {
            return DateUtil.toLocalDateTime(date);
        }
        if (value instanceof Number number) {
            return DateUtil.toLocalDateTime(
                    org.apache.poi.ss.usermodel.DateUtil.getJavaDate(number.doubleValue()));
        }
        return DateUtil.parseLocalDateTime(text);
    }

    private static Object getDefaultValue(Class<?> targetType) {
        if (targetType == boolean.class) {
            return false;
        }
        if (targetType.isPrimitive()) {
            return 0;
        }
        return null;
    }
}
