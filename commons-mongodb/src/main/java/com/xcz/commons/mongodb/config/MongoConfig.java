package com.xcz.commons.mongodb.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import com.xcz.commons.mongodb.properties.MongoSettingsProperties;

/**
 * MongoDB 多数据源自动装配。
 * <p>
 * 通过 {@link MongoDataSourceBeanDefinitionRegistrar} 按 {@code mongo.datasources} 动态注册
 * {@link com.mongodb.client.MongoClient}、{@link org.springframework.data.mongodb.core.MongoTemplate} 等 Bean。
 * 主数据源（{@code mongo.primary}）会额外注册无 qualifier 的默认 Bean。
 */
@AutoConfiguration
@EnableConfigurationProperties(MongoSettingsProperties.class)
@ConditionalOnProperty(prefix = "mongo", name = "primary")
public class MongoConfig {

    /**
     * 注册多数据源 Bean 定义处理器。
     * <p>
     * 必须为 static 方法：{@link BeanDefinitionRegistryPostProcessor} 需在容器早期执行，
     * 非 static 的 @Bean 工厂方法此时还不可用。
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static MongoDataSourceBeanDefinitionRegistrar mongoDataSourceBeanDefinitionRegistrar() {
        return new MongoDataSourceBeanDefinitionRegistrar();
    }
}
