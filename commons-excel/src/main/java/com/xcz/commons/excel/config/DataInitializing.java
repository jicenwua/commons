package com.xcz.commons.excel.config;

import com.xcz.commons.excel.client.DataClient;
import com.xcz.commons.excel.database.Config;
import com.xcz.commons.excel.support.ConfigResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public final class DataInitializing {

    private final DataProvider dataProvider;
    private final DataClient dataClient;
    private final ConfigResolver configResolver;

    /**
     * 启动时加载配置。
     */
    @PostConstruct
    public void init() {
        log.info("------------------开始加载配置数据-----------------------");
        dataProvider.onInit();
        List<Config> configs = dataProvider.getConfigs();
        if (configs.isEmpty()) {
            log.info("------------------------未进行配置--------------------------------");
            return;
        }
        loadConfig(configs);
        dataProvider.onComplete();
        log.info("------------------配置数据加载完成，共 {} 项-----------------------", configs.size());
    }

    /**
     * 热更新配置。
     *
     * @param configs 配置列表
     */
    public void update(List<Config> configs) {
        loadConfig(configs);
        dataProvider.onChange(configs);
    }

    /**
     * 新增配置。
     *
     * @param configs 配置列表
     */
    public void add(List<Config> configs) {
        loadConfig(configs);
        dataProvider.onAddConfig(configs);
    }

    /**
     * 删除配置。
     *
     * @param configs 配置列表
     */
    public void delete(List<Config> configs) {
        for (Config config : configs) {
            dataProvider.removeConfig(config.getKey());
            dataClient.remove(config.getKey());
        }
        dataProvider.onRemoveConfig(configs);
    }

    /**
     * 加载配置到内存。
     *
     * @param configs 配置列表
     */
    public void loadConfig(List<Config> configs) {
        Map<String, Object> data = new HashMap<>(configs.size());
        for (Config config : configs) {
            try {
                Object value = configResolver.resolve(config);
                data.put(config.getKey(), value);
                log.info("加载配置成功: key={}, title={}, type={}", config.getKey(), config.getTitle(), config.getDataType());
            } catch (Exception e) {
                log.error("加载配置失败: key={}, type={}", config.getKey(), config.getDataType(), e);
                throw new IllegalStateException("加载配置失败: " + config.getKey(), e);
            }
        }
        data.forEach(dataClient::put);
    }
}
