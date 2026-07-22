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
 * Servlet 环境登录校验过滤器。
 */
@Slf4j
@RequiredArgsConstructor
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    /** 免认证路径配置 */
    private final IgnoreProperties ignoreProperties;

    /** @Release 扫描到的免认证路径 */
    private final ReleasePathCollector releasePathCollector;

    /** token 解析、续签、权限版本检测 */
    private final TokenService tokenService;

    /**
     * 将 LoginUser 写入 SecurityContext。
     */
    private static void setAuthentication(LoginUser loginUser) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 白名单路径：无 token 放行；有 token 时解析并写入 SecurityContext。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean ignored = isIgnoredPath(request);
        String requestToken = SecurityUtils.getToken(request);
        if (StringUtils.isEmpty(requestToken)) {
            if (ignored) {
                filterChain.doFilter(request, response);
                return;
            }
            handlerAuthFail(response, "请先登录");
            return;
        }

        try {
            AuthenticationSessionSupport.RefreshResult result =
                    AuthenticationSessionSupport.authenticateAndRefresh(tokenService, requestToken);
            setAuthentication(result.loginUser());
            if (result.permissionRefreshed()) {
                AuthenticationSessionSupport.writePermissionHeader(response, result.loginUser());
            }
            filterChain.doFilter(request, response);
        } catch (ArithmeticException e) {
            log.debug("认证失败: {}", e.getMessage());
            if (ignored) {
                filterChain.doFilter(request, response);
                return;
            }
            handlerAuthFail(response, e.getMessage());
        }
    }

    private boolean isIgnoredPath(HttpServletRequest request) {
        return AuthenticationSessionSupport.isIgnoredPath(
                request.getRequestURI(),
                ReleasePathCollector.mergeIgnoreUrls(ignoreProperties, releasePathCollector));
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
