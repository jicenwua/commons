package com.xcz.commons.core.security;

import org.springframework.web.server.WebFilter;

/**
 * Reactive Security 链扩展点：在认证 Filter 前插入自定义 WebFilter。
 */
@FunctionalInterface
public interface ReactiveSecurityChainFilter {

    WebFilter filter();
}
