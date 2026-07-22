package com.xcz.commons.log.support;

import com.xcz.commons.core.utils.uuid.IdUtils;

/**
 * 生成全局唯一 traceId。
 */
public final class TraceIdGenerator {

    private TraceIdGenerator() {
    }

    /**
     * 生成下一个 traceId。
     *
     * @return 全局唯一的 traceId
     */
    public static String nextTraceId() {
        return IdUtils.fastSimpleUUID();
    }
}
