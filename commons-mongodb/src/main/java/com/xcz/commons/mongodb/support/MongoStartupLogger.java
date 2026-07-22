package com.xcz.commons.mongodb.support;

import com.xcz.commons.mongodb.factory.MongoClientFactory;
import com.xcz.commons.mongodb.properties.MongoDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MongoDB 启动日志输出。
 */
@Slf4j
public final class MongoStartupLogger {

    private static final AtomicBoolean BANNER_PRINTED = new AtomicBoolean(false);

    private static final String STARTUP_BANNER = """

            MM    MM                               DDDDD   BBBBB
            MMM  MMM  oooo  nn nnn   gggggg  oooo  DD  DD  BB   B
            MM MM MM oo  oo nnn  nn gg   gg oo  oo DD   DD BBBBBB
            MM    MM oo  oo nn   nn ggggggg oo  oo DD   DD BB   BB
            MM    MM  oooo  nn   nn      gg  oooo  DDDDDD  BBBBBB
                                     ggggg
            """;

    private MongoStartupLogger() {
    }

    /**
     * 在 MongoClient 创建前输出数据源摘要。
     */
    public static void logBeforeClientCreate(String dataSourceName, boolean primary,
                                             MongoDataSourceProperties properties,
                                             Environment environment) {
        String appName = environment.getProperty("spring.application.name", "application");
        String mode = properties.getMode().name().toLowerCase().replace('_', '-');
        log.info("[Commons MongoDB] Successfully initialized MongoClient for [{}], datasource: {}, database: {}, mode: {}, hosts: {}{}",
                appName,
                dataSourceName,
                properties.getDatabase(),
                mode,
                MongoClientFactory.formatHostsSummary(properties),
                primary ? " (primary)" : "");
        if (primary && BANNER_PRINTED.compareAndSet(false, true)) {
            log.info(STARTUP_BANNER);
        }
    }
}
