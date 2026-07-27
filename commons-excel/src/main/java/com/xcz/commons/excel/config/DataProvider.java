package com.xcz.commons.excel.config;

import com.xcz.commons.excel.database.Config;
import com.xcz.commons.excel.database.DataType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class DataProvider {

    private final Map<String, Config> configMap = new ConcurrentHashMap<>();

    /**
     * 注册配置项，启动时调用一次。
     */
    public abstract void onInit();

    /**
     * 配置热更新成功回调。
     *
     * @param configs 变更的配置列表
     */
    public void onChange(List<Config> configs) {
    }

    /**
     * 配置新增回调。
     *
     * @param configs 新增的配置列表
     */
    public void onAddConfig(List<Config> configs) {
    }

    /**
     * 配置删除回调。
     *
     * @param configs 删除的配置列表
     */
    public void onRemoveConfig(List<Config> configs) {
    }

    /**
     * 配置全量加载完成回调。
     */
    public void onComplete() {
    }

    /**
     * 获取所有已注册配置。
     *
     * @return 配置列表
     */
    public List<Config> getConfigs() {
        return new ArrayList<>(configMap.values());
    }

    /**
     * 移除配置项。
     *
     * @param key 配置标识
     */
    public void removeConfig(String key) {
        configMap.remove(key);
    }

    /**
     * 获取配置 Map。
     *
     * @return 不可变配置 Map
     */
    public Map<String, Config> getConfigMap() {
        return Collections.unmodifiableMap(configMap);
    }

    /**
     * 按 key 获取配置项。
     *
     * @param key 配置标识
     * @return 配置项，不存在时返回 null
     */
    public Config getConfig(String key) {
        return configMap.get(key);
    }

    /**
     * 注册配置项。
     *
     * @param config 配置项
     */
    private void registerConfig(Config config) {
        String key = config.getKey();
        if (configMap.containsKey(key)) {
            log.error("配置项 key 重复: {}", key);
            throw new IllegalArgumentException("配置项 key 重复: " + key);
        }
        configMap.put(key, config);
    }

    /**
     * 构建基础类型配置项。
     *
     * @param key      配置标识
     * @param title    配置标题
     * @param value    配置值
     * @param dataType 数据类型
     * @return 配置项
     */
    protected Config buildConfig(String key, String title, String value, DataType dataType) {
        return Config.builder()
                .key(key)
                .title(title)
                .value(value)
                .dataType(dataType)
                .build();
    }

    /**
     * 构建 JSON 类型配置项。
     *
     * @param key      配置标识
     * @param title    配置标题
     * @param value    JSON 文本
     * @param dataType 数据类型
     * @param clazz    目标类名
     * @return 配置项
     */
    protected Config buildConfig(String key, String title, String value, DataType dataType, String clazz) {
        return Config.builder()
                .key(key)
                .title(title)
                .value(value)
                .dataType(dataType)
                .clazz(clazz)
                .build();
    }

    /**
     * 构建 Excel 类型配置项。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param path  Excel 目录
     * @param clazz 配置类名
     * @return 配置项
     */
    protected Config buildExcelConfig(String key, String title, String path, String clazz) {
        return Config.builder()
                .key(key)
                .title(title)
                .path(path)
                .dataType(DataType.EXCEL)
                .clazz(clazz)
                .build();
    }

    /**
     * 注册 STRING 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param value 配置值
     */
    protected void dataString(String key, String title, String value) {
        registerConfig(buildConfig(key, title, value, DataType.STRING));
    }

    /**
     * 注册 INT 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param value 配置值
     */
    protected void dataInt(String key, String title, int value) {
        registerConfig(buildConfig(key, title, String.valueOf(value), DataType.INT));
    }

    /**
     * 注册 LONG 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param value 配置值
     */
    protected void dataLong(String key, String title, long value) {
        registerConfig(buildConfig(key, title, String.valueOf(value), DataType.LONG));
    }

    /**
     * 注册 DOUBLE 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param value 配置值
     */
    protected void dataDouble(String key, String title, double value) {
        registerConfig(buildConfig(key, title, String.valueOf(value), DataType.DOUBLE));
    }

    /**
     * 注册 BOOLEAN 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param value 配置值
     */
    protected void dataBool(String key, String title, boolean value) {
        registerConfig(buildConfig(key, title, String.valueOf(value), DataType.BOOLEAN));
    }

    /**
     * 注册 BIG_DECIMAL 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param value 配置值
     */
    protected void dataBigDecimal(String key, String title, String value) {
        registerConfig(buildConfig(key, title, value, DataType.BIG_DECIMAL));
    }

    /**
     * 注册 JSON 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param value JSON 文本
     * @param clazz 目标类型
     */
    protected void dataJson(String key, String title, String value, Class<?> clazz) {
        registerConfig(buildConfig(key, title, value, DataType.JSON, clazz.getName()));
    }

    /**
     * 注册 JSON 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param value JSON 文本
     * @param clazz 目标类名
     */
    protected void dataJson(String key, String title, String value, String clazz) {
        registerConfig(buildConfig(key, title, value, DataType.JSON, clazz));
    }

    /**
     * 注册 EXCEL 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param path  Excel 目录
     * @param clazz 配置类型
     */
    protected void dataExcel(String key, String title, String path, Class<?> clazz) {
        registerConfig(buildExcelConfig(key, title, path, clazz.getName()));
    }

    /**
     * 注册 EXCEL 配置。
     *
     * @param key   配置标识
     * @param title 配置标题
     * @param path  Excel 目录
     * @param clazz 配置类名
     */
    protected void dataExcel(String key, String title, String path, String clazz) {
        registerConfig(buildExcelConfig(key, title, path, clazz));
    }
}
