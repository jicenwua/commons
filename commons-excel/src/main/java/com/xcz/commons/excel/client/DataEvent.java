package com.xcz.commons.excel.client;

import com.xcz.commons.excel.config.DataInitializing;
import com.xcz.commons.excel.database.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public final class DataEvent {

    private final DataInitializing dataInitializing;

    /**
     * 新增单个配置。
     *
     * @param config 配置项
     */
    public void addConfig(Config config) {
        dataInitializing.add(List.of(config));
    }

    /**
     * 批量新增配置。
     *
     * @param configs 配置列表
     */
    public void addConfig(Collection<Config> configs) {
        dataInitializing.add(new ArrayList<>(configs));
    }

    /**
     * 更新单个配置。
     *
     * @param config 配置项
     */
    public void updateConfig(Config config) {
        dataInitializing.update(List.of(config));
    }

    /**
     * 批量更新配置。
     *
     * @param configs 配置列表
     */
    public void updateConfig(Collection<Config> configs) {
        dataInitializing.update(new ArrayList<>(configs));
    }

    /**
     * 删除单个配置。
     *
     * @param config 配置项
     */
    public void deleteConfig(Config config) {
        dataInitializing.delete(List.of(config));
    }

    /**
     * 批量删除配置。
     *
     * @param configs 配置列表
     */
    public void deleteConfig(Collection<Config> configs) {
        dataInitializing.delete(new ArrayList<>(configs));
    }
}
