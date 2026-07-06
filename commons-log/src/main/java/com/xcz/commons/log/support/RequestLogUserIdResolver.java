package com.xcz.commons.log.support;

/**
 * 解析当前请求用户 ID，供日志打印使用。
 * <p>
 * 默认由自动配置提供实现；业务方可注册自定义 Bean 覆盖默认行为。
 */
@FunctionalInterface
public interface RequestLogUserIdResolver {

    /**
     * 解析当前登录用户 ID。
     *
     * @return 当前用户 ID；未登录或解析失败时返回 {@code 0L}
     */
    Long resolveUserId();
}
