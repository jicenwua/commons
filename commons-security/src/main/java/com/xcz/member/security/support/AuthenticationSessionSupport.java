package com.xcz.member.security.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.member.core.constant.SecurityConstants;
import com.xcz.member.core.utils.StringUtils;
import com.xcz.member.security.extend.LoginUser;
import com.xcz.member.security.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.CollectionUtils;

/**
 * Servlet / Reactive 认证过滤器共用的会话解析与权限版本刷新逻辑。
 * <p>
 * 由 {@link com.xcz.member.security.interceptor.HeaderAuthenticationFilter} 与
 * {@link com.xcz.member.security.interceptor.HeadReactAuthenticationFilter} 调用，
 * 保证两种 Web 栈下的认证行为一致，避免逻辑分叉。
 * </p>
 */
@Slf4j
public final class AuthenticationSessionSupport {

    /** 权限响应头 JSON 序列化（与过滤器解耦，避免重复创建 ObjectMapper） */
    private static final ObjectMapper PERMISSION_HEADER_JSON = new ObjectMapper();

    private AuthenticationSessionSupport() {
    }

    /**
     * 认证刷新结果。
     *
     * @param effectiveToken      当前有效 token（可能因 JWT 续签或权限刷新而与请求 token 不同）
     * @param loginUser           已加载/已更新的登录用户
     * @param permissionRefreshed 是否因角色/权限版本变更而签发了新 token
     */
    public record RefreshResult(String effectiveToken, LoginUser loginUser, boolean permissionRefreshed) {
    }

    /**
     * 解析会话并检测权限/角色版本变更。
     * <p>
     * 流程：{@link TokenService#resolveSession} → {@link TokenService#checkVersion}；
     * 正常路径 1 次 Redis GET + 1 次角色变更 EXISTS，权限刷新时复用内存中的 LoginUser。
     * </p>
     *
     * @param tokenService  token 服务
     * @param requestToken  请求头中的 token
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
     * 权限版本刷新时，将最新 permission map 与 token 写入 Servlet 响应头，供前端/小程序同步。
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
     * 权限版本刷新时，将最新 permission map 与 token 写入 Reactive 响应头，供前端/小程序同步。
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
     * 判断请求路径是否命中 {@code security.ignore.urls} 白名单（Ant 风格匹配）。
     *
     * @param path       请求路径
     * @param ignoreUrls 白名单模式列表
     * @return 命中白名单时返回 true，跳过认证
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
