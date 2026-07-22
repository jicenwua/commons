package com.xcz.commons.core.security;

import jakarta.servlet.Filter;

/**
 * Servlet Security 链扩展点：在认证 Filter 前插入自定义 Filter。
 */
@FunctionalInterface
public interface SecurityChainFilter {

    Filter filter();
}
