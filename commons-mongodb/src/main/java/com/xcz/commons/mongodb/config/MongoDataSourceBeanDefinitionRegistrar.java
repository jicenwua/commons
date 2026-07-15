package com.xcz.commons.mongodb.config;

import com.mongodb.client.MongoClient;
import com.xcz.commons.mongodb.factory.MongoClientFactory;
import com.xcz.commons.mongodb.properties.MongoDataSourceProperties;
import com.xcz.commons.mongodb.properties.MongoSettingsProperties;
import com.xcz.commons.mongodb.support.MongoBeanNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * 根据 {@code mongo.datasources} 动态注册多组 MongoDB Bean。
 * <p>
 * 在 Spring 容器刷新早期阶段（Bean 定义注册期）按配置项逐个注册
 * {@link MongoClient} → {@link SimpleMongoClientDatabaseFactory}
 * → {@link MongoTemplate} → {@link MongoTransactionManager} 依赖链。
 * 主数据源（{@code mongo.primary}）的 Bean 会标记 {@code @Primary} 并注册默认别名。
 */
@Slf4j
public class MongoDataSourceBeanDefinitionRegistrar
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final String STARTUP_BANNER = """

              __  __                            _
             |  \\/  | ___  _ __ ___  _ __   ___| |__
             | |\\/| |/ _ \\| '_ ` _ \\| '_ \\ / _ \\ '_ \\
             | |  | | (_) | | | | | | |_) |  __/ | | |
             |_|  |_|\\___/|_| |_| |_| .__/ \\___|_| |_|
                                    |_|
            """;

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 从 Environment 绑定 {@code mongo.*} 配置，校验后为每个数据源注册 Bean 定义。
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
        logStartupSummary(settings);

        String primary = settings.getPrimary();
        settings.getDatasources().forEach((name, properties) ->
                registerDataSource(registry, name, properties, name.equals(primary)));
    }

    /**
     * 为单个数据源注册完整的 Bean 依赖链。
     *
     * @param registry   Bean 定义注册表
     * @param name       数据源名称，对应 {@code mongo.datasources} 的 key
     * @param properties 该数据源的连接配置
     * @param primary    是否为主数据源；主库会设置 {@code @Primary} 并注册默认别名
     */
    private void registerDataSource(BeanDefinitionRegistry registry, String name,
                                    MongoDataSourceProperties properties, boolean primary) {
        String clientBeanName = MongoBeanNames.mongoClient(name);
        String factoryBeanName = MongoBeanNames.mongoDatabaseFactory(name);
        String templateBeanName = MongoBeanNames.mongoTemplate(name);
        String transactionManagerBeanName = MongoBeanNames.transactionManager(name);

        // MongoClient：无依赖，通过工厂方法延迟创建
        RootBeanDefinition clientDefinition = new RootBeanDefinition(MongoClient.class);
        clientDefinition.setInstanceSupplier(() -> MongoClientFactory.create(name, properties, environment));
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

        // 主数据源注册默认别名，兼容直接 @Autowired MongoTemplate 的写法
        if (primary) {
            registry.registerAlias(clientBeanName, MongoBeanNames.MONGO_CLIENT);
            registry.registerAlias(factoryBeanName, MongoBeanNames.MONGO_DATABASE_FACTORY);
            registry.registerAlias(templateBeanName, MongoBeanNames.MONGO_TEMPLATE);
            registry.registerAlias(transactionManagerBeanName, MongoBeanNames.TRANSACTION_MANAGER);
        }
    }

    /**
     * 启动时输出各数据源连接摘要与 ASCII 标识，风格与 MyBatis Plus 等组件启动日志一致。
     */
    private void logStartupSummary(MongoSettingsProperties settings) {
        String appName = environment.getProperty("spring.application.name", "application");
        settings.getDatasources().forEach((name, properties) -> {
            String mode = properties.getMode().name().toLowerCase().replace('_', '-');
            log.info("[Commons MongoDB] Register datasource [{}] for [{}], database: {}, mode: {}, hosts: {}{}",
                    name,
                    appName,
                    properties.getDatabase(),
                    mode,
                    MongoClientFactory.formatHostsSummary(properties),
                    name.equals(settings.getPrimary()) ? " (primary)" : "");
        });
        log.info(STARTUP_BANNER);
    }
}
