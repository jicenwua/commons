package com.xcz.commons.security.interceptor;

import com.xcz.commons.core.constant.Constants;
import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.security.config.properties.IgnoreProperties;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.service.TokenService;
import com.xcz.commons.security.support.AuthenticationSessionSupport;
import com.xcz.commons.security.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactive 环境（Gateway 等）登录校验过滤器。
 * <p>
 * 与 {@link HeaderAuthenticationFilter} 共用 {@link AuthenticationSessionSupport}，
 * 核心认证逻辑一致：加载 {@link LoginUser}、JWT 续签、权限版本刷新。
 * </p>
 * <p>
 * 网关额外职责：token 发生变更时改写转发请求的 {@code authorization} 头，
 * 使下游服务直接使用有效 token，减少重复 Redis 访问。
 * </p>
 * <p>
 * 单次请求处理流程：
 * </p>
 * <ol>
 *   <li>OPTIONS / 白名单路径直接放行</li>
 *   <li>在 boundedElastic 线程池执行 Redis 阻塞 IO（避免阻塞 Netty 事件循环）</li>
 *   <li>建立 {@link ReactiveSecurityContextHolder} 上下文</li>
 *   <li>按需改写下游请求头、回写权限响应头</li>
 *   <li>继续 Gateway 路由转发</li>
 * </ol>
 */
@Slf4j
@RequiredArgsConstructor
public class HeadReactAuthenticationFilter implements WebFilter {

    /** token 解析、续签、权限版本检测 */
    private final TokenService tokenService;

    /** 免认证路径配置（{@code security.ignore.urls}） */
    private final IgnoreProperties ignoreProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        // CORS 预检请求不含业务 token，直接放行
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }
        // 登录、验证码、Swagger 等白名单路径
        if (ignoreProperties != null
                && AuthenticationSessionSupport.isIgnoredPath(request.getURI().getPath(), ignoreProperties.getUrls())) {
            return chain.filter(exchange);
        }

        String requestToken = SecurityUtils.getToken(request);
        if (StringUtils.isEmpty(requestToken)) {
            return failHandler(exchange.getResponse());
        }

        // authenticateAndRefresh 内含 Redis GET / EXISTS 等阻塞调用
        return Mono.fromCallable(() -> AuthenticationSessionSupport.authenticateAndRefresh(tokenService, requestToken))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> continueWithAuthenticatedSession(exchange, chain, request, requestToken, result))
                .onErrorResume(ArithmeticException.class, e -> {
                    // 可预期的认证失败（token 无效、会话过期等）
                    log.debug("登录校验失败: {}", e.getMessage());
                    return failHandler(exchange.getResponse(), e.getMessage());
                })
                .onErrorResume(Exception.class, e -> {
                    // 非预期异常统一返回 401，避免泄露内部细节
                    log.error("登录校验异常", e);
                    return failHandler(exchange.getResponse(), "请先登录");
                });
    }

    /**
     * 认证成功后写入 Reactive 上下文，并按需改写 Exchange。
     *
     * @param exchange     当前请求上下文
     * @param chain        过滤器链
     * @param request      原始请求（用于 mutate 请求头）
     * @param requestToken 客户端传入的 token
     * @param result       认证刷新结果（含有效 token 与 LoginUser）
     */
    private Mono<Void> continueWithAuthenticatedSession(
            ServerWebExchange exchange,
            WebFilterChain chain,
            ServerHttpRequest request,
            String requestToken,
            AuthenticationSessionSupport.RefreshResult result) {
        LoginUser loginUser = result.loginUser();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginUser, null, loginUser.getAuthorities());

        ServerWebExchange effectiveExchange = exchange;
        // effectiveToken 变化：JWT 过期续签，或 checkVersion 签发了新 token
        if (!result.effectiveToken().equals(requestToken)) {
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(SecurityConstants.AUTHORIZATION_HEADER, loginUser.getToken())
                    .build();
            effectiveExchange = exchange.mutate().request(mutatedRequest).build();
        }

        // 权限版本变更时回写响应头，供前端/小程序同步 token 与 permission map
        if (result.permissionRefreshed()) {
            AuthenticationSessionSupport.writePermissionHeader(effectiveExchange.getResponse(), loginUser);
        }

        // contextWrite 使后续 Gateway Filter / 路由处理器可读取 ReactiveSecurityContext
        return chain.filter(effectiveExchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Mono<Void> failHandler(ServerHttpResponse response) {
        return failHandler(response, "请先登录");
    }

    /**
     * 返回 401 JSON 响应并终止请求链。
     */
    private Mono<Void> failHandler(ServerHttpResponse response, String msg) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, Constants.CONTENT_TYPE);
        String body = "{\"code\":401,\"msg\":\"" + msg + "\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}
