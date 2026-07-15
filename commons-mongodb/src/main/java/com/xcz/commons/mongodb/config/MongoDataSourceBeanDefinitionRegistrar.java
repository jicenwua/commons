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
 * 根据 {@code mongo.datasources} 动态注册多组 MongoDB Bean。
 * <p>
 * 在 Spring 容器刷新早期阶段（Bean 定义注册期）按配置项逐个注册
 * {@link MongoClient} → {@link SimpleMongoClientDatabaseFactory}
 * → {@link MongoTemplate} → {@link MongoTransactionManager} 依赖链。
 * 主数据源（{@code mongo.primary}）直接使用默认 Bean 名（{@code mongoTemplate} 等），
 * 并额外注册 {@code primaryMongoTemplate} 等命名别名。
 * <p>
 * 启动日志在 {@link MongoClient} 实际实例化时输出，而非本阶段。
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
     * 从 Environment 绑定 {@code mongo.*} 配置，校验后一次性注册全部数据源 Bean 定义。
     * <p>
     * 使用 {@link Binder} 而非直接注入 {@link MongoSettingsProperties}，
     * 是因为本处理器运行在 Bean 工厂初始化之前，配置 Bean 尚未就绪。
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
     * 所有 Bean 定义注册完成后再次清理 Spring Boot 默认 Mongo Bean。
     * <p>
     * Spring Boot 的 {@code MongoAutoConfiguration} 可能在本处理器之后才注册 {@code mongo} Bean，
     * 需在实例化前做最后一轮移除，避免重复创建 MongoClient 导致日志分散。
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
     * 一次性注册全部数据源：冲突 Bean 清理只执行一次，循环在方法内部完成。
     *
     * @param registry Bean 定义注册表
     * @param settings 多数据源顶层配置
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
     * 为单个数据源注册完整的 Bean 依赖链。
     *
     * @param registry   Bean 定义注册表
     * @param name       数据源名称，对应 {@code mongo.datasources} 的 key
     * @param properties 该数据源的连接配置
     * @param primary    是否为主数据源；主库使用默认 Bean 名并标记 {@code @Primary}
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

        // MongoTransactionManager：依赖 MongoDatabaseFactory，供 @Transactional 使用
        RootBeanDefinition transactionManagerDefinition = new RootBeanDefinition(MongoTransactionManager.class);
        transactionManagerDefinition.getConstructorArgumentValues()
                .addIndexedArgumentValue(0, new RuntimeBeanReference(factoryBeanName));
        transactionManagerDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        if (primary) {
            transactionManagerDefinition.setPrimary(true);
        }
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
     * 移除 Spring Boot 默认 Mongo 自动配置可能已注册的 Bean，为本模块主库 Bean 让路。
     * <p>
     * 仅在 {@link #registerAllDataSources} 开头调用一次；不可在 {@link #postProcessBeanFactory} 中移除
     * {@code mongoTemplate} 等，否则会误删本模块已注册的 Bean。
     */
    private void removeConflictingPrimaryBeans(BeanDefinitionRegistry registry) {
        removeBeanDefinitionIfExists(registry, "mongo");
        removeBeanDefinitionIfExists(registry, MongoBeanNames.MONGO_CLIENT);
        removeBeanDefinitionIfExists(registry, MongoBeanNames.MONGO_DATABASE_FACTORY);
        removeBeanDefinitionIfExists(registry, MongoBeanNames.MONGO_TEMPLATE);
        removeBeanDefinitionIfExists(registry, MongoBeanNames.TRANSACTION_MANAGER);
    }

    private void removeBeanDefinitionIfExists(BeanDefinitionRegistry registry, String beanName) {
        if (registry.containsBeanDefinition(beanName)) {
            registry.removeBeanDefinition(beanName);
        }
    }
}
