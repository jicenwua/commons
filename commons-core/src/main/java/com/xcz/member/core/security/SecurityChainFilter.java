package com.xcz.member.core.security;

import jakarta.servlet.Filter;

/**
 * Servlet Security 链扩展点：在 {@code UsernamePasswordAuthenticationFilter} 之前插入自定义 Filter。
 * <p>
 * Reactive 环境请使用 {@link ReactiveSecurityChainFilter}。
 * 由业务或 Feign 等模块实现并注册为 Bean；{@code commons-security} 仅依赖本接口，不依赖具体模块。
 */
@FunctionalInterface
public interface SecurityChainFilter {

    Filter filter();
}
