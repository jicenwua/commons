package com.xcz.commons.log.support;

/**
 * 默认用户 ID 解析器，固定返回 {@code 0L}。
 */
public class DefaultRequestLogUserIdResolver implements RequestLogUserIdResolver {

    @Override
    public Long resolveUserId() {
        return 0L;
    }
}
