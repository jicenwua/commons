package com.xcz.commons.mongodb.support;

/**
 * 多数据源 Bean 命名约定。
 */
public final class MongoBeanNames {

    /** 主数据源默认 MongoClient Bean 名 */
    public static final String MONGO_CLIENT = "mongoClient";

    /** 主数据源默认 MongoDatabaseFactory Bean 名 */
    public static final String MONGO_DATABASE_FACTORY = "mongoDatabaseFactory";

    /** 主数据源默认 MongoTemplate Bean 名 */
    public static final String MONGO_TEMPLATE = "mongoTemplate";

    /** 主数据源 MongoTransactionManager Bean 名（非默认，不与 JDBC transactionManager 冲突） */
    public static final String TRANSACTION_MANAGER = "mongoDBTransactionManager";

    private MongoBeanNames() {
    }

    /**
     * @param dataSourceName 数据源名称
     * @return 命名 MongoClient Bean 名
     */
    public static String mongoClient(String dataSourceName) {
        return dataSourceName + "MongoClient";
    }

    /**
     * @param dataSourceName 数据源名称
     * @return 命名 MongoDatabaseFactory Bean 名
     */
    public static String mongoDatabaseFactory(String dataSourceName) {
        return dataSourceName + "MongoDatabaseFactory";
    }

    /**
     * @param dataSourceName 数据源名称
     * @return 命名 MongoTemplate Bean 名
     */
    public static String mongoTemplate(String dataSourceName) {
        return dataSourceName + "MongoTemplate";
    }

    /**
     * @param dataSourceName 数据源名称
     * @return 命名 MongoTransactionManager Bean 名
     */
    public static String transactionManager(String dataSourceName) {
        return dataSourceName + "MongoTransactionManager";
    }
}
