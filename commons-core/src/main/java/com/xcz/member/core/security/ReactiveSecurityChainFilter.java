package com.xcz.member.core.security;

import org.springframework.web.server.WebFilter;

/**
 * Reactive Security 链扩展点：在 {@code SecurityWebFiltersOrder.AUTHENTICATION} 之前插入自定义 {@link WebFilter}。
 * <p>
 * 由业务或 Feign 等模块实现并注册为 Bean；{@code commons-security} 仅依赖本接口，不依赖具体模块。
 */
@FunctionalInterface
public interface ReactiveSecurityChainFilter {

    WebFilter filter();
}
