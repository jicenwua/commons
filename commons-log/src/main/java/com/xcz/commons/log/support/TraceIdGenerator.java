package com.xcz.commons.log.support;

import com.xcz.commons.core.utils.uuid.IdUtils;

/**
 * 全局 traceId 生成器，基于 Hutool 雪花算法保证分布式唯一性。
 */
public final class TraceIdGenerator {

    private TraceIdGenerator() {
    }

    /**
     * 生成下一个 traceId 字符串。
     *
     * @return 全局唯一的 traceId
     */
    public static String nextTraceId() {
        return IdUtils.fastSimpleUUID();
    }
}
