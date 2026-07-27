package com.xcz.commons.excel.config;

import com.xcz.commons.excel.client.DataClient;
import com.xcz.commons.excel.client.DataEvent;
import com.xcz.commons.excel.support.ConfigResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnBean(DataProvider.class)
public class ExcelAutoConfiguration {

    /**
     * 注册配置解析器。
     *
     * @return ConfigResolver
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigResolver configResolver() {
        return new ConfigResolver();
    }

    /**
     * 注册内存数据客户端。
     *
     * @return DataClient
     */
    @Bean
    @ConditionalOnMissingBean
    public DataClient dataClient() {
        return new DataClient();
    }

    /**
     * 注册配置加载器。
     *
     * @param dataProvider   配置注册器
     * @param dataClient     数据客户端
     * @param configResolver 配置解析器
     * @return DataInitializing
     */
    @Bean
    @ConditionalOnMissingBean
    public DataInitializing dataInitializing(DataProvider dataProvider,
                                           DataClient dataClient,
                                           ConfigResolver configResolver) {
        return new DataInitializing(dataProvider, dataClient, configResolver);
    }

    /**
     * 注册热更新入口。
     *
     * @param dataInitializing 配置加载器
     * @return DataEvent
     */
    @Bean
    @ConditionalOnMissingBean
    public DataEvent dataEvent(DataInitializing dataInitializing) {
        return new DataEvent(dataInitializing);
    }
}
