package com.xcz.commons.mongodb.transaction;

import com.xcz.commons.mongodb.support.MongoBeanNames;
import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 显式绑定 MongoDB 事务管理器。
 * <p>
 * 仅副本集或分片集群可用；单机 MongoDB 请勿使用。
 * 默认 {@code @Transactional} 仍走 JDBC 的 {@code transactionManager}。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Transactional(rollbackFor = Exception.class, transactionManager = MongoBeanNames.TRANSACTION_MANAGER)
public @interface MongoDBTransactional {

    @AliasFor(annotation = Transactional.class, attribute = "readOnly")
    boolean readOnly() default false;
}
