package com.xcz.commons.log.support;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * 全局 traceId 生成器，基于 Hutool 雪花算法保证分布式唯一性。
 */
public final class TraceIdGenerator {

    /** 复用单例 Snowflake，避免每次请求重复创建 */
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake();

    private TraceIdGenerator() {
    }

    /**
     * 生成下一个 traceId 字符串。
     *
     * @return 全局唯一的 traceId
     */
    public static String nextTraceId() {
        return SNOWFLAKE.nextIdStr();
    }
}
