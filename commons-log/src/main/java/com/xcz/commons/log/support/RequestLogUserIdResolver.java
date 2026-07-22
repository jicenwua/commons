package com.xcz.commons.log.support;

/**
 * 解析当前请求用户 ID，供日志打印。
 */
@FunctionalInterface
public interface RequestLogUserIdResolver {

    /**
     * 解析当前登录用户 ID。
     *
     * @return 用户 ID；未登录或失败时返回 {@code 0L}
     */
    Long resolveUserId();
}
