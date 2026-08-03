package com.xcz.commons.security.utils;

import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.commons.core.constant.TokenConstants;
import com.xcz.commons.core.utils.ServletUtils;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.security.extend.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文与请求 Token 读取工具。
 */
public class SecurityUtils {

    /**
     * 获取当前线程 Spring Security 认证对象。
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录用户。
     */
    public static LoginUser getLoginUser() {
        LoginUser principal = (LoginUser) getAuthentication().getPrincipal();
        if (principal == null) {
            throw new ArithmeticException("请先登录");
        }
        return principal;
    }

    /**
     * 是否登录
     */
    public static boolean isLogin() {
        HttpServletRequest request = ServletUtils.getRequest();
        if (StringUtils.isEmpty(getToken(request))) {
            return false;
        }
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof LoginUser;
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
     * 从 Reactive 请求头解析 JWT token。
     */
    public static String getToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.startsWith(header, TokenConstants.PREFIX)) {
            return StringUtils.substring(header, TokenConstants.PREFIX.length());
        }
        return header;
    }

    /**
     * 从 Servlet 请求头解析 JWT token。
     */
    public static String getToken(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.startsWith(token, TokenConstants.PREFIX)) {
            return StringUtils.substring(token, TokenConstants.PREFIX.length());
        }
        return token;
    }
}
