package com.xcz.commons.mongodb.config;

import com.mongodb.client.MongoClient;
import com.xcz.commons.mongodb.factory.MongoClientFactory;
import com.xcz.commons.mongodb.properties.MongoDataSourceProperties;
import com.xcz.commons.mongodb.properties.MongoSettingsProperties;
import com.xcz.commons.mongodb.support.MongoBeanNames;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.Map;

/**
 * 按配置动态注册多组 MongoDB Bean。
 */
public class MongoDataSourceBeanDefinitionRegistrar
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, PriorityOrdered {

    private Environment environment;
    private boolean multiDataSourceEnabled;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }


    /**
     * 绑定 mongo 配置并注册全部数据源 Bean 定义。
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        MongoSettingsProperties settings = Binder.get(environment)
                .bind("mongo", Bindable.of(MongoSettingsProperties.class))
                .orElse(null);
        if (settings == null || settings.getDatasources() == null || settings.getDatasources().isEmpty()) {
            return;
        }

        settings.validate();
        multiDataSourceEnabled = true;
        registerAllDataSources(registry, settings);
    }

    /**
     * 清理 Spring Boot 晚注册的默认 mongo Bean。
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        if (!multiDataSourceEnabled || !(beanFactory instanceof BeanDefinitionRegistry registry)) {
            return;
        }
        // 仅移除 Spring Boot 晚注册的 mongo Bean，不能移除本模块已注册的 mongoTemplate 等
        removeBeanDefinitionIfExists(registry, "mongo");
    }

    /**
     * 一次性注册全部数据源 Bean。
     */
    private void registerAllDataSources(BeanDefinitionRegistry registry, MongoSettingsProperties settings) {
        removeConflictingPrimaryBeans(registry);

        String primary = settings.getPrimary();
        for (Map.Entry<String, MongoDataSourceProperties> entry : settings.getDatasources().entrySet()) {
            registerSingleDataSource(registry, entry.getKey(), entry.getValue(),
                    entry.getKey().equals(primary));
        }
    }

    /**
     * 为单个数据源注册 Client / Factory / Template / TransactionManager。
     */
    private void registerSingleDataSource(BeanDefinitionRegistry registry, String name,
                                          MongoDataSourceProperties properties, boolean primary) {
        String clientBeanName = primary ? MongoBeanNames.MONGO_CLIENT : MongoBeanNames.mongoClient(name);
        String factoryBeanName = primary ? MongoBeanNames.MONGO_DATABASE_FACTORY
                : MongoBeanNames.mongoDatabaseFactory(name);
        String templateBeanName = primary ? MongoBeanNames.MONGO_TEMPLATE : MongoBeanNames.mongoTemplate(name);
        String transactionManagerBeanName = primary ? MongoBeanNames.TRANSACTION_MANAGER
                : MongoBeanNames.transactionManager(name);

        // MongoClient：无依赖，延迟到首次使用时才创建并打印启动日志
        RootBeanDefinition clientDefinition = new RootBeanDefinition(MongoClient.class);
        clientDefinition.setInstanceSupplier(() -> MongoClientFactory.create(name, properties, environment, primary));
        clientDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        if (primary) {
            clientDefinition.setPrimary(true);
        }
        registry.registerBeanDefinition(clientBeanName, clientDefinition);

        // MongoDatabaseFactory：依赖 MongoClient + database 名
        RootBeanDefinition factoryDefinition = new RootBeanDefinition(SimpleMongoClientDatabaseFactory.class);
        factoryDefinition.getConstructorArgumentValues()
                .addIndexedArgumentValue(0, new RuntimeBeanReference(clientBeanName));
        factoryDefinition.getConstructorArgumentValues()
                .addIndexedArgumentValue(1, properties.getDatabase());
        factoryDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        if (primary) {
            factoryDefinition.setPrimary(true);
        }
        registry.registerBeanDefinition(factoryBeanName, factoryDefinition);

        // MongoTemplate：依赖 MongoDatabaseFactory
        RootBeanDefinition templateDefinition = new RootBeanDefinition(MongoTemplate.class);
        templateDefinition.getConstructorArgumentValues()
                .addIndexedArgumentValue(0, new RuntimeBeanReference(factoryBeanName));
        templateDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        if (primary) {
            templateDefinition.setPrimary(true);
        }
        registry.registerBeanDefinition(templateBeanName, templateDefinition);

        // MongoTransactionManager：仅副本集可用，须显式指定 transactionManager，不设为默认 Primary
        RootBeanDefinition transactionManagerDefinition = new RootBeanDefinition(MongoTransactionManager.class);
        transactionManagerDefinition.getConstructorArgumentValues()
                .addIndexedArgumentValue(0, new RuntimeBeanReference(factoryBeanName));
        transactionManagerDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(transactionManagerBeanName, transactionManagerDefinition);

        // 主数据源额外注册命名别名，供 @Qualifier("primaryMongoTemplate") 使用
        if (primary) {
            registry.registerAlias(clientBeanName, MongoBeanNames.mongoClient(name));
            registry.registerAlias(factoryBeanName, MongoBeanNames.mongoDatabaseFactory(name));
            registry.registerAlias(templateBeanName, MongoBeanNames.mongoTemplate(name));
            registry.registerAlias(transactionManagerBeanName, MongoBeanNames.transactionManager(name));
        }
    }

    /**
     * 移除与主库默认名冲突的 Spring Boot Mongo Bean。
     */
    private void removeConflictingPrimaryBeans(BeanDefinitionRegistry registry) {
        removeBeanDefinitionIfExists(registry, "mongo");
        removeBeanDefinitionIfExists(registry, MongoBeanNames.MONGO_CLIENT);
        removeBeanDefinitionIfExists(registry, MongoBeanNames.MONGO_DATABASE_FACTORY);
        removeBeanDefinitionIfExists(registry, MongoBeanNames.MONGO_TEMPLATE);
    }

    private void removeBeanDefinitionIfExists(BeanDefinitionRegistry registry, String beanName) {
        if (registry.containsBeanDefinition(beanName)) {
            registry.removeBeanDefinition(beanName);
        }
    }
}
