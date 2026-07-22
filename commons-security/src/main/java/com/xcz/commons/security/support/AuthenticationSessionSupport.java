package com.xcz.commons.security.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.CollectionUtils;

/**
 * Servlet / Reactive 认证过滤器共用的会话解析与权限刷新逻辑。
 */
@Slf4j
public final class AuthenticationSessionSupport {

    /** 权限响应头 JSON 序列化 */
    private static final ObjectMapper PERMISSION_HEADER_JSON = new ObjectMapper();

    private AuthenticationSessionSupport() {
    }

    /**
     * 认证刷新结果。
     *
     * @param effectiveToken      当前有效 token
     * @param loginUser           登录用户
     * @param permissionRefreshed 是否因权限变更签发了新 token
     */
    public record RefreshResult(String effectiveToken, LoginUser loginUser, boolean permissionRefreshed) {
    }

    /**
     * 解析会话并检测权限/角色版本变更。
     *
     * @param tokenService token 服务
     * @param requestToken 请求头中的 token
     * @return 有效 token、LoginUser 及是否发生权限刷新
     */
    public static RefreshResult authenticateAndRefresh(TokenService tokenService, String requestToken) {
        TokenService.AuthSession session = tokenService.resolveSession(requestToken);
        String currentToken = session.token();
        LoginUser loginUser = session.loginUser();

        String versioned = tokenService.checkVersion(currentToken, loginUser);
        if (versioned != null) {
            // 权限版本变更：作废旧 token 缓存，LoginUser 已在 checkVersion 内更新权限
            if (!versioned.equals(currentToken)) {
                tokenService.logout(currentToken);
            }
            loginUser.setToken(versioned);
            return new RefreshResult(versioned, loginUser, true);
        }
        return new RefreshResult(currentToken, loginUser, false);
    }

    /**
     * 权限版本刷新时，将 permission 与 token 写入 Servlet 响应头。
     */
    public static void writePermissionHeader(HttpServletResponse response, LoginUser loginUser) {
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getPermissions())) {
            return;
        }
        try {
            response.setHeader(
                    SecurityConstants.ROLE_PERMISSION,
                    PERMISSION_HEADER_JSON.writeValueAsString(loginUser.getPermissions()));
            response.setHeader(SecurityConstants.AUTHORIZATION_HEADER, loginUser.getToken());
        } catch (JsonProcessingException e) {
            log.warn("写入权限响应头失败", e);
        }
    }

    /**
     * 权限版本刷新时，将 permission 与 token 写入 Reactive 响应头。
     */
    public static void writePermissionHeader(ServerHttpResponse response, LoginUser loginUser) {
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getPermissions())) {
            return;
        }
        try {
            response.getHeaders().set(
                    SecurityConstants.ROLE_PERMISSION,
                    PERMISSION_HEADER_JSON.writeValueAsString(loginUser.getPermissions()));
            response.getHeaders().set(SecurityConstants.AUTHORIZATION_HEADER, loginUser.getToken());
        } catch (JsonProcessingException e) {
            log.warn("写入权限响应头失败", e);
        }
    }

    /**
     * 判断请求路径是否命中白名单。
     */
    public static boolean isIgnoredPath(String path, Iterable<String> ignoreUrls) {
        if (StringUtils.isEmpty(path) || ignoreUrls == null) {
            return false;
        }
        for (String pattern : ignoreUrls) {
            if (StringUtils.isMatch(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
