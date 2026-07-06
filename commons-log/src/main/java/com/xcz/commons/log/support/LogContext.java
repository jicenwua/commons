package com.xcz.commons.log.support;

/**
 * 请求日志上下文，基于 ThreadLocal 保存单次请求的计时信息（Servlet 栈）。
 * <p>
 * 在 {@code preHandle} 记录开始时间，{@code afterCompletion} 计算耗时后清理，防止线程池复用导致数据串扰。
 */
public final class LogContext {

    /** 请求开始时间（毫秒时间戳） */
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
     * 清理当前线程的上下文，防止线程池复用导致数据串扰。
     */
    public static void clear() {
        START_TIME.remove();
    }
}
