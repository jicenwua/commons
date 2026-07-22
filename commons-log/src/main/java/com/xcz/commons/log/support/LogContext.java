package com.xcz.commons.log.support;

/**
 * 保存请求开始时间戳（ThreadLocal）。
 */
public final class LogContext {

    /**
     * 请求开始时间（毫秒时间戳）
     */
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    private LogContext() {
    }

    /**
     * 记录当前请求的开始时间。
     */
    public static void setStartTime() {
        START_TIME.set(System.currentTimeMillis());
    }

    /**
     * 获取当前请求的开始时间。
     *
     * @return 开始时间戳；未设置时返回 {@code null}
     */
    public static Long getStartTime() {
        return START_TIME.get();
    }

    /**
     * 清理当前线程上下文
     */
    public static void clear() {
        START_TIME.remove();
    }
}
