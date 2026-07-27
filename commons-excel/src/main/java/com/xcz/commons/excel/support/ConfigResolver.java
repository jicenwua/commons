package com.xcz.commons.excel.support;

import com.alibaba.fastjson2.JSON;
import com.xcz.commons.excel.database.Config;
import com.xcz.commons.excel.database.DataType;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public class ConfigResolver {

    /**
     * 解析配置值为 Java 对象。
     *
     * @param config 配置项
     * @return 解析结果
     */
    public Object resolve(Config config) {
        return switch (config.getDataType()) {
            case STRING -> config.getValue();
            case INT -> Integer.parseInt(config.getValue());
            case LONG -> Long.parseLong(config.getValue());
            case DOUBLE -> Double.parseDouble(config.getValue());
            case BOOLEAN -> Boolean.parseBoolean(config.getValue());
            case BIG_DECIMAL -> new BigDecimal(config.getValue());
            case JSON -> parseJson(config);
            case EXCEL -> parseExcel(config);
        };
    }

    /**
     * 解析 JSON 配置。
     *
     * @param config 配置项
     * @return 反序列化对象
     */
    public Object parseJson(Config config) {
        Class<?> clazz = loadClass(config.getClazz());
        return JSON.parseObject(config.getValue(), clazz);
    }

    /**
     * 解析 Excel 配置。
     *
     * @param config 配置项
     * @return 配置类实例
     */
    public Object parseExcel(Config config) {
        if (!StringUtils.hasText(config.getPath())) {
            throw new IllegalArgumentException("Excel 配置缺少 path: " + config.getKey());
        }
        Class<?> clazz = loadClass(config.getClazz());
        return ExcelSupport.readExcel(config.getPath(), clazz);
    }

    /**
     * 加载类定义。
     *
     * @param className 全限定类名
     * @return Class 对象
     */
    public Class<?> loadClass(String className) {
        if (!StringUtils.hasText(className)) {
            throw new IllegalArgumentException("配置缺少 clazz");
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("类不存在: " + className, e);
        }
    }

    /**
     * 判断是否为基于 value 的热更新类型。
     *
     * @param dataType 数据类型
     * @return 是否仅更新 value 即可
     */
    public boolean isValueBasedType(DataType dataType) {
        return dataType != DataType.EXCEL;
    }
}
