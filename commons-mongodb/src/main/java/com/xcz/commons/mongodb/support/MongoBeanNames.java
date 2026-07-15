package com.xcz.commons.mongodb.support;

/**
 * 多数据源 Bean 命名约定。
 * <p>
 * 每个数据源注册 4 个命名 Bean；主数据源直接使用默认 Bean 名（{@code mongoTemplate} 等），
 * 并额外注册 {@code primaryMongoTemplate} 等命名别名。
 */
public final class MongoBeanNames {

    /** 主数据源默认 {@link com.mongodb.client.MongoClient} Bean 名 */
    public static final String MONGO_CLIENT = "mongoClient";

    /** 主数据源默认 {@link org.springframework.data.mongodb.MongoDatabaseFactory} Bean 名 */
    public static final String MONGO_DATABASE_FACTORY = "mongoDatabaseFactory";

    /** 主数据源默认 {@link org.springframework.data.mongodb.core.MongoTemplate} Bean 名 */
    public static final String MONGO_TEMPLATE = "mongoTemplate";

    /** 主数据源默认 {@link org.springframework.data.mongodb.MongoTransactionManager} Bean 名 */
    public static final String TRANSACTION_MANAGER = "transactionManager";

    private MongoBeanNames() {
    }

    /**
     * @param dataSourceName 数据源名称，对应 {@code mongo.datasources} 的 key
     * @return 形如 {@code primaryMongoClient} 的 Bean 名
     */
    public static String mongoClient(String dataSourceName) {
        return dataSourceName + "MongoClient";
    }

    /**
     * @param dataSourceName 数据源名称
     * @return 形如 {@code primaryMongoDatabaseFactory} 的 Bean 名
     */
    public static String mongoDatabaseFactory(String dataSourceName) {
        return dataSourceName + "MongoDatabaseFactory";
    }

    /**
     * @param dataSourceName 数据源名称
     * @return 形如 {@code primaryMongoTemplate} 的 Bean 名，供 {@code @Qualifier} 或 {@code mongoTemplateRef} 使用
     */
    public static String mongoTemplate(String dataSourceName) {
        return dataSourceName + "MongoTemplate";
    }

    /**
     * @param dataSourceName 数据源名称
     * @return 形如 {@code primaryMongoTransactionManager} 的 Bean 名
     */
    public static String transactionManager(String dataSourceName) {
        return dataSourceName + "MongoTransactionManager";
    }
}
