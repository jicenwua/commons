package com.xcz.member.log.support;

/**
 * 默认用户 ID 解析器：未集成安全模块时固定返回 {@code 0L}。
 */
public class DefaultRequestLogUserIdResolver implements RequestLogUserIdResolver {

    /**
     * {@inheritDoc}
     */
    @Override
    public Long resolveUserId() {
        return 0L;
    }
}
