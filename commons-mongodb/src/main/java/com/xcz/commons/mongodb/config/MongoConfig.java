package com.xcz.commons.mongodb.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import com.xcz.commons.mongodb.properties.MongoSettingsProperties;

/**
 * MongoDB 多数据源自动装配。
 */
@AutoConfiguration
@AutoConfigureBefore({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@EnableConfigurationProperties(MongoSettingsProperties.class)
@ConditionalOnProperty(prefix = "mongo", name = "primary")
public class MongoConfig {

    /**
     * 注册多数据源 Bean 定义处理器（须为 static，以便早期执行）。
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static MongoDataSourceBeanDefinitionRegistrar mongoDataSourceBeanDefinitionRegistrar() {
        return new MongoDataSourceBeanDefinitionRegistrar();
    }
}
