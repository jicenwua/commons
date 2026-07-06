package com.xcz.member.security.utils;

import com.xcz.member.core.constant.SecurityConstants;
import com.xcz.member.core.constant.TokenConstants;
import com.xcz.member.core.utils.StringUtils;
import com.xcz.member.security.extend.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文与请求 Token 读取工具。
 * <p>
 * {@link #getLoginUser()} 等依赖 {@link SecurityContextHolder}，适用于 Servlet 业务线程
 * （由 {@link com.xcz.member.security.interceptor.HeaderAuthenticationFilter} 写入上下文）。
 * Reactive 环境（Gateway）请在响应式链内通过 {@code ReactiveSecurityContextHolder} 获取认证信息。
 * </p>
 */
public class SecurityUtils {

    /**
     * 获取当前线程 Spring Security 认证对象。
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录用户（{@link LoginUser} 作为 Authentication 的 principal）。
     *
     * @throws ArithmeticException 未登录或 principal 为空时抛出，与认证过滤器错误语义一致
     */
    public static LoginUser getLoginUser() {
        LoginUser principal = (LoginUser) getAuthentication().getPrincipal();
        if (principal == null) {
            throw new ArithmeticException("请先登录");
        }
        return principal;
    }

    /**
     * 获取当前登录用户名。
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 获取当前登录用户 ID。
     */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * 从 Reactive 请求头 {@code authorization} 中解析 JWT token。
     * <p>
     * 支持 {@code Bearer xxx} 与裸 token 两种格式，供 Gateway 等 WebFlux 过滤器使用。
     * </p>
     */
    public static String getToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.startsWith(header, TokenConstants.PREFIX)) {
            return StringUtils.substring(header, TokenConstants.PREFIX.length());
        }
        return header;
    }

    /**
     * 从 Servlet 请求头 {@code authorization} 中解析 JWT token。
     * <p>
     * 支持 {@code Bearer xxx} 与裸 token 两种格式，供业务微服务认证过滤器使用。
     * </p>
     */
    public static String getToken(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.startsWith(token, TokenConstants.PREFIX)) {
            return StringUtils.substring(token, TokenConstants.PREFIX.length());
        }
        return token;
    }
}
