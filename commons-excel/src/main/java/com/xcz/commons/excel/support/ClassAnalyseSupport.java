package com.xcz.commons.excel.support;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@UtilityClass
public final class ClassAnalyseSupport {

    /**
     * 判断是否为基本类型。
     *
     * @param type 类型
     * @return 是否基本类型
     */
    public static boolean isBasicType(Class<?> type) {
        if (type.isPrimitive()) {
            return true;
        }
        if (type == String.class || type == BigDecimal.class || type == BigInteger.class) {
            return true;
        }
        if (type == Boolean.class || type == Character.class) {
            return true;
        }
        if (Number.class.isAssignableFrom(type) && type.getPackageName().equals("java.lang")) {
            return true;
        }
        if (Date.class.isAssignableFrom(type)) {
            return true;
        }
        return type.getPackageName().startsWith("java.time");
    }

    /**
     * 解析字段泛型类型。
     *
     * @param field 字段
     * @return List 返回元素类型；Map 返回 key 和 value 类型
     */
    public static List<Class<?>> analyse(Field field) {
        Class<?> type = field.getType();
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (List.class.isAssignableFrom(type)) {
                Class<?> actualTypeArgument = (Class<?>) actualTypeArguments[0];
                return List.of(actualTypeArgument);
            } else if (Set.class.isAssignableFrom(type)) {
                Class<?> actualTypeArgument = (Class<?>) actualTypeArguments[0];
                return List.of(actualTypeArgument);
            } else if (Map.class.isAssignableFrom(type)) {
                Class<?> keyType = (Class<?>) actualTypeArguments[0];
                Class<?> valueType = (Class<?>) actualTypeArguments[1];
                return List.of(keyType, valueType);
            }
        }
        return List.of(type);
    }
}
