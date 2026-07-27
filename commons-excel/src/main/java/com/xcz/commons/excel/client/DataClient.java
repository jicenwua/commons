package com.xcz.commons.excel.client;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class DataClient {

    private final Map<String, Object> dataMap = new ConcurrentHashMap<>();

    /**
     * 保存配置数据。
     *
     * @param key   配置标识
     * @param value 配置值
     */
    public void put(String key, Object value) {
        dataMap.put(key, value);
        log.debug("配置数据已加载: key={}, type={}", key, value != null ? value.getClass().getSimpleName() : "null");
    }

    /**
     * 移除配置数据。
     *
     * @param key 配置标识
     */
    public void remove(String key) {
        dataMap.remove(key);
    }

    /**
     * 获取配置值。
     *
     * @param key 配置标识
     * @param <T> 值类型
     * @return 配置值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object value = dataMap.get(key);
        if (value == null && !dataMap.containsKey(key)) {
            throw new IllegalArgumentException("未配置数据 key：" + key);
        }
        return (T) value;
    }

    /**
     * 获取字符串配置。
     *
     * @param key 配置标识
     * @return 字符串值
     */
    public String getString(String key) {
        return get(key);
    }

    /**
     * 获取整数配置。
     *
     * @param key 配置标识
     * @return 整数值
     */
    public Integer getInt(String key) {
        return toNumber(get(key)).intValue();
    }

    /**
     * 获取长整数配置。
     *
     * @param key 配置标识
     * @return 长整型值
     */
    public Long getLong(String key) {
        return toNumber(get(key)).longValue();
    }

    /**
     * 获取浮点数配置。
     *
     * @param key 配置标识
     * @return 双精度值
     */
    public Double getDouble(String key) {
        return toNumber(get(key)).doubleValue();
    }

    /**
     * 获取布尔配置。
     *
     * @param key 配置标识
     * @return 布尔值
     */
    public Boolean getBool(String key) {
        Object value = get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 获取 BigDecimal 配置。
     *
     * @param key 配置标识
     * @return BigDecimal 值
     */
    public BigDecimal getBigDecimal(String key) {
        Object value = get(key);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    /**
     * 转换为数值类型。
     *
     * @param value 原始值
     * @return 数值
     */
    private Number toNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return Double.parseDouble(String.valueOf(value));
    }
}
