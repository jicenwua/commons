package com.xcz.commons.security.interceptor;

import com.xcz.commons.core.constant.Constants;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.security.config.properties.IgnoreProperties;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.service.TokenService;
import com.xcz.commons.security.support.AuthenticationSessionSupport;
import com.xcz.commons.security.support.ReleasePathCollector;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet 环境（业务微服务等）登录校验过滤器。
 * <p>
 * 在 {@code UsernamePasswordAuthenticationFilter} 之前执行（见 {@code SecurityConfig}），
 * 与 {@link HeadReactAuthenticationFilter} 共用 {@link AuthenticationSessionSupport}，
 * 保证 Reactive / Servlet 认证行为一致。
 * </p>
 * <p>
 * 单次请求处理流程：
 * </p>
 * <ol>
 *   <li>从 {@code authorization} 请求头读取 token</li>
 *   <li>{@link AuthenticationSessionSupport#authenticateAndRefresh} 解析 Redis 会话、检测权限版本</li>
 *   <li>将 {@link LoginUser} 写入 {@link SecurityContextHolder}</li>
 *   <li>权限变更时回写 {@code authorization}、{@code role_permission} 响应头</li>
 *   <li>放行后续 Filter / Controller（{@code @PreAuthorize} 等在此之后生效）</li>
 * </ol>
 * <p>
 * 经 Gateway 转发的请求通常已在网关完成 token 刷新；本过滤器作为兜底再次校验。
 * 白名单路径由 Nacos / 本地配置的 {@code security.ignore.urls} 控制。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    /** 免认证路径配置（{@code security.ignore.urls}） */
    private final IgnoreProperties ignoreProperties;

    /** {@link com.xcz.commons.security.annotation.Release} 扫描到的免认证路径 */
    private final ReleasePathCollector releasePathCollector;

    /** token 解析、续签、权限版本检测 */
    private final TokenService tokenService;

    /**
     * 将 LoginUser 写入 SecurityContext，供 {@code @PreAuthorize}、{@link SecurityUtils} 使用。
     */
    private static void setAuthentication(LoginUser loginUser) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 白名单路径跳过本过滤器（登录、验证码、Swagger 等）。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return AuthenticationSessionSupport.isIgnoredPath(
                request.getRequestURI(),
                ReleasePathCollector.mergeIgnoreUrls(ignoreProperties, releasePathCollector));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        //判断是否有token
        String requestToken = SecurityUtils.getToken(request);
        if (StringUtils.isEmpty(requestToken)) {
            handlerAuthFail(response, "请先登录");
            return;
        }

        try {
            //获取登录用户信息
            AuthenticationSessionSupport.RefreshResult result =
                    AuthenticationSessionSupport.authenticateAndRefresh(tokenService, requestToken);
            setAuthentication(result.loginUser());
            // 仅角色/权限版本变更时通知客户端；JWT 单纯续签不写响应头
            if (result.permissionRefreshed()) {
                AuthenticationSessionSupport.writePermissionHeader(response, result.loginUser());
            }
            filterChain.doFilter(request, response);
        } catch (ArithmeticException e) {
            // TokenService / JwtUtils 约定用 ArithmeticException 表示可预期的认证失败
            log.debug("认证失败: {}", e.getMessage());
            handlerAuthFail(response, e.getMessage());
        }
    }

    /**
     * 返回与全局异常处理一致的 401 JSON 响应。
     */
    private void handlerAuthFail(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(Constants.CONTENT_TYPE);
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\"}");
    }
}
